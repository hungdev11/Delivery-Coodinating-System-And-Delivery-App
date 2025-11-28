<script setup lang="ts">
/**
 * Delivery Succeeded Message Component
 *
 * Displays a notification when parcel status changes to SUCCEEDED
 * Distinguishes between user confirmation and automatic timeout
 */

import { computed } from 'vue'

interface Props {
  messageData: {
    type: string
    parcelId: string
    parcelCode: string
    succeededAt: string
    source: 'USER_CONFIRM' | 'AUTO_TIMEOUT'
    isUserConfirmed: boolean
    confirmedBy?: string
    confirmedAt?: string
    deliveryManId?: string
    senderId?: string
    receiverId: string
  }
  sentAt: string
}

const props = defineProps<Props>()

/**
 * Format message time
 */
const formatMessageTime = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
}

/**
 * Format completion time
 */
const formatCompletionTime = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/**
 * Get status text based on source
 */
const statusText = computed(() => {
  if (props.messageData.isUserConfirmed) {
    return 'Đã xác nhận nhận hàng'
  }
  return 'Tự động hoàn thành'
})

/**
 * Get status description
 */
const statusDescription = computed(() => {
  if (props.messageData.isUserConfirmed) {
    return 'Người nhận đã xác nhận nhận hàng thành công'
  }
  return 'Đơn hàng đã tự động chuyển sang hoàn thành sau 24 giờ'
})
</script>

<template>
  <div
    class="max-w-xs lg:max-w-md px-4 py-3 rounded-lg bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20 border-2 border-blue-500"
  >
    <div class="flex items-start justify-between mb-2">
      <div class="flex-1">
        <div class="flex items-center space-x-2 mb-1">
          <div class="w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center">
            <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
          </div>
          <p class="text-sm font-semibold text-gray-900 dark:text-gray-100">
            {{ statusText }}
          </p>
        </div>
      </div>
      <UBadge
        :color="messageData.isUserConfirmed ? 'success' : 'info'"
        variant="subtle"
        size="xs"
      >
        {{ messageData.isUserConfirmed ? 'Xác nhận' : 'Tự động' }}
      </UBadge>
    </div>

    <!-- Description -->
    <p class="text-xs text-gray-600 dark:text-gray-400 mb-3">
      {{ statusDescription }}
    </p>

    <!-- Parcel Info Card -->
    <div
      class="bg-white dark:bg-gray-800 rounded-lg p-3 mb-2 border border-gray-200 dark:border-gray-700"
    >
      <div class="space-y-2">
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-gray-600 dark:text-gray-400">Mã đơn hàng:</span>
          <span class="text-xs font-semibold text-gray-900 dark:text-gray-100">
            {{ messageData.parcelCode || messageData.parcelId.substring(0, 8) + '...' }}
          </span>
        </div>
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-gray-600 dark:text-gray-400"
            >Thời gian hoàn thành:</span
          >
          <span class="text-xs text-gray-900 dark:text-gray-100">
            {{ formatCompletionTime(messageData.succeededAt) }}
          </span>
        </div>
        <div
          v-if="messageData.isUserConfirmed && messageData.confirmedAt"
          class="flex items-center justify-between"
        >
          <span class="text-xs font-medium text-gray-600 dark:text-gray-400"
            >Thời gian xác nhận:</span
          >
          <span class="text-xs text-gray-900 dark:text-gray-100">
            {{ formatCompletionTime(messageData.confirmedAt) }}
          </span>
        </div>
      </div>
    </div>

    <p class="text-xs text-gray-400 mt-2">
      {{ formatMessageTime(sentAt) }}
    </p>
  </div>
</template>
