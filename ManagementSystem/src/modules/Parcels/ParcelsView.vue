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
import { ParcelDto, type ParcelStatus, type ParcelEvent } from './model.type'
import { changeParcelStatus } from './api'
import { useSeedProgress } from './composables/useSeedProgress'
import type { SeedProgressEvent } from './composables/useSeedProgress'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { useTemplateRef } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import AdvancedFilterDrawer from '../../common/components/filters/AdvancedFilterDrawer.vue'
import TableFilters from '../../common/components/table/TableFilters.vue'
import type { SortingState, Column } from '@tanstack/table-core'
import type { FilterCondition, FilterGroup } from '../../common/types/filter'
import { createSortConfig } from '../../common/utils/query-builder'
import TableHeaderCell from '../../common/components/TableHeaderCell.vue'
import { useRouter } from 'vue-router'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'
import { useGlobalChat, type GlobalChatListener } from '../Communication/composables'
import { useConversations } from '../Communication/composables'
import { useProposals } from '../Communication/composables'
import { onUnmounted } from 'vue'
import type { TabsItem } from '@nuxt/ui'
import type {
  FilterGroupItemV2,
  FilterConditionItemV2,
  FilterOperatorItemV2,
  FilterItemV2,
} from '@/common/types/filter-v2'
import { FilterItemType } from '@/common/types/filter-v2'
import { getParcelsV2 } from './api'
import { convertV1ToV2Filter } from '@/common/utils/filter-v2-converter'

// Dynamic imports to avoid TypeScript issues
const PageHeader = defineAsyncComponent(() => import('../../common/components/PageHeader.vue'))

// Lazy load modals
const LazyParcelFormModal = defineAsyncComponent(() => import('./components/ParcelFormModal.vue'))
const LazyParcelDeleteModal = defineAsyncComponent(
  () => import('./components/ParcelDeleteModal.vue'),
)
const LazyParcelQRModal = defineAsyncComponent(() => import('./components/ParcelQRModal.vue'))
const LazyChangeStatusModal = defineAsyncComponent(
  () => import('./components/ChangeStatusModal.vue'),
)
// const LazyParcelDetailModal = defineAsyncComponent(
//   () => import('./components/ParcelDetailModal.vue'),
// )
const UCheckbox = resolveComponent('UCheckbox')
const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')
const UDropdownMenu = resolveComponent('UDropdownMenu')

const overlay = useOverlay()
const table = useTemplateRef('table')
const toast = useToast()
const router = useRouter()
const currentUser = getCurrentUser()
const { findOrCreateConversation } = useConversations()
const { create: createProposalRequest } = useProposals()

// Global chat service for update notifications
const globalChat = useGlobalChat()

// Update notification listener
const updateNotificationListener: GlobalChatListener = {
  onUpdateNotificationReceived: (updateNotification: {
    entityType?: string
    action?: string
    message?: string
  }) => {
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
        loadParcelsForTab(activeTab.value)
        // Refresh counts for all tabs after status change
        loadAllTabCounts()
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
        loadParcelsForTab(activeTab.value)
        // Refresh counts for all tabs after status change
        loadAllTabCounts()
      }
    }
  },
}

// Export composable
const { exportParcels } = useParcelExport()

// Auto seed state
const autoSeeding = ref(false)

// Seed progress composable
const { startSeedWithProgress } = useSeedProgress()

// Tab state - mỗi tab có state riêng
type TabState = {
  parcels: ParcelDto[]
  loading: boolean
  page: number
  total: number
}

const tabStates = ref<Record<string, TabState>>({
  all: { parcels: [], loading: false, page: 0, total: 0 },
  pending: { parcels: [], loading: false, page: 0, total: 0 },
  delivering: { parcels: [], loading: false, page: 0, total: 0 },
  'need-confirm': { parcels: [], loading: false, page: 0, total: 0 },
  delivered: { parcels: [], loading: false, page: 0, total: 0 },
  cancelled: { parcels: [], loading: false, page: 0, total: 0 },
})

