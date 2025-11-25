<script setup lang="ts">
/**
 * Parcels List View
 *
 * Main view for managing parcels with UTable and Nuxt UI v3 best practices
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
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import { useParcels } from './composables'
import { useParcelExport } from './composables/useParcelExport'
import type { ParcelDto, ParcelStatus } from './model.type'
import { seedParcels, type SeedParcelsRequest } from './api'
import UserSelect from '@/common/components/UserSelect.vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { useTemplateRef } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import AdvancedFilterDrawer from '../../common/components/filters/AdvancedFilterDrawer.vue'
import type { SortingState, Column } from '@tanstack/table-core'
import type { FilterCondition, FilterGroup } from '../../common/types/filter'
import { createSortConfig } from '../../common/utils/query-builder'
import TableHeaderCell from '../../common/components/TableHeaderCell.vue'
import { useRouter } from 'vue-router'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'
import { useGlobalChat, type GlobalChatListener } from '../Communication/composables'
import { useConversations } from '../Communication/composables'
import { onUnmounted } from 'vue'

// Dynamic imports to avoid TypeScript issues
const PageHeader = defineAsyncComponent(() => import('../../common/components/PageHeader.vue'))

// Lazy load modals
const LazyParcelFormModal = defineAsyncComponent(() => import('./components/ParcelFormModal.vue'))
const LazyParcelDeleteModal = defineAsyncComponent(() => import('./components/ParcelDeleteModal.vue'))
const LazyParcelQRModal = defineAsyncComponent(() => import('./components/ParcelQRModal.vue'))
const UCheckbox = resolveComponent('UCheckbox')
const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')

const overlay = useOverlay()
const table = useTemplateRef('table')
const toast = useToast()
const router = useRouter()
const currentUser = getCurrentUser()
const { findOrCreateConversation } = useConversations()

// Global chat service for update notifications
const globalChat = useGlobalChat()

// Update notification listener
const updateNotificationListener: GlobalChatListener = {
  onUpdateNotificationReceived: (updateNotification: any) => {
    if (!updateNotification) return

    const entityType = updateNotification.entityType
    const action = updateNotification.action

    // Handle SESSION_UPDATE: when session completed, clear in-progress parcels
    if (entityType === 'SESSION' && action === 'COMPLETED') {
      const message = updateNotification.message || 'Phiên giao hàng đã kết thúc'
      toast.add({
        title: 'Phiên giao hàng đã kết thúc',
        description: message,
        color: 'info',
        icon: 'i-heroicons-truck',
      })

      // Refresh parcel list to clear in-progress parcels
      loadParcels()
    }

    // Handle PARCEL_UPDATE notifications
    if (entityType === 'PARCEL') {
      if (action === 'STATUS_CHANGED' || action === 'UPDATED' || action === 'COMPLETED') {
        // Show light notification
        const message = updateNotification.message || 'Đơn hàng đã được cập nhật'
        toast.add({
          title: 'Cập nhật đơn hàng',
          description: message,
          color: 'success',
        })

        // Refresh parcel list
        loadParcels()
      }
    }
  },
}

// Export composable
const { exportParcels } = useParcelExport()

// Seed parcels state
const showSeedModal = ref(false)
const seeding = ref(false)
const seedForm = ref<SeedParcelsRequest>({
  count: 20,
  shopId: undefined,
  clientId: undefined,
})

// Composables
const {
  parcels,
  loading,
  page,
  pageSize,
  total,
  filters,
  sorts,
  loadParcels,
  create,
  update,
  remove,
  bulkDelete,
  handleSearch,
  updateFilters,
  updateSorts,
  clearFilters,
  getFilterableColumns,
  handlePageChange,
} = useParcels()

// Handle page change from UPagination (1-indexed) to API (0-indexed)
const handlePaginationChange = (newPage: number) => {
  // UPagination uses 1-indexed pages, convert to 0-indexed for API
  handlePageChange(Math.max(newPage - 1, 0))
}

// Table state
const selected = ref<ParcelDto[]>([])
const sorting = ref<Array<{ id: string; desc: boolean }>>([])
const activeFilters = ref<FilterCondition[]>([])
const columnFiltersState = reactive<Record<string, FilterCondition[]>>({})
const advancedFiltersGroup = ref<FilterGroup | undefined>(undefined)

// Search and filter state
const searchValue = ref('')

// Advanced filter state
const showAdvancedFilters = ref(false)
const filterableColumns = computed(() => getFilterableColumns())

/**
 * Setup header component for table columns
 */
