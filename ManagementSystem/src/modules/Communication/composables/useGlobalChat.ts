/**
 * useGlobalChat Composable
 *
 * Global chat service to listen to all messages and proposals app-wide
 * Similar to GlobalChatService in Android app
 */

import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useWebSocket } from './useWebSocket'
import { useNotifications } from './useNotifications'
import { useConversations } from './useConversations'
import type { MessageResponse, ProposalUpdateDTO } from '../model.type'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'
import { useChatStore } from '@/stores/chatStore'

/**
 * Global Chat Listener Interface
 */
export interface GlobalChatListener {
  onMessageReceived?: (message: MessageResponse) => void
  onUnreadCountChanged?: (count: number) => void
  onConnectionStatusChanged?: (connected: boolean) => void
  onError?: (error: string) => void
  onNotificationReceived?: (notification: any) => void
  onProposalReceived?: (proposal: MessageResponse) => void
  onProposalUpdate?: (update: ProposalUpdateDTO) => void
  onUpdateNotificationReceived?: (updateNotification: any) => void
  onUserStatusUpdate?: (userId: string, isOnline: boolean) => void
}

let globalChatInstance: ReturnType<typeof useGlobalChat> | null = null

export function useGlobalChat() {
  // Singleton pattern - return existing instance if available
  if (globalChatInstance) {
    return globalChatInstance
  }

  const router = useRouter()
  const { connected, connect, disconnect, sendMessage, sendTyping } = useWebSocket()
  const { handleNotification } = useNotifications()
  const { loadConversations } = useConversations()
  const chatStore = useChatStore()

  const unreadCounts = ref<Map<string, number>>(new Map())
  const processedMessageIds = ref<Set<string>>(new Set())
  const pendingProposals = ref<MessageResponse[]>([])
  const listeners = ref<GlobalChatListener[]>([])

  const currentUser = getCurrentUser()
  const currentUserId = ref(currentUser?.id || '')

  /**
   * Get total unread message count across all conversations
   */
  const getTotalUnreadCount = (): number => {
    let total = 0
    unreadCounts.value.forEach((count) => {
      total += count
    })
    return total
  }

  /**
   * Get unread count for a specific conversation
   */
  const getUnreadCount = (conversationId: string): number => {
    return unreadCounts.value.get(conversationId) || 0
  }

  /**
   * Clear unread count for a specific conversation
   */
  const clearUnreadCount = (conversationId: string) => {
    if (conversationId && unreadCounts.value.has(conversationId)) {
      unreadCounts.value.set(conversationId, 0)
      notifyUnreadCountChanged()
    }
  }

  /**
   * Update unread count for a conversation
   */
  const updateUnreadCount = (conversationId: string, count: number) => {
    if (conversationId) {
      if (count > 0) {
        unreadCounts.value.set(conversationId, count)
      } else {
        unreadCounts.value.delete(conversationId)
      }
      notifyUnreadCountChanged()
    }
  }

  /**
   * Sync unread counts from backend
   */
  const syncUnreadCounts = (counts: Record<string, number>) => {
    unreadCounts.value.clear()
    Object.entries(counts).forEach(([conversationId, count]) => {
      unreadCounts.value.set(conversationId, count)
    })
    notifyUnreadCountChanged()
  }

  /**
   * Register listener for global chat events
   */
  const addListener = (listener: GlobalChatListener) => {
    if (listener && !listeners.value.includes(listener)) {
      listeners.value.push(listener)
    }
  }

  /**
   * Unregister listener
   */
  const removeListener = (listener: GlobalChatListener) => {
    const index = listeners.value.indexOf(listener)
    if (index > -1) {
      listeners.value.splice(index, 1)
    }
  }

  /**
   * Handle message received
   */
  const handleMessageReceived = (message: MessageResponse) => {
    if (!message || !message.id) return

    // Avoid duplicate processing
    if (processedMessageIds.value.has(message.id)) {
      console.debug('[GlobalChat] Message already processed:', message.id)
      return
    }

    // Limit processed message IDs set size to prevent memory leak
    if (processedMessageIds.value.size > 1000) {
      processedMessageIds.value.clear()
      console.debug('[GlobalChat] Cleared processed message IDs cache')
    }
    processedMessageIds.value.add(message.id)

    console.log('[GlobalChat] Message received:', message.id, message.type)

    // Add message to store (for all conversations)
    if (message.conversationId) {
      chatStore.addMessage(message.conversationId, message)
    }

    // Handle proposal messages specially
    if (message.type === 'INTERACTIVE_PROPOSAL') {
      handleProposalMessage(message)
    } else {
      // Check if message is delivery completed notification
      try {
        const content = message.content
        if (content && typeof content === 'string') {
          const messageData = JSON.parse(content)
          if (messageData && messageData.type === 'DELIVERY_COMPLETED') {
            // This is a delivery completed message - handle specially
            console.log('[GlobalChat] Delivery completed message received:', messageData)
            // Still increment unread count
            if (message.conversationId && message.senderId !== currentUserId.value) {
              const currentCount = getUnreadCount(message.conversationId)
              updateUnreadCount(message.conversationId, currentCount + 1)
            }
          } else {
            // Normal message - increment unread count if not from current user
            if (message.conversationId && message.senderId !== currentUserId.value) {
              const currentCount = getUnreadCount(message.conversationId)
              updateUnreadCount(message.conversationId, currentCount + 1)
            }
          }
        } else {
          // Normal message - increment unread count if not from current user
          if (message.conversationId && message.senderId !== currentUserId.value) {
            const currentCount = getUnreadCount(message.conversationId)
            updateUnreadCount(message.conversationId, currentCount + 1)
          }
        }
      } catch (e) {
        // Not JSON or parse error - treat as normal message
        if (message.conversationId && message.senderId !== currentUserId.value) {
          const currentCount = getUnreadCount(message.conversationId)
          updateUnreadCount(message.conversationId, currentCount + 1)
        }
      }
    }

    // Notify all listeners
    listeners.value.forEach((listener) => {
      if (listener.onMessageReceived) {
        try {
          listener.onMessageReceived(message)
        } catch (error) {
          console.error('[GlobalChat] Error notifying listener:', error)
        }
      }
    })
  }

  /**
   * Handle proposal message
   */
  const handleProposalMessage = (message: MessageResponse) => {
    console.log('[GlobalChat] Handling proposal message:', message.id)
    pendingProposals.value.push(message)

    // Notify all listeners
    listeners.value.forEach((listener) => {
      if (listener.onProposalReceived) {
        try {
          listener.onProposalReceived(message)
        } catch (error) {
          console.error('[GlobalChat] Error notifying proposal listener:', error)
        }
      }
    })
  }

  /**
   * Handle proposal update
   */
  const handleProposalUpdate = (update: ProposalUpdateDTO) => {
    console.log('[GlobalChat] Proposal update received:', update.proposalId)

    // Notify all listeners
    listeners.value.forEach((listener) => {
      if (listener.onProposalUpdate) {
        try {
          listener.onProposalUpdate(update)
        } catch (error) {
          console.error('[GlobalChat] Error notifying proposal update listener:', error)
        }
      }
    })
  }

  /**
   * Notify unread count changed
   */
  const notifyUnreadCountChanged = () => {
    const total = getTotalUnreadCount()
    console.log('[GlobalChat] Unread count changed:', total)

    listeners.value.forEach((listener) => {
      if (listener.onUnreadCountChanged) {
        try {
          listener.onUnreadCountChanged(total)
        } catch (error) {
          console.error('[GlobalChat] Error notifying unread count listener:', error)
        }
      }
    })
  }

  /**
   * Initialize global chat connection
   */
  const initialize = async () => {
    if (!currentUserId.value) {
      console.warn('[GlobalChat] Cannot initialize: No user ID')
      return
    }

    if (connected.value) {
      console.debug('[GlobalChat] Already connected')
      return
    }

    console.log('[GlobalChat] Initializing global chat service...')

    await connect(
      currentUserId.value,
      handleMessageReceived, // onMessageReceived
      async () => {
        // onReconnect - reload conversations to sync state
        console.log('[GlobalChat] Reconnected, syncing conversations...')
        await loadConversations(currentUserId.value)
      },
      (statusUpdate) => {
        // onStatusUpdate - handle user online/offline status updates
        console.log('[GlobalChat] Status update received:', statusUpdate)
        
        // Check if this is a user status update (online/offline)
        if (statusUpdate && typeof statusUpdate === 'object' && 'userId' in statusUpdate && 'isOnline' in statusUpdate) {
          const userId = statusUpdate.userId as string
          const isOnline = statusUpdate.isOnline as boolean
          
          // Update conversation online status in store
          const conversations = chatStore.conversations
          const conversation = Array.from(conversations.values()).find(
            (conv) => conv.partnerId === userId
          )
          
          if (conversation) {
            chatStore.setConversation({
              ...conversation,
              isOnline,
            })
            console.log(`[GlobalChat] Updated online status for user ${userId}: ${isOnline}`)
          }
          
          // Notify all listeners about user status update
          listeners.value.forEach((listener) => {
            if (listener.onUserStatusUpdate) {
              try {
                listener.onUserStatusUpdate(userId, isOnline)
              } catch (error) {
                console.error('[GlobalChat] Error notifying status update listener:', error)
              }
            }
          })
        }
      },
      (typingIndicator) => {
        // onTypingIndicator - handle typing indicators if needed
        console.debug('[GlobalChat] Typing indicator:', typingIndicator)
      },
      (notification) => {
        // onNotification - handle notifications
        console.log('[GlobalChat] Notification received:', notification)
        handleNotification(notification)

        // Notify all listeners
        listeners.value.forEach((listener) => {
          if (listener.onNotificationReceived) {
            try {
              listener.onNotificationReceived(notification)
            } catch (error) {
              console.error('[GlobalChat] Error notifying notification listener:', error)
            }
          }
        })
      },
      handleProposalUpdate, // onProposalUpdate
      (updateNotification) => {
        // onUpdateNotification - handle update notifications (parcel/assignment updates)
        console.log('[GlobalChat] Update notification received:', updateNotification)

        // Notify all listeners
        listeners.value.forEach((listener) => {
          if (listener.onUpdateNotificationReceived) {
            try {
              listener.onUpdateNotificationReceived(updateNotification)
            } catch (error) {
              console.error('[GlobalChat] Error notifying update notification listener:', error)
            }
          }
        })
      },
    )
  }

  /**
   * Cleanup
   */
  const cleanup = () => {
    disconnect()
    listeners.value = []
    unreadCounts.value.clear()
    processedMessageIds.value.clear()
    pendingProposals.value = []
  }

  // Auto-initialize on mount if user is logged in
  onMounted(() => {
    if (currentUserId.value) {
      initialize()
    }
  })

  // Cleanup on unmount
  onUnmounted(() => {
    cleanup()
  })

  const instance = {
    connected,
    unreadCounts,
    getTotalUnreadCount,
    getUnreadCount,
    clearUnreadCount,
    updateUnreadCount,
    syncUnreadCounts,
    addListener,
    removeListener,
    initialize,
    cleanup,
    pendingProposals,
    sendMessage, // Expose sendMessage from the same WebSocket instance
    sendTyping, // Expose sendTyping from the same WebSocket instance
  }

  // Store as singleton
  globalChatInstance = instance

  return instance
}
