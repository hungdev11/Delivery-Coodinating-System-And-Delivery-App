<template>
  <Transition name="slide-down">
    <UCard
      v-if="notification"
      class="fixed top-[175px] right-4 z-50 max-w-sm shadow-lg cursor-pointer"
      :ui="{
        root: 'bg-white dark:bg-gray-900',
        body: 'ring-1 ring-gray-200 dark:ring-gray-700',
      }"
      @click="handleClick"
    >
      <div class="flex items-start gap-3">
        <div class="flex-shrink-0">
          <div
            class="w-10 h-10 rounded-full bg-primary-100 dark:bg-primary-900 flex items-center justify-center"
          >
            <UIcon
              name="i-heroicons-chat-bubble-left-right"
              class="w-5 h-5 text-primary-600 dark:text-primary-400"
            />
          </div>
        </div>
        <div class="flex-1 min-w-0">
          <h3 class="text-sm font-semibold text-gray-900 dark:text-gray-100">
            {{ notification.partnerName || 'New message' }}
          </h3>
          <p class="mt-1 text-sm text-gray-600 dark:text-gray-400 line-clamp-2">
            {{ notification.preview }}
          </p>
        </div>
        <UButton
          color="neutral"
          variant="ghost"
          size="xs"
          icon="i-lucide-x"
          @click.stop="dismiss"
        />
      </div>
    </UCard>
  </Transition>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import type { MessageResponse } from '@/modules/Communication/model.type'

interface ChatNotification {
  conversationId: string
  partnerId: string
  partnerName: string
  preview: string
  message: MessageResponse
}

const router = useRouter()
const notification = ref<ChatNotification | null>(null)
let dismissTimer: number | null = null

const dismiss = () => {
  notification.value = null
  if (dismissTimer) {
    clearTimeout(dismissTimer)
    dismissTimer = null
  }
}

const handleClick = () => {
  if (notification.value) {
    router.push({
      name: 'communication-chat',
      params: { conversationId: notification.value.conversationId },
      query: { partnerId: notification.value.partnerId },
    })
    dismiss()
  }
}

// Auto-dismiss after 5 seconds
watch(
  () => notification.value,
  (newVal) => {
    if (dismissTimer) {
      clearTimeout(dismissTimer)
      dismissTimer = null
    }
    
    if (newVal) {
      dismissTimer = window.setTimeout(() => {
        dismiss()
      }, 5000)
    }
  },
)

onUnmounted(() => {
  if (dismissTimer) {
    clearTimeout(dismissTimer)
  }
})

// Expose method to show notification
const show = (notif: ChatNotification) => {
  notification.value = notif
}

defineExpose({
  show,
  dismiss,
})
</script>

<style scoped>
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease-out;
}

.slide-up-enter-from {
  transform: translateX(100%);
  opacity: 0;
}

.slide-up-leave-to {
  transform: translateX(100%);
  opacity: 0;
}
</style>
