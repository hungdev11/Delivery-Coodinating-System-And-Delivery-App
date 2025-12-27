<script setup lang="ts">
/**
 * Chat View
 *
 * Real-time chat interface for conversations with interactive proposals
 */

import { onMounted, onUnmounted, ref, computed, watch, nextTick, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { useConversations, useProposals, useTypingIndicator } from './composables'
import type {
  MessageResponse,
  ChatMessagePayload,
  ProposalTypeConfig,
  ProposalType,
  ProposalUpdateDTO,
} from './model.type'
import { getCurrentUser, getUserRoles } from '@/common/guards/roleGuard.guard'
import { useChatStore } from '@/stores/chatStore'
import { useGlobalChat } from './composables/useGlobalChat'
import ChatMessage from './components/ChatMessage.vue'
import ProposalMessage from './components/ProposalMessage.vue'
import ProposalMenu from './components/ProposalMenu.vue'
import TypingIndicator from './components/TypingIndicator.vue'
import NotificationCenter from './components/NotificationCenter.vue'
import { getParcelsV2 } from '../Parcels/api'
import type { ParcelDto } from '../Parcels/model.type'
import type { QueryPayload } from '@/common/types/filter'
import { getActiveSessionForDeliveryMan, getAssignmentsBySessionId } from '../Delivery/api'
import type { DeliveryAssignmentTask, DeliveryAssignmentTaskResponse } from '../Delivery/model.type'

// Lazy load modals
const LazyDateTimePickerModal = defineAsyncComponent(
  () => import('./components/DateTimePickerModal.vue'),
)
const LazyDateTimeRangePickerModal = defineAsyncComponent(
  () => import('./components/DateTimeRangePickerModal.vue'),
)
const LazyTextInputModal = defineAsyncComponent(() => import('./components/TextInputModal.vue'))

const route = useRoute()
const router = useRouter()
const overlay = useOverlay()

const conversationId = computed(() => route.params.conversationId as string)
const partnerId = computed(() => route.query.partnerId as string)
const partnerName = ref<string>('')
const partnerUsername = ref<string>('')
const partnerIsOnline = ref<boolean | null>(null)

const currentUser = getCurrentUser()
const currentUserId = computed(() => currentUser?.id || '')
const currentUserRoles = computed(() => getUserRoles())
const isClient = computed(() => currentUserRoles.value.includes('CLIENT'))

// Current parcel for client case
const currentParcel = ref<ParcelDto | null>(null)
const loadingParcel = ref(false)

// Active session and assignments for client-shipper chat
const activeSessionId = ref<string | null>(null)
const sessionAssignments = ref<DeliveryAssignmentTask[]>([])
const loadingSession = ref(false)

const {
  loadMessages,
  loadMoreMessages,
  loadConversations,
  hasMoreMessages,
  isLoadingMore,
  messages: conversationMessages, // Get messages ref from useConversations
} = useConversations()
const chatStore = useChatStore()
const globalChat = useGlobalChat()
// Use sendMessage and sendTyping from globalChat to ensure same WebSocket instance
const { sendMessage, sendTyping } = globalChat

// Get messages from store (for display)
const messages = computed(() => {
  if (!conversationId.value) return []
  return chatStore.getMessages(conversationId.value)
})

// Get connection status from global chat
const connected = computed(() => globalChat.connected.value)
const connecting = ref(false)
const {
  availableConfigs,
  loadAvailableConfigs,
  create: createProposal,
  respond: respondToProposal,
} = useProposals()
const { getTypingUsers, clearConversationTyping } = useTypingIndicator()

const messageInput = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const sending = ref(false)
const loadingProposals = ref(false)
const typingTimer = ref<number | null>(null)
const isTyping = ref(false)
const refreshing = ref(false)
const canLoadMoreAfterReload = ref(true) // Flag to prevent immediate loadMore after reload

// Parcels popover state
const showParcelsPopover = ref(false)

// Get typing users for this conversation
const typingUsers = computed(() => {
  if (!conversationId.value || !currentUserId.value) return []
  return getTypingUsers(conversationId.value, currentUserId.value)
})

const isPartnerTyping = computed(() => typingUsers.value.length > 0)

const selectedProposalConfig = ref<ProposalTypeConfig | null>(null)
const postponeType = ref<'SPECIFIC' | 'BEFORE' | 'AFTER' | null>(null)

/**
 * Load messages from store or server
 * Scroll to bottom FIRST, then load messages (reverse order to avoid triggering loadMore)
 */
const loadConversationMessages = async () => {
  if (!conversationId.value || !currentUserId.value) return

  // Set active conversation first
  chatStore.setActiveConversation(conversationId.value)

  // Scroll to bottom FIRST (before loading) to avoid triggering loadMore
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
  await nextTick()

  // Check if messages exist in store
  const storeMessages = chatStore.getMessages(conversationId.value)

  if (storeMessages.length === 0) {
    // Load from server if store is empty
    console.log(`📥 Loading messages from server for conversation ${conversationId.value}`)
    await loadMessages(conversationId.value, currentUserId.value)
    // Update store with loaded messages
    const { messages: loadedMessages } = useConversations()
    chatStore.setMessages(conversationId.value, loadedMessages.value)
  } else {
    // Use messages from store
    console.log(
      `📦 Using ${storeMessages.length} messages from store for conversation ${conversationId.value}`,
    )
  }

  // Ensure scroll to bottom after messages are loaded
  await nextTick()
  scrollToBottom()
}

/**
 * Watch conversationId changes to load messages from store
 */
watch(
  () => conversationId.value,
  async (newId, oldId) => {
    if (newId && newId !== oldId) {
      await loadConversationMessages()
      await loadPartnerInfo()
      await loadActiveSessionAndAssignments()
    }
  },
  { immediate: true },
)

/**
 * Load messages and setup on mount
 */
onMounted(async () => {
  if (conversationId.value && currentUserId.value) {
    await loadConversationMessages()
    await loadPartnerInfo()
    await loadAvailableProposalConfigs()
    // Load current parcel if user is CLIENT
    if (isClient.value) {
      await loadCurrentParcel()
    }

    // Load active session and assignments for both CLIENT and SHIPPER
    // CLIENT: view shipper's (partner's) parcels
    // SHIPPER: view own parcels
    await loadActiveSessionAndAssignments()

    // Ensure global chat is connected (don't create new connection)
    if (!globalChat.connected.value && currentUserId.value) {
      await globalChat.initialize()
    }

    // Listen for new messages and updates from global chat
    globalChat.addListener({
      onMessageReceived: handleMessageReceived,
      onProposalUpdate: handleProposalUpdate,
      onUpdateNotificationReceived: handleUpdateNotification,
      onUserStatusUpdate: handleUserStatusUpdate,
    })
  }
})

/**
 * Handle user status update (online/offline)
 */
const handleUserStatusUpdate = (userId: string, isOnline: boolean) => {
  // Only update if this is the current chat partner
  if (partnerId.value === userId) {
    partnerIsOnline.value = isOnline
    console.log(`📡 Partner ${userId} is now ${isOnline ? 'online' : 'offline'}`)
  }
}

/**
 * Load partner information from conversations list or store
 */
const loadPartnerInfo = async () => {
  if (!conversationId.value) return

  // First try to get from store
  const storeConversation = chatStore.getConversation(conversationId.value)
  if (storeConversation) {
    partnerName.value = storeConversation.partnerName
    partnerUsername.value = storeConversation.partnerUsername || ''
    partnerIsOnline.value = storeConversation.isOnline ?? null
    return
  }

  // If not in store, load conversations and find (with messages)
  if (currentUserId.value) {
    await loadConversations(currentUserId.value, true)
    // Update store with conversations
    const { conversations } = useConversations()
    chatStore.setConversations(conversations.value)
  }

  // Find the current conversation to get partner info
  const { conversations } = useConversations()
  const currentConv = conversations.value.find((c) => c.conversationId === conversationId.value)
  if (currentConv) {
    partnerName.value = currentConv.partnerName
    partnerUsername.value = currentConv.partnerUsername || ''
    partnerIsOnline.value = currentConv.isOnline ?? null
  } else {
    // Fallback to partnerId
    partnerName.value = partnerId.value || 'Unknown User'
    partnerIsOnline.value = null
  }
}

/**
 * Load current parcel being shipped for CLIENT user
 * Shows parcel that is ON_ROUTE and owned by current client (receiverId = currentUserId)
 */
const loadCurrentParcel = async () => {
  if (!currentUserId.value || !isClient.value) return

  loadingParcel.value = true
  try {
    // Query for parcels where receiverId = currentUserId and status = ON_ROUTE
    const query: QueryPayload = {
      page: 0,
      size: 1,
      filters: {
        logic: 'AND',
        conditions: [
          {
            field: 'receiverId',
            operator: 'eq',
            value: currentUserId.value,
            logic: 'AND',
          },
          {
            field: 'status',
            operator: 'eq',
            value: 'ON_ROUTE',
            logic: undefined,
          },
        ],
      },
      sorts: [
        {
          field: 'updatedAt',
          direction: 'desc',
        },
      ],
    }

    const response = await getParcelsV2(query)
    if (response.result?.data && response.result.data.length > 0) {
      currentParcel.value = response.result.data[0]
    } else {
      currentParcel.value = null
    }
  } catch (error) {
    console.error('Failed to load current parcel:', error)
    currentParcel.value = null
  } finally {
    loadingParcel.value = false
  }
}

/**
 * Load active session and assignments for shipper (partner)
 * Loads ALL assignments in the shipper's active session (not filtered by receiverId)
 * Works for both CLIENT (viewing shipper's parcels) and SHIPPER (viewing own parcels)
 */
const loadActiveSessionAndAssignments = async () => {
  // Determine which shipper's session to load
  // If user is CLIENT, load partner's (shipper's) session
  // If user is SHIPPER, load their own session
  const shipperId = isClient.value ? partnerId.value : currentUserId.value

  if (!shipperId) return

  loadingSession.value = true
  try {
    // Get active session for the shipper
    const sessionResponse = await getActiveSessionForDeliveryMan(shipperId)

    if (sessionResponse.result && sessionResponse.result.id) {
      activeSessionId.value = sessionResponse.result.id

      // Get all assignments in this session (no filtering - show all parcels)
      const assignmentsResponse = await getAssignmentsBySessionId(activeSessionId.value, {
        page: 0,
        size: 100,
      })
      console.log('📦 assignmentsResponse:', assignmentsResponse)
      
      // Handle both direct response and wrapped response formats
      let assignments: DeliveryAssignmentTask[] = []
      if (assignmentsResponse) {
        // Check if response is wrapped in 'result' (IApiResponse format)
        if ('result' in assignmentsResponse && assignmentsResponse.result) {
          const unwrapped = assignmentsResponse.result as DeliveryAssignmentTaskResponse
          assignments = unwrapped.content || []
          console.log('📦 Unwrapped from result:', assignments.length, 'assignments')
        } 
        // Check if response has 'content' directly (DeliveryAssignmentTaskResponse format)
        else if ('content' in assignmentsResponse) {
          assignments = assignmentsResponse.content || []
          console.log('📦 Using content directly:', assignments.length, 'assignments')
        }
        // Fallback: check if response is an array directly
        else if (Array.isArray(assignmentsResponse)) {
          assignments = assignmentsResponse
          console.log('📦 Response is array:', assignments.length, 'assignments')
        } else {
          console.warn('⚠️ Unexpected response format:', assignmentsResponse)
        }
      } else {
        console.warn('⚠️ assignmentsResponse is null or undefined')
      }
      
        // Show ALL assignments in the session (not filtered by receiverId)
      sessionAssignments.value = assignments
        console.log(
          '📦 Loaded',
          sessionAssignments.value.length,
          'parcels in active session for shipper:',
          shipperId,
        )
    } else {
      activeSessionId.value = null
      sessionAssignments.value = []
      console.log('ℹ️ No active session found for shipper:', shipperId)
    }
  } catch (error) {
    console.error('Failed to load active session and assignments:', error)
    activeSessionId.value = null
    sessionAssignments.value = []
  } finally {
    loadingSession.value = false
  }
}

/**
 * Cleanup on unmount
 */
onUnmounted(() => {
  // Don't disconnect WebSocket (global connection)
  // Just clear typing indicator
  if (conversationId.value) {
    clearConversationTyping(conversationId.value)
  }
  if (typingTimer.value) {
    clearTimeout(typingTimer.value)
  }
  // Clear active conversation
  chatStore.setActiveConversation(null)
})

/**
 * Load available proposal configs for current user roles
 * If user is ADMIN, also check for undelivered parcels and add USER role if needed
 */
const loadAvailableProposalConfigs = async () => {
  console.log('🔍 Loading proposal configs for roles:', currentUserRoles.value)
  if (currentUserRoles.value.length > 0) {
    loadingProposals.value = true
    // Pass userId so ADMIN users can get USER role configs if they have undelivered parcels
    await loadAvailableConfigs(currentUserRoles.value, currentUserId.value)
    console.log('📋 Available proposal configs loaded:', availableConfigs.value.length, 'configs')
    loadingProposals.value = false
  } else {
    console.warn('⚠️ No roles found for current user, cannot load proposal configs')
  }
}

/**
 * Handle message received from global chat
 * (Messages are already added to store by globalChat, we just need to handle UI updates)
 */
const handleMessageReceived = async (message: MessageResponse) => {
  // Message is already in store via globalChat
  // Just handle UI-specific updates for current conversation
  if (message.conversationId !== conversationId.value) return

        // If this is a DELIVERY_COMPLETED message, refresh session assignments and update parcel status
        if (message.type === 'DELIVERY_COMPLETED') {
          console.log('📦 DELIVERY_COMPLETED message received, refreshing session assignments...')

          // Parse message content to get parcelId
          try {
            const messageData =
              typeof message.content === 'string' ? JSON.parse(message.content) : message.content
            const parcelId = messageData?.parcelId || messageData?.parcelCode

            if (parcelId) {
              // Update assignment status in local list immediately (optimistic update)
              const assignmentIndex = sessionAssignments.value.findIndex(
                (a) => a.parcelId === parcelId || a.parcelCode === parcelId,
              )
              if (assignmentIndex !== -1) {
                sessionAssignments.value[assignmentIndex].status = 'COMPLETED'
                if (messageData?.completedAt) {
                  // Update completedAt if available
                  sessionAssignments.value[assignmentIndex].completedAt = messageData.completedAt
                }
                console.log('✅ Updated assignment status to COMPLETED in local list:', parcelId)
              }
            }
          } catch (e) {
            console.warn('Failed to parse DELIVERY_COMPLETED message content:', e)
          }

          // Refresh from server to ensure consistency
          await loadActiveSessionAndAssignments()
        }

        // If this is a postpone message (TEXT with postpone data), refresh session assignments
        if (message.type === 'TEXT' && message.content) {
          try {
            const messageData =
              typeof message.content === 'string' ? JSON.parse(message.content) : message.content
            if (messageData?.postponeDateTime || (messageData?.parcelId && messageData?.reason)) {
              console.log('⏸️ Postpone message received, refreshing session assignments...')

              const parcelId = messageData?.parcelId
              if (parcelId) {
                // Update assignment status in local list (remove from active session if postponed)
                const assignmentIndex = sessionAssignments.value.findIndex(
                  (a) => a.parcelId === parcelId || a.parcelCode === parcelId,
                )
                if (assignmentIndex !== -1) {
                  // Remove from active session list (parcel is postponed, no longer in active session)
                  sessionAssignments.value.splice(assignmentIndex, 1)
                  console.log('✅ Removed postponed parcel from active session list:', parcelId)
                }
              }

              // Refresh from server to ensure consistency
              await loadActiveSessionAndAssignments()
            }
    } catch {
            // Not a JSON message, ignore
          }
        }

  // Scroll to bottom when new message arrives in active conversation
  await nextTick()
  scrollToBottom()
}

/**
 * Handle update notification (session completed, parcel updated, etc.)
 */
interface UpdateNotification {
  entityType?: string
  action?: string
  entityId?: string
  message?: string
}

const handleUpdateNotification = (updateNotification: UpdateNotification) => {
  if (!updateNotification) return

  const entityType = updateNotification.entityType
  const action = updateNotification.action
  const entityId = updateNotification.entityId

  console.log('🔄 Update notification received in ChatView:', {
    entityType,
    action,
    entityId,
    message: updateNotification.message,
  })

  // Handle SESSION_UPDATE: refresh session assignments if session completed
  if (entityType === 'SESSION' && action === 'COMPLETED') {
    console.log('📦 Session completed, refreshing session assignments...')
    // Refresh session assignments to clear in-progress parcels
    loadActiveSessionAndAssignments()
  }

  // Handle PARCEL_UPDATE: refresh session assignments if parcel status changed
  if (entityType === 'PARCEL' && (action === 'STATUS_CHANGED' || action === 'UPDATED')) {
    console.log('📦 Parcel updated, refreshing session assignments...')
    // Refresh session assignments to reflect parcel status changes
    loadActiveSessionAndAssignments()
  }

  // Handle TICKET_UPDATE: log ticket notifications (tickets are related to assignments)
  if (entityType === 'ASSIGNMENT' && updateNotification.data && (updateNotification.data as any).ticketId) {
    const ticketData = updateNotification.data as any
    console.log('🎫 Ticket update notification received:', {
      ticketId: ticketData.ticketId,
      ticketType: ticketData.ticketType,
      ticketStatus: ticketData.ticketStatus,
      action,
    })
    // Tickets are related to assignments, so proposals will be updated via WebSocket proposal-updates
  }
}

/**
 * Handle input change - send typing indicator
 */
const handleInputChange = () => {
  if (!conversationId.value || !connected.value) return

  // Send typing indicator
  if (!isTyping.value) {
    isTyping.value = true
    sendTyping(conversationId.value, true)
  }

  // Reset typing timer
  if (typingTimer.value) {
    clearTimeout(typingTimer.value)
  }

  // Stop typing after 3 seconds of inactivity
  typingTimer.value = window.setTimeout(() => {
    if (isTyping.value) {
      isTyping.value = false
      sendTyping(conversationId.value!, false)
    }
  }, 3000)
}

/**
 * Send message
 */
const handleSendMessage = async () => {
  if (!messageInput.value.trim() || sending.value || !partnerId.value) return

  sending.value = true
  const content = messageInput.value.trim()

  // Stop typing indicator
  if (isTyping.value && conversationId.value) {
    isTyping.value = false
    sendTyping(conversationId.value, false)
  }
  if (typingTimer.value) {
    clearTimeout(typingTimer.value)
  }

  // Create optimistic message for immediate display
  const optimisticMessage: MessageResponse = {
    id: `temp-${Date.now()}`,
    conversationId: conversationId.value, // Include conversationId
    senderId: currentUserId.value,
    content,
    sentAt: new Date().toISOString(),
    type: 'TEXT',
    status: 'SENT', // Initial status
  }

  // Add message to store immediately for optimistic UI update
  chatStore.addMessage(conversationId.value, optimisticMessage)
  messageInput.value = ''
  await nextTick()
  scrollToBottom()

  const payload: ChatMessagePayload = {
    content,
    recipientId: partnerId.value,
    conversationId: conversationId.value,
  }

  const success = sendMessage(payload)

  if (!success) {
    // Remove optimistic message if send failed
    const storeMessages = chatStore.getMessages(conversationId.value)
    const index = storeMessages.findIndex((m) => m.id === optimisticMessage.id)
    if (index !== -1) {
      const updatedMessages = [...storeMessages]
      updatedMessages.splice(index, 1)
      chatStore.setMessages(conversationId.value, updatedMessages)
    }
  } else {
    // Strategy: Wait for WebSocket to deliver the real message
    // The backend sends the saved message via WebSocket to the recipient
    // For the sender, we keep the optimistic message until we receive confirmation
    // or reload after a delay if WebSocket doesn't deliver

    // Set a timeout to reload if WebSocket doesn't deliver the message
    // This handles cases where WebSocket might be slow or fail
    setTimeout(async () => {
      // Check if optimistic message still exists (real message didn't arrive via WebSocket)
      const storeMessages = chatStore.getMessages(conversationId.value)
      const stillHasOptimistic = storeMessages.some((m) => m.id === optimisticMessage.id)
      if (stillHasOptimistic && conversationId.value) {
        console.log('Optimistic message still exists, reloading from server')
        await loadMessages(conversationId.value, currentUserId.value)
        const { messages: loadedMessages } = useConversations()
        chatStore.setMessages(conversationId.value, loadedMessages.value)
        await nextTick()
        scrollToBottom()
      }
    }, 2000) // Wait 2 seconds for WebSocket delivery

    // Reload conversations list to update lastMessageTime (without messages to avoid heavy load)
    if (currentUserId.value) {
      await loadConversations(currentUserId.value, false)
    }
  }

  sending.value = false
}

const showPostponeOptionsModal = ref(false)

/**
 * Handle proposal selection
 */
const handleProposalSelect = async (config: ProposalTypeConfig) => {
  selectedProposalConfig.value = config

  // Use creationActionType for sender's UI when creating proposal
  const actionType = config.creationActionType
  if (actionType === 'DATE_PICKER') {
    showPostponeOptionsModal.value = true
  } else if (actionType === 'TEXT_INPUT') {
    await showTextInputModal()
  } else {
    // ACCEPT_DECLINE or default
    await sendProposalRequest(config.type, '{}')
  }
}

/**
 * Handle postpone option selection
 */
const handlePostponeOption = async (type: 'SPECIFIC' | 'BEFORE' | 'AFTER' | 'RANGE') => {
  showPostponeOptionsModal.value = false
  postponeType.value = type === 'RANGE' ? null : type

  if (type === 'RANGE') {
    await showDateTimeRangePickerModal()
  } else {
    await showDateTimePickerModal()
  }
}

/**
 * Show date/time picker modal
 */
const showDateTimePickerModal = async () => {
  if (!selectedProposalConfig.value || !postponeType.value) return

  const modal = overlay.create(LazyDateTimePickerModal)
  const instance = modal.open({ postponeType: postponeType.value })
  const result = await instance.result

  if (result && result.date && result.time) {
    await handleDateTimePickerConfirm(result.date, result.time)
  }
  selectedProposalConfig.value = null
  postponeType.value = null
}

/**
 * Show date/time range picker modal
 */
const showDateTimeRangePickerModal = async () => {
  if (!selectedProposalConfig.value) return

  const modal = overlay.create(LazyDateTimeRangePickerModal)
  const instance = modal.open({})
  const result = await instance.result

  if (result && result.startDate && result.startTime && result.endDate && result.endTime) {
    await handleDateTimeRangePickerConfirm(
      result.startDate,
      result.startTime,
      result.endDate,
      result.endTime,
    )
  }
  selectedProposalConfig.value = null
}

/**
 * Show text input modal
 */
const showTextInputModal = async () => {
  if (!selectedProposalConfig.value) return

  const modal = overlay.create(LazyTextInputModal)
  const instance = modal.open({
    title: selectedProposalConfig.value.description || 'Enter Reason',
  })
  const result = await instance.result

  if (result && result.text) {
    await handleTextInputConfirm(result.text)
  }
  selectedProposalConfig.value = null
}

/**
 * Handle date/time picker confirm
 */
const handleDateTimePickerConfirm = async (date: string, time: string) => {
  if (!selectedProposalConfig.value || !postponeType.value) return

  const [hours, minutes] = time.split(':')
  const dateTime = new Date(date)
  dateTime.setHours(parseInt(hours), parseInt(minutes))

  const year = dateTime.getFullYear()
  const month = String(dateTime.getMonth() + 1).padStart(2, '0')
  const day = String(dateTime.getDate()).padStart(2, '0')
  const hour = String(dateTime.getHours()).padStart(2, '0')
  const minute = String(dateTime.getMinutes()).padStart(2, '0')

  const resultData = `${year}-${month}-${day}T${hour}:${minute}:00`
  let dataJson = '{}'

  if (postponeType.value === 'SPECIFIC') {
    dataJson = JSON.stringify({ specific_datetime: resultData })
  } else if (postponeType.value === 'AFTER') {
    dataJson = JSON.stringify({ after_datetime: resultData })
  } else {
    dataJson = JSON.stringify({ before_datetime: resultData })
  }

  await sendProposalRequest(selectedProposalConfig.value.type, dataJson)
  selectedProposalConfig.value = null
  postponeType.value = null
}

/**
 * Handle date/time range picker confirm
 */
const handleDateTimeRangePickerConfirm = async (
  startDate: string,
  startTime: string,
  endDate: string,
  endTime: string,
) => {
  if (!selectedProposalConfig.value) return

  const [startHours, startMinutes] = startTime.split(':')
  const [endHours, endMinutes] = endTime.split(':')

  const startDateTime = new Date(startDate)
  startDateTime.setHours(parseInt(startHours), parseInt(startMinutes))

  const endDateTime = new Date(endDate)
  endDateTime.setHours(parseInt(endHours), parseInt(endMinutes))

  if (endDateTime <= startDateTime) {
    alert('End time must be after start time')
    return
  }

  const formatDateTime = (date: Date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hour = String(date.getHours()).padStart(2, '0')
    const minute = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day}T${hour}:${minute}:00`
  }

  const startTimeStr = formatDateTime(startDateTime)
  const endTimeStr = formatDateTime(endDateTime)

  const dataJson = JSON.stringify({
    start_datetime: startTimeStr,
    end_datetime: endTimeStr,
  })

  await sendProposalRequest(selectedProposalConfig.value.type, dataJson)
  selectedProposalConfig.value = null
}

/**
 * Handle text input confirm
 */
const handleTextInputConfirm = async (text: string) => {
  if (!selectedProposalConfig.value) return

  const dataJson = JSON.stringify({ reason: text })
  await sendProposalRequest(selectedProposalConfig.value.type, dataJson)
  selectedProposalConfig.value = null
}

/**
 * Send proposal request
 */
const sendProposalRequest = async (type: string, data: string) => {
  if (!conversationId.value || !partnerId.value || !currentUserId.value) {
    console.warn('⚠️ Missing required fields for proposal:', {
      conversationId: conversationId.value,
      partnerId: partnerId.value,
      currentUserId: currentUserId.value,
    })
    return
  }

  // Find config to get description for fallbackContent
  const config = availableConfigs.value.find((c) => c.type === type)
  const fallbackContent = config?.description || `Proposal: ${type}`

  // For POSTPONE_REQUEST and CONFIRM_REFUSAL, include parcelId in data
  let proposalData = data
  if ((type === 'POSTPONE_REQUEST' || type === 'CONFIRM_REFUSAL') && currentParcel.value?.id) {
    try {
      const dataObj = JSON.parse(data)
      dataObj.parcelId = currentParcel.value.id
      proposalData = JSON.stringify(dataObj)
    } catch (e) {
      console.warn('⚠️ Failed to parse proposal data, using original data:', e)
      // Fallback: create new object with parcelId
      proposalData = JSON.stringify({
        parcelId: currentParcel.value.id,
        ...JSON.parse(data || '{}'),
      })
    }
  }

  // Include sessionId if available (for client-shipper proposals)
  const result = await createProposal({
    conversationId: conversationId.value,
    recipientId: partnerId.value,
    type: type as ProposalType,
    data: proposalData,
    fallbackContent,
    senderId: currentUserId.value,
    senderRoles: currentUserRoles.value,
    sessionId: activeSessionId.value || undefined, // Include sessionId if available
  })

  if (result && conversationId.value) {
    // Reload messages to show the new proposal
    await loadMessages(conversationId.value, currentUserId.value)
    const { messages: loadedMessages } = useConversations()
    chatStore.setMessages(conversationId.value, loadedMessages.value)
  }
}

/**
 * Handle proposal response
 */
const handleProposalResponse = async (proposalId: string, resultData: string) => {
  if (!currentUserId.value) return

  const result = await respondToProposal(proposalId, currentUserId.value, {
    resultData,
  })

  if (result && conversationId.value) {
    // Reload messages to show updated proposal status
    await loadMessages(conversationId.value, currentUserId.value)
    const { messages: loadedMessages } = useConversations()
    chatStore.setMessages(conversationId.value, loadedMessages.value)
  }
}

/**
 * Handle proposal update from WebSocket
 */
const handleProposalUpdate = (update: ProposalUpdateDTO) => {
  console.log('📋 Proposal update received:', update)

  if (!conversationId.value) return

  // Find message with matching proposal ID and update its status in store
  const storeMessages = chatStore.getMessages(conversationId.value)
  const messageIndex = storeMessages.findIndex((msg) => msg.proposal?.id === update.proposalId)

  if (messageIndex !== -1) {
    const message = storeMessages[messageIndex]
    if (message.proposal) {
      // Update proposal status
      const updatedMessage = {
        ...message,
        proposal: {
          ...message.proposal,
          status: update.newStatus,
          resultData: update.resultData || message.proposal.resultData,
        },
      }

      // Update in store
      chatStore.updateMessage(conversationId.value, message.id, updatedMessage)

      console.log('✅ Updated proposal status:', {
        proposalId: update.proposalId,
        newStatus: update.newStatus,
        messageIndex,
      })
    }
  } else {
    console.warn('⚠️ Proposal not found in messages:', update.proposalId)
  }
}

/**
 * Scroll to bottom of messages
 */
const scrollToBottom = () => {
  if (messagesContainer.value) {
    // Use requestAnimationFrame to ensure DOM is updated
    requestAnimationFrame(() => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      }
    })
  }
}

/**
 * Reload chat history from server (pull-to-refresh)
 * This replaces the messages in store for THIS conversation only
 */
const reloadChatHistory = async () => {
  if (!conversationId.value || !currentUserId.value || refreshing.value) return

  refreshing.value = true
  canLoadMoreAfterReload.value = false // Prevent loadMore immediately after reload
  console.log('🔄 Reloading chat history from server for conversation:', conversationId.value)

  try {
    // Scroll to bottom FIRST (before loading) to avoid triggering loadMore
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
    await nextTick()

    // Load messages from server (page 0, first page)
    // This will update the conversationMessages ref in useConversations
    await loadMessages(conversationId.value, currentUserId.value, 0, 30)

    // Get the loaded messages from useConversations
    // Messages are already sorted oldest first (reversed in loadMessages)
    const loadedMessages = conversationMessages.value || []

    // Replace messages in store (this will trigger UI update via computed messages)
    // Use nextTick to ensure DOM is updated before scrolling
    await nextTick()

    if (loadedMessages.length > 0) {
      chatStore.setMessages(conversationId.value, loadedMessages)
      console.log('✅ Reloaded chat history:', loadedMessages.length, 'messages')
    } else {
      // If no messages, clear the store for this conversation
      chatStore.setMessages(conversationId.value, [])
      console.log('✅ Reloaded chat history: 0 messages (cleared)')
    }

    // Wait for DOM to update with new messages
    await nextTick()

    // Scroll to bottom after reload (always scroll to bottom for refresh)
    scrollToBottom()

    // Delay 1 second before allowing loadMore again (prevent immediate trigger)
    setTimeout(() => {
      canLoadMoreAfterReload.value = true
      console.log('✅ Can load more messages after reload delay')
    }, 1000)
  } catch (error) {
    console.error('❌ Failed to reload chat history:', error)
    canLoadMoreAfterReload.value = true // Re-enable on error
  } finally {
    refreshing.value = false
  }
}

/**
 * Handle scroll event for infinite scroll
 */
const handleScroll = () => {
  if (!messagesContainer.value || !conversationId.value || !currentUserId.value) return
  if (!canLoadMoreAfterReload.value) return // Prevent loadMore immediately after reload

  const scrollTop = messagesContainer.value.scrollTop
  const scrollHeight = messagesContainer.value.scrollHeight
  const clientHeight = messagesContainer.value.clientHeight

  // Check if can scroll (has overflow)
  const canScroll = scrollHeight > clientHeight
  const isAtTop = scrollTop < 100

  // Load more when:
  // 1. Scrolled to top (within 100px) - reached oldest messages
  // 2. OR not enough messages to overflow - need more messages to fill screen
  const shouldLoadMore =
    (isAtTop || (!canScroll && messages.value.length < 30)) &&
    !isLoadingMore.value &&
    hasMoreMessages.value

  if (shouldLoadMore) {
    console.log('📜 Loading more messages...', {
      isAtTop,
      canScroll,
      messageCount: messages.value.length,
    })

    // Save current scroll height
    const oldScrollHeight = scrollHeight

    loadMoreMessages(conversationId.value, currentUserId.value).then((newMessages) => {
      // newMessages are the newly loaded older messages (oldest first, already reversed)
      // Merge them into store (prepend at beginning)
      if (newMessages && newMessages.length > 0) {
        chatStore.prependMessages(conversationId.value, newMessages)
        console.log(
          `📦 Merged ${newMessages.length} new messages into store (loadMore), store now has ${chatStore.getMessages(conversationId.value).length} messages`,
        )
      } else {
        console.log('📦 No new messages to merge')
      }

      // Restore scroll position after loading
      nextTick(() => {
        if (messagesContainer.value) {
          const newScrollHeight = messagesContainer.value.scrollHeight
          messagesContainer.value.scrollTop = newScrollHeight - oldScrollHeight
        }
      })
    })
  }
}

/**
 * Watch for new messages and scroll to bottom
 */
watch(
  () => messages.value.length,
  () => {
    // Only scroll if we're at or near the bottom (within 100px)
    if (messagesContainer.value) {
      const isNearBottom =
        messagesContainer.value.scrollHeight -
          messagesContainer.value.scrollTop -
          messagesContainer.value.clientHeight <
        100

      if (isNearBottom) {
        nextTick(() => scrollToBottom())
      }
    }
  },
)

/**
 * Watch conversationId to scroll to bottom when switching conversations
 */
watch(
  () => conversationId.value,
  () => {
    nextTick(() => scrollToBottom())
  },
)

/**
 * Check if message is from current user
 */
const isMyMessage = (message: MessageResponse) => {
  return message.senderId === currentUserId.value
}

/**
 * Handle delivery confirmation
 */
const handleDeliveryConfirm = async (parcelId: string, messageId: string, note?: string) => {
  if (!parcelId || !messageId) return

  console.log('📦 Confirming delivery for parcel:', parcelId)

  const toast = useToast()

  try {
    const { confirmParcelReceived } = await import('../Parcels/api')
    const response = await confirmParcelReceived(parcelId, {
      confirmationSource: 'CHAT',
      note: note || undefined,
    })

    if (response.result) {
      console.log('✅ Delivery confirmed successfully')

      // Update message content to include confirmedAt
      const storeMessages = chatStore.getMessages(conversationId.value)
      const messageIndex = storeMessages.findIndex((m) => m.id === messageId)

      if (messageIndex !== -1) {
        const message = storeMessages[messageIndex]
        try {
          // Parse existing content
          let messageData: Record<string, unknown> = {}
          if (message.content) {
            messageData =
              typeof message.content === 'string'
                ? JSON.parse(message.content)
                : message.content
          }

          // Add confirmedAt timestamp
          messageData.confirmedAt = new Date().toISOString()

          // Update message in store
          const updatedMessage = {
            ...message,
            content: JSON.stringify(messageData),
          }

          chatStore.updateMessage(conversationId.value, messageId, updatedMessage)
          console.log('✅ Updated message with confirmedAt')
        } catch (e) {
          console.error('Error updating message content:', e)
        }
      }

      // Show success toast
      toast.add({
        title: 'Thành công',
        description: 'Đã xác nhận nhận hàng thành công',
        color: 'success',
      })
    }
  } catch (error) {
    console.error('❌ Failed to confirm delivery:', error)
    toast.add({
      title: 'Lỗi',
      description: 'Không thể xác nhận nhận hàng. Vui lòng thử lại.',
      color: 'error',
    })
  }
}
</script>

<template>
  <div class="flex flex-col h-full">
    <!-- Chat Header -->
    <div
      class="p-4 border-b border-neutral-200 dark:border-neutral-700 bg-background flex items-center justify-between flex-shrink-0"
    >
      <div class="flex items-center space-x-3">
        <UButton
          icon="i-heroicons-arrow-left"
          variant="ghost"
          @click="router.push({ name: 'communication-conversations' })"
        />
        <div
          class="w-10 h-10 rounded-full bg-neutral-200 dark:bg-neutral-800 flex items-center justify-center text-foreground font-semibold"
        >
          {{ (partnerName || '?').charAt(0).toUpperCase() }}
        </div>
        <div>
          <div class="flex items-center gap-2">
            <p class="font-semibold text-foreground">
              {{ partnerName || 'Chat' }}
            </p>
            <!-- Online status indicator -->
            <span
              v-if="partnerIsOnline !== null"
              class="w-2 h-2 rounded-full"
              :class="partnerIsOnline ? 'bg-success-500' : 'bg-neutral-400'"
              :title="partnerIsOnline ? 'Online' : 'Offline'"
            />
          </div>
          <p v-if="partnerUsername" class="text-sm text-muted-foreground">
            @{{ partnerUsername }}
          </p>
          <p v-else-if="!partnerName && partnerId" class="text-sm text-muted-foreground">
            {{ partnerId }}
          </p>
          <p v-else class="text-xs text-muted-foreground">
            {{ partnerIsOnline === true ? 'Online' : partnerIsOnline === false ? 'Offline' : '' }}
          </p>
        </div>
      </div>
      <div class="flex items-center space-x-2">
        <!-- Parcels Popover (moved to toolbar) -->
        <UPopover v-model:open="showParcelsPopover" :content="{ side: 'bottom', align: 'end' }">
          <UButton variant="ghost" color="neutral" icon="i-heroicons-cube" class="relative">
            <span v-if="sessionAssignments.length > 0" class="ml-1">{{
              sessionAssignments.length
            }}</span>
            <!-- Red dot indicator when parcels exist -->
            <span
              v-if="sessionAssignments.length > 0"
              class="absolute top-0 right-0 w-2 h-2 bg-error-500 rounded-full border-2 border-background"
            />
          </UButton>
          <template #content>
            <div class="p-4 min-w-80 max-w-md max-h-96 overflow-y-auto">
              <div class="mb-3">
                <h3 class="font-semibold text-foreground">
                  Parcels in Active Session
                </h3>
                <p
                  v-if="sessionAssignments.length > 0"
                  class="text-sm text-muted-foreground"
                >
                  {{ sessionAssignments.length }} parcel(s) being delivered
                </p>
              </div>

              <USkeleton v-if="loadingSession" class="h-32 w-full" />

              <div
                v-else-if="sessionAssignments.length === 0"
                class="text-center py-8 text-muted-foreground"
              >
                <p>Không có đơn hàng trong phiên hiện tại.</p>
              </div>

              <div v-else class="space-y-3">
                <div
                  v-for="assignment in sessionAssignments"
                  :key="assignment.parcelId"
                  class="p-3 border border-neutral-200 dark:border-neutral-700 rounded-lg hover:bg-neutral-50 dark:hover:bg-neutral-800 transition-colors"
                >
                  <div class="flex items-start justify-between">
                    <div class="flex-1">
                      <div class="flex items-center space-x-2 mb-2">
                        <span class="font-semibold text-foreground">
                          {{ assignment.parcelCode || assignment.parcelId }}
                        </span>
                        <UBadge
                          :color="
                            assignment.status === 'COMPLETED'
                              ? 'success'
                              : assignment.status === 'FAILED'
                                ? 'error'
                                : 'warning'
                          "
                          variant="soft"
                          class="capitalize"
                        >
                          {{ assignment.status.toLowerCase() }}
                        </UBadge>
                      </div>
                      <div class="text-sm text-muted-foreground space-y-1">
                        <p v-if="assignment.deliveryType">
                          Type: <strong>{{ assignment.deliveryType }}</strong>
                        </p>
                        <p v-if="assignment.deliveryLocation">
                          Location: <strong>{{ assignment.deliveryLocation }}</strong>
                        </p>
                        <p v-if="assignment.value">
                          Value: <strong>{{ assignment.value.toLocaleString() }} VND</strong>
                        </p>
                        <p v-if="assignment.weight">
                          Weight: <strong>{{ assignment.weight }} kg</strong>
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </UPopover>

        <!-- Notification Center -->
        <NotificationCenter />

        <UBadge
          :color="connected ? 'success' : 'neutral'"
          variant="subtle"
          :label="connected ? 'Connected' : connecting ? 'Connecting...' : 'Disconnected'"
        />

        <!-- Refresh Button (Pull-to-refresh) -->
        <UButton
          variant="ghost"
          color="neutral"
          icon="i-heroicons-arrow-path"
          :loading="refreshing"
          :disabled="refreshing || !conversationId"
          size="sm"
          class="md:size-md"
          @click="reloadChatHistory"
        />
      </div>
    </div>

    <!-- Messages Container -->
    <div
      ref="messagesContainer"
      class="flex-1 overflow-y-auto p-4 space-y-4 bg-neutral-50 dark:bg-neutral-900/50"
      style="scroll-behavior: smooth"
      @scroll="handleScroll"
    >
      <!-- Loading More Indicator (at top) -->
      <div v-if="isLoadingMore" class="flex justify-center py-4">
        <div class="flex items-center space-x-2 text-muted-foreground">
          <UIcon name="i-heroicons-arrow-path" class="animate-spin" />
          <span class="text-sm">Loading more messages...</span>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="messages.length === 0" class="flex items-center justify-center h-full">
        <div class="text-center text-muted-foreground">
          <p>No messages yet. Start the conversation!</p>
        </div>
      </div>

      <!-- Messages -->
      <div
        v-for="message in messages"
        :key="message.id"
        class="flex"
        :class="{ 'justify-end': isMyMessage(message), 'justify-start': !isMyMessage(message) }"
      >
        <!-- Delivery Completed Message -->
        <ChatMessage
          v-if="message.type === 'DELIVERY_COMPLETED'"
          :message="message"
          :is-my-message="isMyMessage(message)"
          :current-user-id="currentUserId"
          @confirm-delivery="handleDeliveryConfirm"
        />

        <!-- Delivery Succeeded Message -->
        <ChatMessage
          v-if="message.type === 'DELIVERY_SUCCEEDED'"
          :message="message"
          :is-my-message="isMyMessage(message)"
          :current-user-id="currentUserId"
        />

        <!-- Text Message -->
        <ChatMessage
          v-else-if="message.type === 'TEXT'"
          :message="message"
          :is-my-message="isMyMessage(message)"
        />

        <!-- Proposal Message -->
        <ProposalMessage
          v-else-if="message.type === 'INTERACTIVE_PROPOSAL' && message.proposal"
          :proposal="message.proposal"
          :content="message.content"
          :sent-at="message.sentAt"
          :current-user-id="currentUserId"
          @respond="handleProposalResponse"
        />
      </div>

      <!-- Typing Indicator -->
      <TypingIndicator
        v-if="isPartnerTyping"
        :show="isPartnerTyping"
        :user-name="partnerName || 'Partner'"
      />
    </div>

    <!-- Message Input -->
    <div class="p-4 border-t border-neutral-200 dark:border-neutral-700 bg-background">
      <div class="flex items-center space-x-2">
        <!-- Proposal Menu -->
        <ProposalMenu
          :available-configs="availableConfigs"
          :loading="loadingProposals"
          @select="handleProposalSelect"
        />

        <UInput
          v-model="messageInput"
          class="flex-1"
          placeholder="Type a message..."
          :disabled="!connected || sending"
          @input="handleInputChange"
          @keyup.enter="handleSendMessage"
        />
        <UButton
          icon="i-heroicons-paper-airplane"
          :disabled="!messageInput.trim() || !connected || sending"
          @click="handleSendMessage"
        >
          Send
        </UButton>
      </div>
    </div>

    <!-- Postpone Options Modal -->
    <UModal v-model:open="showPostponeOptionsModal" title="Select Postpone Type">
      <template #body>
        <div class="space-y-2 p-4">
          <UButton block variant="ghost" @click="handlePostponeOption('SPECIFIC')">
            At a specific time
          </UButton>
          <UButton block variant="ghost" @click="handlePostponeOption('BEFORE')">
            Before a time
          </UButton>
          <UButton block variant="ghost" @click="handlePostponeOption('AFTER')">
            After a time
          </UButton>
          <UButton block variant="ghost" @click="handlePostponeOption('RANGE')">
            Within a time range
          </UButton>
        </div>
      </template>
    </UModal>
  </div>
</template>