type HeaderConfig = {
  variant: 'link' | 'solid' | 'outline' | 'soft' | 'ghost'
  label: string
  class: string
  activeColor?: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
  inactiveColor?: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
  filterable?: boolean
}

type HeaderColumn = Column<ParcelDto, unknown>

const setupHeader = ({
  column,
  config,
}: {
  column: HeaderColumn
  config: HeaderConfig
}) =>
  h(TableHeaderCell<ParcelDto>, {
    column,
    config,
    filterableColumns: filterableColumns.value,
    activeFilters: activeFilters.value,
    'onUpdate:filters': handleFiltersUpdate,
  })

// Helper function to get all active filters
const getAllActiveFilters = (): FilterCondition[] | undefined => {
  if (!filters.value || !filters.value.conditions) return undefined

  // Extract all conditions from the filter group structure
  const extractConditions = (item: FilterCondition | FilterGroup): FilterCondition[] => {
    if ('field' in item) {
      // It's a FilterCondition
      return [item as FilterCondition]
    } else if ('conditions' in item) {
      // It's a FilterGroup
      return item.conditions.flatMap(extractConditions)
    }
    return []
  }

  const allFilters = filters.value.conditions.flatMap(extractConditions)
  return allFilters.length > 0 ? allFilters : undefined
}

// Helper function to get filter structure for display
const getFilterStructure = (): string => {
  if (!filters.value || !filters.value.conditions) return ''

  const formatItem = (item: FilterCondition | FilterGroup): string => {
    if ('field' in item) {
      // It's a FilterCondition
      const condition = item as FilterCondition
      return `${getColumnLabel(condition.field)} ${getOperatorLabel(condition.operator)} ${condition.value}`
    } else if ('conditions' in item) {
      // It's a FilterGroup
      const group = item as FilterGroup
      const groupContent = group.conditions
        .map((subItem, subIndex) => {
          const itemStr = formatItem(subItem)
          // Add logic operator between items (except first item)
          return subIndex > 0 && subItem.logic ? `${subItem.logic} ${itemStr}` : itemStr
        })
        .join(' ')
      return `(${groupContent})`
    }
    return ''
  }

  return filters.value.conditions
    .map((item, index) => {
      const itemStr = formatItem(item)
      // Add logic operator between items (except first item)
      return index > 0 && item.logic ? `${item.logic} ${itemStr}` : itemStr
    })
    .join(' ')
}

// Helper function to get active filter group
const getActiveFilterGroup = (): FilterGroup | undefined => {
  if (!filters.value || !filters.value.conditions) return undefined
  return filters.value
}

// Advanced filter handlers
const handleAdvancedFilterApply = (filterGroup: FilterGroup) => {
  advancedFiltersGroup.value = filterGroup && filterGroup.conditions.length > 0 ? filterGroup : undefined
  applyCombinedFilters()
  showAdvancedFilters.value = false
}

const handleAdvancedFilterClear = () => {
  advancedFiltersGroup.value = undefined
  Object.keys(columnFiltersState).forEach((key) => {
    delete columnFiltersState[key]
  })
  activeFilters.value = []
  clearFilters()
  showAdvancedFilters.value = false
}

/**
 * Handle filters update from column filters
 */
interface ColumnFilterUpdatePayload {
  columnId: string
  filters: FilterCondition[]
}

