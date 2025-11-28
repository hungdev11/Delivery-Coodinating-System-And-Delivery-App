import { useAuthStore } from './auth.store'
import { useConfigStore } from './config.store'
import { useResponsiveStore } from './responsive.store'

export async function initializeStores() {
  // Initialize auth store from localStorage
  const authStore = useAuthStore()
  authStore.initialize()

  // Fetch public config (API keys, etc.) from backend
  const configStore = useConfigStore()
  await configStore.fetchConfig()

  // Initialize responsive store (attach resize listener)
  const responsiveStore = useResponsiveStore()
  responsiveStore.attachResizeListener()
}
