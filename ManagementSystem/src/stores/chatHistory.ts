/**
 * Chat History Store (Pinia)
 * 
 * Manages chat messages with localStorage persistence
 * Syncs with server on load and handles offline scenarios
 */

import { defineStore } from 'pinia'
import type { MessageResponse } from '@/modules/Communication/model.type'

interface ChatHistoryState {
  messages: Map<string, MessageResponse[]> // conversationId -> messages
  lastSync: Map<string, number> // conversationId -> timestamp
}

export const useChatHistoryStore = defineStore('chatHistory', {
  state: (): ChatHistoryState => ({
    messages: new Map(),
    lastSync: new Map(),
  }),

  getters: {
    /**
     * Get messages for a conversation
     */
    getConversationMessages: (state) => (conversationId: string) => {
      return state.messages.get(conversationId) || []
    },

    /**
     * Get last sync time for a conversation
     */
    getLastSync: (state) => (conversationId: string) => {
      return state.lastSync.get(conversationId) || 0
    },

    /**
     * Check if conversation needs sync (older than 5 minutes)
     */
    needsSync: (state) => (conversationId: string) => {
      const lastSync = state.lastSync.get(conversationId) || 0
      const fiveMinutes = 5 * 60 * 1000
      return Date.now() - lastSync > fiveMinutes
    },
  },

  actions: {
    /**
     * Load messages from localStorage
     */
    loadFromStorage(conversationId: string) {
      try {
        const key = `chat_${conversationId}`
        const stored = localStorage.getItem(key)
        if (stored) {
          const data = JSON.parse(stored)
          this.messages.set(conversationId, data.messages || [])
          this.lastSync.set(conversationId, data.lastSync || 0)
          return data.messages || []
        }
      } catch (error) {
        console.error('Failed to load chat history from localStorage:', error)
      }
      return []
    },

    /**
     * Save messages to localStorage
     */
    saveToStorage(conversationId: string) {
      try {
        const key = `chat_${conversationId}`
        const data = {
          messages: this.messages.get(conversationId) || [],
          lastSync: Date.now(),
        }
        localStorage.setItem(key, JSON.stringify(data))
        this.lastSync.set(conversationId, Date.now())
      } catch (error) {
        console.error('Failed to save chat history to localStorage:', error)
      }
    },

    /**
     * Set messages for a conversation
     */
    setMessages(conversationId: string, messages: MessageResponse[]) {
      this.messages.set(conversationId, messages)
      this.saveToStorage(conversationId)
    },

    /**
     * Add message to conversation
     */
    addMessage(conversationId: string, message: MessageResponse) {
      const messages = this.messages.get(conversationId) || []
      
      // Check if message already exists (by ID)
      const existingIndex = messages.findIndex(m => m.id === message.id)
      if (existingIndex !== -1) {
        // Update existing message
        messages[existingIndex] = message
      } else {
        // Add new message
        messages.push(message)
      }
      
      // Sort by timestamp
      messages.sort((a, b) => 
        new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime()
      )
      
      this.messages.set(conversationId, messages)
      this.saveToStorage(conversationId)
    },

    /**
     * Update message (e.g., status update)
     */
    updateMessage(conversationId: string, messageId: string, updates: Partial<MessageResponse>) {
      const messages = this.messages.get(conversationId) || []
      const messageIndex = messages.findIndex(m => m.id === messageId)
      
      if (messageIndex !== -1) {
        messages[messageIndex] = { ...messages[messageIndex], ...updates }
        this.messages.set(conversationId, messages)
        this.saveToStorage(conversationId)
      }
    },

    /**
     * Clear conversation messages
     */
    clearConversation(conversationId: string) {
      this.messages.delete(conversationId)
      this.lastSync.delete(conversationId)
      
      try {
        const key = `chat_${conversationId}`
        localStorage.removeItem(key)
      } catch (error) {
        console.error('Failed to clear conversation from localStorage:', error)
      }
    },

    /**
     * Clear all messages
     */
    clearAll() {
      this.messages.clear()
      this.lastSync.clear()
      
      try {
        // Clear all chat_* keys from localStorage
        const keys = Object.keys(localStorage)
        keys.forEach(key => {
          if (key.startsWith('chat_')) {
            localStorage.removeItem(key)
          }
        })
      } catch (error) {
        console.error('Failed to clear all chats from localStorage:', error)
      }
    },
  },
})
