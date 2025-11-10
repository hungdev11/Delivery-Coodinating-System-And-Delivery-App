<script setup lang="ts">
/**
 * Chat Message Component
 *
 * Displays a single chat message (text or proposal)
 */

import type { MessageResponse } from '../model.type'

interface Props {
  message: MessageResponse
  isMyMessage: boolean
}

defineProps<Props>()

/**
 * Format message time
 */
const formatMessageTime = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
}
</script>

<template>
  <div
    v-if="message.type === 'TEXT'"
    class="max-w-xs lg:max-w-md px-4 py-2 rounded-lg"
    :class="
      isMyMessage
        ? 'bg-blue-500 text-white'
        : 'bg-white text-gray-900 border border-gray-200'
    "
  >
    <p class="text-sm whitespace-pre-wrap break-words">{{ message.content }}</p>
    <p
      class="text-xs mt-1"
      :class="isMyMessage ? 'text-blue-100' : 'text-gray-500'"
    >
      {{ formatMessageTime(message.sentAt) }}
    </p>
  </div>
</template>
