<template>
  <Transition name="slide-up">
    <UCard
      v-if="needRefresh"
      class="fixed bottom-4 left-4 right-4 z-50 max-w-md mx-auto shadow-lg"
      :ui="{
        root: 'bg-white dark:bg-gray-900',
        body: 'ring-1 ring-gray-200 dark:ring-gray-700',
      }"
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
import { ref } from 'vue'
import { usePWA } from '@/common/composables/usePWA'

// useToast is auto-imported in Nuxt UI
const { needRefresh, requestUpdate } = usePWA()
const isUpdating = ref(false)
const toast = useToast()

const handleUpdate = async () => {
  isUpdating.value = true
  try {
    console.log('[PWA] Updating service worker')
    await requestUpdate()
    // Note: reload should happen in requestUpdate, but if it doesn't, we'll keep loading state
    // The page will reload, so this component will unmount anyway
    // If reload doesn't happen after 3 seconds, show error
    setTimeout(() => {
      if (isUpdating.value) {
        console.warn('[PWA] Reload did not happen, showing error')
        isUpdating.value = false
        toast.add({
          title: 'Lỗi cập nhật',
          description: 'Không thể cập nhật ứng dụng. Vui lòng tải lại trang thủ công.',
          color: 'error',
        })
      }
    }, 3000)
  } catch (error) {
    console.error('[PWA] Update error:', error)
    isUpdating.value = false
    // Show error to user if update fails
    toast.add({
      title: 'Lỗi cập nhật',
      description: 'Không thể cập nhật ứng dụng. Vui lòng thử lại sau.',
      color: 'error',
    })
  }
}

const dismissUpdate = () => {
  // Store dismissal in localStorage to prevent showing again for this version
  const currentVersion =
    document.querySelector('meta[name="version"]')?.getAttribute('content') || 'unknown'
  localStorage.setItem('pwa-update-dismissed', currentVersion)
  needRefresh.value = false
  // Note: needRefresh will still be true, but we can add a flag to hide it
  // In a real scenario, you might want to show it again after some time
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
