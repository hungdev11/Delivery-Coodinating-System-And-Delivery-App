/**
 * Config Store
 * Manages public configuration and API keys fetched from backend
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getPublicConfig, type PublicConfig } from '@/modules/Config/api'

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
      const result = await getPublicConfig()

      if (result.result) {
        config.value = result.result
        isInitialized.value = true
        console.log('[ConfigStore] Public config loaded', config.value)
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