const applyCombinedFilters = () => {
  const columnFilters = Object.values(columnFiltersState).flat()

  const combinedConditions: (FilterCondition | FilterGroup)[] = []

  if (advancedFiltersGroup.value && advancedFiltersGroup.value.conditions.length > 0) {
    combinedConditions.push(advancedFiltersGroup.value)
  }

  if (columnFilters.length > 0) {
    combinedConditions.push(...columnFilters)
  }

  if (combinedConditions.length === 0) {
    clearFilters()
    return
  }

  updateFilters({
    logic: 'AND',
    conditions: combinedConditions,
  })
}

const handleFiltersUpdate = ({ columnId, filters }: ColumnFilterUpdatePayload) => {
  if (filters.length > 0) {
    columnFiltersState[columnId] = filters.map((filter) => ({ ...filter }))
  } else {
    delete columnFiltersState[columnId]
  }

  const columnFilters = Object.values(columnFiltersState).flat()
  activeFilters.value = columnFilters

  applyCombinedFilters()
}

// Table columns configuration
const columns: TableColumn<ParcelDto>[] = [
  {
    accessorKey: 'actions',
    header: 'Actions',
    cell: ({ row }) => {
      const parcel = row.original
      return h('div', { class: 'flex space-x-2' }, [
        h(UButton, {
          icon: 'i-heroicons-chat-bubble-left-right',
          size: 'sm',
          variant: 'ghost',
          title: 'Chat with receiver',
          onClick: () => openChat(parcel),
        }),
        h(UButton, {
          icon: 'i-heroicons-qr-code',
          size: 'sm',
          variant: 'ghost',
          title: 'Show QR Code',
          onClick: () => openQRModal(parcel),
        }),
        h(UButton, {
          icon: 'i-heroicons-pencil',
          size: 'sm',
          variant: 'ghost',
          title: 'Edit parcel',
          onClick: () => openEditModal(parcel),
        }),
        h(UButton, {
          icon: 'i-heroicons-trash',
          size: 'sm',
          variant: 'ghost',
          color: 'error',
          title: 'Delete parcel',
          onClick: () => openDeleteModal(parcel),
        }),
      ])
    },
  },
  {
    id: 'select',
    header: ({ table }) =>
      h(UCheckbox, {
        modelValue: table.getIsSomePageRowsSelected()
          ? 'indeterminate'
          : table.getIsAllPageRowsSelected(),
        'onUpdate:modelValue': (value: boolean | 'indeterminate') =>
          table.toggleAllPageRowsSelected(!!value),
        'aria-label': 'Select all',
      }),
    cell: ({ row }) =>
      h(UCheckbox, {
        modelValue: row.getIsSelected(),
        'onUpdate:modelValue': (value: boolean | 'indeterminate') => row.toggleSelected(!!value),
        'aria-label': 'Select row',
      }),
  },
  {
    accessorKey: 'code',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Code',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
  },
  {
    accessorKey: 'senderId',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Sender',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
    cell: ({ row }) => {
      const parcel = row.original
      return h('span', parcel.senderName || parcel.senderId)
    },
  },
  {
    accessorKey: 'receiverId',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Receiver',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
    cell: ({ row }) => {
      const parcel = row.original
      return h('span', parcel.receiverName || parcel.receiverId)
    },
  },
  {
    accessorKey: 'deliveryType',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Delivery Type',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
    cell: ({ row }) => {
      const deliveryType = row.getValue('deliveryType') as string
      return h(UBadge, { variant: 'soft', color: 'primary', size: 'sm' }, () => deliveryType)
    },
  },
  {
    accessorKey: 'status',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Status',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
    cell: ({ row }) => {
      const status = row.getValue('status') as ParcelStatus
      const color = getStatusColor(status)
      const displayStatus = getDisplayStatus(status)
      return h(UBadge, { class: 'capitalize', variant: 'soft', color }, () => displayStatus)
    },
  },
  {
    accessorKey: 'createdAt',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Created At',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
    cell: ({ row }) => {
      const createdAt = row.getValue('createdAt') as string
      return h('span', new Date(createdAt).toLocaleString())
    },
  },
  {
    accessorKey: 'weight',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Weight',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
    cell: ({ row }) => {
      const weight = row.getValue('weight') as number
      return h('span', `${weight} kg`)
    },
  },
  {
    accessorKey: 'value',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Value',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
    cell: ({ row }) => {
      const value = row.getValue('value') as number
      return h('span', new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value))
    },
  },
  {
    accessorKey: 'lat',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Latitude',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
    cell: ({ row }) => {
      const lat = row.getValue('lat') as number | undefined
      return h('span', lat !== undefined ? lat.toFixed(6) : '-')
    },
  },
  {
    accessorKey: 'lon',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Longitude',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
    cell: ({ row }) => {
      const lon = row.getValue('lon') as number | undefined
      return h('span', lon !== undefined ? lon.toFixed(6) : '-')
    },
  },
  {
    accessorKey: 'id',
    header: 'ID',
  },
]

