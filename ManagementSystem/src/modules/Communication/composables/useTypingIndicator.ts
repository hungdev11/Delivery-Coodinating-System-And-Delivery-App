/**
 * useTypingIndicator Composable
 *
 * Manages typing indicators for real-time feedback
 */

import { ref, computed } from 'vue'

export interface TypingIndicator {
  conversationId: string
  userId: string
  isTyping: boolean
  timestamp: number
}

export function useTypingIndicator() {
  const typingUsers = ref<Map<string, TypingIndicator>>(new Map())
  const typingTimeout = 5000 // Auto-clear after 5 seconds

  /**
   * Handle typing indicator from WebSocket
   */
  const handleTypingIndicator = (indicator: TypingIndicator) => {
    const key = `${indicator.conversationId}-${indicator.userId}`

    if (indicator.isTyping) {
      typingUsers.value.set(key, indicator)

      // Auto-clear after timeout
      setTimeout(() => {
        const current = typingUsers.value.get(key)
        if (current && current.timestamp === indicator.timestamp) {
          typingUsers.value.delete(key)
        }
      }, typingTimeout)
    } else {
      typingUsers.value.delete(key)
    }
  }

  /**
   * Check if user is typing in conversation
   */
  const isUserTyping = (conversationId: string, userId: string): boolean => {
    const key = `${conversationId}-${userId}`
    return typingUsers.value.has(key)
  }

  /**
   * Get all users typing in conversation (excluding current user)
   */
  const getTypingUsers = (conversationId: string, excludeUserId?: string) => {
    const users: string[] = []
    typingUsers.value.forEach((indicator, key) => {
      if (
        indicator.conversationId === conversationId &&
        indicator.isTyping &&
        indicator.userId !== excludeUserId
      ) {
        users.push(indicator.userId)
      }
    })
    return users
  }

  /**
   * Clear all typing indicators
   */
  const clearTypingIndicators = () => {
    typingUsers.value.clear()
  }

  /**
   * Clear typing indicators for specific conversation
   */
  const clearConversationTyping = (conversationId: string) => {
    const keysToDelete: string[] = []
    typingUsers.value.forEach((indicator, key) => {
      if (indicator.conversationId === conversationId) {
        keysToDelete.push(key)
      }
    })
    keysToDelete.forEach((key) => typingUsers.value.delete(key))
  }

  return {
    typingUsers,
    handleTypingIndicator,
    isUserTyping,
    getTypingUsers,
    clearTypingIndicators,
    clearConversationTyping,
  }
}
