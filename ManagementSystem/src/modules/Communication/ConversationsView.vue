<script setup lang="ts">
/**
 * Conversations List View
 *
 * View for listing and selecting conversations
 */

import { onMounted, onUnmounted, ref, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import { useConversations, useGlobalChat, type GlobalChatListener } from './composables'
import type { ConversationResponse, MessageResponse } from './model.type'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'
import { defineAsyncComponent } from 'vue'
import { useChatStore } from '@/stores/chatStore'

const router = useRouter()
const route = useRoute()
const overlay = useOverlay()
const { conversations, loading, loadConversations, findOrCreateConversation } = useConversations()

const currentUser = getCurrentUser()
const selectedConversationId = ref<string | null>(null)
const isMobileView = ref(false)
const showConversationList = ref(true)

// Initialize global chat to listen for messages even when not in chat view
const globalChat = useGlobalChat()

// Lazy load create chat modal
const LazyCreateChatModal = defineAsyncComponent(() => import('./components/CreateChatModal.vue'))

/**
 * Load conversations on mount
 */
const isChatActive = computed(() => route.name === 'communication-chat')

const syncListVisibility = () => {
  if (!isMobileView.value) {
    showConversationList.value = true
    return
  }
  showConversationList.value = !isChatActive.value
}

const updateResponsiveState = () => {
  if (typeof window === 'undefined') return
  isMobileView.value = window.innerWidth < 1024
  syncListVisibility()
}

const handleRouteChange = () => {
  selectedConversationId.value = (route.params.conversationId as string) || null
  syncListVisibility()
}

// Global chat listener to receive messages even when not in chat view
const globalChatListener: GlobalChatListener = {
  onMessageReceived: (message: MessageResponse) => {
    // Push message to conversation
    const chatStore = useChatStore()
    chatStore.addMessage(message.conversationId || '', message)
    // Reload conversations list to get updated last message (without messages to avoid heavy load)
    // if (currentUser?.id) {
    //   loadConversations(currentUser.id, false).catch(console.error)
    // }
  },
  onNotificationReceived: () => {
    // Reload conversations on notification (without messages to avoid heavy load)
    // if (currentUser?.id) {
    //   loadConversations(currentUser.id, false).catch(console.error)
    // }
  },
  onUserStatusUpdate: (userId: string, isOnline: boolean) => {
    // Update online status in conversations list
    const conversation = conversations.value.find((c) => c.partnerId === userId)
    if (conversation) {
      // Update in store
      const chatStore = useChatStore()
      chatStore.setConversation({
        ...conversation,
        isOnline,
      })
      // Update in local list
      const index = conversations.value.findIndex((c) => c.partnerId === userId)
      if (index !== -1) {
        conversations.value[index] = {
          ...conversations.value[index],
          isOnline,
        }
      }
    }
  },
}

onMounted(async () => {
  if (currentUser?.id) {
    // Load conversations with messages included (default: true)
    await loadConversations(currentUser.id, true)
    // Initialize global chat listener
    globalChat.addListener(globalChatListener)
    // Ensure global chat is initialized
    if (!globalChat.connected.value && currentUser.id) {
      await globalChat.initialize()
    }
  }
  updateResponsiveState()
  window.addEventListener('resize', updateResponsiveState)
  handleRouteChange()
})

onUnmounted(() => {
  window.removeEventListener('resize', updateResponsiveState)
  // Remove global chat listener
  globalChat.removeListener(globalChatListener)
})

watch(
  () => route.fullPath,
  () => handleRouteChange(),
)

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
  // Force navigation even if same conversation
  const targetConversationId = conversation.conversationId
  const currentRouteId = route.params.conversationId as string

  // If clicking the same conversation, force reload by navigating away first
  if (targetConversationId === currentRouteId) {
    // Navigate to conversations list first, then to chat (forces reload)
    router.push({ name: 'communication-conversations' }).then(() => {
      nextTick(() => {
        router.push({
          name: 'communication-chat',
          params: { conversationId: targetConversationId },
          query: { partnerId: conversation.partnerId },
        })
      })
    })
  } else {
    // Normal navigation
    selectedConversationId.value = targetConversationId
    router.push({
      name: 'communication-chat',
      params: { conversationId: targetConversationId },
      query: { partnerId: conversation.partnerId },
    })
  }

  if (isMobileView.value) {
    showConversationList.value = false
  }
}

/**
 * Open create chat modal
 */
const openCreateChat = async () => {
  const modal = overlay.create(LazyCreateChatModal)
  const instance = modal.open({ currentUserId: currentUser?.id || '' })
  const result = (await instance.result) as { partnerId: string } | null

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
  <div class="flex h-full">
    <!-- Conversations List -->
    <div
      v-if="!isMobileView || showConversationList"
      class="w-full lg:w-1/3 border-r border-gray-200 flex flex-col"
    >
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

      <div
        v-else-if="sortedConversations.length === 0"
        class="flex items-center justify-center h-full"
      >
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
          :class="{
            'bg-primary-50 dark:bg-primary-900/20':
              selectedConversationId === conversation.conversationId,
          }"
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
                :class="conversation.isOnline ? 'bg-success-500' : 'bg-neutral-400'"
                :title="conversation.isOnline ? 'Online' : 'Offline'"
              />
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between">
                <div class="flex flex-col flex-1 min-w-0">
                  <p class="text-sm font-medium text-gray-900 truncate">
                    {{ conversation.partnerName }}
                  </p>
                  <p v-if="conversation.partnerUsername" class="text-xs text-gray-500 truncate">
                    @{{ conversation.partnerUsername }}
                  </p>
                  <p
                    v-if="conversation.lastMessageContent"
                    class="text-xs text-gray-600 truncate mt-1"
                  >
                    {{ conversation.lastMessageContent }}
                  </p>
                  <p v-else class="text-xs text-gray-400 italic mt-1">No messages yet</p>
                </div>
                <div class="flex flex-col items-end ml-2">
                  <span class="text-xs text-gray-500">
                    {{ formatLastMessageTime(conversation.lastMessageTime) }}
                  </span>
                  <UBadge
                    v-if="conversation.unreadCount && conversation.unreadCount > 0"
                    color="primary"
                    variant="solid"
                    size="xs"
                    class="mt-1"
                  >
                    {{ conversation.unreadCount }}
                  </UBadge>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Chat Container -->
    <div v-if="!isMobileView || !showConversationList" class="flex-1 flex flex-col bg-gray-50">
      <RouterView v-slot="{ Component }">
        <component v-if="Component" :is="Component" />
        <div v-else class="flex-1 flex items-center justify-center bg-gray-50">
          <div class="text-center">
            <UIcon name="i-heroicons-chat-bubble-left-right" class="text-6xl text-gray-300 mb-4" />
            <p class="text-gray-500">Select a conversation to start chatting</p>
          </div>
        </div>
      </RouterView>
    </div>
  </div>
</template>
