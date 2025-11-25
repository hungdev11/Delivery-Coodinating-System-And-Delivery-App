<script setup lang="ts">
/**
 * Postpone Message Component
 *
 * Displays a postpone notification message when parcel is postponed (out of session)
 * Shows parcel info and postpone datetime
 */

interface Props {
  messageData: {
    parcelId: string
    parcelCode?: string
    postponeDateTime?: string
    reason?: string
    deliveryManId?: string
    receiverId?: string
    receiverName?: string
  }
  sentAt: string
}

defineProps<Props>()

/**
 * Format message time
 */
const formatMessageTime = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
}

/**
 * Format postpone datetime
 */
const formatPostponeDateTime = (dateString?: string) => {
  if (!dateString) return 'Thời gian sau'
  try {
    const date = new Date(dateString)
    return date.toLocaleString('vi-VN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return dateString
  }
}
</script>

<template>
  <div
    class="max-w-xs lg:max-w-md px-4 py-3 rounded-lg bg-gradient-to-r from-amber-50 to-orange-50 dark:from-amber-900/20 dark:to-orange-900/20 border-2 border-amber-500"
  >
    <div class="flex items-start justify-between mb-2">
      <div class="flex-1">
        <div class="flex items-center space-x-2 mb-1">
          <div class="w-8 h-8 rounded-full bg-amber-500 flex items-center justify-center">
            <svg
              class="w-5 h-5 text-white"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
          </div>
          <p class="text-sm font-semibold text-gray-900 dark:text-gray-100">
            Đơn hàng đã được hoãn
          </p>
        </div>
      </div>
      <UBadge color="warning" variant="subtle" size="xs">
        Hoãn
      </UBadge>
    </div>

    <!-- Parcel Info -->
    <div class="bg-white dark:bg-gray-800 rounded-lg p-3 mb-2 border border-gray-200 dark:border-gray-700">
      <div class="space-y-2">
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-gray-600 dark:text-gray-400">Mã đơn hàng:</span>
          <span class="text-xs font-semibold text-gray-900 dark:text-gray-100">
            {{ messageData.parcelCode || messageData.parcelId.substring(0, 8) + '...' }}
          </span>
        </div>
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-gray-600 dark:text-gray-400">Thời gian hoãn đến:</span>
          <span class="text-xs text-gray-900 dark:text-gray-100">
            {{ formatPostponeDateTime(messageData.postponeDateTime) }}
          </span>
        </div>
        <div
          v-if="messageData.reason"
          class="flex items-start justify-between"
        >
          <span class="text-xs font-medium text-gray-600 dark:text-gray-400">Lý do:</span>
          <span class="text-xs text-gray-900 dark:text-gray-100 text-right flex-1 ml-2">
            {{ messageData.reason }}
          </span>
        </div>
      </div>
    </div>

    <p class="text-xs text-gray-400 mt-2">
      {{ formatMessageTime(sentAt) }}
    </p>
  </div>
</template>
