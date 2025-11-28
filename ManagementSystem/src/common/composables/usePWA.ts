import { ref, computed, onMounted, onUnmounted } from 'vue'
// @ts-expect-error - virtual module from vite-plugin-pwa
import { useRegisterSW } from 'virtual:pwa-register/vue'

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

/**
 * Composable for PWA update check and install prompt
 */
export function usePWA() {
  const installPrompt = ref<BeforeInstallPromptEvent | null>(null)
  const isInstalled = ref(false)
  const isIOS = ref(false)
  const isInStandaloneMode = ref(false)

  // Store service worker registration for manual updates
  const swRegistration = ref<ServiceWorkerRegistration | null>(null)

  // Check if app is already installed
  const checkInstallStatus = () => {
    // Check if running in standalone mode (installed as PWA)
    isInStandaloneMode.value =
      window.matchMedia('(display-mode: standalone)').matches ||
      (window.navigator as { standalone?: boolean }).standalone === true ||
      document.referrer.includes('android-app://')

    // Check if iOS
    const ua = navigator.userAgent
    const isIPad =
      /iPad/.test(ua) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
    const isIPhone = /iPhone/.test(ua) && !(window as unknown as { MSStream?: unknown }).MSStream
    isIOS.value = isIPad || isIPhone || /iPod/.test(ua)

    // Consider installed if in standalone mode or has service worker registration
    isInstalled.value = isInStandaloneMode.value
  }

  // Setup controller change listener (should be done before registration)
  if (typeof window !== 'undefined' && 'serviceWorker' in navigator) {
    navigator.serviceWorker.addEventListener('controllerchange', () => {
      console.log('[PWA] Service worker controller changed, reloading...')
      window.location.reload()
    })
  }

  // Track if update is available
  const updateAvailable = ref(false)

  // Register service worker with update detection
  const { needRefresh, updateServiceWorker, offlineReady } = useRegisterSW({
    immediate: true,
    onRegistered(registration: ServiceWorkerRegistration) {
      console.log('[PWA] Service Worker registered:', registration)
      swRegistration.value = registration

      // Check for waiting service worker immediately after registration
      if (registration.waiting) {
        console.log('[PWA] Waiting service worker detected on registration')
        updateAvailable.value = true
      }

      // Listen for updatefound event
      registration.addEventListener('updatefound', () => {
        console.log('[PWA] Service worker update found')
        const newWorker = registration.installing || registration.waiting
        if (newWorker) {
          newWorker.addEventListener('statechange', () => {
            console.log('[PWA] Service worker state changed:', newWorker.state)
            if (newWorker.state === 'installed') {
              if (navigator.serviceWorker.controller) {
                // There's a new service worker available
              console.log('[PWA] New service worker installed and waiting')
                updateAvailable.value = true
              } else {
                // First time installation
                console.log('[PWA] Service worker installed for the first time')
              }
            }
          })
        }
      })

      // Periodically check for updates
      setInterval(() => {
        registration.update().catch((err) => {
          console.error('[PWA] Error checking for updates:', err)
        })
      }, 60 * 60 * 1000) // Check every hour
    },
    onRegisterError(error: Error) {
      console.error('[PWA] Service Worker registration error:', error)
    },
    onNeedRefresh() {
      console.log('[PWA] Update available - needRefresh triggered')
      updateAvailable.value = true
    },
    onOfflineReady() {
      console.log('[PWA] App ready to work offline')
    },
  })

  // Handle install prompt
  const handleBeforeInstallPrompt = (e: Event) => {
    // Prevent the mini-infobar from appearing on mobile
    e.preventDefault()
    // Save the event so it can be triggered later
    installPrompt.value = e as BeforeInstallPromptEvent
    console.log('[PWA] Install prompt available', e)
    
    // Dispatch custom event to notify components
    window.dispatchEvent(new CustomEvent('pwa-install-prompt-available'))
  }

  // Handle app installed
  const handleAppInstalled = () => {
    console.log('[PWA] App installed')
    installPrompt.value = null
    isInstalled.value = true
  }

  // Request update (reload app with new service worker)
  async function requestUpdate() {
    try {
      console.log('[PWA] Requesting update...')

      // Method 1: Use the updateServiceWorker from useRegisterSW if available
      if (updateServiceWorker.value) {
        console.log('[PWA] Using updateServiceWorker from useRegisterSW')
        try {
          await updateServiceWorker.value(true) // true = reload immediately
          // If reload didn't happen, force it
          window.location.reload()
          return
        } catch (error) {
          console.warn('[PWA] updateServiceWorker failed, trying fallback:', error)
          // Continue to fallback
        }
      }

      // Method 2: Use stored registration
      if (swRegistration.value) {
        const registration = swRegistration.value
        if (registration.waiting) {
          console.log('[PWA] Found waiting service worker in stored registration, sending SKIP_WAITING')
          registration.waiting.postMessage({ type: 'SKIP_WAITING' })
          // Wait for controller change event to reload
          await new Promise((resolve) => setTimeout(resolve, 1000))
          // Force reload if controller didn't change
          if (navigator.serviceWorker.controller) {
            window.location.reload()
          }
          return
        }
      }

      // Method 3: Get registration manually
      if ('serviceWorker' in navigator) {
        console.log('[PWA] Getting service worker registration manually')
        const registration = await navigator.serviceWorker.getRegistration()

        if (!registration) {
          console.error('[PWA] No service worker registration found')
          return
        }

        // Check for waiting service worker
        if (registration.waiting) {
          console.log('[PWA] Found waiting service worker, sending SKIP_WAITING message')
          registration.waiting.postMessage({ type: 'SKIP_WAITING' })

          // Wait a bit for skip waiting to take effect
          await new Promise((resolve) => setTimeout(resolve, 1000))

          // Reload the page
          console.log('[PWA] Reloading page')
          window.location.reload()
          return
        }

        // If no waiting worker, try to update
        if (needRefresh.value) {
          console.log('[PWA] Updating service worker registration')
          await registration.update()

          // Check again after update
          const updatedRegistration = await navigator.serviceWorker.getRegistration()
          if (updatedRegistration?.waiting) {
            console.log('[PWA] Found waiting service worker after update')
            updatedRegistration.waiting.postMessage({ type: 'SKIP_WAITING' })
            await new Promise((resolve) => setTimeout(resolve, 1000))
            window.location.reload()
            return
          }
        }

        console.warn('[PWA] No waiting service worker found')
      } else {
        console.error('[PWA] Service workers are not supported in this browser')
      }
    } catch (error) {
      console.error('[PWA] Update error:', error)
      throw error
    }
  }

  // Trigger install prompt
  async function triggerInstall(): Promise<boolean> {
    if (!installPrompt.value) {
      return false
    }

    try {
      // Show the install prompt
      await installPrompt.value.prompt()

      // Wait for the user to respond to the prompt
      const { outcome } = await installPrompt.value.userChoice

      if (outcome === 'accepted') {
        console.log('[PWA] User accepted the install prompt')
        installPrompt.value = null
        return true
      } else {
        console.log('[PWA] User dismissed the install prompt')
        return false
      }
    } catch (error) {
      console.error('[PWA] Error triggering install:', error)
      return false
    }
  }

  // Check for updates manually
  async function checkForUpdate() {
    if ('serviceWorker' in navigator) {
      try {
        const registration = await navigator.serviceWorker.getRegistration()
        if (registration) {
          await registration.update()
          console.log('[PWA] Update check completed')

          // Store registration for manual updates
          swRegistration.value = registration
        } else {
          console.warn('[PWA] No service worker registration found during update check')
        }
      } catch (error) {
        console.error('[PWA] Error checking for update:', error)
      }
    }
  }

  // Periodic update check (every hour)
  let updateCheckInterval: number | null = null

  const startPeriodicUpdateCheck = () => {
    // Check for updates every hour
    updateCheckInterval = window.setInterval(
      () => {
        checkForUpdate()
      },
      60 * 60 * 1000,
    )
  }

  const stopPeriodicUpdateCheck = () => {
    if (updateCheckInterval !== null) {
      clearInterval(updateCheckInterval)
      updateCheckInterval = null
    }
  }

  // Setup event listeners
  onMounted(() => {
    checkInstallStatus()

    // Listen for beforeinstallprompt event (Chrome, Edge, etc.)
    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt)

    // Listen for app installed event
    window.addEventListener('appinstalled', handleAppInstalled)

    // Start periodic update check
    startPeriodicUpdateCheck()

    // Also check on focus (user returns to app)
    window.addEventListener('focus', checkForUpdate)

    // Check for updates on visibility change
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) {
        checkForUpdate()
      }
    })

    // Initial update check after a short delay
    setTimeout(() => {
      checkForUpdate()
    }, 2000)
  })

  onUnmounted(() => {
    window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt)
    window.removeEventListener('appinstalled', handleAppInstalled)
    window.removeEventListener('focus', checkForUpdate)
    stopPeriodicUpdateCheck()
  })

  const canInstall = computed(() => !!installPrompt.value && !isInstalled.value)
  
  // Computed that combines needRefresh and updateAvailable
  const hasUpdate = computed(() => needRefresh.value || updateAvailable.value)

  return {
    // State
    needRefresh: hasUpdate,
    offlineReady,
    installPrompt,
    isInstalled,
    isIOS,
    isInStandaloneMode,
    canInstall,
    updateAvailable,

    // Actions
    requestUpdate,
    triggerInstall,
    checkForUpdate,
  }
}
