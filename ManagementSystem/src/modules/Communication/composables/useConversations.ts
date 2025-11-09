/**
 * useConversations Composable
 *
 * Business logic for conversation management
 */

import { ref } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import {
  getConversations,
  getConversationByUsers,
  getMessages,
  type ConversationResponse,
  type MessageResponse,
} from '../api'

export function useConversations() {
  const toast = useToast()

  const conversations = ref<ConversationResponse[]>([])
  const currentConversation = ref<ConversationResponse | null>(null)
  const messages = ref<MessageResponse[]>([])
  const loading = ref(false)
  const messagesLoading = ref(false)

  /**
   * Load conversations for a user
   */
  const loadConversations = async (userId: string) => {
    loading.value = true
    try {
      const response = await getConversations(userId)
      if (response.result) {
        conversations.value = response.result
      }
    } catch (error) {
      console.error('Failed to load conversations:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to load conversations',
        color: 'error',
      })
    } finally {
      loading.value = false
    }
  }

  /**
   * Find or create conversation between two users
   */
  const findOrCreateConversation = async (userId1: string, userId2: string) => {
    loading.value = true
    try {
      const response = await getConversationByUsers(userId1, userId2)
      if (response.result) {
        currentConversation.value = response.result
        return response.result
      }
      return null
    } catch (error) {
      console.error('Failed to find or create conversation:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to find or create conversation',
        color: 'error',
      })
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * Load messages for a conversation
   */
  const loadMessages = async (
    conversationId: string,
    userId: string,
    page: number = 0,
    size: number = 30,
  ) => {
    messagesLoading.value = true
    try {
      const response = await getMessages(conversationId, userId, page, size)
      if (response.result) {
        // Reverse messages to show newest first (but display oldest first in UI)
        messages.value = response.result.content.reverse()
      }
    } catch (error) {
      console.error('Failed to load messages:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to load messages',
        color: 'error',
      })
    } finally {
      messagesLoading.value = false
    }
  }

  /**
   * Add a new message to the current conversation
   */
  const addMessage = (message: MessageResponse) => {
    messages.value.push(message)
  }

  /**
   * Clear current conversation
   */
  const clearConversation = () => {
    currentConversation.value = null
    messages.value = []
  }

  return {
    conversations,
    currentConversation,
    messages,
    loading,
    messagesLoading,
    loadConversations,
    findOrCreateConversation,
    loadMessages,
    addMessage,
    clearConversation,
  }
}
