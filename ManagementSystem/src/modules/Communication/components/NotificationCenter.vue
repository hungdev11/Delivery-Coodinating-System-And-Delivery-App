<script setup lang="ts">
/**
 * NotificationCenter Component
 *
 * Displays in-app notifications with badge and dropdown
 */

import { computed } from 'vue'
import { useNotifications } from '../composables'
import type { Notification } from '../composables/useNotifications'

const { notifications, unreadCount, markAsRead, markAllAsRead, removeNotification } = useNotifications()

const hasUnread = computed(() => unreadCount.value > 0)

/**
 * Handle notification click
 */
const handleNotificationClick = (notification: Notification) => {
  // Mark as read
  if (!notification.read) {
    markAsRead(notification.id)
  }

  // Navigate to action URL if provided
  if (notification.actionUrl) {
    window.location.href = notification.actionUrl
  }
}

/**
 * Get notification icon based on type
 */
const getNotificationIcon = (type: Notification['type']) => {
  switch (type) {
    case 'NEW_MESSAGE':
      return 'i-heroicons-chat-bubble-left'
    case 'NEW_PROPOSAL':
      return 'i-heroicons-document-text'
    case 'PROPOSAL_UPDATE':
      return 'i-heroicons-arrow-path'
    case 'DELIVERY_UPDATE':
      return 'i-heroicons-truck'
    case 'ERROR':
      return 'i-heroicons-exclamation-circle'
    case 'WARNING':
      return 'i-heroicons-exclamation-triangle'
    case 'SYSTEM':
      return 'i-heroicons-cog-6-tooth'
    case 'INFO':
    default:
      return 'i-heroicons-information-circle'
  }
}

/**
 * Get notification color based on type
 */
const getNotificationColor = (type: Notification['type']) => {
  switch (type) {
    case 'ERROR':
      return 'text-red-500'
    case 'WARNING':
      return 'text-yellow-500'
    case 'NEW_MESSAGE':
    case 'NEW_PROPOSAL':
      return 'text-blue-500'
    case 'PROPOSAL_UPDATE':
    case 'DELIVERY_UPDATE':
      return 'text-green-500'
    case 'SYSTEM':
    case 'INFO':
    default:
      return 'text-gray-500'
  }
}

/**
 * Format timestamp
 */
const formatTime = (timestamp: string) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  // Less than 1 minute
  if (diff < 60000) {
    return 'Just now'
  }

  // Less than 1 hour
  if (diff < 3600000) {
    const minutes = Math.floor(diff / 60000)
    return `${minutes}m ago`
  }

  // Less than 24 hours
  if (diff < 86400000) {
    const hours = Math.floor(diff / 3600000)
    return `${hours}h ago`
  }

  // More than 24 hours
  const days = Math.floor(diff / 86400000)
  if (days === 1) return 'Yesterday'
  if (days < 7) return `${days} days ago`

  // Format as date
  return date.toLocaleDateString()
}
</script>

<template>
  <UDropdownMenu
    :items="[
      [{
        label: 'Notifications',
        slot: 'header',
      }],
      notifications.slice(0, 10).map(n => ({
        id: n.id,
        label: n.title,
        description: n.message,
        icon: getNotificationIcon(n.type),
        iconClass: getNotificationColor(n.type),
        badge: !n.read ? '•' : undefined,
        click: () => handleNotificationClick(n),
      })),
      [{
        label: 'Mark all as read',
        icon: 'i-heroicons-check-badge',
        click: markAllAsRead,
      }, {
        label: 'Clear all',
        icon: 'i-heroicons-trash',
        click: () => notifications.length = 0,
      }]
    ]"
    :popper="{ placement: 'bottom-end' }"
  >
    <UButton
      icon="i-heroicons-bell"
      color="neutral"
      variant="ghost"
      :badge="hasUnread ? String(unreadCount) : undefined"
      badge-color="error"
    >
      <template #badge v-if="hasUnread">
        <span class="absolute -top-1 -right-1 flex h-5 w-5">
          <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
          <span class="relative inline-flex rounded-full h-5 w-5 bg-red-500 items-center justify-center text-xs text-white">
            {{ unreadCount > 9 ? '9+' : unreadCount }}
          </span>
        </span>
      </template>
    </UButton>

    <template #header>
      <div class="flex items-center justify-between px-4 py-2">
        <h3 class="font-semibold">Notifications</h3>
        <UBadge v-if="hasUnread" color="error" variant="subtle">
          {{ unreadCount }}
        </UBadge>
      </div>
    </template>

    <template #item="{ item }">
      <div class="flex items-start space-x-3 px-4 py-3 hover:bg-gray-50 cursor-pointer" :class="{ 'bg-blue-50': item.badge }">
        <UIcon :name="item.icon" :class="item.iconClass" class="w-5 h-5 flex-shrink-0 mt-0.5" />
        <div class="flex-1 min-w-0">
          <div class="flex items-start justify-between">
            <p class="text-sm font-medium text-gray-900 truncate">{{ item.label }}</p>
            <UBadge v-if="item.badge" color="primary" variant="solid" size="xs" class="ml-2">New</UBadge>
          </div>
          <p class="text-sm text-gray-500 line-clamp-2">{{ item.description }}</p>
        </div>
      </div>
    </template>
  </UDropdownMenu>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
