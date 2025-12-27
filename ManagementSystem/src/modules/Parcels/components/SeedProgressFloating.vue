<script setup lang="ts">
/**
 * Seed Progress Floating Component
 *
 * Draggable floating component that displays real-time progress for parcel seed process
 * Auto-subscribes to WebSocket topic for progress updates
 */

import { ref, computed, onUnmounted, onMounted } from 'vue'
import { useWebSocket } from '../../Communication/composables/useWebSocket'
import { useGlobalChat } from '../../Communication/composables/useGlobalChat'
import type { SeedProgressEvent } from '../composables/useSeedProgress'

interface Props {
  sessionKey: string
}

interface Emits {
  (e: 'close'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// Progress state
const currentEvent = ref<SeedProgressEvent | null>(null)
const isCompleted = ref(false)
const isError = ref(false)

// WebSocket subscription
const { subscribeTo } = useWebSocket()
const globalChat = useGlobalChat()

// Drag state
const isDragging = ref(false)
const position = ref({ x: 0, y: 0 })
const dragStart = ref({ x: 0, y: 0 })
const containerRef = ref<HTMLElement | null>(null)

// Initialize position (bottom-right corner with some margin)
onMounted(() => {
  position.value = {
    x: window.innerWidth - 400 - 20, // 400px width + 20px margin
    y: window.innerHeight - 300 - 20, // 300px height + 20px margin
  }
})

// Drag handlers
const handleMouseDown = (e: MouseEvent) => {
  if (containerRef.value) {
    isDragging.value = true
    const rect = containerRef.value.getBoundingClientRect()
    dragStart.value = {
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
    }
    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)
    e.preventDefault()
  }
}

const handleMouseMove = (e: MouseEvent) => {
  if (isDragging.value) {
    position.value = {
      x: e.clientX - dragStart.value.x,
      y: e.clientY - dragStart.value.y,
    }

    // Keep within viewport bounds
    const maxX = window.innerWidth - (containerRef.value?.offsetWidth || 400)
    const maxY = window.innerHeight - (containerRef.value?.offsetHeight || 300)

    position.value.x = Math.max(0, Math.min(position.value.x, maxX))
    position.value.y = Math.max(0, Math.min(position.value.y, maxY))
  }
}

const handleMouseUp = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
}

// Cleanup on unmount
onUnmounted(() => {
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
})

// Subscribe to WebSocket topic on mount
const subscribeToProgress = () => {
  // Ensure WebSocket is connected
  if (!globalChat.connected.value) {
    globalChat.initialize().then(() => {
      if (globalChat.connected.value) {
        doSubscribe()
      }
    })
    return
  }

  doSubscribe()
}

const doSubscribe = () => {
  const topic = `/topic/seed-progress/${props.sessionKey}`
  console.log(`📡 [SeedProgressFloating] Subscribing to: ${topic}`)

  subscribeTo(topic, (event: SeedProgressEvent) => {
    console.log('📊 [SeedProgressFloating] Progress event received:', event)
    currentEvent.value = event

    if (event.eventType === 'COMPLETED') {
      isCompleted.value = true
    } else if (event.eventType === 'ERROR') {
      isError.value = true
    }
  })
}

// Subscribe on mount
subscribeToProgress()

// Computed values
const progress = computed(() => currentEvent.value?.progress ?? 0)
const currentStep = computed(() => currentEvent.value?.currentStep ?? 0)
const totalSteps = computed(() => currentEvent.value?.totalSteps ?? 5)
const stepDescription = computed(() => currentEvent.value?.stepDescription ?? 'Starting...')
const failedOldParcelsCount = computed(() => currentEvent.value?.failedOldParcelsCount ?? 0)
const seededParcelsCount = computed(() => currentEvent.value?.seededParcelsCount ?? 0)
const skippedAddressesCount = computed(() => currentEvent.value?.skippedAddressesCount ?? 0)
const currentClient = computed(() => currentEvent.value?.currentClient ?? 0)
const totalClients = computed(() => currentEvent.value?.totalClients ?? 0)
const errorMessage = computed(() => currentEvent.value?.errorMessage)

