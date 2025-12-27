<script setup lang="ts">
/**
 * Auto Assignment Modal
 * Component for triggering automatic assignment using VRP solver
 */

import { ref, computed, watch } from 'vue'
import { resolveComponent } from 'vue'
import type { AutoAssignmentRequest } from '../model.type'

const UButton = resolveComponent('UButton')
const UInput = resolveComponent('UInput')
const UCheckbox = resolveComponent('UCheckbox')

interface Props {
  selectedParcels?: string[] // Optional: specific parcels to assign
  availableShippers?: Array<{ id: string; name: string }>
  zoneId?: string
}

interface Emits {
  (e: 'close', result: AutoAssignmentRequest | null): void
}

const props = withDefaults(defineProps<Props>(), {
  selectedParcels: () => [],
  availableShippers: () => [],
  zoneId: undefined,
})

const emit = defineEmits<Emits>()

const useSelectedParcels = ref(props.selectedParcels.length > 0)
const selectedShipperIds = ref<string[]>([])
const zoneIdInput = ref<string>(props.zoneId || '')
const selectAllShippers = ref(true)

const parcelIds = computed(() => {
  return useSelectedParcels.value ? props.selectedParcels : undefined
})

const shipperIds = computed(() => {
  if (selectAllShippers.value || selectedShipperIds.value.length === 0) {
    return undefined // undefined means all available shippers
  }
  return selectedShipperIds.value
})

const handleConfirm = () => {
  const request: AutoAssignmentRequest = {
    parcelIds: parcelIds.value,
    shipperIds: shipperIds.value,
    zoneId: zoneIdInput.value || undefined,
  }

  emit('close', request)
}

const handleCancel = () => {
  emit('close', null)
}

const toggleShipper = (shipperId: string) => {
  const index = selectedShipperIds.value.indexOf(shipperId)
  if (index > -1) {
    selectedShipperIds.value.splice(index, 1)
  } else {
    selectedShipperIds.value.push(shipperId)
  }
}

watch(
  () => props.selectedParcels,
  (newParcels) => {
    useSelectedParcels.value = newParcels.length > 0
  },
  { immediate: true },
)

watch(
  () => props.zoneId,
  (newZoneId) => {
    if (newZoneId) {
      zoneIdInput.value = newZoneId
    }
  },
  { immediate: true },
)

watch(
  () => selectAllShippers.value,
  (newVal) => {
    if (newVal) {
      selectedShipperIds.value = []
    }
  },
)
</script>

<template>
  <UModal
    title="Gán Task Tự Động (VRP Solver)"
    description="Sử dụng VRP solver để tự động phân bổ tasks tối ưu"
    :close="{ onClick: handleCancel }"
    :ui="{
      content: 'min-w-[600px]',
    }"
  >
    <template #body>
      <div class="space-y-4">
        <!-- Selected Parcels Info -->
        <div v-if="selectedParcels.length > 0" class="rounded-lg bg-gray-50 p-3">
          <div class="flex items-center gap-2">
            <UCheckbox v-model="useSelectedParcels" />
            <div>
              <div class="text-sm text-gray-600">Sử dụng {{ selectedParcels.length }} đơn hàng đã chọn</div>
              <div class="text-xs text-gray-500">
                Bỏ chọn để gán tất cả đơn hàng chưa được gán
              </div>
            </div>
          </div>
        </div>
        <div v-else class="rounded-lg bg-blue-50 p-3">
          <div class="text-sm text-blue-800">
            Sẽ gán tất cả đơn hàng chưa được gán cho các shippers
          </div>
        </div>

        <!-- Shipper Selection -->
        <div>
          <label class="mb-2 block text-sm font-medium">Chọn Shippers</label>
          <div class="rounded-lg border border-gray-200 p-3">
            <div class="mb-2 flex items-center gap-2">
              <UCheckbox v-model="selectAllShippers" />
              <span class="text-sm font-medium">Tất cả shippers có sẵn</span>
            </div>
            <div v-if="!selectAllShippers" class="mt-2 space-y-2 max-h-48 overflow-y-auto">
              <div
                v-for="shipper in availableShippers"
                :key="shipper.id"
                class="flex items-center gap-2"
              >
                <UCheckbox
                  :model-value="selectedShipperIds.includes(shipper.id)"
                  @update:model-value="toggleShipper(shipper.id)"
                />
                <span class="text-sm">{{ shipper.name }}</span>
              </div>
            </div>
            <div v-else class="mt-2 text-sm text-gray-500">
              {{ availableShippers.length }} shippers sẽ được sử dụng
            </div>
          </div>
        </div>

        <!-- Zone ID (Optional) -->
        <div>
          <label class="mb-2 block text-sm font-medium">Zone ID (Tùy chọn)</label>
          <UInput v-model="zoneIdInput" placeholder="Nhập zone ID để filter" />
        </div>

        <!-- Info Note -->
        <div class="rounded-lg bg-blue-50 p-3">
          <div class="text-sm text-blue-800">
            <strong>Lưu ý:</strong> Hệ thống sẽ sử dụng VRP solver để tự động phân bổ tasks tối ưu
            cho các shippers dựa trên vị trí, workload, và time windows.
          </div>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <UButton variant="outline" color="neutral" @click="handleCancel"> Hủy </UButton>
        <UButton @click="handleConfirm"> Chạy Auto Assignment </UButton>
      </div>
    </template>
  </UModal>
</template>
