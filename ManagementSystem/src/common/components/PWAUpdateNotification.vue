<template>
  <Transition name="slide-up">
    <UCard
      v-if="showUpdateNotification && needRefresh"
      class="fixed bottom-4 left-4 right-4 z-50 max-w-md mx-auto shadow-lg bg-white dark:bg-gray-900 ring-1 ring-gray-200 dark:ring-gray-700"
    >
      <div class="flex items-start gap-3">
        <div class="flex-shrink-0">
          <UIcon name="i-lucide-refresh-cw" class="w-5 h-5 text-primary-500 animate-spin" />
        </div>
        <div class="flex-1 min-w-0">
          <h3 class="text-sm font-semibold text-gray-900 dark:text-gray-100">
            Phiên bản mới khả dụng
          </h3>
          <p class="mt-1 text-sm text-gray-600 dark:text-gray-400">
            Ứng dụng đã có phiên bản mới. Vui lòng cập nhật để sử dụng các tính năng mới nhất.
          </p>
        </div>
      </div>

      <template #footer>
        <div class="flex gap-2 justify-end">
          <UButton color="neutral" variant="ghost" size="sm" @click="dismissUpdate">
            Để sau
          </UButton>
          <UButton color="primary" size="sm" :loading="isUpdating" @click="handleUpdate">
            <UIcon name="i-lucide-download" class="w-4 h-4 mr-1" />
            Cập nhật ngay
          </UButton>
        </div>
      </template>
    </UCard>
  </Transition>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { usePWA } from '@/common/composables/usePWA'

// useToast is auto-imported in Nuxt UI
const { needRefresh, requestUpdate, checkForUpdate } = usePWA()
const isUpdating = ref(false)
const showUpdateNotification = ref(false)
const toast = useToast()

// Watch for needRefresh changes
watch(
  () => needRefresh.value,
  (newValue) => {
    if (newValue) {
      console.log('[PWAUpdateNotification] Update available, showing notification', {
        needRefresh: needRefresh.value,
        timestamp: new Date().toISOString(),
        location: window.location.href,
      })
      
      // Check if dismissed for this version
      const dismissedVersion = localStorage.getItem('pwa-update-dismissed')
      const currentVersion =
        document.querySelector('meta[name="version"]')?.getAttribute('content') || 
        new Date().toISOString()
      
      // Only show if not dismissed for this version
      if (dismissedVersion !== currentVersion) {
        showUpdateNotification.value = true
      } else {
        console.log('[PWAUpdateNotification] Update notification dismissed for this version')
      }
    }
  },
  { immediate: true },
)

// Also listen for custom update available event
onMounted(() => {
  const handleUpdateAvailable = () => {
    console.log('[PWAUpdateNotification] Custom update available event received')
    if (needRefresh.value) {
      const dismissedVersion = localStorage.getItem('pwa-update-dismissed')
      const currentVersion =
        document.querySelector('meta[name="version"]')?.getAttribute('content') || 
        new Date().toISOString()
      
      if (dismissedVersion !== currentVersion) {
        showUpdateNotification.value = true
      }
    }
  }
  
  window.addEventListener('pwa-update-available', handleUpdateAvailable)
  
  onUnmounted(() => {
    window.removeEventListener('pwa-update-available', handleUpdateAvailable)
  })
})

// Check for updates periodically
let updateCheckInterval: number | null = null

const handleVisibilityChange = () => {
  if (!document.hidden) {
    checkForUpdate()
  }
}

onMounted(() => {
  console.log('[PWAUpdateNotification] Component mounted', {
    needRefresh: needRefresh.value,
    location: window.location.href,
    isProduction: import.meta.env.PROD,
  })

  // Check for updates on mount
  setTimeout(() => {
    console.log('[PWAUpdateNotification] Initial update check')
    checkForUpdate()
  }, 2000)

  // Check for updates when page becomes visible
  document.addEventListener('visibilitychange', handleVisibilityChange)

  // Periodic update check (every 30 minutes on production, every hour on dev)
  const checkInterval = import.meta.env.PROD ? 30 * 60 * 1000 : 60 * 60 * 1000
  updateCheckInterval = window.setInterval(() => {
    console.log('[PWAUpdateNotification] Periodic update check')
    checkForUpdate()
  }, checkInterval)
  
  // Also check immediately if needRefresh is already true
  if (needRefresh.value) {
    console.log('[PWAUpdateNotification] needRefresh is already true on mount')
    const dismissedVersion = localStorage.getItem('pwa-update-dismissed')
    const currentVersion =
      document.querySelector('meta[name="version"]')?.getAttribute('content') || 
      new Date().toISOString()
    
    if (dismissedVersion !== currentVersion) {
      showUpdateNotification.value = true
    }
  }
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  if (updateCheckInterval) {
    clearInterval(updateCheckInterval)
    updateCheckInterval = null
  }
})

const handleUpdate = async () => {
  isUpdating.value = true
  console.log('[PWAUpdateNotification] Starting update process')
  
  try {
    console.log('[PWAUpdateNotification] Calling requestUpdate')
    await requestUpdate()
    
    console.log('[PWAUpdateNotification] requestUpdate completed, waiting for reload')
    
    // Note: reload should happen in requestUpdate, but if it doesn't, we'll keep loading state
    // The page will reload, so this component will unmount anyway
    // If reload doesn't happen after 5 seconds (longer timeout for production), show error
    setTimeout(() => {
      if (isUpdating.value) {
        console.warn('[PWAUpdateNotification] Reload did not happen after 5 seconds, showing error')
        isUpdating.value = false
        toast.add({
          title: 'Lỗi cập nhật',
          description: 'Không thể cập nhật ứng dụng. Vui lòng tải lại trang thủ công (F5 hoặc Ctrl+R).',
          color: 'error',
          timeout: 5000,
        })
      }
    }, 5000) // Increased timeout for production
  } catch (error) {
    console.error('[PWAUpdateNotification] Update error:', error)
    isUpdating.value = false
    // Show error to user if update fails
    toast.add({
      title: 'Lỗi cập nhật',
      description: `Không thể cập nhật ứng dụng: ${error instanceof Error ? error.message : 'Lỗi không xác định'}. Vui lòng thử lại sau hoặc tải lại trang thủ công.`,
      color: 'error',
      timeout: 5000,
    })
  }
}

const dismissUpdate = () => {
  // Store dismissal in localStorage to prevent showing again for this version
  const currentVersion =
    document.querySelector('meta[name="version"]')?.getAttribute('content') || 
    new Date().toISOString()
  localStorage.setItem('pwa-update-dismissed', currentVersion)
  showUpdateNotification.value = false
  
  // Show again after 1 hour
  setTimeout(() => {
    if (needRefresh.value) {
      showUpdateNotification.value = true
    }
  }, 60 * 60 * 1000)
}
</script>

<style scoped>
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease-out;
}

.slide-up-enter-from {
  transform: translateY(100%);
  opacity: 0;
}

.slide-up-leave-to {
  transform: translateY(100%);
  opacity: 0;
}
</style>
