/**
 * useWebSocket Composable
 *
 * WebSocket client for real-time chat communication
 */

import { ref, onUnmounted } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { useCookies } from '@vueuse/integrations/useCookies'
import type { MessageResponse, ChatMessagePayload } from '../model.type'

// STOMP client type (simplified)
interface StompClient {
  connect(headers: any, connectCallback: () => void, errorCallback?: (error: any) => void): void
  subscribe(destination: string, callback: (message: any) => void): any
  send(destination: string, headers: any, body: string): void
  disconnect(callback?: () => void): void
}

export function useWebSocket() {
  const toast = useToast()
  const cookie = useCookies(['jwt_token'])

  const connected = ref(false)
  const connecting = ref(false)
  const stompClient = ref<StompClient | null>(null)
  const subscriptions = ref<any[]>([])

  /**
   * Get WebSocket URL from environment
   */
  const getWebSocketUrl = (): string => {
    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:21500'
    // Convert http:// to ws:// or https:// to wss://
    const wsUrl = apiUrl.replace(/^http/, 'ws')
    return `${wsUrl}/ws`
  }

  /**
   * Connect to WebSocket
   */
  const connect = async (userId: string, onMessageReceived?: (message: MessageResponse) => void) => {
    if (connected.value || connecting.value) {
      return
    }

    connecting.value = true

    try {
      // Dynamically import SockJS and STOMP client
      const SockJS = (await import('sockjs-client')).default
      const { Client } = await import('@stomp/stompjs')

      const token = cookie.get('jwt_token')
      if (!token) {
        toast.add({
          title: 'Error',
          description: 'No authentication token found',
          color: 'error',
        })
        connecting.value = false
        return
      }

      const socket = new SockJS(getWebSocketUrl())
      const client = new Client({
        webSocketFactory: () => socket as any,
        connectHeaders: {
          Authorization: `Bearer ${userId}`, // Communication service expects userId in Bearer token
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        debug: (str) => {
          console.log('STOMP:', str)
        },
        onConnect: () => {
          console.log('WebSocket connected')
          connected.value = true
          connecting.value = false

          // Subscribe to user's message queue
          if (onMessageReceived && client) {
            const subscription = client.subscribe(`/user/${userId}/queue/messages`, (message) => {
              try {
                const messageData: MessageResponse = JSON.parse(message.body)
                onMessageReceived(messageData)
              } catch (error) {
                console.error('Failed to parse message:', error)
              }
            })
            subscriptions.value.push(subscription)
          }
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame)
          connecting.value = false
          connected.value = false
          toast.add({
            title: 'Connection Error',
            description: frame.headers?.['message'] || 'Failed to connect to chat server',
            color: 'error',
          })
        },
        onWebSocketClose: () => {
          console.log('WebSocket disconnected')
          connected.value = false
          connecting.value = false
        },
        onDisconnect: () => {
          console.log('STOMP disconnected')
          connected.value = false
          connecting.value = false
        },
      })

      client.activate()
      stompClient.value = client as any
    } catch (error) {
      console.error('Failed to connect WebSocket:', error)
      connecting.value = false
      toast.add({
        title: 'Connection Error',
        description: 'Failed to initialize WebSocket connection',
        color: 'error',
      })
    }
  }

  /**
   * Send a message via WebSocket
   */
  const sendMessage = (payload: ChatMessagePayload) => {
    if (!stompClient.value || !connected.value) {
      toast.add({
        title: 'Error',
        description: 'Not connected to chat server',
        color: 'error',
      })
      return false
    }

    try {
      const client = stompClient.value as any
      if (client.publish) {
        // @stomp/stompjs v7 API
        client.publish({
          destination: '/app/chat.send',
          body: JSON.stringify(payload),
        })
      } else if (client.send) {
        // Fallback for older API
        client.send('/app/chat.send', {}, JSON.stringify(payload))
      }
      return true
    } catch (error) {
      console.error('Failed to send message:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to send message',
        color: 'error',
      })
      return false
    }
  }

  /**
   * Disconnect from WebSocket
   */
  const disconnect = () => {
    if (stompClient.value) {
      // Unsubscribe from all subscriptions
      subscriptions.value.forEach((subscription) => {
        try {
          if (subscription && typeof subscription.unsubscribe === 'function') {
            subscription.unsubscribe()
          }
        } catch (error) {
          console.error('Failed to unsubscribe:', error)
        }
      })
      subscriptions.value = []

      // Disconnect client
      const client = stompClient.value as any
      if (client.deactivate) {
        // @stomp/stompjs v7 API
        client.deactivate().then(() => {
          console.log('WebSocket disconnected')
          connected.value = false
          connecting.value = false
        })
      } else if (client.disconnect) {
        // Fallback for older API
        client.disconnect(() => {
          console.log('WebSocket disconnected')
          connected.value = false
          connecting.value = false
        })
      }
      stompClient.value = null
    }
  }

  // Cleanup on unmount
  onUnmounted(() => {
    disconnect()
  })

  return {
    connected,
    connecting,
    connect,
    sendMessage,
    disconnect,
  }
}