// Note: With server-side pagination and filtering, we use parcels directly
// No client-side filtering needed

/**
 * Open create modal
 */
const openCreateModal = async () => {
  const modal = overlay.create(LazyParcelFormModal)
  const instance = modal.open({ mode: 'create' })
  const formData = await instance.result

  if (formData) {
    await create(formData)
  }
}

/**
 * Open edit modal
 */
const openEditModal = async (parcel: ParcelDto) => {
  const modal = overlay.create(LazyParcelFormModal)
  const instance = modal.open({ mode: 'edit', parcel })
  const formData = await instance.result

  if (formData) {
    await update(parcel.id, formData)
  }
}

/**
 * Open delete modal
 */
const openDeleteModal = async (parcel: ParcelDto) => {
  const modal = overlay.create(LazyParcelDeleteModal)
  const instance = modal.open({ parcelCode: parcel.code })
  const confirmed = await instance.result

  if (confirmed) {
    await remove(parcel.id)
  }
}

/**
 * Open QR code modal
 */
const openQRModal = async (parcel: ParcelDto) => {
  const modal = overlay.create(LazyParcelQRModal)
  const instance = modal.open({ parcelId: parcel.id, parcelCode: parcel.code })
  await instance.result
}

/**
 * Open chat with receiver
 */
const openChat = async (parcel: ParcelDto) => {
  if (!currentUser?.id || !parcel.receiverId) {
    toast.add({
      title: 'Error',
      description: 'Cannot open chat: missing user or receiver information',
      color: 'error',
    })
    return
  }

  try {
    // Find or create conversation between current user and receiver
    const conversation = await findOrCreateConversation(currentUser.id, parcel.receiverId)

    if (!conversation || !conversation.conversationId) {
      toast.add({
        title: 'Error',
        description: 'Failed to create or find conversation',
        color: 'error',
      })
      return
    }

    // Navigate to chat with conversationId as required param
    router.push({
      name: 'communication-chat',
      params: { conversationId: conversation.conversationId },
      query: { partnerId: parcel.receiverId },
    })
  } catch (error) {
    console.error('Failed to open chat:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to open chat',
      color: 'error',
    })
  }
}

/**
 * Handle seed parcels
 */
const handleSeedParcels = async () => {
  if (!seedForm.value.count || seedForm.value.count < 1) {
    toast.add({
      title: 'Error',
      description: 'Please enter a valid count (at least 1)',
      color: 'error',
    })
    return
  }

  seeding.value = true
  try {
    const response = await seedParcels(seedForm.value)
    if (response.success && response.result) {
      toast.add({
        title: 'Success',
        description: `Successfully created ${response.result.successCount} parcel(s), ${response.result.failCount} failed`,
        color: 'success',
      })
      showSeedModal.value = false
      // Reset form
      seedForm.value = {
        count: 20,
        shopId: undefined,
        clientId: undefined,
      }
      // Reload parcels
      await loadParcels()
    } else {
      toast.add({
        title: 'Error',
        description: response.message || 'Failed to seed parcels',
        color: 'error',
      })
    }
  } catch (error) {
    console.error('Failed to seed parcels:', error)
    toast.add({
      title: 'Error',
      description: error instanceof Error ? error.message : 'Failed to seed parcels',
      color: 'error',
    })
  } finally {
    seeding.value = false
  }
}

