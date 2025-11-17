<script setup lang="ts">
/**
 * Chat View
 *
 * Real-time chat interface for conversations with interactive proposals
 */

import { onMounted, onUnmounted, ref, computed, watch, nextTick, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import {
  useConversations,
  useWebSocket,
  useProposals,
  useMessageStatus,
  useTypingIndicator,
  useNotifications
} from './composables'
import type {
  MessageResponse,
  ChatMessagePayload,
  ProposalTypeConfig,
  ProposalType,
  ProposalUpdateDTO,
} from './model.type'
import { getCurrentUser, getUserRoles } from '@/common/guards/roleGuard.guard'
import { useChatHistoryStore } from '@/stores/chatHistory'
import ChatMessage from './components/ChatMessage.vue'
import ProposalMessage from './components/ProposalMessage.vue'
import ProposalMenu from './components/ProposalMenu.vue'
import TypingIndicator from './components/TypingIndicator.vue'
import NotificationCenter from './components/NotificationCenter.vue'
import { getParcelsV2 } from '../Parcels/api'
import type { ParcelDto } from '../Parcels/model.type'
import type { QueryPayload } from '@/common/types/filter'
import { getActiveSessionForDeliveryMan, getAssignmentsBySessionId } from '../Delivery/api'
import type { DeliveryAssignmentTask } from '../Delivery/model.type'

// Lazy load modals
const LazyDateTimePickerModal = defineAsyncComponent(() => import('./components/DateTimePickerModal.vue'))
const LazyDateTimeRangePickerModal = defineAsyncComponent(() => import('./components/DateTimeRangePickerModal.vue'))
const LazyTextInputModal = defineAsyncComponent(() => import('./components/TextInputModal.vue'))

const route = useRoute()
const router = useRouter()
const overlay = useOverlay()

const conversationId = computed(() => route.params.conversationId as string)
const partnerId = computed(() => route.query.partnerId as string)
const partnerName = ref<string>('')
const partnerUsername = ref<string>('')

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
  messages,
  loadMessages,
  loadMoreMessages,
  addMessage,
  clearConversation,
  loadConversations,
  loadMissedMessages,
  hasMoreMessages,
  isLoadingMore,
} = useConversations()
const { connected, connecting, connect, sendMessage, sendTyping, disconnect } = useWebSocket()
const {
  availableConfigs,
  loadAvailableConfigs,
  create: createProposal,
  respond: respondToProposal,
} = useProposals()
const { handleStatusUpdate } = useMessageStatus()
const { handleTypingIndicator, getTypingUsers, clearConversationTyping } = useTypingIndicator()
const { handleNotification } = useNotifications()
const chatHistoryStore = useChatHistoryStore()

const messageInput = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const sending = ref(false)
const loadingProposals = ref(false)
const typingTimer = ref<number | null>(null)
const isTyping = ref(false)

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
 * Load messages and connect WebSocket on mount
 */
onMounted(async () => {
  if (conversationId.value && currentUserId.value) {
    await loadMessages(conversationId.value, currentUserId.value)
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
    await connectWebSocket()
    scrollToBottom()
  }
})

/**
 * Load partner information from conversations list
 * (CASE 1: Initial load when opening chat)
 */
