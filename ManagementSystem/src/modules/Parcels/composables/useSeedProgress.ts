/**
 * useSeedProgress Composable
 *
 * Handles seed progress tracking via WebSocket
 */

import { ref } from 'vue'
import { useGlobalChat } from '../../Communication/composables/useGlobalChat'
import { useWebSocket } from '../../Communication/composables/useWebSocket'
import { generateSeedSessionKey } from '../../Communication/api'
import { autoSeedParcels } from '../api'
import { useProgressTrackerStore } from '@/stores/progressTrackerStore'

export interface SeedProgressEvent {
  sessionKey: string
  eventType: 'STARTED' | 'PROGRESS' | 'COMPLETED' | 'ERROR'
  currentStep?: number
  totalSteps?: number
  stepDescription?: string
  progress?: number
  failedOldParcelsCount?: number
  seededParcelsCount?: number
  skippedAddressesCount?: number
  currentClient?: number
  totalClients?: number
  errorMessage?: string
  timestamp?: string
}

export interface SeedProgressListener {
  onProgress?: (event: SeedProgressEvent) => void
  onCompleted?: (event: SeedProgressEvent) => void
  onError?: (event: SeedProgressEvent) => void
}

const STORAGE_KEY_PREFIX = 'seed-session-key'

/**
 * Store session key in localStorage
 */
export const storeSeedSessionKey = (sessionKey: string) => {
  localStorage.setItem(`${STORAGE_KEY_PREFIX}-${sessionKey}`, Date.now().toString())
}

/**
 * Get stored session keys from localStorage
 */
export const getStoredSeedSessionKeys = (): string[] => {
  const keys: string[] = []
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i)
    if (key?.startsWith(STORAGE_KEY_PREFIX)) {
      const sessionKey = key.replace(`${STORAGE_KEY_PREFIX}-`, '')
      keys.push(sessionKey)
    }
  }
  return keys
}

/**
 * Remove session key from localStorage
 */
export const removeSeedSessionKey = (sessionKey: string) => {
  localStorage.removeItem(`${STORAGE_KEY_PREFIX}-${sessionKey}`)
}

/**
 * Composable for seed progress tracking
 */
