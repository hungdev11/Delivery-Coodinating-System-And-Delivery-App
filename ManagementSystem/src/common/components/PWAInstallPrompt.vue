<template>
  <Transition name="slide">
    <UCard
      v-if="showPrompt"
      :class="[
        'fixed z-50 shadow-lg bg-white dark:bg-gray-900 ring-1 ring-gray-200 dark:ring-gray-700',
        isMobile ? 'top-4 left-4 right-4 max-w-sm' : 'bottom-4 left-4 max-w-sm',
      ]"
    >
      <div class="flex items-start gap-3">
        <div class="flex-shrink-0">
          <div
            class="w-10 h-10 rounded-lg bg-primary-100 dark:bg-primary-900 flex items-center justify-center"
          >
            <UIcon
              name="i-lucide-download"
              class="w-5 h-5 text-primary-600 dark:text-primary-400"
            />
          </div>
        </div>
        <div class="flex-1 min-w-0">
          <h3 class="text-sm font-semibold text-gray-900 dark:text-gray-100">Cài đặt ứng dụng</h3>
          <p class="mt-1 text-sm text-gray-600 dark:text-gray-400">
            Cài đặt ứng dụng để truy cập nhanh hơn và sử dụng offline.
          </p>
        </div>
        <UButton color="neutral" variant="ghost" size="xs" icon="i-lucide-x" @click="dismissPrompt" />
      </div>

      <template #footer>
        <div class="flex gap-2 justify-end">
          <UButton color="neutral" variant="ghost" size="sm" @click="dismissPrompt">
            Không, cảm ơn
          </UButton>
          <UButton color="primary" size="sm" :loading="isInstalling" @click="handleInstall">
            <UIcon name="i-lucide-download" class="w-4 h-4 mr-1" />
            Cài đặt
          </UButton>
        </div>
      </template>
    </UCard>
  </Transition>

  <!-- iOS Install Instructions -->
  <Transition name="slide">
    <UCard
      v-if="showIOSInstructions"
      class="fixed top-4 left-4 right-4 z-50 max-w-sm shadow-lg bg-white dark:bg-gray-900 ring-1 ring-gray-200 dark:ring-gray-700"
    >
      <div class="flex items-start gap-3">
        <div class="flex-shrink-0">
          <div
            class="w-10 h-10 rounded-lg bg-primary-100 dark:bg-primary-900 flex items-center justify-center"
          >
            <UIcon name="i-lucide-share" class="w-5 h-5 text-primary-600 dark:text-primary-400" />
          </div>
        </div>
        <div class="flex-1 min-w-0">
          <h3 class="text-sm font-semibold text-gray-900 dark:text-gray-100">Cài đặt trên iOS</h3>
          <p class="mt-1 text-sm text-gray-600 dark:text-gray-400">
            Nhấn vào nút <UIcon name="i-lucide-share" class="w-4 h-4 inline" />
            ở dưới cùng và chọn "Thêm vào Màn hình chính".
          </p>
        </div>
        <UButton
          color="neutral"
          variant="ghost"
          size="xs"
          icon="i-lucide-x"
          @click="dismissIOSInstructions"
        />
      </div>
    </UCard>
  </Transition>

  <!-- Android Install Instructions -->
  <Transition name="slide">
    <UCard
      v-if="showAndroidInstructions"
      class="fixed top-4 left-4 right-4 z-50 max-w-sm shadow-lg bg-white dark:bg-gray-900 ring-1 ring-gray-200 dark:ring-gray-700"
    >
      <div class="flex items-start gap-3">
        <div class="flex-shrink-0">
          <div
            class="w-10 h-10 rounded-lg bg-primary-100 dark:bg-primary-900 flex items-center justify-center"
          >
            <UIcon name="i-lucide-menu" class="w-5 h-5 text-primary-600 dark:text-primary-400" />
          </div>
        </div>
        <div class="flex-1 min-w-0">
          <h3 class="text-sm font-semibold text-gray-900 dark:text-gray-100">
            Cài đặt trên Android
          </h3>
          <p class="mt-1 text-sm text-gray-600 dark:text-gray-400">
            Nhấn vào menu <UIcon name="i-lucide-menu" class="w-4 h-4 inline" />
            của trình duyệt và chọn "Cài đặt ứng dụng" hoặc "Thêm vào màn hình chính".
          </p>
        </div>
        <UButton
          color="neutral"
          variant="ghost"
          size="xs"
          icon="i-lucide-x"
          @click="dismissAndroidInstructions"
        />
      </div>
    </UCard>
  </Transition>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { usePWA } from '@/common/composables/usePWA'

const { canInstall, isInstalled, isIOS, isAndroid, isBrave, isInStandaloneMode, triggerInstall } = usePWA()

const showPrompt = ref(false)
const showIOSInstructions = ref(false)
const showAndroidInstructions = ref(false)
const isInstalling = ref(false)
const dismissedPrompt = ref(false)
const dismissedIOSInstructions = ref(false)
const dismissedAndroidInstructions = ref(false)

// Check if mobile device
const isMobile = computed(() => {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
})

// Show prompt conditions
const shouldShowPrompt = computed(() => {
  return (
    !isInstalled.value &&
    !isInStandaloneMode.value &&
    !dismissedPrompt.value &&
    (canInstall.value || isIOS.value || (isAndroid.value && isBrave.value))
  )
})

// Watch for install prompt availability
watch(
  () => canInstall.value,
  (newValue) => {
    if (newValue && !isInstalled.value && !isInStandaloneMode.value) {
      // Delay showing prompt to avoid annoying users immediately
      setTimeout(() => {
        if (!dismissedPrompt.value) {
          showPrompt.value = true
        }
      }, 3000) // Show after 3 seconds
    }
  },
  { immediate: true },
)

