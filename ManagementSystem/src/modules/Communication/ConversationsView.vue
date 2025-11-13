<script setup lang="ts">
/**
 * Conversations List View
 *
 * View for listing and selecting conversations
 */

import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import { useConversations } from './composables'
import type { ConversationResponse } from './model.type'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'
import { defineAsyncComponent } from 'vue'

const router = useRouter()
const overlay = useOverlay()
const { conversations, loading, loadConversations, findOrCreateConversation } = useConversations()

const currentUser = getCurrentUser()
const selectedConversationId = ref<string | null>(null)

// Lazy load create chat modal
// @ts-expect-error - Vue SFC import
const LazyCreateChatModal = defineAsyncComponent(
  () => import('./components/CreateChatModal.vue'),
)

/**
 * Load conversations on mount
 */
onMounted(async () => {
  if (currentUser?.id) {
    await loadConversations(currentUser.id)
  }
})

/**
 * Format last message time
 */
const formatLastMessageTime = (timeString?: string | null): string => {
  if (!timeString) return 'No messages'

  const date = new Date(timeString)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 1) return 'Just now'
  if (diffMins < 60) return `${diffMins}m ago`
  if (diffHours < 24) return `${diffHours}h ago`
  if (diffDays < 7) return `${diffDays}d ago`

  return date.toLocaleDateString()
}

/**
 * Sort conversations by last message time (most recent first)
 */
const sortedConversations = computed(() => {
  return [...conversations.value].sort((a, b) => {
    const timeA = a.lastMessageTime ? new Date(a.lastMessageTime).getTime() : 0
    const timeB = b.lastMessageTime ? new Date(b.lastMessageTime).getTime() : 0
    return timeB - timeA // Most recent first
  })
})

/**
 * Open chat for a conversation
 */
const openChat = (conversation: ConversationResponse) => {
  selectedConversationId.value = conversation.conversationId
  router.push({
    name: 'communication-chat',
    params: { conversationId: conversation.conversationId },
    query: { partnerId: conversation.partnerId },
  })
}

/**
 * Open create chat modal
 */
const openCreateChat = async () => {
  const modal = overlay.create(LazyCreateChatModal)
  const instance = modal.open({ currentUserId: currentUser?.id || '' })
  const result = await instance.result as { partnerId: string } | null

  if (result && result.partnerId && currentUser?.id) {
    // Find or create conversation
    const conversation = await findOrCreateConversation(currentUser.id, result.partnerId)
    if (conversation) {
      // Reload conversations to get updated list
      await loadConversations(currentUser.id)
      // Open the new conversation
      openChat(conversation)
    }
  }
}

</script>

<template>
  <div class="flex h-screen">
    <!-- Conversations List -->
    <div class="w-1/3 border-r border-gray-200 flex flex-col">
      <div class="p-4 border-b border-gray-200 flex items-center justify-between">
        <h1 class="text-xl font-semibold">Conversations</h1>
        <UButton
          icon="i-heroicons-plus"
          size="sm"
          variant="soft"
          color="primary"
          @click="openCreateChat"
          title="Start new conversation"
        >
          New Chat
        </UButton>
      </div>

      <div v-if="loading" class="flex items-center justify-center h-full">
        <UIcon name="i-heroicons-arrow-path" class="animate-spin text-2xl" />
      </div>

      <div v-else-if="sortedConversations.length === 0" class="flex items-center justify-center h-full">
        <div class="text-center text-gray-500">
          <p>No conversations yet</p>
          <UButton
            class="mt-4"
            icon="i-heroicons-plus"
            variant="soft"
            color="primary"
            @click="openCreateChat"
          >
            Start a conversation
          </UButton>
        </div>
      </div>

      <div v-else class="flex-1 overflow-y-auto">
        <div
          v-for="conversation in sortedConversations"
          :key="conversation.conversationId"
          class="p-4 border-b border-gray-100 cursor-pointer hover:bg-gray-50 transition-colors"
          :class="{ 'bg-blue-50': selectedConversationId === conversation.conversationId }"
          @click="openChat(conversation)"
        >
          <div class="flex items-center space-x-3">
            <div class="relative">
              <div
                class="w-12 h-12 rounded-full bg-gray-300 flex items-center justify-center text-gray-600 font-semibold"
              >
                {{ conversation.partnerName.charAt(0).toUpperCase() }}
              </div>
              <!-- Online status indicator -->
              <div
                v-if="conversation.isOnline !== null"
                class="absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-white"
                :class="conversation.isOnline ? 'bg-green-500' : 'bg-gray-400'"
                :title="conversation.isOnline ? 'Online' : 'Offline'"
              />
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between">
                <div class="flex flex-col">
                  <p class="text-sm font-medium text-gray-900 truncate">
                    {{ conversation.partnerName }}
                  </p>
                  <p v-if="conversation.partnerUsername" class="text-xs text-gray-500 truncate">
                    @{{ conversation.partnerUsername }}
                  </p>
                </div>
                <span class="text-xs text-gray-500 ml-2">
                  {{ formatLastMessageTime(conversation.lastMessageTime) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div class="flex-1 flex items-center justify-center bg-gray-50">
      <div class="text-center">
        <UIcon name="i-heroicons-chat-bubble-left-right" class="text-6xl text-gray-300 mb-4" />
        <p class="text-gray-500">Select a conversation to start chatting</p>
      </div>
    </div>
  </div>
</template>