/**
 * Handle bulk delete
 */
const handleBulkDelete = async () => {
  if (selected.value.length === 0) return

  const modal = overlay.create(LazyParcelDeleteModal)
  const instance = modal.open({ count: selected.value.length })
  const confirmed = await instance.result

  if (confirmed) {
    const ids = selected.value.map((parcel) => parcel.id)
    await bulkDelete(ids)
    selected.value = []
  }
}

/**
 * Handle bulk export
 */
const handleBulkExport = () => {
  if (!table.value?.tableApi?.getFilteredSelectedRowModel()?.rows) return

  const selectedParcels = table.value.tableApi
    .getFilteredSelectedRowModel()
    .rows.map((row) => row.original)

  if (selectedParcels.length === 0) return

  exportParcels(selectedParcels)
}

/**
 * Get status color
 */
const getStatusColor = (
  status: ParcelStatus,
): 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral' => {
  const colorMap: Record<
    ParcelStatus,
    'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
  > = {
    IN_WAREHOUSE: 'info',
    ON_ROUTE: 'primary',
    DELIVERED: 'success',
    SUCCEEDED: 'success',
    FAILED: 'error',
    DELAYED: 'warning',
    DISPUTE: 'error',
    LOST: 'error',
  }
  return colorMap[status] || 'neutral'
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
 * Get column label for sorting summary
 */
const getColumnLabel = (columnId: string): string => {
  const labelMap: Record<string, string> = {
    code: 'Code',
    senderId: 'Sender ID',
    receiverId: 'Receiver ID',
    deliveryType: 'Delivery Type',
    status: 'Status',
    weight: 'Weight',
    value: 'Value',
    lat: 'Latitude',
    lon: 'Longitude',
    createdAt: 'Created At',
    updatedAt: 'Updated At',
  }
  return labelMap[columnId] || columnId
}

/**
 * Get operator label for display
 */
const getOperatorLabel = (operator: string): string => {
  const operatorMap: Record<string, string> = {
    eq: '=',
    ne: '!=',
    contains: 'contains',
    startsWith: 'starts with',
    endsWith: 'ends with',
    gt: '>',
    gte: '>=',
    lt: '<',
    lte: '<=',
    in: 'in',
    notIn: 'not in',
  }
  return operatorMap[operator] || operator
}

// Load parcels on mount
// Register update notification listener
onMounted(async () => {
  globalChat.addListener(updateNotificationListener)
  await loadParcels()
})

// Cleanup: remove listener on unmount
onUnmounted(() => {
  globalChat.removeListener(updateNotificationListener)
})

// Watch for search changes with debounce
let searchTimeout: NodeJS.Timeout
watch(searchValue, (newValue) => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    handleSearch(newValue)
  }, 300)
})

const onSortingChange = (newSorting: SortingState): void => {
  const newSorts = newSorting.map((sort) => createSortConfig(sort.id, sort.desc ? 'desc' : 'asc'))
  updateSorts(newSorts)
  sorting.value = newSorting
}

// Watch for sorts changes and sync with sorting
watch(
  sorts,
  (newSorts) => {
    const newSorting = newSorts.map((sort) => ({
      id: sort.field,
      desc: sort.direction === 'desc',
    }))
    sorting.value = newSorting
  },
  { deep: true },
)
</script>

