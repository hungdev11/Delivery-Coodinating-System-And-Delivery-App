/**
 * Config Store
 * Manages public configuration and API keys fetched from backend
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

interface PublicConfig {
  secrets: Record<string, string>
  version: string
}

export const useConfigStore = defineStore('config', () => {
  // State
  const config = ref<PublicConfig | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)
  const isInitialized = ref(false)

  // Getters
  const secrets = computed(() => config.value?.secrets || {})
  const mapTilerApiKey = computed(() => secrets.value['MAPTILER_API_KEY'] || '')
  const googleMapsApiKey = computed(() => secrets.value['GOOGLE_MAPS_API_KEY'] || '')
  const appVersion = computed(() => config.value?.version || '1.0.0')

  /**
   * Fetch public configuration from API Gateway
   */
  async function fetchConfig(): Promise<void> {
    if (isInitialized.value) return

    isLoading.value = true
    error.value = null

    try {
      const apiUrl = import.meta.env.VITE_API_URL || '/api'
      const response = await fetch(`${apiUrl}/v1/config/public`)

      if (!response.ok) {
        throw new Error(`Failed to fetch config: ${response.status}`)
      }

      const result = await response.json()

      if (result.success && result.data) {
        config.value = result.data
        isInitialized.value = true
        console.log('[ConfigStore] Public config loaded')
      } else {
        throw new Error(result.message || 'Invalid response format')
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Unknown error'
      error.value = message
      console.warn('[ConfigStore] Failed to fetch config:', message)

      // Set empty config to prevent repeated fetch attempts
      config.value = { secrets: {}, version: '1.0.0' }
      isInitialized.value = true
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Get a secret value by key
   */
  function getSecret(key: string): string {
    return secrets.value[key] || ''
  }

  /**
   * Check if a secret is configured (non-empty)
   */
  function hasSecret(key: string): boolean {
    const value = secrets.value[key]
    return !!value && value.trim().length > 0
  }

  /**
   * Reset store state
   */
  function reset(): void {
    config.value = null
    isLoading.value = false
    error.value = null
    isInitialized.value = false
  }

  return {
    // State
    config,
    isLoading,
    error,
    isInitialized,
    // Getters
    secrets,
    mapTilerApiKey,
    googleMapsApiKey,
    appVersion,
    // Actions
    fetchConfig,
    getSecret,
    hasSecret,
    reset,
  }
})
