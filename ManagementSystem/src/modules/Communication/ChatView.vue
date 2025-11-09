<script setup lang="ts">
/**
 * Chat View
 *
 * Real-time chat interface for conversations
 */

import { onMounted, onUnmounted, ref, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useConversations, useWebSocket } from './composables'
import type { MessageResponse, ChatMessagePayload } from './model.type'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'

const route = useRoute()
const router = useRouter()

const conversationId = computed(() => route.params.conversationId as string)
const partnerId = computed(() => route.query.partnerId as string)

const currentUser = getCurrentUser()
const currentUserId = computed(() => currentUser?.id || '')

const { messages, loadMessages, addMessage, clearConversation } = useConversations()
const { connected, connecting, connect, sendMessage, disconnect } = useWebSocket()

const messageInput = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const sending = ref(false)

/**
 * Load messages and connect WebSocket on mount
 */
onMounted(async () => {
  if (conversationId.value && currentUserId.value) {
    await loadMessages(conversationId.value, currentUserId.value)
    await connectWebSocket()
    scrollToBottom()
  }
})

/**
 * Cleanup on unmount
 */
onUnmounted(() => {
  clearConversation()
  disconnect()
})

/**
 * Connect WebSocket
 */
const connectWebSocket = async () => {
  if (!currentUserId.value) return

  await connect(currentUserId.value, (message: MessageResponse) => {
    // Only add message if it belongs to current conversation
    if (
      message.senderId === partnerId.value ||
      (message.senderId === currentUserId.value && conversationId.value)
    ) {
      addMessage(message)
      nextTick(() => scrollToBottom())
    }
  })
}

/**
 * Send message
 */
const handleSendMessage = async () => {
  if (!messageInput.value.trim() || sending.value || !partnerId.value) return

  sending.value = true

  const payload: ChatMessagePayload = {
    content: messageInput.value.trim(),
    recipientId: partnerId.value,
  }

  const success = sendMessage(payload)

  if (success) {
    messageInput.value = ''
    nextTick(() => scrollToBottom())
  }

  sending.value = false
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
 * Watch for new messages and scroll
 */
watch(
  () => messages.value.length,
  () => {
    nextTick(() => scrollToBottom())
  },
)

/**
 * Format message time
 */
const formatMessageTime = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
}

/**
 * Check if message is from current user
 */
const isMyMessage = (message: MessageResponse) => {
  return message.senderId === currentUserId.value
}
</script>

<template>
  <div class="flex flex-col h-screen">
    <!-- Chat Header -->
    <div class="p-4 border-b border-gray-200 flex items-center justify-between">
      <div class="flex items-center space-x-3">
        <UButton
          icon="i-heroicons-arrow-left"
          variant="ghost"
          @click="router.push({ name: 'communication-conversations' })"
        />
        <div
          class="w-10 h-10 rounded-full bg-gray-300 flex items-center justify-center text-gray-600 font-semibold"
        >
          {{ partnerId?.charAt(0).toUpperCase() || '?' }}
        </div>
        <div>
          <p class="font-semibold">Chat</p>
          <p class="text-sm text-gray-500">{{ partnerId }}</p>
        </div>
      </div>
      <div class="flex items-center space-x-2">
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
      style="scroll-behavior: smooth;"
    >
      <div v-if="messages.length === 0" class="flex items-center justify-center h-full">
        <div class="text-center text-gray-500">
          <p>No messages yet. Start the conversation!</p>
        </div>
      </div>

      <div
        v-for="message in messages"
        :key="message.id"
        class="flex"
        :class="{ 'justify-end': isMyMessage(message), 'justify-start': !isMyMessage(message) }"
      >
        <div
          class="max-w-xs lg:max-w-md px-4 py-2 rounded-lg"
          :class="
            isMyMessage(message)
              ? 'bg-blue-500 text-white'
              : 'bg-white text-gray-900 border border-gray-200'
          "
        >
          <p class="text-sm whitespace-pre-wrap break-words">{{ message.content }}</p>
          <p
            class="text-xs mt-1"
            :class="isMyMessage(message) ? 'text-blue-100' : 'text-gray-500'"
          >
            {{ formatMessageTime(message.sentAt) }}
          </p>
        </div>
      </div>
    </div>

    <!-- Message Input -->
    <div class="p-4 border-t border-gray-200 bg-white">
      <div class="flex items-center space-x-2">
        <UInput
          v-model="messageInput"
          placeholder="Type a message..."
          :disabled="!connected || sending"
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
  </div>
</template>