<template>
  <div class="container mx-auto px-4 py-6">
    <PageHeader title="Parcels" description="Manage system parcels">
      <template #actions>
        <UButton
          icon="i-heroicons-sparkles"
          color="primary"
          variant="soft"
          @click="showSeedModal = true"
        >
          Seed Parcels
        </UButton>
        <UButton icon="i-heroicons-plus" @click="openCreateModal"> Add Parcel </UButton>
      </template>
    </PageHeader>

    <!-- Filters and Search -->
    <div class="mb-6 space-y-4">
      <!-- Simple Search -->
      <div class="flex flex-col sm:flex-row gap-4">
        <!-- Search Input -->
        <div class="flex-1">
          <UInput
            v-model="searchValue"
            placeholder="Search parcels..."
            icon="i-heroicons-magnifying-glass"
            size="lg"
          />
        </div>
      </div>

      <!-- Advanced Filters Button -->
      <div class="flex justify-between items-center">
        <!-- Active Filters Display -->
        <div
          v-if="getAllActiveFilters() && getAllActiveFilters()!.length > 0"
          class="flex items-center gap-2"
        >
          <span class="text-sm text-gray-600 dark:text-gray-400">Active filters:</span>
          <div class="flex items-center gap-1">
            <UBadge color="primary" variant="soft" size="sm" class="max-w-md">
              {{ getFilterStructure() }}
            </UBadge>
          </div>
          <UButton
            variant="ghost"
            size="xs"
            color="neutral"
            icon="i-heroicons-x-mark"
            @click="handleAdvancedFilterClear"
            title="Clear all filters"
          />
        </div>

        <!-- Advanced Filters Button -->
        <UButton
          variant="soft"
          color="primary"
          icon="i-heroicons-cog-6-tooth"
          @click="showAdvancedFilters = true"
        >
          Advanced Filters
        </UButton>
      </div>

      <!-- Sorting Summary -->
      <div
        v-if="sorting.length > 0"
        class="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400"
      >
        <span>Sorted by:</span>
        <div class="flex items-center gap-1">
          <UBadge
            v-for="sort in sorting"
            :key="sort.id"
            :color="sort.desc ? 'error' : 'success'"
            variant="soft"
            size="sm"
          >
            {{ getColumnLabel(sort.id) }}
            <UIcon
              :name="
                sort.desc ? 'i-lucide-arrow-down-wide-narrow' : 'i-lucide-arrow-up-narrow-wide'
              "
              class="ml-1"
            />
          </UBadge>
        </div>
        <UButton
          variant="ghost"
          size="xs"
          color="neutral"
          icon="i-heroicons-x-mark"
          @click="sorting = []"
          title="Clear all sorting"
        />
      </div>
    </div>

    <!-- Bulk Actions -->
    <div
      v-if="
        table &&
        table?.tableApi?.getFilteredSelectedRowModel()?.rows &&
        (table?.tableApi?.getFilteredSelectedRowModel()?.rows?.length || 0) > 0
      "
      class="mb-4 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg"
    >
      <div class="flex items-center justify-between">
        <span class="text-sm text-gray-600 dark:text-gray-400">
          {{ table?.tableApi?.getFilteredSelectedRowModel()?.rows?.length || 0 }} of
          {{ table?.tableApi?.getFilteredRowModel()?.rows?.length || 0 }} row(s) selected.
        </span>
        <div class="flex space-x-2">
          <UButton
            size="sm"
            variant="soft"
            icon="i-heroicons-arrow-down-tray"
            @click="handleBulkExport"
          >
            Export
          </UButton>
          <UButton
            size="sm"
            variant="soft"
            color="error"
            icon="i-heroicons-trash"
            @click="handleBulkDelete"
          >
            Delete
          </UButton>
        </div>
      </div>
    </div>

    <!-- Table -->
    <UCard>
      <UTable
        ref="table"
        :sorting="sorting"
        :data="parcels"
        :columns="columns"
        :loading="loading"
        :manual-sorting="true"
        enable-multi-sort
        @update:sorting="onSortingChange($event)"
      />

      <!-- Empty State -->
      <template v-if="!loading && parcels.length === 0">
        <div class="text-center py-12">
          <div class="mx-auto h-12 w-12 text-gray-400">
            <UIcon name="i-heroicons-cube" class="h-12 w-12" />
          </div>
          <h3 class="mt-2 text-sm font-medium text-gray-900 dark:text-gray-100">No parcels found</h3>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
            {{
              searchValue || (filters.conditions && filters.conditions.length > 0)
                ? 'Try adjusting your search or filter criteria.'
                : 'Get started by creating a new parcel.'
            }}
          </p>
          <div class="mt-6">
            <UButton
              v-if="!searchValue && (!filters.conditions || filters.conditions.length === 0)"
              icon="i-heroicons-plus"
              @click="openCreateModal"
            >
              Add Parcel
            </UButton>
            <UButton v-else variant="soft" @click="clearFilters"> Clear Filters </UButton>
          </div>
        </div>
      </template>
    </UCard>

    <!-- Pagination -->
    <div v-if="!loading && total > 0" class="mt-6 flex items-center justify-between">
      <div class="text-sm text-gray-700 dark:text-gray-300">
        Showing {{ page * pageSize + 1 }} to {{ Math.min((page + 1) * pageSize, total) }} of
        {{ total }} results
      </div>
      <UPagination
        :model-value="page + 1"
        :page-count="pageSize"
        :total="total"
        @update:page="handlePaginationChange"
      />
    </div>

    <!-- Advanced Filter Drawer -->
    <AdvancedFilterDrawer
      :show="showAdvancedFilters"
      :columns="filterableColumns"
      :active-filters="getAllActiveFilters()"
      :active-filter-group="getActiveFilterGroup()"
      @apply="handleAdvancedFilterApply"
      @clear="handleAdvancedFilterClear"
      @update:show="showAdvancedFilters = $event"
    />

    <!-- Seed Parcels Modal -->
    <UModal v-model:open="showSeedModal" title="Seed Parcels" description="Create parcels randomly or with specific shop/client. Uses primary addresses automatically.">
      <template #body>
        <form @submit.prevent="handleSeedParcels" class="space-y-4">
          <UFormField label="Number of Parcels" required>
            <UInput
              v-model.number="seedForm.count"
              type="number"
              min="1"
              placeholder="20"
              :disabled="seeding"
            />
            <template #hint>
              Number of parcels to create (randomly selects shop/client if not specified)
            </template>
          </UFormField>

          <UFormField label="Shop (Optional)">
            <UserSelect
              v-model="seedForm.shopId"
              placeholder="Select shop (or leave empty for random)"
              :allow-seed-id="true"
              :disabled="seeding"
            />
            <template #hint>
              Select a specific shop as sender. If not selected, randomly selects from available shops.
            </template>
          </UFormField>

          <UFormField label="Client (Optional)">
            <UserSelect
              v-model="seedForm.clientId"
              placeholder="Select client (or leave empty for random)"
              :allow-seed-id="true"
              :disabled="seeding"
            />
            <template #hint>
              Select a specific client as receiver. If not selected, randomly selects from available clients.
            </template>
          </UFormField>

          <UAlert
            color="info"
            variant="soft"
            title="Note"
            description="Parcels will be created using the primary addresses of the selected shop and client. If shop/client is not specified, they will be randomly selected."
          />
        </form>
      </template>
      <template #footer>
        <div class="flex justify-end gap-2">
          <UButton
            color="neutral"
            variant="ghost"
            :disabled="seeding"
            @click="showSeedModal = false"
          >
            Cancel
          </UButton>
          <UButton
            color="primary"
            :loading="seeding"
            @click="handleSeedParcels"
          >
            {{ seeding ? 'Seeding...' : 'Seed Parcels' }}
          </UButton>
        </div>
      </template>
    </UModal>
  </div>
</template>
