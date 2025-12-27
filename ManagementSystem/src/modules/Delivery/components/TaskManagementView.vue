<script setup lang="ts">
/**
 * Task Management View
 * Admin page for managing task assignments (manual and auto)
 */

import {
  onMounted,
  defineAsyncComponent,
  ref,
  resolveComponent,
  h,
} from 'vue'
import { useRouter } from 'vue-router'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import type { Column } from '@tanstack/table-core'
import type { FilterGroup, SortConfig } from '@/common/types/filter'
import { useTaskManagement } from '../composables/useTaskManagement'
import { getParcelsV2 } from '../../Parcels/api'
import { getDeliveryMenV2 } from '../api'
import TableHeaderCell from '@/common/components/TableHeaderCell.vue'
import { ParcelDto, type ParcelStatus } from '../../Parcels/model.type'
import { DeliveryManDto } from '../model.type'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import type { QueryPayload } from '@/common/types/filter'
import type { TableColumn } from '@nuxt/ui'

const PageHeader = defineAsyncComponent(() => import('@/common/components/PageHeader.vue'))
const LazyManualAssignmentModal = defineAsyncComponent(
  () => import('./ManualAssignmentModal.vue'),
)
const LazyAutoAssignmentModal = defineAsyncComponent(() => import('./AutoAssignmentModal.vue'))

const overlay = useOverlay()
const router = useRouter()
const toast = useToast()

const UCheckbox = resolveComponent('UCheckbox')
const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')
const UCard = resolveComponent('UCard')

// Task management composable
const {
  assignManually,
  assignAutomatically,
  loading: taskLoading,
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


// Filters
const filters = ref<FilterGroup | undefined>(undefined)
const sorts = ref<SortConfig[]>([])


/**
 * Get status color
 */
const getStatusColor = (status: ParcelStatus): string => {
  const colorMap: Record<ParcelStatus, string> = {
    IN_WAREHOUSE: 'blue',
    ON_ROUTE: 'yellow',
    DELIVERED: 'green',
    SUCCEEDED: 'green',
    FAILED: 'red',
    DELAYED: 'orange',
    DISPUTE: 'purple',
    LOST: 'gray',
  }
  return colorMap[status] || 'gray'
}

/**
 * Get display status
 */
const getDisplayStatus = (status: ParcelStatus): string => {
  const statusMap: Record<ParcelStatus, string> = {
    IN_WAREHOUSE: 'In Warehouse',
    ON_ROUTE: 'On Route',
    DELIVERED: 'Delivered',
    SUCCEEDED: 'Succeeded',
    FAILED: 'Failed',
    DELAYED: 'Delayed',
    DISPUTE: 'Dispute',
    LOST: 'Lost',
  }
  return statusMap[status] || status
}

/**
 * Setup header component for table columns
 */
type HeaderConfig = {
  variant: 'ghost' | 'outline' | 'solid' | 'soft' | 'link'
  label: string
  class: string
  activeColor?: string
  inactiveColor?: string
  filterable?: boolean
}

type HeaderColumn = Column<ParcelDto, unknown>

const setupHeader = ({ column, config }: { column: HeaderColumn; config: HeaderConfig }) =>
  h(TableHeaderCell<ParcelDto>, {
    column,
    config,
    filterableColumns: [],
    activeFilters: [],
    'onUpdate:filters': () => {},
  })

/**
 * Wrap cell with click handler for selection
 */
const wrapCellWithClickHandler = (content: ReturnType<typeof h>, parcel: ParcelDto) => {
  return h(
    'div',
    {
      onClick: () => toggleParcelSelection(parcel.id),
      style: { cursor: 'pointer' },
    },
    content,
  )
}

// Table columns configuration
const columns: TableColumn<ParcelDto>[] = [
  {
    accessorKey: 'select',
    header: ({ table }) =>
      h(UCheckbox, {
        modelValue: parcels.value.length > 0 && parcels.value.every((p) => selectedParcelIds.value.includes(p.id))
          ? true
          : parcels.value.some((p) => selectedParcelIds.value.includes(p.id))
            ? 'indeterminate'
            : false,
        'onUpdate:modelValue': (value: boolean | 'indeterminate') => {
          if (value) {
            // Select all
            selectedParcelIds.value = parcels.value.map((p) => p.id)
          } else {
            // Deselect all
            selectedParcelIds.value = []
          }
        },
        'aria-label': 'Select all',
      }),
    cell: ({ row }) => {
      const parcel = row.original
      return h(UCheckbox, {
        modelValue: selectedParcelIds.value.includes(parcel.id),
        'onUpdate:modelValue': () => toggleParcelSelection(parcel.id),
        'aria-label': 'Select row',
        onClick: (e: MouseEvent) => e.stopPropagation(),
      })
    },
  },
  {
    accessorKey: 'code',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost' as const,
          label: 'Mã đơn',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: false,
        },
      }),
    cell: ({ row }) => {
      const parcel = row.original
      return h(
        'span',
        {
          class: 'font-mono text-sm',
          onClick: (e: MouseEvent) => {
            e.stopPropagation()
            toggleParcelSelection(parcel.id)
          },
          style: { cursor: 'pointer' },
        },
        parcel.code,
      )
    },
  },
  {
    accessorKey: 'receiverId',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost' as const,
          label: 'Người nhận',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: false,
        },
      }),
    cell: ({ row }) => {
      const parcel = row.original
      // Use nested object if available, fallback to old fields
      const receiverName = parcel.receiver?.fullName || parcel.receiverName || parcel.receiverId || 'N/A'
      return wrapCellWithClickHandler(h('span', receiverName), parcel)
    },
  },
  {
    accessorKey: 'targetDestination',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost' as const,
          label: 'Địa chỉ giao',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: false,
        },
      }),
    cell: ({ row }) => {
      const parcel = row.original
      // Use nested address object if available, fallback to targetDestination
      const address = parcel.receiverAddress?.note || parcel.targetDestination || 'N/A'
      return wrapCellWithClickHandler(h('span', { class: 'line-clamp-1' }, address), parcel)
    },
  },
  {
    accessorKey: 'status',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost' as const,
          label: 'Trạng thái',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: false,
        },
      }),
    cell: ({ row }) => {
      const parcel = row.original
      const status = row.getValue('status') as ParcelStatus
      const color = getStatusColor(status)
      const displayStatus = getDisplayStatus(status)
      const badge = h(UBadge, { class: 'capitalize', variant: 'soft', color }, () => displayStatus)
      return wrapCellWithClickHandler(badge, parcel)
    },
  },
  {
    accessorKey: 'deliveryType',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost' as const,
          label: 'Loại giao hàng',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: false,
        },
      }),
    cell: ({ row }) => {
      const parcel = row.original
      const deliveryType = row.getValue('deliveryType') as string
      const badge = h(UBadge, { variant: 'soft', color: 'primary', size: 'sm' }, () => deliveryType)
      return wrapCellWithClickHandler(badge, parcel)
    },
  },
  {
    accessorKey: 'zoneId',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost' as const,
          label: 'Zone ID',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: false,
        },
      }),
    cell: ({ row }) => {
      const parcel = row.original
      // Get zoneId from receiverAddress or direct zoneId field
      const zoneId = parcel.zoneId || parcel.receiverAddress?.zoneId || 'N/A'
      return wrapCellWithClickHandler(h('span', { class: 'font-mono text-xs' }, zoneId), parcel)
    },
  },
]

