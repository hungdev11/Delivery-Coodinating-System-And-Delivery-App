/**
 * Global Chat Store (Pinia)
 *
 * Manages all chat messages for all conversations in memory
 * Messages are kept in memory and synced via WebSocket
 */

import { defineStore } from 'pinia'
import type { MessageResponse, ConversationResponse } from '@/modules/Communication/model.type'

interface ChatStoreState {
  // Messages by conversation ID
  messages: Map<string, MessageResponse[]>
  // Conversations metadata
  conversations: Map<string, ConversationResponse>
  // Last message time per conversation
  lastMessageTime: Map<string, string>
  // Unread count per conversation
  unreadCount: Map<string, number>
  // Currently active conversation ID
  activeConversationId: string | null
}

export const useChatStore = defineStore('chat', {
  state: (): ChatStoreState => ({
    messages: new Map(),
    conversations: new Map(),
    lastMessageTime: new Map(),
    unreadCount: new Map(),
    activeConversationId: null,
  }),

  getters: {
    /**
     * Get messages for a conversation
     */
    getMessages: (state) => (conversationId: string): MessageResponse[] => {
      return state.messages.get(conversationId) || []
    },

    /**
     * Get conversation metadata
     */
    getConversation: (state) => (conversationId: string): ConversationResponse | undefined => {
      return state.conversations.get(conversationId)
    },

    /**
     * Get unread count for a conversation
     */
    getUnreadCount: (state) => (conversationId: string): number => {
      return state.unreadCount.get(conversationId) || 0
    },

    /**
     * Get last message time for a conversation
     */
    getLastMessageTime: (state) => (conversationId: string): string | null => {
      return state.lastMessageTime.get(conversationId) || null
    },

    /**
     * Check if conversation has messages
     */
    hasMessages: (state) => (conversationId: string): boolean => {
      const messages = state.messages.get(conversationId)
      return messages ? messages.length > 0 : false
    },
  },

  actions: {
    /**
     * Set messages for a conversation (initial load)
     */
    setMessages(conversationId: string, messages: MessageResponse[]) {
      // Sort messages by sentAt (oldest first)
      const sortedMessages = [...messages].sort(
        (a, b) => new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime(),
      )
      this.messages.set(conversationId, sortedMessages)

      // Update last message time
      if (sortedMessages.length > 0) {
        const lastMessage = sortedMessages[sortedMessages.length - 1]
        this.lastMessageTime.set(conversationId, lastMessage.sentAt)
      }
    },

    /**
     * Add a new message to a conversation
     */
    addMessage(conversationId: string, message: MessageResponse) {
      const messages = this.messages.get(conversationId) || []

      // Check if message already exists (avoid duplicates)
      const exists = messages.some((m) => m.id === message.id)
      if (exists) {
        return
      }

      // Remove optimistic message with same content if exists
      const optimisticIndex = messages.findIndex(
        (m) =>
          m.id.startsWith('temp-') &&
          m.content === message.content &&
          m.senderId === message.senderId,
      )
      if (optimisticIndex !== -1) {
        messages.splice(optimisticIndex, 1)
      }

      // Insert message in chronological order
      const sentAtValue = message.sentAt || new Date().toISOString()
      const messageTime = new Date(sentAtValue).getTime()

      let insertIndex = messages.length
      for (let i = 0; i < messages.length; i++) {
        const currentSentAt = messages[i].sentAt || new Date().toISOString()
        const currentTime = new Date(currentSentAt).getTime()
        if (messageTime < currentTime) {
          insertIndex = i
          break
        }
      }

      messages.splice(insertIndex, 0, message)
      this.messages.set(conversationId, messages)

      // Update last message time
      if (!this.lastMessageTime.get(conversationId) || messageTime > new Date(this.lastMessageTime.get(conversationId) || 0).getTime()) {
        this.lastMessageTime.set(conversationId, sentAtValue)
      }

      // Increment unread count if not active conversation
      if (conversationId !== this.activeConversationId) {
        const currentUnread = this.unreadCount.get(conversationId) || 0
        this.unreadCount.set(conversationId, currentUnread + 1)
      }
    },

    /**
     * Prepend older messages to a conversation (for infinite scroll)
     * Merges new messages at the beginning, avoiding duplicates
     */
    prependMessages(conversationId: string, newMessages: MessageResponse[]) {
      if (!conversationId || !newMessages || newMessages.length === 0) return

      const existingMessages = this.messages.get(conversationId) || []
      const existingIds = new Set(existingMessages.map((m) => m.id))

      // Filter out duplicates and sort new messages
      const uniqueNewMessages = newMessages
        .filter((m) => !existingIds.has(m.id))
        .sort((a, b) => {
          const timeA = new Date(a.sentAt || 0).getTime()
          const timeB = new Date(b.sentAt || 0).getTime()
          return timeA - timeB
        })

      if (uniqueNewMessages.length > 0) {
        // Prepend new messages at the beginning (oldest first)
        const mergedMessages = [...uniqueNewMessages, ...existingMessages]
        this.messages.set(conversationId, mergedMessages)
        console.log(
          `📦 Prepended ${uniqueNewMessages.length} messages to conversation ${conversationId}, total: ${mergedMessages.length}`,
        )
      }
    },

    /**
     * Update a message (e.g., status update)
     */
    updateMessage(conversationId: string, messageId: string, updates: Partial<MessageResponse>) {
      const messages = this.messages.get(conversationId) || []
      const messageIndex = messages.findIndex((m) => m.id === messageId)

      if (messageIndex !== -1) {
        messages[messageIndex] = { ...messages[messageIndex], ...updates }
        this.messages.set(conversationId, messages)
      }
    },

    /**
     * Set conversation metadata
     */
    setConversation(conversation: ConversationResponse) {
      this.conversations.set(conversation.conversationId, conversation)
    },

    /**
     * Set multiple conversations
     */
    setConversations(conversations: ConversationResponse[]) {
      conversations.forEach((conv) => {
        this.conversations.set(conv.conversationId, conv)
        // Update unread count from conversation
        if (conv.unreadCount) {
          this.unreadCount.set(conv.conversationId, conv.unreadCount)
        }
      })
    },

    /**
     * Set active conversation
     */
    setActiveConversation(conversationId: string | null) {
      this.activeConversationId = conversationId
      // Clear unread count when conversation becomes active
      if (conversationId) {
        this.unreadCount.set(conversationId, 0)
      }
    },

    /**
     * Mark conversation as read
     */
    markAsRead(conversationId: string) {
      this.unreadCount.set(conversationId, 0)
    },

    /**
     * Clear messages for a conversation
     */
    clearConversation(conversationId: string) {
      this.messages.delete(conversationId)
      this.lastMessageTime.delete(conversationId)
    },

    /**
     * Clear all data
     */
    clearAll() {
      this.messages.clear()
      this.conversations.clear()
      this.lastMessageTime.clear()
      this.unreadCount.clear()
      this.activeConversationId = null
    },
  },
})
