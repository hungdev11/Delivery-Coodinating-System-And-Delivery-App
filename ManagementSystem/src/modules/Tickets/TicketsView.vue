<script setup lang="ts">
/**
 * Tickets List View
 *
 * Main view for managing tickets with UTable and Nuxt UI v3 best practices
 */

import {
  onMounted,
  defineAsyncComponent,
  ref,
  computed,
  watch,
  reactive,
  h,
  resolveComponent,
} from 'vue'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import { useTickets } from './composables'
import type { TicketDto, TicketStatus, TicketType } from './model.type'
import { useRouter } from 'vue-router'
import type { TableColumn } from '@nuxt/ui'
import type { TabsItem } from '@nuxt/ui'
import { getTicketsV2 } from './api'
import TableFilters from '../../common/components/table/TableFilters.vue'
import AdvancedFilterDrawer from '../../common/components/filters/AdvancedFilterDrawer.vue'
import type { SortingState, Column } from '@tanstack/table-core'
import type { FilterCondition, FilterGroup } from '../../common/types/filter'
import { createSortConfig } from '../../common/utils/query-builder'
import TableHeaderCell from '../../common/components/TableHeaderCell.vue'
import type {
  FilterGroupItemV2,
  FilterConditionItemV2,
  FilterOperatorItemV2,
  FilterItemV2,
} from '../../common/types/filter-v2'
import { FilterItemType } from '../../common/types/filter-v2'
import { convertV1ToV2Filter } from '../../common/utils/filter-v2-converter'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'

// Dynamic imports
const PageHeader = defineAsyncComponent(() => import('../../common/components/PageHeader.vue'))

// Lazy load modals
const LazyResolveTicketModal = defineAsyncComponent(
  () => import('./components/ResolveTicketModal.vue'),
)
const LazyCancelTicketModal = defineAsyncComponent(
  () => import('./components/CancelTicketModal.vue'),
)
const LazyReassignParcelModal = defineAsyncComponent(
  () => import('./components/ReassignParcelModal.vue'),
)

const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')

const overlay = useOverlay()
const router = useRouter()
const toast = useToast()

// Composables - dùng để tạo filters và sorts
const {
  pageSize,
  filters: baseFilters,
  sorts,
  searchQuery,
  statistics,
  resolve,
  cancel,
  reassign,
  loadStatistics,
  updateFilters: baseUpdateFilters,
  updateSorts,
  clearFilters: baseClearFilters,
  getFilterableColumns,
} = useTickets()

// Computed filters for helper functions
const filters = computed(() => baseFilters.value)

// Search and filter state
const searchValue = ref('')

// Table state
const sorting = ref<Array<{ id: string; desc: boolean }>>([])
const activeFilters = ref<FilterCondition[]>([])
const columnFiltersState = reactive<Record<string, FilterCondition[]>>({})
const advancedFiltersGroup = ref<FilterGroup | undefined>(undefined)

// Advanced filter state
const showAdvancedFilters = ref(false)
const filterableColumns = computed(() => getFilterableColumns())


// Tab state - mỗi tab có state riêng
type TabState = {
  tickets: TicketDto[]
  loading: boolean
  page: number
  total: number
}

const tabStates = ref<Record<string, TabState>>({
  all: { tickets: [], loading: false, page: 0, total: 0 },
  pending: { tickets: [], loading: false, page: 0, total: 0 },
  resolved: { tickets: [], loading: false, page: 0, total: 0 },
  cancelled: { tickets: [], loading: false, page: 0, total: 0 },
})

// Computed để lấy state của tab hiện tại
const activeTab = ref<string | number>('all')
const currentTabState = computed(() => {
  const tabKey = String(activeTab.value)
  return tabStates.value[tabKey] || tabStates.value.all
})

const currentTickets = computed(() => currentTabState.value.tickets)
const currentLoading = computed(() => currentTabState.value.loading)
const currentPage = computed(() => currentTabState.value.page)
const currentTotal = computed(() => currentTabState.value.total)

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

type HeaderColumn = Column<TicketDto, unknown>