// Composables - dùng để tạo filters và sorts
const {
  pageSize,
  filters: baseFilters,
  sorts,
  create,
  update,
  remove,
  bulkDelete,
  handleSearch,
  updateFilters: baseUpdateFilters,
  updateSorts,
  clearFilters: baseClearFilters,
  getFilterableColumns,
} = useParcels()

// Computed để lấy state của tab hiện tại
const activeTab = ref<string | number>('all')
const currentTabState = computed(() => {
  const tabKey = String(activeTab.value)
  return tabStates.value[tabKey] || tabStates.value.all
})

const parcels = computed(() => currentTabState.value.parcels)
const loading = computed(() => currentTabState.value.loading)
const page = computed(() => currentTabState.value.page)
const total = computed(() => currentTabState.value.total)

// Computed filters for helper functions
const filters = computed(() => baseFilters.value)

// Handle page change from UPagination (1-indexed) to API (0-indexed)
const handlePaginationChange = (newPage: number) => {
  const tabKeyStr = String(activeTab.value)
  const tabState = tabStates.value[tabKeyStr]
  if (!tabState) return
  // UPagination uses 1-indexed pages, convert to 0-indexed for API
  tabState.page = Math.max(newPage - 1, 0)
  loadParcelsForTab(activeTab.value)
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

const setupHeader = ({ column, config }: { column: HeaderColumn; config: HeaderConfig }) =>
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
  advancedFiltersGroup.value =
    filterGroup && filterGroup.conditions.length > 0 ? filterGroup : undefined
  applyCombinedFilters()
  showAdvancedFilters.value = false
}

