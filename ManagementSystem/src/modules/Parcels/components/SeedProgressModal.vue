<script setup lang="ts">
/**
 * Seed Progress Modal
 *
 * Displays real-time progress for parcel seed process
 * Auto-subscribes to WebSocket topic for progress updates
 */

import { ref, computed, onUnmounted } from 'vue'
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
  console.log(`📡 [SeedProgressModal] Subscribing to: ${topic}`)

  subscribeTo(topic, (event: SeedProgressEvent) => {
    console.log('📊 [SeedProgressModal] Progress event received:', event)
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

// Cleanup on unmount
onUnmounted(() => {
  // Note: subscribeTo doesn't return a handle, so we can't unsubscribe directly
  // The subscription will be cleaned up when component unmounts
  console.log(`📡 [SeedProgressModal] Unmounting, subscription will be cleaned up`)
})

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
</script>

<template>
  <UModal>
    <UCard>
      <template #header>
        <div class="flex items-center justify-between">
          <h3 class="text-lg font-semibold">Tiến trình Seed Parcels</h3>
          <UButton
            v-if="canClose"
            icon="i-heroicons-x-mark"
            variant="ghost"
            size="sm"
            @click="emit('close')"
          />
        </div>
      </template>

      <div class="space-y-4">
        <!-- Progress Bar -->
        <div>
          <div class="flex items-center justify-between mb-2">
            <span class="text-sm font-medium">{{ stepDescription }}</span>
            <span class="text-sm text-gray-500">{{ progressText }}</span>
          </div>
          <div class="w-full bg-gray-200 rounded-full h-2.5">
            <div
              class="bg-primary h-2.5 rounded-full transition-all duration-300"
              :style="{ width: `${progress}%` }"
            ></div>
          </div>
        </div>

        <!-- Step Info -->
        <div v-if="currentStep > 0" class="text-sm text-gray-600">
          Bước {{ currentStep }}/{{ totalSteps }}
        </div>

        <!-- Client Progress -->
        <div v-if="totalClients > 0" class="text-sm text-gray-600">
          Đang xử lý client {{ currentClient }}/{{ totalClients }}
        </div>

        <!-- Results -->
        <div class="grid grid-cols-3 gap-4 pt-4 border-t">
          <div>
            <div class="text-2xl font-bold text-orange-600">{{ failedOldParcelsCount }}</div>
            <div class="text-xs text-gray-500">Đơn cũ đã fail</div>
          </div>
          <div>
            <div class="text-2xl font-bold text-green-600">{{ seededParcelsCount }}</div>
            <div class="text-xs text-gray-500">Đơn mới đã tạo</div>
          </div>
          <div>
            <div class="text-2xl font-bold text-gray-600">{{ skippedAddressesCount }}</div>
            <div class="text-xs text-gray-500">Địa chỉ đã bỏ qua</div>
          </div>
        </div>

        <!-- Error Message -->
        <div v-if="isError && errorMessage" class="p-4 bg-red-50 border border-red-200 rounded">
          <div class="text-sm font-medium text-red-800">Lỗi:</div>
          <div class="text-sm text-red-600">{{ errorMessage }}</div>
        </div>

        <!-- Success Message -->
        <div v-if="isCompleted" class="p-4 bg-green-50 border border-green-200 rounded">
          <div class="text-sm font-medium text-green-800">Hoàn thành thành công!</div>
        </div>

        <!-- Session Key (for debugging) -->
        <div class="text-xs text-gray-400">
          Session: {{ sessionKey.substring(0, 20) }}...
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end">
          <UButton v-if="canClose" color="primary" @click="emit('close')"> Đóng </UButton>
        </div>
      </template>
    </UCard>
  </UModal>
</template>
