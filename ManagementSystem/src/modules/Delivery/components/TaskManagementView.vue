<script setup lang="ts">
/**
 * Task Management View
 * Admin page for managing task assignments (manual and auto)
 */

import {
  onMounted,
  defineAsyncComponent,
  ref,
  computed,
  watch,
  reactive,
  resolveComponent,
  h,
} from 'vue'
import { useRouter } from 'vue-router'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import { useTemplateRef } from 'vue'
import type { SortingState, Column } from '@tanstack/table-core'
import type { FilterCondition, FilterGroup, SortConfig } from '@/common/types/filter'
import { useTaskManagement } from '../composables/useTaskManagement'
import { getParcelsV2 } from '../../Parcels/api'
import { getDeliveryMenV2 } from '../api'
import TableHeaderCell from '@/common/components/TableHeaderCell.vue'
import AdvancedFilterDrawer from '@/common/components/filters/AdvancedFilterDrawer.vue'
import TableFilters from '@/common/components/table/TableFilters.vue'
import type { ParcelDto } from '../../Parcels/model.type'
import { DeliveryManDto } from '../model.type'
import type { ManualAssignmentRequest, AutoAssignmentRequest } from '../model.type'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import type { QueryPayload } from '@/common/types/filter'
import { createSortConfig } from '@/common/utils/query-builder'

const PageHeader = defineAsyncComponent(() => import('@/common/components/PageHeader.vue'))
const ManualAssignmentModal = defineAsyncComponent(
  () => import('./ManualAssignmentModal.vue'),
)
const AutoAssignmentModal = defineAsyncComponent(() => import('./AutoAssignmentModal.vue'))

const overlay = useOverlay()
const router = useRouter()
const table = useTemplateRef('table')
const toast = useToast()

const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')
const UCard = resolveComponent('UCard')

// Task management composable
const {
  assignManually,
  assignAutomatically,
  loading: taskLoading,
  manualAssigning,
  autoAssigning,
} = useTaskManagement()

// Parcels state
const parcels = ref<ParcelDto[]>([])
const parcelsLoading = ref(false)
const parcelsPage = ref(0)
const parcelsPageSize = ref(20)
const parcelsTotal = ref(0)
const selectedParcelIds = ref<string[]>([])

// Shippers state
const shippers = ref<DeliveryManDto[]>([])
const shippersLoading = ref(false)

// Modal state
const manualAssignmentModalData = ref<{
  selectedParcels: string[]
  availableShippers: Array<{ id: string; name: string }>
  zoneId?: string
} | null>(null)

const autoAssignmentModalData = ref<{
  selectedParcels: string[]
  availableShippers: Array<{ id: string; name: string }>
  zoneId?: string
} | null>(null)

// Filters
const filters = ref<FilterGroup | undefined>(undefined)
const sorts = ref<SortConfig[]>([])
const searchQuery = ref('')

/**
 * Load parcels (unassigned or all)
 */
const loadParcels = async () => {
  parcelsLoading.value = true
  try {
    const payload: QueryPayload = {
      page: parcelsPage.value,
      size: parcelsPageSize.value,
      filters: filters.value ? { conditions: [filters.value] } : undefined,
      sorts: sorts.value.length > 0 ? sorts.value : undefined,
    }

    const response = await getParcelsV2(payload)
    if (response.result?.data) {
      parcels.value = response.result.data.map((p: any) => new ParcelDto(p))
      parcelsTotal.value = response.result.page?.totalElements || 0
    }
  } catch (error: any) {
    toast.add({
      title: 'Lỗi tải danh sách đơn hàng',
      description: error.message || 'Không thể tải danh sách đơn hàng',
      color: 'error',
    })
  } finally {
    parcelsLoading.value = false
  }
}

/**
 * Load available shippers
 */
const loadShippers = async () => {
  shippersLoading.value = true
  try {
    const payload: QueryPayload = {
      page: 0,
      size: 100, // Get all shippers
    }

    const response = await getDeliveryMenV2(payload)
    if (response.result?.data) {
      shippers.value = response.result.data.map((s: any) => new DeliveryManDto(s))
    }
  } catch (error: any) {
    toast.add({
      title: 'Lỗi tải danh sách shippers',
      description: error.message || 'Không thể tải danh sách shippers',
      color: 'error',
    })
  } finally {
    shippersLoading.value = false
  }
}

