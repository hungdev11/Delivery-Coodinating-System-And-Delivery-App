/**
 * useWebSocket Composable
 *
 * WebSocket client for real-time chat communication
 */

import { ref, onUnmounted } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { useCookies } from '@vueuse/integrations/useCookies'
import type { MessageResponse, ChatMessagePayload } from '../model.type'
import { ErrorLog } from '@/common/utils/debug'

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
  const reconnectAttempts = ref(0)
  const maxReconnectAttempts = 10
  const reconnectTimer = ref<number | null>(null)
  const keepAliveTimer = ref<number | null>(null)
  const currentUserId = ref<string | null>(null)
  const messageCallback = ref<((message: MessageResponse) => void) | null>(null)
  const onReconnectCallback = ref<(() => void) | null>(null)
  const statusUpdateCallback = ref<((statusUpdate: any) => void) | null>(null)
  const typingCallback = ref<((typingIndicator: any) => void) | null>(null)
  const notificationCallback = ref<((notification: any) => void) | null>(null)
  const proposalUpdateCallback = ref<((proposalUpdate: any) => void) | null>(null)

  /**
   * Get WebSocket URL from environment or auto-detect from current domain
   * Note: For SockJS connection, use HTTP/HTTPS URL (e.g., https://domain/ws)
   * SockJS client will automatically append /websocket and upgrade to WebSocket
   * For native WebSocket, use the full path: wss://domain/ws/websocket
   */
  const getWebSocketUrl = (): string => {
    // Check for explicit WebSocket URL in environment
    const wsUrl = import.meta.env.VITE_WS_URL || import.meta.env.VITE_API_URL

    if (wsUrl) {
      // Handle legacy /api/ws path - convert to /ws
      if (wsUrl === '/api/ws' || wsUrl.endsWith('/api/ws')) {
        return `${window.location.protocol}//${window.location.host}/ws`
      }

      // If it's a relative path, build full URL from current domain
      if (wsUrl.startsWith('/')) {
        return `${window.location.protocol}//${window.location.host}${wsUrl}`
      }

      // If it's already an absolute URL (http:// or https://), use it directly
      if (wsUrl.startsWith('http://') || wsUrl.startsWith('https://')) {
        // Check if it contains /api/ws and convert to /ws
        if (wsUrl.includes('/api/ws')) {
          return wsUrl.replace('/api/ws', '/ws')
        }
        return wsUrl
      }

      // If it's ws:// or wss://, convert to http:// or https://
      if (wsUrl.startsWith('ws://')) {
        const httpUrl = wsUrl.replace(/^ws:/, 'http:')
        // Convert /api/ws to /ws if present
        return httpUrl.includes('/api/ws') ? httpUrl.replace('/api/ws', '/ws') : httpUrl
      }
      if (wsUrl.startsWith('wss://')) {
        const httpsUrl = wsUrl.replace(/^wss:/, 'https:')
        // Convert /api/ws to /ws if present
        return httpsUrl.includes('/api/ws') ? httpsUrl.replace('/api/ws', '/ws') : httpsUrl
      }
    }

    // Default: Use /ws directly (not /api/ws)
    // The WebSocket endpoint is proxied directly at /ws by nginx
    // SockJS will handle the /websocket upgrade internally
    return `${window.location.protocol}//${window.location.host}/ws`
  }

  /**
   * Connect to WebSocket
   */
  const connect = async (
    userId: string,
    onMessageReceived?: (message: MessageResponse) => void,
    onReconnect?: () => void,
    onStatusUpdate?: (statusUpdate: any) => void,
    onTypingIndicator?: (typingIndicator: any) => void,
    onNotification?: (notification: any) => void,
    onProposalUpdate?: (proposalUpdate: any) => void
  ) => {
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

      // Get user roles from token
      const { getUserRoles: getUserRolesFromToken } = await import('@/common/utils/jwtDecode/jwtDecode.util')
      const userRoles = getUserRolesFromToken(token)
      const rolesHeader = userRoles.length > 0 ? userRoles.join(',') : ''

      const socket = new SockJS(getWebSocketUrl())
      const client = new Client({
        webSocketFactory: () => socket as any,
        connectHeaders: {
          Authorization: `Bearer ${userId}`, // Communication service expects userId in Bearer token
          'X-User-Id': userId, // Also send as header for consistency
          'X-User-Roles': rolesHeader, // Send roles as comma-separated string
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 10000, // Server heartbeat every 10 seconds
        heartbeatOutgoing: 10000, // Client heartbeat every 10 seconds
        // Note: Heartbeat callbacks are handled via debug() function
        // Enable automatic reconnection
        connectionTimeout: 5000,
        // Keep connection alive
        beforeConnect: () => {
          console.log('STOMP: Attempting to connect...')
        },
        debug: (str) => {
          // Log important messages including heartbeats
          if (
            str.includes('error') ||
            str.includes('disconnect') ||
            str.includes('connect') ||
            str.includes('heartbeat') ||
            str.includes('SUBSCRIBE') ||
            str.includes('MESSAGE')
          ) {
            console.log('STOMP:', str)
            // Log heartbeat specifically
            if (str.includes('heartbeat')) {
              console.log('💓 Heartbeat:', str)
            }
          }
        },
        onConnect: (frame) => {
          console.log('✅ WebSocket connected successfully', {
            userId,
            connectedHeaders: frame.headers,
          })
          connected.value = true
          connecting.value = false
          reconnectAttempts.value = 0 // Reset reconnect attempts on successful connection

          // Clear any existing reconnect timer
          if (reconnectTimer.value) {
            clearTimeout(reconnectTimer.value)
            reconnectTimer.value = null
          }

          // Start keep-alive mechanism
          startKeepAlive()

          // Call reconnect callback if this is a reconnection
          if (onReconnectCallback.value && reconnectAttempts.value > 0) {
            onReconnectCallback.value()
          }

          // Subscribe to user's message queue
          // IMPORTANT: Spring's convertAndSendToUser() automatically prepends /user/{userId}
          // So we subscribe to /queue/messages and Spring routes it to the correct user
          if (onMessageReceived && client) {
            const destination = `/user/queue/messages`
            console.log(`📡 Subscribing to: ${destination}`)

            const subscription = client.subscribe(destination, (message) => {
              try {
                console.log('📨 Message received via WebSocket:', {
                  destination: message.headers.destination,
                  messageId: message.headers['message-id'],
                })
                const messageData: MessageResponse = JSON.parse(message.body)
                console.log('📨 Parsed message:', {
                  id: messageData.id,
                  senderId: messageData.senderId,
                  content: messageData.content?.substring(0, 50),
                })
                onMessageReceived(messageData)
              } catch (error) {
                console.error('❌ Failed to parse message:', error, message.body)
              }
            })
            subscriptions.value.push(subscription)
            console.log(`✅ Successfully subscribed to ${destination}`)
          } else {
            console.warn('⚠️ No message callback provided, skipping subscription')
          }

          // Subscribe to status updates
          if (onStatusUpdate && client) {
            const statusDest = `/user/queue/status-updates`
            console.log(`📡 Subscribing to status updates: ${statusDest}`)

            const statusSub = client.subscribe(statusDest, (message) => {
              try {
                const statusUpdate = JSON.parse(message.body)
                console.log('📊 Status update received:', statusUpdate)
                onStatusUpdate(statusUpdate)
              } catch (error) {
                console.error('❌ Failed to parse status update:', error)
              }
            })
            subscriptions.value.push(statusSub)
          }

          // Subscribe to typing indicators
          if (onTypingIndicator && client) {
            const typingDest = `/user/queue/typing`
            console.log(`📡 Subscribing to typing indicators: ${typingDest}`)

            const typingSub = client.subscribe(typingDest, (message) => {
              try {
                const typingIndicator = JSON.parse(message.body)
                console.log('📝 Typing indicator received:', typingIndicator)
                onTypingIndicator(typingIndicator)
              } catch (error) {
                console.error('❌ Failed to parse typing indicator:', error)
              }
            })
            subscriptions.value.push(typingSub)
          }

          // Subscribe to notifications
          if (onNotification && client) {
            const notifDest = `/user/queue/notifications`
            console.log(`📡 Subscribing to notifications: ${notifDest}`)

            const notifSub = client.subscribe(notifDest, (message) => {
              try {
                const notification = JSON.parse(message.body)
                console.log('🔔 Notification received:', notification)
                onNotification(notification)
              } catch (error) {
                console.error('❌ Failed to parse notification:', error)
              }
            })
            subscriptions.value.push(notifSub)
          }

          // Subscribe to proposal updates
          if (onProposalUpdate && client) {
            const proposalDest = `/user/queue/proposal-updates`
            console.log(`📡 Subscribing to proposal updates: ${proposalDest}`)

            const proposalSub = client.subscribe(proposalDest, (message) => {
              try {
                const proposalUpdate = JSON.parse(message.body)
                console.log('📋 Proposal update received:', proposalUpdate)
                onProposalUpdate(proposalUpdate)
              } catch (error) {
                console.error('❌ Failed to parse proposal update:', error)
              }
            })
            subscriptions.value.push(proposalSub)
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
          stopKeepAlive()
          // Attempt to reconnect if we have a user ID
          if (currentUserId.value && reconnectAttempts.value < maxReconnectAttempts) {
            scheduleReconnect()
          }
        },
        onDisconnect: () => {
          console.log('STOMP disconnected')
          connected.value = false
          connecting.value = false
          stopKeepAlive()
          // Attempt to reconnect if we have a user ID
          if (currentUserId.value && reconnectAttempts.value < maxReconnectAttempts) {
            scheduleReconnect()
          }
        },
      })

      client.activate()
      stompClient.value = client as any
      currentUserId.value = userId
      messageCallback.value = onMessageReceived || null
      onReconnectCallback.value = onReconnect || null
      statusUpdateCallback.value = onStatusUpdate || null
      typingCallback.value = onTypingIndicator || null
      notificationCallback.value = onNotification || null
      proposalUpdateCallback.value = onProposalUpdate || null
    } catch (error) {
      console.error(ErrorLog('❌ Failed to connect WebSocket', error))
      connecting.value = false
      reconnectAttempts.value++

      if (reconnectAttempts.value < maxReconnectAttempts) {
        scheduleReconnect()
      } else {
      toast.add({
        title: 'Connection Error',
          description: 'Failed to initialize WebSocket connection after multiple attempts',
        color: 'error',
      })
      }
    }
  }

  /**
   * Schedule reconnection attempt
   */
  const scheduleReconnect = () => {
    if (reconnectTimer.value) {
      clearTimeout(reconnectTimer.value)
    }

    reconnectAttempts.value++
    const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.value), 30000) // Exponential backoff, max 30s

    console.log(`Scheduling reconnect attempt ${reconnectAttempts.value} in ${delay}ms`)

    reconnectTimer.value = window.setTimeout(() => {
      if (!connected.value && currentUserId.value && messageCallback.value) {
        console.log('Attempting to reconnect WebSocket...')
        connect(
          currentUserId.value,
          messageCallback.value,
          onReconnectCallback.value || undefined,
          statusUpdateCallback.value || undefined,
          typingCallback.value || undefined,
          notificationCallback.value || undefined
        )
      }
    }, delay)
  }

  /**
   * Start keep-alive mechanism
   * Logs connection status periodically (STOMP handles heartbeats automatically)
   */
  const startKeepAlive = () => {
    stopKeepAlive() // Clear any existing timer

    // Log connection status every 30 seconds (heartbeats are handled by STOMP)
    keepAliveTimer.value = window.setInterval(() => {
      if (stompClient.value && connected.value) {
        const client = stompClient.value as any
        console.log('💓 Keep-alive check: WebSocket connection active', {
          connected: connected.value,
          userId: currentUserId.value,
          subscriptions: subscriptions.value.length,
        })

        // Verify we can still access the client
        if (!client.connected && !client.active) {
          console.warn('⚠️ WebSocket client appears disconnected, scheduling reconnect')
          connected.value = false
          if (currentUserId.value && messageCallback.value) {
            scheduleReconnect()
          }
        }
      } else {
        console.warn('⚠️ Keep-alive check: WebSocket not connected')
      }
    }, 30000) // Every 30 seconds
  }

  /**
   * Stop keep-alive mechanism
   */
  const stopKeepAlive = () => {
    if (keepAliveTimer.value) {
      clearInterval(keepAliveTimer.value)
      keepAliveTimer.value = null
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
   * Send typing indicator
   */
  const sendTyping = (conversationId: string, isTyping: boolean) => {
    if (!stompClient.value || !connected.value) {
      return false
    }

    try {
      const client = stompClient.value as any
      const payload = {
        conversationId,
        isTyping,
        timestamp: Date.now(),
      }

      if (client.publish) {
        client.publish({
          destination: '/app/chat.typing',
          body: JSON.stringify(payload),
        })
      } else if (client.send) {
        client.send('/app/chat.typing', {}, JSON.stringify(payload))
      }
      return true
    } catch (error) {
      console.error('Failed to send typing indicator:', error)
      return false
    }
  }

  /**
   * Mark messages as read
   */
  const markAsRead = (messageIds: string[], conversationId: string) => {
    if (!stompClient.value || !connected.value) {
      return false
    }

    try {
      const client = stompClient.value as any
      const payload = {
        messageIds,
        conversationId,
      }

      if (client.publish) {
        client.publish({
          destination: '/app/chat.read',
          body: JSON.stringify(payload),
        })
      } else if (client.send) {
        client.send('/app/chat.read', {}, JSON.stringify(payload))
      }
      return true
    } catch (error) {
      console.error('Failed to mark messages as read:', error)
      return false
    }
  }

  /**
   * Send quick action for proposals
   */
  const sendQuickAction = (proposalId: string, action: 'ACCEPT' | 'REJECT' | 'POSTPONE', data?: any) => {
    if (!stompClient.value || !connected.value) {
      return false
    }

    try {
      const client = stompClient.value as any
      const payload = {
        proposalId,
        action,
        ...data,
      }

      if (client.publish) {
        client.publish({
          destination: '/app/chat.quick-action',
          body: JSON.stringify(payload),
        })
      } else if (client.send) {
        client.send('/app/chat.quick-action', {}, JSON.stringify(payload))
      }
      return true
    } catch (error) {
      console.error('Failed to send quick action:', error)
      return false
    }
  }

  /**
   * Disconnect from WebSocket
   */
  const disconnect = () => {
    stopKeepAlive()

    if (reconnectTimer.value) {
      clearTimeout(reconnectTimer.value)
      reconnectTimer.value = null
    }

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

    currentUserId.value = null
    messageCallback.value = null
    onReconnectCallback.value = null
    statusUpdateCallback.value = null
    typingCallback.value = null
    notificationCallback.value = null
    reconnectAttempts.value = 0
  }

  /**
   * Handle visibility change - reconnect when tab becomes visible
   */
  const handleVisibilityChange = () => {
    if (document.visibilityState === 'visible' && !connected.value && currentUserId.value && messageCallback.value) {
      console.log('Tab became visible, attempting to reconnect WebSocket...')
      reconnectAttempts.value = 0 // Reset attempts
      connect(
        currentUserId.value,
        messageCallback.value,
        onReconnectCallback.value || undefined,
        statusUpdateCallback.value || undefined,
        typingCallback.value || undefined,
        notificationCallback.value || undefined
      )
    }
  }

  // Listen for visibility changes
  if (typeof document !== 'undefined') {
    document.addEventListener('visibilitychange', handleVisibilityChange)
  }

  // Cleanup on unmount
  onUnmounted(() => {
    disconnect()
    if (typeof document !== 'undefined') {
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  })

  return {
    connected,
    connecting,
    connect,
    sendMessage,
    sendTyping,
    markAsRead,
    sendQuickAction,
    disconnect,
  }
}
