<script setup lang="ts">
/**
 * Change Parcel Status Modal
 *
 * Modal for admin to change parcel status and notify client via proposal
 */

import { ref, computed } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import type { ParcelDto, ParcelStatus, ParcelEvent } from '../model.type'

interface Props {
  parcel: ParcelDto
}

const props = defineProps<Props>()

// Use useOverlay pattern - emit close with result
const emit = defineEmits<{
  close: [result: { event: ParcelEvent; notifyClient: boolean } | null]
}>()

const toast = useToast()

// Status options based on current status
const availableEvents = computed<ParcelEvent[]>(() => {
  const currentStatus = props.parcel.status

  switch (currentStatus) {
    case 'IN_WAREHOUSE':
      return ['DELIVERY_SUCCESSFUL', 'CUSTOMER_REJECT', 'POSTPONE', 'CUSTOMER_CONFIRM_NOT_RECEIVED']
    case 'ON_ROUTE':
      return ['DELIVERY_SUCCESSFUL', 'CAN_NOT_DELIVERY', 'POSTPONE', 'CUSTOMER_CONFIRM_NOT_RECEIVED']
    case 'DELIVERED':
      return ['CUSTOMER_RECEIVED', 'CUSTOMER_CONFIRM_NOT_RECEIVED']
    case 'DISPUTE':
      return ['MISSUNDERSTANDING_DISPUTE', 'FAULT_DISPUTE']
    case 'DELAYED':
      return ['DELIVERY_SUCCESSFUL', 'CAN_NOT_DELIVERY', 'CUSTOMER_CONFIRM_NOT_RECEIVED']
    default:
      return []
  }
})

const selectedEvent = ref<ParcelEvent | ''>('')
const notifyClient = ref(true)
const loading = ref(false)

// Event labels
const eventLabels: Record<ParcelEvent, string> = {
  DELIVERY_SUCCESSFUL: 'Đã giao hàng (DELIVERED)',
  CUSTOMER_RECEIVED: 'Khách hàng đã nhận (SUCCEEDED)',
  CAN_NOT_DELIVERY: 'Không thể giao hàng',
  CUSTOMER_REJECT: 'Khách hàng từ chối (FAILED)',
  POSTPONE: 'Hoãn giao hàng',
  CUSTOMER_CONFIRM_NOT_RECEIVED: 'Khách hàng báo chưa nhận (DISPUTE)',
  MISSUNDERSTANDING_DISPUTE: 'Giải quyết tranh chấp: Lỗi từ khách hàng (SUCCEEDED)',
  FAULT_DISPUTE: 'Giải quyết tranh chấp: Lỗi từ shipper (LOST)',
}

// Status that will result from event
const getResultStatus = (event: ParcelEvent): ParcelStatus => {
  switch (event) {
    case 'DELIVERY_SUCCESSFUL':
      return 'DELIVERED'
    case 'CUSTOMER_RECEIVED':
      return 'SUCCEEDED'
    case 'CUSTOMER_CONFIRM_NOT_RECEIVED':
      return 'DISPUTE'
    case 'CUSTOMER_REJECT':
      return 'FAILED' // Khách từ chối = hủy đơn = FAILED
    case 'MISSUNDERSTANDING_DISPUTE':
      return 'SUCCEEDED'
    case 'FAULT_DISPUTE':
      return 'LOST'
    default:
      return props.parcel.status
  }
}

const handleConfirm = () => {
  if (!selectedEvent.value) {
    toast.add({
      title: 'Error',
      description: 'Vui lòng chọn trạng thái mới',
      color: 'error',
    })
    return
  }

  emit('close', {
    event: selectedEvent.value as ParcelEvent,
    notifyClient: notifyClient.value,
  })
}

const handleCancel = () => {
  emit('close', null)
}
</script>

<template>
  <UModal>
    <template #header>
      <h3 class="text-lg font-semibold">Đổi trạng thái đơn hàng</h3>
    </template>

    <template #body>
      <div class="space-y-4">
        <!-- Current Status -->
        <div>
          <label class="text-sm font-medium text-gray-700 dark:text-gray-300">
            Trạng thái hiện tại:
          </label>
          <p class="mt-1 text-sm text-gray-900 dark:text-gray-100 font-semibold">
            {{ parcel.status }} - {{ parcel.code }}
          </p>
        </div>

        <!-- Available Events -->
        <div>
          <label class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 block">
            Chọn trạng thái mới:
          </label>
          <div class="space-y-2">
            <div
              v-for="event in availableEvents"
              :key="event"
              class="flex items-center space-x-2 p-2 rounded border"
              :class="
                selectedEvent === event
                  ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                  : 'border-gray-200 dark:border-gray-700'
              "
            >
              <input
                :id="`event-${event}`"
                v-model="selectedEvent"
                type="radio"
                :value="event"
                class="h-4 w-4 text-primary-600"
              />
              <label :for="`event-${event}`" class="flex-1 cursor-pointer">
                <div class="font-medium text-sm">{{ eventLabels[event] }}</div>
                <div class="text-xs text-gray-500">
                  Kết quả: {{ getResultStatus(event) }}
                </div>
              </label>
            </div>
          </div>
        </div>

        <!-- Notify Client Option -->
        <div v-if="selectedEvent" class="flex items-center space-x-2">
          <input
            id="notify-client"
            v-model="notifyClient"
            type="checkbox"
            class="h-4 w-4 text-primary-600"
          />
          <label for="notify-client" class="text-sm text-gray-700 dark:text-gray-300">
            Gửi thông báo cho khách hàng (proposal)
          </label>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <UButton color="neutral" variant="ghost" @click="handleCancel"> Hủy </UButton>
        <UButton
          color="primary"
          :disabled="!selectedEvent"
          :loading="loading"
          @click="handleConfirm"
        >
          Xác nhận
        </UButton>
      </div>
    </template>
  </UModal>
</template>