export function useSeedProgress() {
  const { subscribeTo } = useWebSocket()
  const currentSessionKey = ref<string | null>(null)
  const listener = ref<SeedProgressListener | null>(null)

  /**
   * Generate a new session key
   */
  const generateSessionKey = async (): Promise<string | null> => {
    try {
      const response = await generateSeedSessionKey()
      if (response.success && response.result?.sessionKey) {
        const sessionKey = response.result.sessionKey
        storeSeedSessionKey(sessionKey)
        return sessionKey
      }
      return null
    } catch (error) {
      console.error('Failed to generate session key:', error)
      return null
    }
  }

  /**
   * Start seed process with progress tracking
   */
  const startSeedWithProgress = async (
    progressListener?: SeedProgressListener,
  ): Promise<string | null> => {
    try {
      // Generate session key
      const sessionKey = await generateSessionKey()
      if (!sessionKey) {
        throw new Error('Failed to generate session key')
      }

      currentSessionKey.value = sessionKey
      listener.value = progressListener || null

      // Add task to global tracker store
      const trackerStore = useProgressTrackerStore()
      const taskId = `seed-${sessionKey}`

      trackerStore.addTask({
        id: taskId,
        type: 'seed',
        title: 'Seed Parcels',
        progress: 0,
        status: 'running',
        message: 'Starting seed process...',
        details: {
          currentStep: 0,
          totalSteps: 5,
          stepDescription: 'Starting...',
        },
        onClose: () => {
          trackerStore.removeTask(taskId)
          removeSeedSessionKey(sessionKey)
        },
      })

      // Subscribe to WebSocket topic with wrapper listener that updates global tracker
      subscribeToSeedProgress(sessionKey, {
        onProgress: (event) => {
          // Update global tracker
          trackerStore.updateTask(taskId, {
            progress: event.progress || 0,
            status: 'running',
            message: event.stepDescription || 'Processing...',
            details: {
              currentStep: event.currentStep,
              totalSteps: event.totalSteps,
              stepDescription: event.stepDescription,
              failedOldParcelsCount: event.failedOldParcelsCount,
              seededParcelsCount: event.seededParcelsCount,
              skippedAddressesCount: event.skippedAddressesCount,
              currentClient: event.currentClient,
              totalClients: event.totalClients,
            },
          })

          // Call original listener if provided
          if (progressListener?.onProgress) {
            progressListener.onProgress(event)
          }
        },
        onCompleted: (event) => {
          // Update global tracker
          trackerStore.updateTask(taskId, {
            progress: 100,
            status: 'completed',
            message: `Hoàn thành: ${event.seededParcelsCount || 0} đơn mới, ${event.failedOldParcelsCount || 0} đơn cũ`,
            details: {
              currentStep: event.totalSteps || 5,
              totalSteps: event.totalSteps || 5,
              stepDescription: 'Hoàn thành',
              failedOldParcelsCount: event.failedOldParcelsCount,
              seededParcelsCount: event.seededParcelsCount,
              skippedAddressesCount: event.skippedAddressesCount,
            },
          })

          // Call original listener if provided
          if (progressListener?.onCompleted) {
            progressListener.onCompleted(event)
          }

          // Cleanup
          removeSeedSessionKey(sessionKey)
          currentSessionKey.value = null
        },
        onError: (event) => {
          // Update global tracker
          trackerStore.updateTask(taskId, {
            status: 'error',
            message: event.errorMessage || 'Failed to seed parcels',
          })

          // Call original listener if provided
          if (progressListener?.onError) {
            progressListener.onError(event)
          }

          // Cleanup
          removeSeedSessionKey(sessionKey)
          currentSessionKey.value = null
        },
      })

      // Start seed process (async, returns immediately)
      autoSeedParcels(sessionKey).catch((error) => {
        console.error('Failed to start seed process:', error)
        // Trigger error event
        trackerStore.updateTask(taskId, {
          status: 'error',
          message: error instanceof Error ? error.message : 'Failed to start seed process',
        })
      })

      return sessionKey
    } catch (error) {
      console.error('Failed to start seed with progress:', error)
      if (currentSessionKey.value) {
        removeSeedSessionKey(currentSessionKey.value)
        currentSessionKey.value = null
      }
      return null
    }
  }

  /**
   * Subscribe to seed progress WebSocket topic
   */
  const subscribeToSeedProgress = (
    sessionKey: string,
    progressListener: SeedProgressListener,
  ) => {
    // Ensure WebSocket is connected via globalChat
    const globalChat = useGlobalChat()
    if (!globalChat.connected.value) {
      console.warn('WebSocket not connected. Cannot subscribe to seed progress.')
      // Try to initialize connection
      globalChat.initialize().then(() => {
        if (globalChat.connected.value) {
          doSubscribe(sessionKey, progressListener)
        }
      })
      return
    }

    doSubscribe(sessionKey, progressListener)
  }

  /**
   * Actually perform the subscription
   */
  const doSubscribe = (sessionKey: string, progressListener: SeedProgressListener) => {
    const topic = `/topic/seed-progress/${sessionKey}`
    console.log(`📡 Subscribing to seed progress topic: ${topic}`)

    subscribeTo(topic, (event: SeedProgressEvent) => {
      console.log('📊 Seed progress event received:', event)

      // Call appropriate listener based on event type (wrapper listener handles cleanup)
      if (event.eventType === 'PROGRESS' && progressListener.onProgress) {
        progressListener.onProgress(event)
      } else if (event.eventType === 'COMPLETED') {
        if (progressListener.onCompleted) {
          progressListener.onCompleted(event)
        }
      } else if (event.eventType === 'ERROR') {
        if (progressListener.onError) {
          progressListener.onError(event)
        }
      }
    })
  }


  return {
    generateSessionKey,
    startSeedWithProgress,
    subscribeToSeedProgress,
    currentSessionKey,
  }
}
