<script setup lang="ts">
/**
 * Manual Assignment Modal
 * Component for manually assigning parcels to a shipper
 */

import { ref, computed, watch } from 'vue'
import { resolveComponent } from 'vue'
import type { ManualAssignmentRequest } from '../model.type'

const UButton = resolveComponent('UButton')
const UInput = resolveComponent('UInput')

interface Props {
  selectedParcels: string[] // Parcel IDs
  availableShippers?: Array<{ id: string; name: string }>
  zoneId?: string
}

interface Emits {
  (e: 'close', result: ManualAssignmentRequest | null): void
}

const props = withDefaults(defineProps<Props>(), {
  availableShippers: () => [],
  zoneId: undefined,
})

const emit = defineEmits<Emits>()

const selectedShipperId = ref<string>('')
const zoneIdInput = ref<string>(props.zoneId || '')

const isValid = computed(() => {
  return selectedShipperId.value.trim() !== '' && props.selectedParcels.length > 0
})

const handleConfirm = () => {
  if (!isValid.value) return

  const request: ManualAssignmentRequest = {
    shipperId: selectedShipperId.value,
    parcelIds: props.selectedParcels,
    zoneId: zoneIdInput.value || undefined,
  }

  emit('close', request)
}

const handleCancel = () => {
  emit('close', null)
}

watch(
  () => props.zoneId,
  (newZoneId) => {
    if (newZoneId) {
      zoneIdInput.value = newZoneId
    }
  },
  { immediate: true },
)
</script>

<template>
  <UModal
    title="Gán Task Thủ Công"
    :description="`Đã chọn ${selectedParcels.length} đơn hàng`"
    :close="{ onClick: handleCancel }"
    :ui="{
      content: 'min-w-[500px]',
    }"
  >
    <template #body>
      <div class="space-y-4">
        <!-- Selected Parcels Count -->
        <div class="rounded-lg bg-gray-50 p-3">
          <div class="text-sm text-gray-600">Số lượng đơn hàng đã chọn</div>
          <div class="text-2xl font-bold text-primary">{{ selectedParcels.length }}</div>
        </div>

        <!-- Shipper Selection -->
        <div>
          <label class="mb-2 block text-sm font-medium">Chọn Shipper *</label>
          <select
            v-model="selectedShipperId"
            class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
          >
            <option value="">-- Chọn shipper --</option>
            <option v-for="shipper in availableShippers" :key="shipper.id" :value="shipper.id">
              {{ shipper.name }}
            </option>
          </select>
        </div>

        <!-- Zone ID (Optional) -->
        <div>
          <label class="mb-2 block text-sm font-medium">Zone ID (Tùy chọn)</label>
          <UInput v-model="zoneIdInput" placeholder="Nhập zone ID để filter" />
        </div>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <UButton variant="outline" color="neutral" @click="handleCancel"> Hủy </UButton>
        <UButton :disabled="!isValid" @click="handleConfirm"> Xác nhận </UButton>
      </div>
    </template>
  </UModal>
</template>
