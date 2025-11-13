/**
 * useMessageStatus Composable
 * 
 * Manages message status tracking (SENT → DELIVERED → READ)
 */

import { ref } from 'vue'

export interface MessageStatus {
  messageId: string
  conversationId: string
  status: 'SENT' | 'DELIVERED' | 'READ'
  userId: string
  timestamp: string
}

export function useMessageStatus() {
  const messageStatuses = ref<Map<string, MessageStatus>>(new Map())

  /**
   * Handle status update from WebSocket
   */
  const handleStatusUpdate = (statusUpdate: MessageStatus) => {
    messageStatuses.value.set(statusUpdate.messageId, statusUpdate)
  }

  /**
   * Get status for a specific message
   */
  const getMessageStatus = (messageId: string): MessageStatus | undefined => {
    return messageStatuses.value.get(messageId)
  }

  /**
   * Clear all status tracking
   */
  const clearStatuses = () => {
    messageStatuses.value.clear()
  }

  /**
   * Get display icon for status
   */
  const getStatusIcon = (status?: 'SENT' | 'DELIVERED' | 'READ') => {
    switch (status) {
      case 'SENT':
        return 'i-heroicons-check'
      case 'DELIVERED':
        return 'i-heroicons-check-check'
      case 'READ':
        return 'i-heroicons-check-check-solid'
      default:
        return 'i-heroicons-clock'
    }
  }

  /**
   * Get display color for status
   */
  const getStatusColor = (status?: 'SENT' | 'DELIVERED' | 'READ') => {
    switch (status) {
      case 'SENT':
        return 'text-gray-400'
      case 'DELIVERED':
        return 'text-blue-500'
      case 'READ':
        return 'text-green-500'
      default:
        return 'text-gray-300'
    }
  }

  return {
    messageStatuses,
    handleStatusUpdate,
    getMessageStatus,
    clearStatuses,
    getStatusIcon,
    getStatusColor,
  }
}