// Show iOS instructions if on iOS and not installed
watch(
  () => isIOS.value,
  (newValue) => {
    if (
      newValue &&
      !isInstalled.value &&
      !isInStandaloneMode.value &&
      !dismissedIOSInstructions.value
    ) {
      // Delay showing iOS instructions
      setTimeout(() => {
        if (!dismissedIOSInstructions.value) {
          showIOSInstructions.value = true
        }
      }, 3000)
    }
  },
  { immediate: true },
)

// Show Android instructions if on Android and Brave (or no install prompt available)
watch(
  () => [isAndroid.value, isBrave.value, canInstall.value],
  ([android, brave, canInstallVal]) => {
    if (
      android &&
      !isInstalled.value &&
      !isInStandaloneMode.value &&
      !dismissedAndroidInstructions.value &&
      (brave || !canInstallVal) // Show if Brave browser or install prompt not available
    ) {
      // Delay showing Android instructions
      setTimeout(() => {
        if (!dismissedAndroidInstructions.value && !canInstall.value) {
          showAndroidInstructions.value = true
        }
      }, 3000)
    }
  },
  { immediate: true },
)

// Check if prompt was previously dismissed and setup event listeners
let checkInterval: number | null = null

const handleInstallPromptAvailable = () => {
  console.log('[PWAInstallPrompt] Install prompt available event received')
  if (!isInstalled.value && !isInStandaloneMode.value && !dismissedPrompt.value) {
    setTimeout(() => {
      if (!dismissedPrompt.value && canInstall.value) {
        showPrompt.value = true
      }
    }, 3000)
  }
}

onMounted(() => {
  const dismissed = localStorage.getItem('pwa-install-dismissed')
  if (dismissed) {
    dismissedPrompt.value = true
  }

  const dismissedIOS = localStorage.getItem('pwa-ios-instructions-dismissed')
  if (dismissedIOS) {
    dismissedIOSInstructions.value = true
  }

  const dismissedAndroid = localStorage.getItem('pwa-android-instructions-dismissed')
  if (dismissedAndroid) {
    dismissedAndroidInstructions.value = true
  }

  // Show prompt if conditions are met and not dismissed
  if (shouldShowPrompt.value && !dismissedPrompt.value && canInstall.value) {
    setTimeout(() => {
      showPrompt.value = true
    }, 3000)
  }

  // Listen for custom event when install prompt becomes available
  window.addEventListener('pwa-install-prompt-available', handleInstallPromptAvailable)

  // Also check periodically if install prompt becomes available
  checkInterval = window.setInterval(() => {
    if (canInstall.value && !isInstalled.value && !isInStandaloneMode.value && !dismissedPrompt.value && !showPrompt.value) {
      console.log('[PWAInstallPrompt] Install prompt available, showing prompt')
      showPrompt.value = true
      if (checkInterval) {
        clearInterval(checkInterval)
        checkInterval = null
      }
    }
  }, 5000) // Check every 5 seconds
})

onUnmounted(() => {
  window.removeEventListener('pwa-install-prompt-available', handleInstallPromptAvailable)
  if (checkInterval) {
    clearInterval(checkInterval)
    checkInterval = null
  }
})

const handleInstall = async () => {
  if (isIOS.value) {
    // For iOS, show instructions instead
    showPrompt.value = false
    showIOSInstructions.value = true
    return
  }

  // For Android on Brave or if install prompt not available, show instructions
  if (isAndroid.value && (isBrave.value || !canInstall.value)) {
    showPrompt.value = false
    showAndroidInstructions.value = true
    return
  }

  isInstalling.value = true
  try {
    if (triggerInstall) {
      const success = await triggerInstall()
      if (success) {
        showPrompt.value = false
        dismissedPrompt.value = true
      }
    }
  } catch (error) {
    console.error('[PWA] Install error:', error)
    // If install fails and on Android, show instructions
    if (isAndroid.value) {
      showPrompt.value = false
      showAndroidInstructions.value = true
    }
  } finally {
    isInstalling.value = false
  }
}

const dismissPrompt = () => {
  showPrompt.value = false
  dismissedPrompt.value = true
  localStorage.setItem('pwa-install-dismissed', 'true')
}

const dismissIOSInstructions = () => {
  showIOSInstructions.value = false
  dismissedIOSInstructions.value = true
  localStorage.setItem('pwa-ios-instructions-dismissed', 'true')
}

const dismissAndroidInstructions = () => {
  showAndroidInstructions.value = false
  dismissedAndroidInstructions.value = true
  localStorage.setItem('pwa-android-instructions-dismissed', 'true')
}

// Hide prompts if app is already installed
watch(
  () => isInstalled.value,
  (newValue) => {
    if (newValue) {
      showPrompt.value = false
      showIOSInstructions.value = false
      showAndroidInstructions.value = false
    }
  },
)

watch(
  () => isInStandaloneMode.value,
  (newValue) => {
    if (newValue) {
      showPrompt.value = false
      showIOSInstructions.value = false
      showAndroidInstructions.value = false
    }
  },
)
</script>

<style scoped>
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease-out;
}

/* Mobile: slide down from top */
.slide-enter-from {
  transform: translateY(-100%);
  opacity: 0;
}

.slide-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}

/* Desktop: slide up from bottom - handled by positioning */
@media (min-width: 768px) {
  .slide-enter-from {
    transform: translateY(100%);
    opacity: 0;
  }

  .slide-leave-to {
    transform: translateY(100%);
    opacity: 0;
  }
}
</style>