const loadPartnerInfo = async () => {
  if (currentUserId.value) {
    await loadConversations(currentUserId.value)
  }
  // Find the current conversation to get partner info
  const { conversations } = useConversations()
  const currentConv = conversations.value.find(c => c.conversationId === conversationId.value)
  if (currentConv) {
    partnerName.value = currentConv.partnerName
    partnerUsername.value = currentConv.partnerUsername || ''
  } else {
    // Fallback to partnerId
    partnerName.value = partnerId.value || 'Unknown User'
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

      if (assignmentsResponse.content) {
        // Show ALL assignments in the session (not filtered by receiverId)
        sessionAssignments.value = assignmentsResponse.content
        console.log('📦 Loaded', sessionAssignments.value.length, 'parcels in active session for shipper:', shipperId)
      } else {
        sessionAssignments.value = []
      }
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
  clearConversation()
  disconnect()
  if (conversationId.value) {
    clearConversationTyping(conversationId.value)
  }
  if (typingTimer.value) {
    clearTimeout(typingTimer.value)
  }
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
 * Connect WebSocket (CASE 1: Initial connection)
 */
const connectWebSocket = async () => {
  if (!currentUserId.value) return

  await connect(
    currentUserId.value,
    async (message: MessageResponse) => {

      // Add message if it belongs to this conversation
      // Backend sends messages to both sender and recipient
      // We need to verify it belongs to the current conversation
      const isFromPartner = message.senderId === partnerId.value
      const isFromMe = message.senderId === currentUserId.value
      const belongsToConversation =
        !message.conversationId || // Backward compatibility: if no conversationId, check by sender
        message.conversationId === conversationId.value

      if ((isFromPartner || isFromMe) && belongsToConversation) {
        console.log('✅ Adding message to chat:', {
          from: isFromPartner ? 'partner' : 'me',
          conversationId: message.conversationId,
          currentConversationId: conversationId.value,
        })
        addMessage(message)

        // Save to local storage
        if (conversationId.value) {
          chatHistoryStore.addMessage(conversationId.value, message)
        }

        nextTick(() => scrollToBottom())

        // ❌ REMOVED: No need to reload conversations on every message
        // Conversations list should be updated via WebSocket events, not polling
      } else {
        console.log('⚠️ Message filtered out:', {
          isFromPartner,
          isFromMe,
          belongsToConversation,
          messageConversationId: message.conversationId,
          currentConversationId: conversationId.value,
        })
      }
    },
    handleReconnect, // Callback for reconnection
    handleStatusUpdate, // Status update callback
    handleTypingIndicator, // Typing indicator callback
    handleNotification, // Notification callback
    handleProposalUpdate // Proposal update callback
  )
}

/**
 * Handle WebSocket reconnection - load missed messages and refresh conversations
 */
const handleReconnect = async () => {
  if (conversationId.value && currentUserId.value) {
    console.log('🔄 WebSocket reconnected!')

    // 1. Load missed messages for current conversation
    await loadMissedMessages(conversationId.value, currentUserId.value)

    // 2. Reload conversations list (one of only 2 cases to call this)
    await loadConversations(currentUserId.value)

    nextTick(() => scrollToBottom())

    console.log('✅ Reconnect complete: missed messages and conversations loaded')
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

  // Add message immediately for optimistic UI update
  addMessage(optimisticMessage)
  messageInput.value = ''
  nextTick(() => scrollToBottom())

  const payload: ChatMessagePayload = {
    content,
    recipientId: partnerId.value,
    conversationId: conversationId.value,
  }

  const success = sendMessage(payload)

  if (!success) {
    // Remove optimistic message if send failed
    const index = messages.value.findIndex((m) => m.id === optimisticMessage.id)
    if (index !== -1) {
      messages.value.splice(index, 1)
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
      const stillHasOptimistic = messages.value.some((m) => m.id === optimisticMessage.id)
      if (stillHasOptimistic && conversationId.value) {
        console.log('Optimistic message still exists, reloading from server')
        await loadMessages(conversationId.value, currentUserId.value)
        nextTick(() => scrollToBottom())
      }
    }, 2000) // Wait 2 seconds for WebSocket delivery

    // Reload conversations list to update lastMessageTime
    if (currentUserId.value) {
      await loadConversations(currentUserId.value)
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

  if (
    result &&
    result.startDate &&
    result.startTime &&
    result.endDate &&
    result.endTime
  ) {
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
      proposalData = JSON.stringify({ parcelId: currentParcel.value.id, ...JSON.parse(data || '{}') })
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
  }
}

/**
 * Handle proposal update from WebSocket
 */
const handleProposalUpdate = (update: ProposalUpdateDTO) => {
  console.log('📋 Proposal update received:', update)

  // Find message with matching proposal ID and update its status
  const messageIndex = messages.value.findIndex(
    (msg) => msg.proposal?.id === update.proposalId
  )

  if (messageIndex !== -1) {
    const message = messages.value[messageIndex]
    if (message.proposal) {
      // Update proposal status
      message.proposal.status = update.newStatus
      if (update.resultData) {
        message.proposal.resultData = update.resultData
      }

      // Trigger reactivity
      messages.value[messageIndex] = { ...message }

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
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

/**
 * Handle scroll event for infinite scroll
 */
const handleScroll = () => {
  if (!messagesContainer.value || !conversationId.value || !currentUserId.value) return

  const scrollTop = messagesContainer.value.scrollTop

  // Load more when scrolled to top (within 100px)
  if (scrollTop < 100 && !isLoadingMore.value && hasMoreMessages.value) {
    console.log('📜 Reached top, loading more messages...')

    // Save current scroll height
    const oldScrollHeight = messagesContainer.value.scrollHeight

    loadMoreMessages(conversationId.value, currentUserId.value).then(() => {
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
 * Watch for new messages and scroll
 */
watch(
  () => messages.value.length,
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
</script>

<template>
  <div class="flex flex-col" style="height: 80vh">
    <!-- Chat Header -->
    <div class="p-4 border-b border-gray-200 flex items-center justify-between flex-shrink-0">
      <div class="flex items-center space-x-3">
        <UButton
          icon="i-heroicons-arrow-left"
          variant="ghost"
          @click="router.push({ name: 'communication-conversations' })"
        />
        <div
          class="w-10 h-10 rounded-full bg-gray-300 flex items-center justify-center text-gray-600 font-semibold"
        >
          {{ (partnerName || partnerId || '?').charAt(0).toUpperCase() }}
        </div>
        <div>
          <p class="font-semibold">{{ partnerName || partnerId || 'Chat' }}</p>
          <p v-if="partnerUsername" class="text-sm text-gray-500">@{{ partnerUsername }}</p>
          <p v-else-if="!partnerName && partnerId" class="text-sm text-gray-500">{{ partnerId }}</p>
        </div>
      </div>
      <div class="flex items-center space-x-2">
        <!-- Parcels Popover (always show icon, with badge if parcels exist) -->
        <UPopover v-model:open="showParcelsPopover" :content="{ side: 'bottom', align: 'end' }">
          <UButton
            variant="ghost"
            color="neutral"
            icon="i-heroicons-cube"
            class="relative"
          >
            <span v-if="sessionAssignments.length > 0" class="ml-1">{{ sessionAssignments.length }}</span>
            <!-- Red dot indicator when parcels exist -->
            <span
              v-if="sessionAssignments.length > 0"
              class="absolute top-0 right-0 w-2 h-2 bg-red-500 rounded-full border-2 border-white dark:border-gray-900"
            />
          </UButton>
          <template #content>
            <div class="p-4 min-w-80 max-w-md max-h-96 overflow-y-auto">
              <div class="mb-3">
                <h3 class="font-semibold text-gray-900 dark:text-gray-100">
                  Parcels in Active Session
                </h3>
                <p v-if="sessionAssignments.length > 0" class="text-sm text-gray-500 dark:text-gray-400">
                  {{ sessionAssignments.length }} parcel(s) being delivered
                </p>
              </div>

              <USkeleton v-if="loadingSession" class="h-32 w-full" />

              <div v-else-if="sessionAssignments.length === 0" class="text-center py-8 text-gray-500">
                <p>Không có đơn hàng trong phiên hiện tại.</p>
              </div>

              <div v-else class="space-y-3">
                <div
                  v-for="assignment in sessionAssignments"
                  :key="assignment.parcelId"
                  class="p-3 border border-gray-200 dark:border-gray-700 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
                >
                  <div class="flex items-start justify-between">
                    <div class="flex-1">
                      <div class="flex items-center space-x-2 mb-2">
                        <span class="font-semibold text-gray-900 dark:text-gray-100">
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
                      <div class="text-sm text-gray-600 dark:text-gray-400 space-y-1">
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
      </div>
    </div>

    <!-- Messages Container -->
    <div
      ref="messagesContainer"
      class="flex-1 overflow-y-auto p-4 space-y-4 bg-gray-50"
      style="scroll-behavior: smooth"
      @scroll="handleScroll"
    >
      <!-- Loading More Indicator (at top) -->
      <div v-if="isLoadingMore" class="flex justify-center py-4">
        <div class="flex items-center space-x-2 text-gray-500">
          <UIcon name="i-heroicons-arrow-path" class="animate-spin" />
          <span class="text-sm">Loading more messages...</span>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="messages.length === 0" class="flex items-center justify-center h-full">
        <div class="text-center text-gray-500">
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
        <!-- Text Message -->
        <ChatMessage
          v-if="message.type === 'TEXT'"
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
    <div class="p-4 border-t border-gray-200 bg-white">
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