const setupHeader = ({ column, config }: { column: HeaderColumn; config: HeaderConfig }) =>
  h(TableHeaderCell<TicketDto>, {
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

// Helper function to get column label
const getColumnLabel = (columnId: string): string => {
  const column = filterableColumns.value.find((col) => col.field === columnId)
  return column?.label || columnId
}

// Helper function to get operator label
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
    isNull: 'is null',
    isNotNull: 'is not null',
  }
  return operatorMap[operator] || operator
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

// Handle sorting changes
const onSortingChange = (newSorting: SortingState | Array<{ id: string; desc: boolean }>) => {
  sorting.value = Array.isArray(newSorting) ? newSorting : []
  const newSorts = sorting.value.map((sort) => createSortConfig(sort.id, sort.desc ? 'desc' : 'asc'))
  updateSorts(newSorts)
}

const handleClearSorting = () => {
  sorting.value = []
  updateSorts([])
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

/**
 * Get status filter for tab
 */
const getStatusFilterForTab = (tab: string | number): TicketStatus[] | undefined => {
  switch (tab) {
    case 'pending':
      return ['PENDING']
    case 'resolved':
      return ['RESOLVED']
    case 'cancelled':
      return ['CANCELLED']
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
 * Load tickets for a specific tab
 */
const loadTicketsForTab = async (tabKey?: string | number) => {
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

    const response = await getTicketsV2(params)

    if (response.result) {
      tabState.tickets = response.result.data
      tabState.total = response.result.page.totalElements
    }
  } catch (error) {
    console.error('Failed to load tickets for tab:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to load tickets',
      color: 'error',
    })
  } finally {
    tabState.loading = false
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

    const response = await getTicketsV2(params)

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
  const tabKeys = ['all', 'pending', 'resolved', 'cancelled']
  await Promise.all(tabKeys.map((tabKey) => loadTabCount(tabKey)))
}

/**
 * Get status color
 */
const getStatusColor = (
  status: TicketStatus,
): 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral' => {
  const colorMap: Record<
    TicketStatus,
    'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
  > = {
    PENDING: 'warning',
    RESOLVED: 'success',
    CANCELLED: 'neutral',
  }
  return colorMap[status] || 'neutral'
}

/**
 * Get type color
 */
const getTypeColor = (
  type: TicketType,
): 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral' => {
  const colorMap: Record<
    TicketType,
    'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
  > = {
    DELIVERY_FAILED: 'error',
    NOT_RECEIVED: 'warning',
  }
  return colorMap[type] || 'neutral'
}

/**
 * Format date
 */
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleString('vi-VN')
}

/**
 * Open resolve modal
 */
const openResolveModal = async (ticket: TicketDto) => {
  const modal = overlay.create(LazyResolveTicketModal)
  const instance = modal.open({ ticket })
  const result = await instance.result

  if (result && typeof result === 'object' && 'resolutionNotes' in result) {
    const resolutionNotes = (result as { resolutionNotes?: string }).resolutionNotes
    await resolve(ticket.id, resolutionNotes || '')
    await loadTicketsForTab(activeTab.value)
    await loadAllTabCounts()
  }
}

/**
 * Open cancel modal
 */
const openCancelModal = async (ticket: TicketDto) => {
  const modal = overlay.create(LazyCancelTicketModal)
  const instance = modal.open({ ticket })
  const confirmed = await instance.result

  if (confirmed) {
    await cancel(ticket.id)
    await loadTicketsForTab(activeTab.value)
    await loadAllTabCounts()
  }
}

/**
 * Open reassign modal
 */
const openReassignModal = async (ticket: TicketDto) => {
  const modal = overlay.create(LazyReassignParcelModal)
  const instance = modal.open({ ticket })
  const result = await instance.result

  if (result) {
    await reassign(ticket.id, result)
    await loadTicketsForTab(activeTab.value)
  }
}

/**
 * Navigate to parcel detail with filter
 */
const viewParcel = (parcelId: string, parcelStatus?: string) => {
  const params: Record<string, string> = { id: parcelId }
  if (parcelStatus) {
    params.status = parcelStatus
  }
  const queryString = new URLSearchParams(params).toString()
  router.push(`/parcels?${queryString}`)
}

