import { useAuthStore } from './auth.store'

export async function initializeStores() {
  // Initialize auth store from localStorage
  const authStore = useAuthStore()
  authStore.initialize()
}