/**
 * Open manual assignment modal
 */
const openManualAssignmentModal = () => {
  if (selectedParcelIds.value.length === 0) {
    toast.add({
      title: 'Chưa chọn đơn hàng',
      description: 'Vui lòng chọn ít nhất một đơn hàng để gán',
      color: 'warning',
    })
    return
  }

  manualAssignmentModalData.value = {
    selectedParcels: [...selectedParcelIds.value],
    availableShippers: shippers.value.map((s) => ({ id: s.id, name: s.displayName })),
  }
}

/**
 * Handle manual assignment
 */
const handleManualAssignment = async (request: ManualAssignmentRequest | null) => {
  if (!request) {
    manualAssignmentModalData.value = null
    return
  }

  try {
    await assignManually(request)
    // Refresh parcels list
    await loadParcels()
    selectedParcelIds.value = []
    manualAssignmentModalData.value = null
  } catch (error) {
    // Error already handled in composable
  }
}

/**
 * Open auto assignment modal
 */
const openAutoAssignmentModal = () => {
  autoAssignmentModalData.value = {
    selectedParcels: selectedParcelIds.value.length > 0 ? [...selectedParcelIds.value] : [],
    availableShippers: shippers.value.map((s) => ({ id: s.id, name: s.displayName })),
  }
}

/**
 * Handle auto assignment
 */
const handleAutoAssignment = async (request: AutoAssignmentRequest | null) => {
  if (!request) {
    autoAssignmentModalData.value = null
    return
  }

  try {
    const result = await assignAutomatically(request)
    // Refresh parcels list
    await loadParcels()
    selectedParcelIds.value = []
    autoAssignmentModalData.value = null
    // Show result details
    if (result) {
      toast.add({
        title: 'Chi tiết kết quả',
        description: `Đã tạo assignments cho ${result.statistics.totalShippers} shippers`,
        color: 'info',
      })
    }
  } catch (error) {
    // Error already handled in composable
  }
}

/**
 * Handle search
 */
const handleSearch = (query: string) => {
  searchQuery.value = query
  // Add search filter
  // TODO: Implement search filter
  loadParcels()
}

/**
 * Handle page change
 */
const handlePageChange = (page: number) => {
  parcelsPage.value = page
  loadParcels()
}

/**
 * Toggle parcel selection
 */
const toggleParcelSelection = (parcelId: string) => {
  const index = selectedParcelIds.value.indexOf(parcelId)
  if (index > -1) {
    selectedParcelIds.value.splice(index, 1)
  } else {
    selectedParcelIds.value.push(parcelId)
  }
}

/**
 * Select all parcels on current page
 */
const selectAllParcels = () => {
  const currentPageIds = parcels.value.map((p) => p.id)
  if (currentPageIds.every((id) => selectedParcelIds.value.includes(id))) {
    // Deselect all
    selectedParcelIds.value = selectedParcelIds.value.filter(
      (id) => !currentPageIds.includes(id),
    )
  } else {
    // Select all
    currentPageIds.forEach((id) => {
      if (!selectedParcelIds.value.includes(id)) {
        selectedParcelIds.value.push(id)
      }
    })
  }
}

onMounted(() => {
  loadParcels()
  loadShippers()
})
</script>

