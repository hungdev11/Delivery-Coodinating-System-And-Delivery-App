<script setup lang="ts">
/**
 * Conversations List View
 *
 * View for listing and selecting conversations
 */

import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useConversations } from './composables'
import type { ConversationResponse } from './model.type'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'

const router = useRouter()
const { conversations, loading, loadConversations } = useConversations()

const currentUser = getCurrentUser()
const selectedConversationId = ref<string | null>(null)

/**
 * Load conversations on mount
 */
onMounted(async () => {
  if (currentUser?.id) {
    await loadConversations(currentUser.id)
  }
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

</script>

<template>
  <div class="flex h-screen">
    <!-- Conversations List -->
    <div class="w-1/3 border-r border-gray-200 flex flex-col">
      <div class="p-4 border-b border-gray-200">
        <h1 class="text-xl font-semibold">Conversations</h1>
      </div>

      <div v-if="loading" class="flex items-center justify-center h-full">
        <UIcon name="i-heroicons-arrow-path" class="animate-spin text-2xl" />
      </div>

      <div v-else-if="conversations.length === 0" class="flex items-center justify-center h-full">
        <div class="text-center text-gray-500">
          <p>No conversations yet</p>
        </div>
      </div>

      <div v-else class="flex-1 overflow-y-auto">
        <div
          v-for="conversation in conversations"
          :key="conversation.conversationId"
          class="p-4 border-b border-gray-100 cursor-pointer hover:bg-gray-50 transition-colors"
          :class="{ 'bg-blue-50': selectedConversationId === conversation.conversationId }"
          @click="openChat(conversation)"
        >
          <div class="flex items-center space-x-3">
            <div
              class="w-12 h-12 rounded-full bg-gray-300 flex items-center justify-center text-gray-600 font-semibold"
            >
              {{ conversation.partnerName.charAt(0).toUpperCase() }}
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between">
                <p class="text-sm font-medium text-gray-900 truncate">
                  {{ conversation.partnerName }}
                </p>
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