// Table columns configuration
const columns: TableColumn<TicketDto>[] = [
  {
    accessorKey: 'id',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'ID',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: false,
        },
      }),
  },
  {
    accessorKey: 'type',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Type',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
    cell: ({ row }) => {
      const type = row.getValue('type') as TicketType
      return h(UBadge, { color: getTypeColor(type), variant: 'soft', class: 'capitalize' }, () =>
        type.replace('_', ' '),
      )
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
      const status = row.getValue('status') as TicketStatus
      return h(UBadge, { color: getStatusColor(status), variant: 'soft', class: 'capitalize' }, () =>
        status,
      )
    },
  },
  {
    accessorKey: 'parcelId',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Parcel',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
    cell: ({ row }) => {
      const ticket = row.original
      const parcelId = ticket.parcelId
      // Try to get parcel status from ticket if available (some tickets may have parcel status info)
      const parcelStatus = (ticket as any).parcelStatus as string | undefined
      return h(
        UButton,
        {
          variant: 'link',
          size: 'sm',
          onClick: () => viewParcel(parcelId, parcelStatus),
        },
        () => parcelId.substring(0, 8) + '...',
      )
    },
  },
  {
    accessorKey: 'userId',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'User',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
    cell: ({ row }) => {
      const userId = row.getValue('userId') as string
      return h('span', { class: 'text-sm' }, userId.substring(0, 8) + '...')
    },
  },
  {
    accessorKey: 'description',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Description',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
    cell: ({ row }) => {
      const description = row.getValue('description') as string | null
      return h(
        'div',
        { class: 'max-w-xs truncate', title: description || 'No description' },
        description || '—',
      )
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
      return h('span', { class: 'text-sm' }, formatDate(createdAt))
    },
  },
  {
    accessorKey: 'actions',
    header: 'Actions',
    cell: ({ row }) => {
      const ticket = row.original
      return h('div', { class: 'flex space-x-2' }, [
        ticket.status === 'PENDING' &&
          h(UButton, {
            icon: 'i-heroicons-check-circle',
            size: 'sm',
            variant: 'ghost',
            color: 'success',
            title: 'Resolve ticket',
            onClick: () => openResolveModal(ticket),
          }),
        ticket.status === 'PENDING' &&
          h(UButton, {
            icon: 'i-heroicons-x-circle',
            size: 'sm',
            variant: 'ghost',
            color: 'error',
            title: 'Cancel ticket',
            onClick: () => openCancelModal(ticket),
          }),
        ticket.status === 'PENDING' &&
          ticket.parcelId &&
          h(UButton, {
            icon: 'i-heroicons-arrow-path',
            size: 'sm',
            variant: 'ghost',
            color: 'primary',
            title: 'Reassign parcel',
            onClick: () => openReassignModal(ticket),
          }),
      ].filter(Boolean))
    },
  },
]

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

// Computed properties for bulk actions (not used for tickets, but needed for TableFilters)
const selectedCount = computed((): number => 0)
const totalCount = computed((): number => currentTotal.value)

// Sync searchValue with searchQuery from composable
watch(searchValue, (newValue) => {
  if (searchQuery.value !== newValue) {
    searchQuery.value = newValue
  }
})

watch(searchQuery, (newValue) => {
  if (searchValue.value !== newValue) {
    searchValue.value = newValue
  }
})

// Watch for search/filter changes to reload current tab
watch([searchValue, baseFilters, sorts], () => {
  // Reset page to 0 when filters change
  const tabKeyStr = String(activeTab.value)
  const tabState = tabStates.value[tabKeyStr]
  if (tabState) {
    tabState.page = 0
    loadTicketsForTab(activeTab.value)
    loadAllTabCounts()
  }
}, { deep: true })

// Watch for tab changes to load data
watch(activeTab, (newTab) => {
  const tabKeyStr = String(newTab)
  const tabState = tabStates.value[tabKeyStr]
  // Only load if tab has no data yet
  if (tabState && tabState.tickets.length === 0 && !tabState.loading) {
    loadTicketsForTab(newTab)
  }
})