<template>
  <div class="space-y-4">
    <PageHeader title="Quản Lý Task" description="Quản lý gán task cho shippers (thủ công và tự động)">
      <template #actions>
        <UButton
          variant="soft"
          color="primary"
          icon="i-heroicons-calendar-days"
          size="sm"
          class="md:size-md"
          @click="router.push({ name: 'delivery-shift-calendar' })"
        >
          <span class="hidden sm:inline">Shift Calendar</span>
          <span class="sm:hidden">Calendar</span>
        </UButton>
        <UButton
          variant="soft"
          color="primary"
          icon="i-heroicons-truck"
          size="sm"
          class="md:size-md"
          @click="router.push({ name: 'delivery-shippers' })"
        >
          <span class="hidden sm:inline">Shippers</span>
          <span class="sm:hidden">Shippers</span>
        </UButton>
      </template>
    </PageHeader>

    <!-- Action Bar -->
    <UCard>
      <div class="flex items-center justify-between p-4">
        <div class="flex items-center gap-2">
          <span class="text-sm text-gray-600">
            Đã chọn: <strong>{{ selectedParcelIds.length }}</strong> đơn hàng
          </span>
        </div>
        <div class="flex gap-2">
          <UButton
            variant="outline"
            :disabled="selectedParcelIds.length === 0"
            @click="openManualAssignmentModal"
          >
            Gán Thủ Công
          </UButton>
          <UButton
            :disabled="taskLoading"
            @click="openAutoAssignmentModal"
          >
            Gán Tự Động (VRP)
          </UButton>
        </div>
      </div>
    </UCard>

    <!-- Parcels Table -->
    <UCard>
      <template #header>
        <div class="flex items-center justify-between">
          <h3 class="text-lg font-semibold">Danh Sách Đơn Hàng</h3>
          <UButton variant="ghost" size="sm" @click="loadParcels"> Refresh </UButton>
        </div>
      </template>

      <div v-if="parcelsLoading" class="flex justify-center py-8">
        <div class="text-gray-500">Đang tải...</div>
      </div>
      <div v-else-if="parcels.length === 0" class="py-8 text-center text-gray-500">
        Không có đơn hàng nào
      </div>
      <div v-else class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-4 py-2 text-left">
                <input
                  type="checkbox"
                  :checked="
                    parcels.length > 0 &&
                    parcels.every((p) => selectedParcelIds.includes(p.id))
                  "
                  @change="selectAllParcels"
                />
              </th>
              <th class="px-4 py-2 text-left">Mã đơn</th>
              <th class="px-4 py-2 text-left">Người nhận</th>
              <th class="px-4 py-2 text-left">Địa chỉ giao</th>
              <th class="px-4 py-2 text-left">Trạng thái</th>
              <th class="px-4 py-2 text-left">Loại giao hàng</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr
              v-for="parcel in parcels"
              :key="parcel.id"
              class="hover:bg-gray-50 cursor-pointer"
              @click="toggleParcelSelection(parcel.id)"
            >
              <td class="px-4 py-2">
                <input
                  type="checkbox"
                  :checked="selectedParcelIds.includes(parcel.id)"
                  @click.stop="toggleParcelSelection(parcel.id)"
                />
              </td>
              <td class="px-4 py-2 font-medium">{{ parcel.code }}</td>
              <td class="px-4 py-2">{{ parcel.receiverName || 'N/A' }}</td>
              <td class="px-4 py-2">{{ parcel.targetDestination || 'N/A' }}</td>
              <td class="px-4 py-2">
                <UBadge :color="parcel.status === 'IN_WAREHOUSE' ? 'blue' : 'gray'">
                  {{ parcel.status }}
                </UBadge>
              </td>
              <td class="px-4 py-2">{{ parcel.deliveryType || 'N/A' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="flex items-center justify-between p-4 border-t">
        <div class="text-sm text-gray-600">
          Trang {{ parcelsPage + 1 }} / {{ Math.ceil(parcelsTotal / parcelsPageSize) }} (Tổng:
          {{ parcelsTotal }})
        </div>
        <div class="flex gap-2">
          <UButton
            variant="ghost"
            size="sm"
            :disabled="parcelsPage === 0"
            @click="handlePageChange(parcelsPage - 1)"
          >
            Trước
          </UButton>
          <UButton
            variant="ghost"
            size="sm"
            :disabled="parcelsPage >= Math.ceil(parcelsTotal / parcelsPageSize) - 1"
            @click="handlePageChange(parcelsPage + 1)"
          >
            Sau
          </UButton>
        </div>
      </div>
    </UCard>

    <!-- Manual Assignment Modal -->
    <ManualAssignmentModal
      v-if="manualAssignmentModalData"
      :selected-parcels="manualAssignmentModalData.selectedParcels"
      :available-shippers="manualAssignmentModalData.availableShippers"
      :zone-id="manualAssignmentModalData.zoneId"
      @close="handleManualAssignment"
    />

    <!-- Auto Assignment Modal -->
    <AutoAssignmentModal
      v-if="autoAssignmentModalData"
      :selected-parcels="autoAssignmentModalData.selectedParcels"
      :available-shippers="autoAssignmentModalData.availableShippers"
      :zone-id="autoAssignmentModalData.zoneId"
      @close="handleAutoAssignment"
    />
  </div>
</template>