const handleAdvancedFilterClear = () => {
  advancedFiltersGroup.value = undefined
  Object.keys(columnFiltersState).forEach((key) => {
    delete columnFiltersState[key]
  })
  activeFilters.value = []
  baseClearFilters()
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
    baseClearFilters()
    return
  }

  baseUpdateFilters({
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

const columnPinning = ref({
  left: ['select', 'actions'],
})

// Helper to wrap cell content with row click handler
const wrapCellWithClickHandler = (content: ReturnType<typeof h>, parcel: ParcelDto) => {
  return h(
    'div',
    {
      onClick: () => openQRModal(parcel),
      style: { cursor: 'pointer' },
      class: 'w-full',
    },
    [content],
  )
}

// Table columns configuration
const columns: TableColumn<ParcelDto>[] = [
  {
    accessorKey: 'select',
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
    accessorKey: 'actions',
    header: 'Actions',
    cell: ({ row }) => {
      const parcel = row.original
      return h(
        UDropdownMenu,
        {
          items: [
            [
              {
                label: 'Change status',
                icon: 'i-heroicons-arrow-path',
                onClick: () => openChangeStatusModal(parcel),
              },
              {
                label: 'Chat with receiver',
                icon: 'i-heroicons-chat-bubble-left-right',
                onClick: () => openChat(parcel),
              },
              {
                label: 'Show QR Code',
                icon: 'i-heroicons-qr-code',
                onClick: () => openQRModal(parcel),
              },
              {
                label: 'Edit parcel',
                icon: 'i-heroicons-pencil',
                onClick: () => openEditModal(parcel),
              },
              {
                label: 'Delete parcel',
                icon: 'i-heroicons-trash',
                onClick: () => openDeleteModal(parcel),
              },
            ],
          ],
        },
        {
          default: () =>
            h(UButton, {
              icon: 'i-heroicons-ellipsis-vertical',
              size: 'sm',
              variant: 'outline',
              title: 'Actions',
            }),
        },
      )
    },
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
    cell: ({ row }) => {
      const parcel = row.original
      return h(
        'span',
        {
          class: 'font-mono text-sm',
          onClick: (e: MouseEvent) => {
            e.stopPropagation()
            openQRModal(parcel)
          },
          style: { cursor: 'pointer' },
        },
        parcel.code,
      )
    },
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
      return h(
        'span',
        {
          onClick: () => openQRModal(parcel),
          style: { cursor: 'pointer' },
        },
        parcel.senderName || parcel.senderId,
      )
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
      return h(
        'span',
        {
          onClick: () => openQRModal(parcel),
          style: { cursor: 'pointer' },
        },
        parcel.receiverName || parcel.receiverId,
      )
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
      const parcel = row.original
      const deliveryType = row.getValue('deliveryType') as string
      const badge = h(UBadge, { variant: 'soft', color: 'primary', size: 'sm' }, () => deliveryType)
      return wrapCellWithClickHandler(badge, parcel)
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
      const parcel = row.original
      const status = row.getValue('status') as ParcelStatus
      const color = getStatusColor(status)
      const displayStatus = getDisplayStatus(status)
      const badge = h(UBadge, { class: 'capitalize', variant: 'soft', color }, () => displayStatus)
      return wrapCellWithClickHandler(badge, parcel)
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
      const parcel = row.original
      const createdAt = row.getValue('createdAt') as string | null | undefined
      const content = h('span', createdAt ? new Date(createdAt).toLocaleString() : '-')
      return wrapCellWithClickHandler(content, parcel)
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
      const parcel = row.original
      const weight = row.getValue('weight') as number | null | undefined
      const content = h('span', weight != null ? `${weight} kg` : '-')
      return wrapCellWithClickHandler(content, parcel)
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
      const parcel = row.original
      const value = row.getValue('value') as number | null | undefined
      const content = h(
        'span',
        value != null
          ? new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
          : '-',
      )
      return wrapCellWithClickHandler(content, parcel)
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
      const parcel = row.original
      const lat = row.getValue('lat') as number | null | undefined
      const content = h('span', lat != null && !isNaN(lat) ? lat.toFixed(6) : '-')
      return wrapCellWithClickHandler(content, parcel)
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
      const parcel = row.original
      const lon = row.getValue('lon') as number | null | undefined
      const content = h('span', lon != null && !isNaN(lon) ? lon.toFixed(6) : '-')
      return wrapCellWithClickHandler(content, parcel)
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
 * Open parcel detail modal
 * Note: Currently not used, but kept for future use
 */
// const openDetailModal = async (parcel: ParcelDto) => {
//   const modal = overlay.create(LazyParcelDetailModal)
//   const instance = modal.open({ parcel })
//   await instance.result
//   // Reload parcels after modal closes (in case dispute was resolved)
//   loadParcelsForTab(activeTab.value)
// }

/**
 * Open change status modal
 */
const openChangeStatusModal = async (parcel: ParcelDto) => {
  const modal = overlay.create(LazyChangeStatusModal)
  const instance = modal.open({ parcel })
  const result = await instance.result

  if (result) {
    await handleChangeStatus(parcel, result.event, result.notifyClient)
  }
}

/**
 * Handle change status and create proposal if needed
 */
const handleChangeStatus = async (
  parcel: ParcelDto,
  event: ParcelEvent,
  notifyClient: boolean,
) => {
  try {
    // Change status
    await changeParcelStatus(parcel.id, event)

    toast.add({
      title: 'Success',
      description: `Đã đổi trạng thái đơn hàng ${parcel.code}`,
      color: 'success',
    })

    // Create proposal to notify client if requested
    if (notifyClient && parcel.receiverId && currentUser?.id) {
      try {
        // Find or create conversation between admin and client
        const conversation = await findOrCreateConversation(currentUser.id, parcel.receiverId)

        if (conversation && conversation.conversationId) {
          // Get result status
          const getResultStatus = (event: ParcelEvent): ParcelStatus => {
            switch (event) {
              case 'DELIVERY_SUCCESSFUL':
                return 'DELIVERED'
              case 'CUSTOMER_RECEIVED':
                return 'SUCCEEDED'
              case 'CUSTOMER_CONFIRM_NOT_RECEIVED':
                return 'DISPUTE'
              case 'CUSTOMER_REJECT':
                return 'FAILED'
              case 'MISSUNDERSTANDING_DISPUTE':
                return 'SUCCEEDED'
              case 'FAULT_DISPUTE':
                return 'LOST'
              default:
                return parcel.status
            }
          }

          const newStatus = getResultStatus(event)

          // Create proposal data
          const proposalData = {
            parcelId: parcel.id,
            parcelCode: parcel.code,
            oldStatus: parcel.status,
            newStatus: newStatus,
            event: event,
            title: `Đơn hàng ${parcel.code} đã được cập nhật trạng thái`,
            content: `Đơn hàng ${parcel.code} đã được chuyển từ ${parcel.status} sang ${newStatus}`,
          }

          // Get current user roles
          const userRoles = currentUser.roles || ['ADMIN']

          // Create proposal
          await createProposalRequest({
            conversationId: conversation.conversationId,
            recipientId: parcel.receiverId,
            type: 'STATUS_CHANGE_NOTIFICATION',
            data: JSON.stringify(proposalData),
            fallbackContent: `Đơn hàng ${parcel.code} đã được cập nhật trạng thái: ${parcel.status} → ${newStatus}`,
            senderId: currentUser.id,
            senderRoles: userRoles,
          })

          toast.add({
            title: 'Success',
            description: `Đã gửi thông báo cho khách hàng`,
            color: 'success',
          })
        }
      } catch (error) {
        console.error('Failed to create proposal notification:', error)
        // Don't show error to user - status change succeeded
      }
    }

    // Reload parcels
    await loadParcelsForTab(activeTab.value)
    await loadAllTabCounts()
  } catch (error) {
    console.error('Failed to change status:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to change parcel status',
      color: 'error',
    })
  }
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
 * Handle auto seed parcels with progress tracking
 */
const handleAutoSeedParcels = async () => {
  autoSeeding.value = true

  try {
    // Start seed process with progress tracking (global tracker will show progress)
    const sessionKey = await startSeedWithProgress({
      onCompleted: async (event: SeedProgressEvent) => {
        toast.add({
          title: 'Thành công',
          description: `Đã fail ${event.failedOldParcelsCount ?? 0} đơn cũ (>48h), tạo ${event.seededParcelsCount ?? 0} đơn mới, bỏ qua ${event.skippedAddressesCount ?? 0} địa chỉ`,
          color: 'success',
        })

        // Reload parcels
        await loadParcelsForTab(activeTab.value)
        // Refresh counts for all tabs after auto seeding
        await loadAllTabCounts()
      },
      onError: (event: SeedProgressEvent) => {
        toast.add({
          title: 'Lỗi',
          description: event.errorMessage || 'Failed to auto seed parcels',
          color: 'error',
        })
      },
    })

    if (!sessionKey) {
      throw new Error('Failed to start seed process')
    }

    // Progress will be shown in global tracker automatically
  } catch (error) {
    console.error('Failed to auto seed parcels:', error)
    toast.add({
      title: 'Error',
      description: error instanceof Error ? error.message : 'Failed to auto seed parcels',
      color: 'error',
    })
  } finally {
    autoSeeding.value = false
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
  // Load counts for all tabs first
  await loadAllTabCounts()
  // Then load data for active tab
  await loadParcelsForTab(activeTab.value)
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

// Handle clear sorting
const handleClearSorting = () => {
  sorting.value = []
  updateSorts([])
}

// Computed properties for bulk actions
const selectedCount = computed((): number => {
  if (!table.value?.tableApi?.getFilteredSelectedRowModel) return 0
  return table.value?.tableApi?.getFilteredSelectedRowModel()?.rows?.length || 0
})

const totalCount = computed((): number => {
  if (!table.value?.tableApi?.getFilteredRowModel) return 0
  return table.value?.tableApi?.getFilteredRowModel()?.rows?.length || 0
})

// Sortable columns list (derived from filterableColumns, excluding non-sortable fields)
const sortableColumnsList = computed(() => {
  return filterableColumns.value
    .filter((col) => {
      // Exclude non-sortable fields like arrays, objects, etc.
      const nonSortableFields = ['id', 'select', 'actions']
      return !nonSortableFields.includes(col.field)
    })
    .map((col) => ({
      id: col.field,
      label: col.label,
    }))
})

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

/**
 * Get status filter for tab
 */
const getStatusFilterForTab = (tab: string | number) => {
  switch (tab) {
    case 'pending':
      return ['IN_WAREHOUSE']
    case 'delivering':
      return ['ON_ROUTE']
    case 'need-confirm':
      return ['DELIVERED', 'DISPUTE']
    case 'delivered':
      return ['SUCCEEDED']
    case 'cancelled':
      return ['FAILED']
    default:
      return undefined
  }
}

/**
 * Build status filter group for a tab
 */
const buildStatusFilterGroup = (tab: string | number): FilterGroupItemV2 | undefined => {
  const statusFilter = getStatusFilterForTab(tab)
  if (!statusFilter || statusFilter.length === 0) return undefined

  if (statusFilter.length === 1) {
    // Single status - use EQUALS
    const condition: FilterConditionItemV2 = {
      type: FilterItemType.CONDITION,
      field: 'status',
      operator: 'EQUALS',
      value: statusFilter[0],
      caseSensitive: false,
    }
    return {
      type: FilterItemType.GROUP,
      items: [condition],
    }
  } else {
    // Multiple statuses - use nested group with OR operator
    const nestedItems: FilterItemV2[] = []
    statusFilter.forEach((status, index) => {
      if (index > 0) {
        // Add OR operator before each condition except the first
        const operator: FilterOperatorItemV2 = {
          type: FilterItemType.OPERATOR,
          value: 'OR',
        }
        nestedItems.push(operator)
      }
      const condition: FilterConditionItemV2 = {
        type: FilterItemType.CONDITION,
        field: 'status',
        operator: 'EQUALS',
        value: status,
        caseSensitive: false,
      }
      nestedItems.push(condition)
    })

    // Wrap in nested group
    const nestedGroup: FilterGroupItemV2 = {
      type: FilterItemType.GROUP,
      items: nestedItems,
    }

    // Wrap nested group in outer group
    return {
      type: FilterItemType.GROUP,
      items: [nestedGroup],
    }
  }
}

/**
 * Load count for a specific tab (without loading data)
 */
const loadTabCount = async (tabKey: string | number) => {
  const tabKeyStr = String(tabKey)
  const tabState = tabStates.value[tabKeyStr]
  if (!tabState) return

  try {
    const statusFilterGroup = buildStatusFilterGroup(tabKey)

    // Convert existing V1 filters to V2 format
    let existingV2Filter: FilterGroupItemV2 | undefined = undefined
    if (baseFilters.value && baseFilters.value.conditions && baseFilters.value.conditions.length > 0) {
      existingV2Filter = convertV1ToV2Filter(baseFilters.value)
    }

    // Merge status filter with existing filters
    let filtersToSend: FilterGroupItemV2 | undefined = undefined

    if (statusFilterGroup || existingV2Filter) {
      const allItems: FilterItemV2[] = []

      // Add status filter group
      if (statusFilterGroup) {
        allItems.push(statusFilterGroup)
      }

      // Add existing filter items with AND operator if both exist
      if (existingV2Filter && existingV2Filter.items && existingV2Filter.items.length > 0) {
        if (statusFilterGroup) {
          // Add AND operator between status filter and existing filters
          const operator: FilterOperatorItemV2 = {
            type: FilterItemType.OPERATOR,
            value: 'AND',
          }
          allItems.push(operator)
        }
        allItems.push(...existingV2Filter.items)
      }

      if (allItems.length > 0) {
        filtersToSend = {
          type: FilterItemType.GROUP,
          items: allItems,
        }
      }
    }

    const params = {
      filters: filtersToSend,
      sorts: sorts.value.length > 0 ? sorts.value : undefined,
      page: 0,
      size: 1, // Only need total, not data
      search: searchValue.value || undefined,
    }

    const response = await getParcelsV2(params)

    if (response.result) {
      tabState.total = response.result.page.totalElements
    }
  } catch (error) {
    console.error(`Failed to load count for tab ${tabKey}:`, error)
  }
}

/**
 * Load counts for all tabs
 */
const loadAllTabCounts = async () => {
  // Load all tab counts in parallel
  const tabKeys = ['all', 'pending', 'delivering', 'need-confirm', 'delivered', 'cancelled']
  await Promise.all(tabKeys.map((tabKey) => loadTabCount(tabKey)))
}

/**
 * Load parcels for a specific tab
 */
const loadParcelsForTab = async (tabKey?: string | number) => {
  const targetTab = tabKey || activeTab.value
  const tabKeyStr = String(targetTab)
  const tabState = tabStates.value[tabKeyStr]

  if (!tabState) return

  tabState.loading = true
  try {
    const statusFilterGroup = buildStatusFilterGroup(targetTab)

    // Convert existing V1 filters to V2 format
    let existingV2Filter: FilterGroupItemV2 | undefined = undefined
    if (baseFilters.value && baseFilters.value.conditions && baseFilters.value.conditions.length > 0) {
      existingV2Filter = convertV1ToV2Filter(baseFilters.value)
    }

    // Merge status filter with existing filters
    let filtersToSend: FilterGroupItemV2 | undefined = undefined

    if (statusFilterGroup || existingV2Filter) {
      const allItems: FilterItemV2[] = []

      // Add status filter group
      if (statusFilterGroup) {
        allItems.push(statusFilterGroup)
      }

      // Add existing filter items with AND operator if both exist
      if (existingV2Filter && existingV2Filter.items && existingV2Filter.items.length > 0) {
        if (statusFilterGroup) {
          // Add AND operator between status filter and existing filters
          const operator: FilterOperatorItemV2 = {
            type: FilterItemType.OPERATOR,
            value: 'AND',
          }
          allItems.push(operator)
        }
        allItems.push(...existingV2Filter.items)
      }

      if (allItems.length > 0) {
        filtersToSend = {
          type: FilterItemType.GROUP,
          items: allItems,
        }
      }
    }

    const params = {
      filters: filtersToSend,
      sorts: sorts.value.length > 0 ? sorts.value : undefined,
      page: tabState.page,
      size: pageSize.value,
      search: searchValue.value || undefined,
    }

    const response = await getParcelsV2(params)

    if (response.result) {
      tabState.parcels = response.result.data.map((p) => new ParcelDto(p))
      tabState.total = response.result.page.totalElements
    }
  } catch (error) {
    console.error('Failed to load parcels:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to load parcels',
      color: 'error',
    })
  } finally {
    tabState.loading = false
  }
}

// Watch for tab changes to load data
watch(activeTab, (newTab) => {
  const tabKeyStr = String(newTab)
  const tabState = tabStates.value[tabKeyStr]
  // Only load if tab has no data yet
  if (tabState && tabState.parcels.length === 0 && !tabState.loading) {
    loadParcelsForTab(newTab)
  }
})

// Watch for search/filter changes to reload current tab
watch([searchValue, baseFilters, sorts], () => {
  // Reset page to 0 when filters change
  const tabKeyStr = String(activeTab.value)
  const tabState = tabStates.value[tabKeyStr]
  if (tabState) {
    tabState.page = 0
    loadParcelsForTab(activeTab.value)
  }
}, { deep: true })

/**
 * Tab items configuration with computed badges
 */
const tabItems = computed<TabsItem[]>(() => [
  {
    label: 'Tất cả',
    icon: 'i-heroicons-squares-2x2',
    value: 'all',
    badge: tabStates.value.all.total || undefined,
  },
  {
    label: 'Đơn chờ giao',
    icon: 'i-heroicons-clock',
    value: 'pending',
    badge: tabStates.value.pending.total || undefined,
  },
  {
    label: 'Đơn đang giao',
    icon: 'i-heroicons-truck',
    value: 'delivering',
    badge: tabStates.value.delivering.total || undefined,
  },
  {
    label: 'Đơn cần xác nhận',
    icon: 'i-heroicons-check-circle',
    value: 'need-confirm',
    badge: tabStates.value['need-confirm'].total || undefined,
  },
  {
    label: 'Đơn đã giao',
    icon: 'i-heroicons-check-badge',
    value: 'delivered',
    badge: tabStates.value.delivered.total || undefined,
  },
  {
    label: 'Đơn đã huỷ',
    icon: 'i-heroicons-x-circle',
    value: 'cancelled',
    badge: tabStates.value.cancelled.total || undefined,
  },
])
</script>

<template>
  <div class="container mx-auto px-2 sm:px-4 py-4 sm:py-6">
    <PageHeader title="Đơn hàng" description="Quản lý đơn hàng hệ thống">
      <template #actions>
        <div class="flex gap-2">
          <UButton
            icon="i-heroicons-arrow-path"
            color="primary"
            variant="soft"
            size="sm"
            class="md:size-md"
            :loading="autoSeeding"
            :disabled="autoSeeding"
            @click="handleAutoSeedParcels"
          >
            <span class="hidden sm:inline">Tự động seed</span>
            <span class="sm:hidden">Auto</span>
          </UButton>
          <UButton icon="i-heroicons-plus" size="sm" class="md:size-md" @click="openCreateModal">
            <span class="hidden sm:inline">Thêm đơn hàng</span>
            <span class="sm:hidden">Thêm</span>
          </UButton>
        </div>
      </template>
    </PageHeader>

    <!-- Table Filters (includes Search + Bulk Actions + Filters + Sort) -->
    <TableFilters
      :search-value="searchValue"
      search-placeholder="Tìm kiếm đơn hàng..."
      :active-filters="getAllActiveFilters()"
      :filter-structure="getFilterStructure()"
      :sorting="sorting"
      :get-column-label="getColumnLabel"
      :selected-count="selectedCount"
      :total-count="totalCount"
      :on-bulk-export="handleBulkExport"
      :on-bulk-delete="handleBulkDelete"
      :sortable-columns="sortableColumnsList"
      @update:search-value="searchValue = $event"
      @update:sorting="onSortingChange"
      @clear-filters="handleAdvancedFilterClear"
      @clear-sorting="handleClearSorting"
      @open-advanced-filters="showAdvancedFilters = true"
    />

    <!-- Tabs for filtering parcels by status -->
    <UTabs
      v-model="activeTab"
      :items="tabItems"
      class="w-full"
      :ui="{
        list: 'overflow-x-auto whitespace-nowrap',
        trigger: 'min-w-[200px]',
        indicator: 'min-w-[200px]',
        root: 'w-full',
      }"
    >
      <template #content>
        <div class="space-y-4">
    <!-- Desktop Table View -->
    <div class="hidden md:block">
      <UCard>
        <UTable
          ref="table"
          :column-pinning="columnPinning"
          :sorting="sorting"
          :data="parcels"
          :columns="columns"
          :loading="loading"
          :manual-sorting="true"
          enable-multi-sort
          @update:sorting="onSortingChange($event)"
          :ui="{
            empty: 'text-center py-12',
            root: 'h-[50vh]',
            thead: 'sticky top-0 bg-white dark:bg-gray-800',
          }"
        />

        <!-- Empty State -->
        <template v-if="!loading && parcels.length === 0">
          <div class="text-center py-12">
            <div class="mx-auto h-12 w-12 text-gray-400">
              <UIcon name="i-heroicons-cube" class="h-12 w-12" />
            </div>
            <h3 class="mt-2 text-sm font-medium text-gray-900 dark:text-gray-100">
              No parcels found
            </h3>
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
                    <UButton v-else variant="soft" @click="handleAdvancedFilterClear"> Clear Filters </UButton>
            </div>
          </div>
        </template>
      </UCard>
    </div>

    <!-- Mobile Card View -->
    <div class="md:hidden space-y-3">
      <template v-if="loading">
        <USkeleton v-for="i in 3" :key="i" class="h-48 w-full rounded-lg" />
      </template>
      <template v-else-if="parcels.length === 0">
        <UCard>
          <div class="text-center py-12">
            <div class="mx-auto h-12 w-12 text-gray-400">
              <UIcon name="i-heroicons-cube" class="h-12 w-12" />
            </div>
            <h3 class="mt-2 text-sm font-medium text-gray-900 dark:text-gray-100">
              No parcels found
            </h3>
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
              <UButton v-else variant="soft" @click="handleAdvancedFilterClear"> Clear Filters </UButton>
            </div>
          </div>
        </UCard>
      </template>
      <template v-else>
        <UCard v-for="parcel in parcels" :key="parcel.id" class="overflow-hidden">
          <div class="space-y-3">
            <!-- Header: Code and Status -->
            <div class="flex items-center justify-between">
              <span class="font-mono text-sm font-semibold text-gray-900 dark:text-gray-100">
                {{ parcel.code }}
              </span>
              <UBadge :color="getStatusColor(parcel.status)" variant="soft" size="sm">
                {{ getDisplayStatus(parcel.status) }}
              </UBadge>
            </div>

            <!-- Info Grid -->
            <div class="grid grid-cols-2 gap-2 text-sm">
              <div>
                <span class="text-gray-500 dark:text-gray-400">Sender:</span>
                <p class="font-medium text-gray-900 dark:text-gray-100 truncate">
                  {{ parcel.senderName || 'N/A' }}
                </p>
              </div>
              <div>
                <span class="text-gray-500 dark:text-gray-400">Type:</span>
                <p class="font-medium text-gray-900 dark:text-gray-100">
                  {{ parcel.deliveryType }}
                </p>
              </div>
            </div>

            <!-- Destination -->
            <div class="text-sm">
              <span class="text-gray-500 dark:text-gray-400">Destination:</span>
              <p class="font-medium text-gray-900 dark:text-gray-100 line-clamp-2">
                {{ parcel.targetDestination || 'N/A' }}
              </p>
            </div>

            <!-- Created Date -->
            <div class="text-xs text-gray-500 dark:text-gray-400">
              Created: {{ new Date(parcel.createdAt).toLocaleString('vi-VN') }}
            </div>

            <!-- Actions -->
            <div
              class="flex items-center justify-end gap-2 pt-2 border-t border-gray-200 dark:border-gray-700"
            >
              <UButton
                icon="i-heroicons-pencil"
                size="sm"
                variant="ghost"
                @click="openEditModal(parcel)"
              >
                Edit
              </UButton>
              <UButton
                icon="i-heroicons-trash"
                size="sm"
                variant="ghost"
                color="error"
                @click="openDeleteModal(parcel)"
              >
                Delete
              </UButton>
            </div>
          </div>
        </UCard>
      </template>
    </div>
        </div>
      </template>
    </UTabs>

    <!-- Pagination -->
    <div
      class="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4"
    >
      <div class="text-sm text-gray-700 dark:text-gray-300">
        Showing {{ page * pageSize + 1 }} to {{ Math.min((page + 1) * pageSize, total) }} of
        {{ total }} results
      </div>
      <UPagination
        :page="page + 1"
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
  </div>
</template>
