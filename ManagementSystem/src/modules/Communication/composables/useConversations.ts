/**
 * useConversations Composable
 *
 * Business logic for conversation management
 */

import { ref } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { getConversations, getConversationByUsers, getMessages } from '../api'
import type { ConversationResponse, MessageResponse, PageResponse } from '../model.type'

export function useConversations() {
  const toast = useToast()

  const conversations = ref<ConversationResponse[]>([])
  const currentConversation = ref<ConversationResponse | null>(null)
  const messages = ref<MessageResponse[]>([])
  const loading = ref(false)
  const messagesLoading = ref(false)

  // Pagination state
  const currentPage = ref(0)
  const hasMoreMessages = ref(true)
  const isLoadingMore = ref(false)

  // Request queue to ensure sequential processing
  let currentRequest: Promise<unknown> | null = null

  // Track last message time for loading missed messages on reconnect
  const lastMessageTime = ref<string | null>(null)

  /**
   * Queue a request to ensure sequential processing
   * Waits for previous request to complete before executing next
   */
  const queueRequest = async <T>(requestFn: () => Promise<T>): Promise<T> => {
    // Wait for current request to complete
    if (currentRequest) {
      await currentRequest.catch(() => {
        // Ignore errors from previous request
      })
    }

    // Execute new request
    const requestPromise = requestFn()
    currentRequest = requestPromise as Promise<unknown>
    try {
      const result = await requestPromise
      return result
    } finally {
      currentRequest = null
    }
  }

  /**
   * Load conversations for a user
   * @param userId - User ID
   * @param includeMessages - Whether to include chat history (default: true)
   */
  const loadConversations = async (userId: string, includeMessages: boolean = true) => {
    return queueRequest(async () => {
      loading.value = true
      try {
        const response = await getConversations(userId, includeMessages, 50)
        // Handle both wrapped response and direct array
        let conversationsArray: ConversationResponse[] = []
        if (Array.isArray(response)) {
          conversationsArray = response
        } else if (response.result && Array.isArray(response.result)) {
          conversationsArray = response.result
        } else {
          conversationsArray = []
        }

        conversations.value = conversationsArray

        // If messages are included, add them to store
        if (includeMessages) {
          const { useChatStore } = await import('@/stores/chatStore')
          const chatStore = useChatStore()

          conversationsArray.forEach((conv) => {
            // Update conversation metadata in store
            chatStore.setConversation(conv)

            // Add messages to store if included
            if (conv.messages && conv.messages.length > 0) {
              // Messages come sorted DESC (newest first) from backend
              // Reverse to display oldest first
              const sortedMessages = [...conv.messages].reverse()
              chatStore.setMessages(conv.conversationId, sortedMessages)
              console.log(`📦 Loaded ${sortedMessages.length} messages for conversation ${conv.conversationId} from API`)
            }
          })
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
    })
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
   * Load messages for a conversation (Initial load - page 0)
   */
  const loadMessages = async (
    conversationId: string,
    userId: string,
    page: number = 0,
    size: number = 30,
  ) => {
    messagesLoading.value = true
    currentPage.value = 0
    hasMoreMessages.value = true

    try {
      const response = await getMessages(conversationId, userId, page, size)

      // Handle different response formats
      let messagesArray: MessageResponse[] = []
      let pageData: PageResponse<MessageResponse> | null = null

      // Check if response has result property (wrapped in IApiResponse)
      if ('result' in response && response.result) {
        pageData = response.result as PageResponse<MessageResponse>
        if (pageData.content && Array.isArray(pageData.content)) {
          messagesArray = pageData.content
        }
      }
      // Check if response is direct PageResponse format
      else if (
        'content' in response &&
        Array.isArray((response as unknown as PageResponse<MessageResponse>).content)
      ) {
        pageData = response as unknown as PageResponse<MessageResponse>
        messagesArray = pageData.content
      }
      // Fallback: direct array
      else if (Array.isArray(response)) {
        messagesArray = response
      }

      // Update pagination state
      if (pageData) {
        hasMoreMessages.value = !pageData.last
        console.log(
          `📜 Loaded page 0: ${messagesArray.length} messages, hasMore=${hasMoreMessages.value}`,
        )
      }

      // Messages come sorted DESC (newest first) from backend
      // Reverse to display oldest first (scroll down to see new messages)
      messages.value = [...messagesArray].reverse()

      // Update lastMessageTime to the latest message
      if (messagesArray.length > 0) {
        // Messages come sorted DESC (newest first), so first one is latest
        const latestMessage = messagesArray[0]
        lastMessageTime.value = latestMessage.sentAt
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
   * Load more messages (Infinite scroll - pagination)
   * Returns the newly loaded messages (oldest first, already reversed)
   */
  const loadMoreMessages = async (
    conversationId: string,
    userId: string,
    size: number = 30,
  ): Promise<MessageResponse[]> => {
    if (isLoadingMore.value || !hasMoreMessages.value) {
      console.log(
        '⏸️ Skip loading more: isLoading=' +
          isLoadingMore.value +
          ', hasMore=' +
          hasMoreMessages.value,
      )
      return []
    }

    isLoadingMore.value = true
    const nextPage = currentPage.value + 1

    console.log(`📜 Loading more messages (page ${nextPage})...`)

    try {
      const response = await getMessages(conversationId, userId, nextPage, size)

      // Handle different response formats
      let messagesArray: MessageResponse[] = []
      let pageData: PageResponse<MessageResponse> | null = null

      if ('result' in response && response.result) {
        pageData = response.result as PageResponse<MessageResponse>
        if (pageData.content && Array.isArray(pageData.content)) {
          messagesArray = pageData.content
        }
      } else if (
        'content' in response &&
        Array.isArray((response as unknown as PageResponse<MessageResponse>).content)
      ) {
        pageData = response as unknown as PageResponse<MessageResponse>
        messagesArray = pageData.content
      } else if (Array.isArray(response)) {
        messagesArray = response
      }

      if (messagesArray.length > 0) {
        // Update pagination state
        currentPage.value = nextPage
        if (pageData) {
          hasMoreMessages.value = !pageData.last
        }

        // Messages come sorted DESC (newest first) from backend
        // Reverse to display oldest first, then prepend older messages at the BEGINNING
        const reversedNewMessages = [...messagesArray].reverse()
        messages.value = [...reversedNewMessages, ...messages.value]

        console.log(
          `✅ Loaded ${messagesArray.length} more messages, total: ${messages.value.length}, hasMore=${hasMoreMessages.value}`,
        )

        // Return the newly loaded messages (oldest first, already reversed)
        return reversedNewMessages
      } else {
        hasMoreMessages.value = false
        console.log('📭 No more messages to load')
        return []
      }
    } catch (error) {
      console.error('Failed to load more messages:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to load more messages',
        color: 'error',
      })
      return []
    } finally {
      isLoadingMore.value = false
    }
  }

  /**
   * Add a new message to the current conversation
   * Messages are kept sorted chronologically (oldest first, newest last)
   */
  const addMessage = (message: MessageResponse) => {
    // Check if message already exists (avoid duplicates)
    const exists = messages.value.some((m) => m.id === message.id)
    if (exists) {
      return
    }

    // Remove optimistic message with same content if exists
    const optimisticIndex = messages.value.findIndex(
      (m) =>
        m.id.startsWith('temp-') &&
        m.content === message.content &&
        m.senderId === message.senderId,
    )
    if (optimisticIndex !== -1) {
      messages.value.splice(optimisticIndex, 1)
    }

    // If sentAt is null, use current time (for messages just sent)
    const sentAtValue = message.sentAt || new Date().toISOString()
    const messageTime = new Date(sentAtValue).getTime()

    // Insert message in chronological order (oldest first, newest last)
    let insertIndex = messages.value.length

    for (let i = 0; i < messages.value.length; i++) {
      const currentSentAt = messages.value[i].sentAt || new Date().toISOString()
      const currentTime = new Date(currentSentAt).getTime()
      if (messageTime < currentTime) {
        insertIndex = i
        break
      }
    }

    // Update sentAt if it was null before inserting
    if (!message.sentAt) {
      message.sentAt = sentAtValue
    }

    messages.value.splice(insertIndex, 0, message)

    // Update lastMessageTime if this message is newer
    if (!lastMessageTime.value || messageTime > new Date(lastMessageTime.value).getTime()) {
      lastMessageTime.value = sentAtValue
    }
  }

  /**
   * Load missed messages after reconnection
   * Loads messages that arrived after lastMessageTime
   */
  const loadMissedMessages = async (conversationId: string, userId: string) => {
    return queueRequest(async () => {
      if (!lastMessageTime.value) {
        // If no lastMessageTime, just reload all messages
        await loadMessages(conversationId, userId)
        // Update lastMessageTime to the latest message
        if (messages.value.length > 0) {
          const latestMessage = messages.value[messages.value.length - 1]
          lastMessageTime.value = latestMessage.sentAt
        }
        return
      }

      try {
        // Load latest messages
        const response = await getMessages(conversationId, userId, 0, 50)

        // Handle different response formats
        let messagesArray: MessageResponse[] = []

        if ('result' in response && response.result) {
          const pageData = response.result as PageResponse<MessageResponse>
          if (pageData.content && Array.isArray(pageData.content)) {
            messagesArray = pageData.content
          }
        } else if (
          'content' in response &&
          Array.isArray((response as unknown as PageResponse<MessageResponse>).content)
        ) {
          const pageData = response as unknown as PageResponse<MessageResponse>
          messagesArray = pageData.content
        } else if (Array.isArray(response)) {
          messagesArray = response
        }

        // Filter messages that arrived after lastMessageTime
        const lastTime = new Date(lastMessageTime.value).getTime()
        const missedMessages = messagesArray.filter((msg) => {
          if (!msg.sentAt) return false // Skip messages with null sentAt
          const msgTime = new Date(msg.sentAt).getTime()
          return msgTime > lastTime
        })

        // Add missed messages to the current messages array
        missedMessages.forEach((msg) => {
          addMessage(msg)
        })

        // Update lastMessageTime to the latest message
        if (messagesArray.length > 0) {
          // Messages come sorted DESC (newest first), so first one is latest
          const latestMessage = messagesArray[0]
          lastMessageTime.value = latestMessage.sentAt
        }
      } catch (error) {
        console.error('Failed to load missed messages:', error)
        // On error, just reload all messages
        await loadMessages(conversationId, userId)
      }
    })
  }

  /**
   * Clear current conversation
   */
  const clearConversation = () => {
    currentConversation.value = null
    messages.value = []
    lastMessageTime.value = null
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
    loadMoreMessages, // NEW: Infinite scroll
    loadMissedMessages,
    addMessage,
    clearConversation,
    lastMessageTime,
    // Pagination
    currentPage,
    hasMoreMessages,
    isLoadingMore,
  }
}