// Handle page change from UPagination (1-indexed) to API (0-indexed)
const handlePaginationChangeForTab = (newPage: number) => {
  const tabKeyStr = String(activeTab.value)
  const tabState = tabStates.value[tabKeyStr]
  if (!tabState) return
  // UPagination uses 1-indexed pages, convert to 0-indexed for API
  tabState.page = Math.max(newPage - 1, 0)
  loadTicketsForTab(activeTab.value)
}

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
    label: 'Pending',
    icon: 'i-heroicons-clock',
    value: 'pending',
    badge: tabStates.value.pending.total || undefined,
  },
  {
    label: 'Resolved',
    icon: 'i-heroicons-check-circle',
    value: 'resolved',
    badge: tabStates.value.resolved.total || undefined,
  },
  {
    label: 'Cancelled',
    icon: 'i-heroicons-x-circle',
    value: 'cancelled',
    badge: tabStates.value.cancelled.total || undefined,
  },
])

// Load data on mount
onMounted(async () => {
  await loadAllTabCounts()
  await loadTicketsForTab(activeTab.value)
  await loadStatistics()
})
</script>

<template>
  <div class="container mx-auto px-2 sm:px-4 py-4 sm:py-6">
    <PageHeader title="Ticket Management" description="Manage delivery issue tickets" />

    <!-- Statistics Cards -->
    <div v-if="statistics" class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
      <UCard>
        <template #header>
          <div class="flex items-center justify-between">
            <span class="text-sm font-medium">Total Tickets</span>
            <UIcon name="i-heroicons-ticket" class="w-5 h-5 text-gray-500" />
          </div>
        </template>
        <div class="text-3xl font-semibold">{{ statistics.totalCount }}</div>
      </UCard>

      <UCard>
        <template #header>
          <div class="flex items-center justify-between">
            <span class="text-sm font-medium">Pending</span>
            <UIcon name="i-heroicons-clock" class="w-5 h-5 text-warning-500" />
          </div>
        </template>
        <div class="text-3xl font-semibold text-warning-600">{{ statistics.pendingCount }}</div>
      </UCard>

      <UCard>
        <template #header>
          <div class="flex items-center justify-between">
            <span class="text-sm font-medium">Resolved</span>
            <UIcon name="i-heroicons-check-circle" class="w-5 h-5 text-success-500" />
          </div>
        </template>
        <div class="text-3xl font-semibold text-success-600">{{ statistics.resolvedCount }}</div>
      </UCard>

      <UCard>
        <template #header>
          <div class="flex items-center justify-between">
            <span class="text-sm font-medium">Cancelled</span>
            <UIcon name="i-heroicons-x-circle" class="w-5 h-5 text-gray-500" />
          </div>
        </template>
        <div class="text-3xl font-semibold text-gray-600">{{ statistics.cancelledCount }}</div>
      </UCard>
    </div>

    <!-- Table Filters (includes Search + Bulk Actions + Filters + Sort) -->
    <TableFilters
      :search-value="searchValue"
      search-placeholder="Search tickets..."
      :active-filters="getAllActiveFilters()"
      :filter-structure="getFilterStructure()"
      :sorting="sorting"
      :get-column-label="getColumnLabel"
      :selected-count="selectedCount"
      :total-count="totalCount"
      :sortable-columns="sortableColumnsList"
      @update:search-value="searchValue = $event"
      @update:sorting="onSortingChange"
      @clear-filters="handleAdvancedFilterClear"
      @clear-sorting="handleClearSorting"
      @open-advanced-filters="showAdvancedFilters = true"
    />

    <!-- Tabs for filtering tickets by status -->
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
                :data="currentTickets"
                :columns="columns"
                :loading="currentLoading"
                :ui="{
                  empty: 'text-center py-12',
                  root: 'h-[50vh]',
                  thead: 'sticky top-0 bg-white dark:bg-gray-800',
                }"
              >
                <template #empty>
                  <div class="text-center py-12">
                    <div class="mx-auto h-12 w-12 text-gray-400">
                      <UIcon name="i-heroicons-ticket" class="h-12 w-12" />
                    </div>
                    <h3 class="mt-2 text-sm font-medium text-gray-900 dark:text-gray-100">
                      No tickets found
                    </h3>
                    <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
                      No tickets match your current filter criteria.
                    </p>
                  </div>
                </template>
              </UTable>
            </UCard>
          </div>

          <!-- Mobile Card View -->
          <div class="md:hidden space-y-3">
            <template v-if="currentLoading">
              <USkeleton v-for="i in 3" :key="i" class="h-48 w-full rounded-lg" />
            </template>
            <UCard v-else-if="currentTickets.length === 0">
              <div class="text-center py-12">
                <div class="mx-auto h-12 w-12 text-gray-400">
                  <UIcon name="i-heroicons-ticket" class="h-12 w-12" />
                </div>
                <h3 class="mt-2 text-sm font-medium text-gray-900 dark:text-gray-100">
                  No tickets found
                </h3>
                <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
                  No tickets match your current filter criteria.
                </p>
              </div>
            </UCard>
            <template v-else>
              <UCard v-for="ticket in currentTickets" :key="ticket.id" class="overflow-hidden">
                <div class="space-y-3">
                  <!-- Header: ID and Status -->
                  <div class="flex items-center justify-between">
                    <span class="font-mono text-sm font-semibold text-gray-900 dark:text-gray-100">
                      {{ ticket.id.substring(0, 8) }}...
                    </span>
                    <UBadge :color="getStatusColor(ticket.status)" variant="soft" size="sm">
                      {{ ticket.status }}
                    </UBadge>
                  </div>

                  <!-- Info Grid -->
                  <div class="grid grid-cols-2 gap-2 text-sm">
                    <div>
                      <span class="text-gray-500 dark:text-gray-400">Type:</span>
                      <p class="font-medium text-gray-900 dark:text-gray-100">
                        {{ ticket.type.replace('_', ' ') }}
                      </p>
                    </div>
                    <div>
                      <span class="text-gray-500 dark:text-gray-400">Parcel:</span>
                      <p class="font-medium text-gray-900 dark:text-gray-100 truncate">
                        {{ ticket.parcelId.substring(0, 8) }}...
                      </p>
                    </div>
                  </div>

                  <!-- Description -->
                  <div v-if="ticket.description" class="text-sm">
                    <span class="text-gray-500 dark:text-gray-400">Description:</span>
                    <p class="font-medium text-gray-900 dark:text-gray-100 line-clamp-2">
                      {{ ticket.description }}
                    </p>
                  </div>

                  <!-- Created Date -->
                  <div class="text-xs text-gray-500 dark:text-gray-400">
                    Created: {{ formatDate(ticket.createdAt) }}
                  </div>

                  <!-- Actions -->
                  <div
                    v-if="ticket.status === 'PENDING'"
                    class="flex items-center justify-end gap-2 pt-2 border-t border-gray-200 dark:border-gray-700"
                  >
                    <UButton
                      icon="i-heroicons-check-circle"
                      size="sm"
                      variant="ghost"
                      color="success"
                      @click="openResolveModal(ticket)"
                    >
                      Resolve
                    </UButton>
                    <UButton
                      icon="i-heroicons-x-circle"
                      size="sm"
                      variant="ghost"
                      color="error"
                      @click="openCancelModal(ticket)"
                    >
                      Cancel
                    </UButton>
                    <UButton
                      v-if="ticket.parcelId"
                      icon="i-heroicons-arrow-path"
                      size="sm"
                      variant="ghost"
                      color="primary"
                      @click="openReassignModal(ticket)"
                    >
                      Reassign
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
    <div class="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4">
      <div class="text-sm text-gray-700 dark:text-gray-300">
        Showing {{ currentPage * pageSize + 1 }} to
        {{ Math.min((currentPage + 1) * pageSize, currentTotal) }} of {{ currentTotal }} results
      </div>
      <UPagination
        :page="currentPage + 1"
        :page-count="pageSize"
        :total="currentTotal"
        :disabled="currentLoading"
        @update:page="handlePaginationChangeForTab"
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
