/**
 * useNotifications Composable
 *
 * Manages in-app notifications with WebSocket real-time delivery
 */

import { ref, computed } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'

export interface Notification {
  id: string
  userId: string
  type:
    | 'NEW_MESSAGE'
    | 'NEW_PROPOSAL'
    | 'PROPOSAL_UPDATE'
    | 'DELIVERY_UPDATE'
    | 'SYSTEM'
    | 'INFO'
    | 'WARNING'
    | 'ERROR'
  title: string
  message: string
  data?: string
  read: boolean
  createdAt: string
  readAt?: string
  actionUrl?: string
}

export function useNotifications() {
  const toast = useToast()
  const notifications = ref<Notification[]>([])
  const unreadCount = computed(() => notifications.value.filter((n) => !n.read).length)

  /**
   * Handle notification from WebSocket
   */
  const handleNotification = (notification: Notification) => {
    // Add to notifications list
    notifications.value.unshift(notification)

    // Show toast notification
    showToastNotification(notification)

    // Keep only last 100 notifications in memory
    if (notifications.value.length > 100) {
      notifications.value = notifications.value.slice(0, 100)
    }
  }

  /**
   * Show toast notification
   */
  const showToastNotification = (notification: Notification) => {
    const color = getNotificationColor(notification.type)

    toast.add({
      title: notification.title,
      description: notification.message,
      color: color as any,
      timeout: 5000,
      callback: notification.actionUrl
        ? () => {
            // Navigate to action URL if provided
            if (notification.actionUrl) {
              window.location.href = notification.actionUrl
            }
          }
        : undefined,
    })
  }

  /**
   * Get notification color based on type
   */
  const getNotificationColor = (type: Notification['type']): string => {
    switch (type) {
      case 'ERROR':
        return 'error'
      case 'WARNING':
        return 'warning'
      case 'NEW_MESSAGE':
      case 'NEW_PROPOSAL':
        return 'primary'
      case 'PROPOSAL_UPDATE':
      case 'DELIVERY_UPDATE':
        return 'info'
      case 'SYSTEM':
      case 'INFO':
      default:
        return 'neutral'
    }
  }

  /**
   * Mark notification as read
   */
  const markAsRead = (notificationId: string) => {
    const notification = notifications.value.find((n) => n.id === notificationId)
    if (notification && !notification.read) {
      notification.read = true
      notification.readAt = new Date().toISOString()
    }
  }

  /**
   * Mark all notifications as read
   */
  const markAllAsRead = () => {
    const now = new Date().toISOString()
    notifications.value.forEach((n) => {
      if (!n.read) {
        n.read = true
        n.readAt = now
      }
    })
  }

  /**
   * Remove notification
   */
  const removeNotification = (notificationId: string) => {
    const index = notifications.value.findIndex((n) => n.id === notificationId)
    if (index !== -1) {
      notifications.value.splice(index, 1)
    }
  }

  /**
   * Clear all notifications
   */
  const clearNotifications = () => {
    notifications.value = []
  }

  /**
   * Get unread notifications
   */
  const getUnreadNotifications = computed(() => {
    return notifications.value.filter((n) => !n.read)
  })

  return {
    notifications,
    unreadCount,
    getUnreadNotifications,
    handleNotification,
    markAsRead,
    markAllAsRead,
    removeNotification,
    clearNotifications,
  }
}