const progressText = computed(() => {
  if (isCompleted.value) {
    return 'Hoàn thành'
  }
  if (isError.value) {
    return 'Lỗi'
  }
  return `${progress.value}%`
})

const canClose = computed(() => isCompleted.value || isError.value)

// Dynamic styling
const containerStyle = computed(() => ({
  position: 'fixed',
  left: `${position.value.x}px`,
  top: `${position.value.y}px`,
  zIndex: 9999,
  width: '380px',
  cursor: isDragging.value ? 'grabbing' : 'grab',
  userSelect: 'none' as const,
}))
</script>

<template>
  <div
    ref="containerRef"
    :style="containerStyle"
    class="seed-progress-floating shadow-2xl rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 overflow-hidden"
  >
    <UCard :ui="{ body: 'p-4', header: 'p-3', footer: 'p-3' }">
      <template #header>
        <div
          class="flex items-center justify-between cursor-grab active:cursor-grabbing"
          @mousedown="handleMouseDown"
        >
          <div class="flex items-center gap-2">
            <div
              class="w-2 h-2 rounded-full"
              :class="{
                'bg-green-500': isCompleted,
                'bg-red-500': isError,
                'bg-yellow-500 animate-pulse': !isCompleted && !isError,
              }"
            ></div>
            <h3 class="text-sm font-semibold">Seed Parcels</h3>
          </div>
          <UButton
            icon="i-heroicons-x-mark"
            variant="ghost"
            size="xs"
            @click="emit('close')"
          />
        </div>
      </template>

      <div class="space-y-3">
        <!-- Progress Bar -->
        <div>
          <div class="flex items-center justify-between mb-1.5">
            <span class="text-xs font-medium truncate flex-1 mr-2">{{ stepDescription }}</span>
            <span class="text-xs text-gray-500 whitespace-nowrap">{{ progressText }}</span>
          </div>
          <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
            <div
              class="bg-primary h-2 rounded-full transition-all duration-300"
              :style="{ width: `${progress}%` }"
            ></div>
          </div>
        </div>

        <!-- Step Info (compact) -->
        <div v-if="currentStep > 0" class="text-xs text-gray-600 dark:text-gray-400">
          Bước {{ currentStep }}/{{ totalSteps }}
          <span v-if="totalClients > 0"> • Client {{ currentClient }}/{{ totalClients }}</span>
        </div>

        <!-- Results (compact grid) -->
        <div class="grid grid-cols-3 gap-2 pt-2 border-t border-gray-200 dark:border-gray-700">
          <div class="text-center">
            <div class="text-lg font-bold text-orange-600">{{ failedOldParcelsCount }}</div>
            <div class="text-xs text-gray-500">Đơn cũ</div>
          </div>
          <div class="text-center">
            <div class="text-lg font-bold text-green-600">{{ seededParcelsCount }}</div>
            <div class="text-xs text-gray-500">Đơn mới</div>
          </div>
          <div class="text-center">
            <div class="text-lg font-bold text-gray-600">{{ skippedAddressesCount }}</div>
            <div class="text-xs text-gray-500">Bỏ qua</div>
          </div>
        </div>

        <!-- Error Message (compact) -->
        <div
          v-if="isError && errorMessage"
          class="p-2 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded text-xs"
        >
          <div class="font-medium text-red-800 dark:text-red-200">Lỗi:</div>
          <div class="text-red-600 dark:text-red-300">{{ errorMessage }}</div>
        </div>

        <!-- Success Message (compact) -->
        <div
          v-if="isCompleted"
          class="p-2 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded text-xs text-green-800 dark:text-green-200"
        >
          Hoàn thành thành công!
        </div>
      </div>
    </UCard>
  </div>
</template>

<style scoped>
.seed-progress-floating {
  transition: box-shadow 0.2s;
}

.seed-progress-floating:hover {
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.seed-progress-floating:active {
  cursor: grabbing;
}
</style>
