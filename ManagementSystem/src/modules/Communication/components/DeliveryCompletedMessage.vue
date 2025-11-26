<script setup lang="ts">
/**
 * Delivery Completed Message Component
 *
 * Displays a delivery completed notification message (similar to proposal)
 * Shows parcel info and completion time
 */

import { computed } from 'vue'

interface Props {
  messageData: {
    type: string
    parcelId: string
    parcelCode: string
    completedAt: string
    deliveryManId: string
    deliveryManName?: string
    receiverId: string
    receiverName?: string
    receiverPhone?: string
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
</script>

<template>
  <div
    class="max-w-xs lg:max-w-md px-4 py-3 rounded-lg bg-gradient-to-r from-green-50 to-emerald-50 dark:from-green-900/20 dark:to-emerald-900/20 border-2 border-green-500"
  >
    <div class="flex items-start justify-between mb-2">
      <div class="flex-1">
        <div class="flex items-center space-x-2 mb-1">
          <div class="w-8 h-8 rounded-full bg-green-500 flex items-center justify-center">
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
                d="M5 13l4 4L19 7"
              />
            </svg>
          </div>
          <p class="text-sm font-semibold text-gray-900 dark:text-gray-100">
            Đơn hàng đã được giao thành công
          </p>
        </div>
      </div>
      <UBadge color="success" variant="subtle" size="xs">
        Hoàn thành
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
          <span class="text-xs font-medium text-gray-600 dark:text-gray-400">Thời gian hoàn thành:</span>
          <span class="text-xs text-gray-900 dark:text-gray-100">
            {{ formatCompletionTime(messageData.completedAt) }}
          </span>
        </div>
        <div
          v-if="messageData.deliveryManName"
          class="flex items-center justify-between"
        >
          <span class="text-xs font-medium text-gray-600 dark:text-gray-400">Người giao:</span>
          <span class="text-xs text-gray-900 dark:text-gray-100">
            {{ messageData.deliveryManName }}
          </span>
        </div>
      </div>
    </div>

    <!-- User Info (if available) -->
    <div
      v-if="messageData.receiverName || messageData.receiverPhone"
      class="bg-gray-50 dark:bg-gray-800/50 rounded-lg p-2 mb-2 text-xs"
    >
      <p class="font-medium text-gray-700 dark:text-gray-300 mb-1">Thông tin người nhận:</p>
      <div class="space-y-1 text-gray-600 dark:text-gray-400">
        <p v-if="messageData.receiverName">
          <span class="font-medium">Tên:</span> {{ messageData.receiverName }}
        </p>
        <p v-if="messageData.receiverPhone">
          <span class="font-medium">SĐT:</span> {{ messageData.receiverPhone }}
        </p>
      </div>
    </div>

    <p class="text-xs text-gray-400 mt-2">
      {{ formatMessageTime(sentAt) }}
    </p>
  </div>
</template>