/**
 * Load parcels (unassigned or all)
 */
const loadParcels = async () => {
  parcelsLoading.value = true
  try {
    const payload: QueryPayload = {
      page: parcelsPage.value,
      size: parcelsPageSize.value,
      filters: filters.value ? { logic: 'AND', conditions: [filters.value] } : undefined,
      sorts: sorts.value.length > 0 ? sorts.value : undefined,
    }

    const response = await getParcelsV2(payload)
    if (response.result?.data) {
      parcels.value = response.result.data.map((p: unknown) => new ParcelDto(p as ParcelDto))
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
      shippers.value = response.result.data.map((s: unknown) => new DeliveryManDto(s as DeliveryManDto))
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
const openManualAssignmentModal = async () => {
  if (selectedParcelIds.value.length === 0) {
    toast.add({
      title: 'Chưa chọn đơn hàng',
      description: 'Vui lòng chọn ít nhất một đơn hàng để gán',
      color: 'warning',
    })
    return
  }

  // Get zoneId from first selected parcel's receiver address
  const firstSelectedParcel = parcels.value.find((p) => selectedParcelIds.value.includes(p.id))
  const parcelZoneId = firstSelectedParcel?.zoneId || firstSelectedParcel?.receiverAddress?.zoneId

  const modal = overlay.create(LazyManualAssignmentModal)
  const instance = modal.open({
    selectedParcels: [...selectedParcelIds.value],
    availableShippers: shippers.value.map((s) => ({ id: s.id, name: s.displayName, zoneId: s.zoneId || undefined })),
    zoneId: parcelZoneId, // Pass parcel zoneId as default
  })
  const request = await instance.result

  if (request) {
    try {
      await assignManually(request)
      // Refresh parcels list
      await loadParcels()
      selectedParcelIds.value = []
    } catch {
      // Error already handled in composable
    }
  }
}

/**
 * Open auto assignment modal
 */
const openAutoAssignmentModal = async () => {
  // Get zoneId from first selected parcel's receiver address (if any selected)
  const firstSelectedParcel = selectedParcelIds.value.length > 0
    ? parcels.value.find((p) => selectedParcelIds.value.includes(p.id))
    : null
  const parcelZoneId = firstSelectedParcel?.zoneId || firstSelectedParcel?.receiverAddress?.zoneId

  const modal = overlay.create(LazyAutoAssignmentModal)
  const instance = modal.open({
    selectedParcels: selectedParcelIds.value.length > 0 ? [...selectedParcelIds.value] : [],
    availableShippers: shippers.value.map((s) => ({ id: s.id, name: s.displayName, zoneId: s.zoneId || undefined })),
    zoneId: parcelZoneId, // Pass parcel zoneId as default
  })
  const request = await instance.result

  if (request) {
    try {
      // Start assignment (non-blocking, progress shown in global tracker)
      await assignAutomatically(request)
      // Refresh parcels list after a short delay to allow process to complete
      setTimeout(async () => {
        await loadParcels()
        selectedParcelIds.value = []
      }, 2000)
    } catch (error) {
      // Error already handled in composable and global tracker
    }
  }
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

      <UTable
        :data="parcels"
        :columns="columns"
        :loading="parcelsLoading"
        :ui="{
          empty: 'text-center py-12',
          root: 'h-[50vh]',
          thead: 'sticky top-0 bg-white dark:bg-gray-800',
        }"
      />

      <!-- Pagination -->
      <div class="flex items-center justify-between p-4 border-t">
        <div class="text-sm text-gray-600">
          Trang {{ parcelsPage + 1 }} / {{ Math.max(1, Math.ceil(parcelsTotal / parcelsPageSize)) }} (Tổng:
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

  </div>
</template>
