<script setup lang="ts">
/**
 * Zones List View
 *
 * Main view for managing zones with UTable and Nuxt UI v3 best practices
 */

import {
  onMounted,
  defineAsyncComponent,
  ref,
  reactive,
  computed,
  watch,
  resolveComponent,
  h,
} from 'vue'
import { useRouter } from 'vue-router'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import { useZonesStore } from './composables'
import type { ZoneDto } from './model.type'
import { storeToRefs } from 'pinia'
import { useTemplateRef } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import AdvancedFilterDrawer from '@/common/components/filters/AdvancedFilterDrawer.vue'
import type { SortingState } from '@tanstack/table-core'
import type { FilterCondition, FilterGroup, FilterableColumn, SortConfig } from '../../common/types/filter'
import TableHeaderCell from '@/common/components/TableHeaderCell.vue'

// Dynamic imports to avoid TypeScript issues
const PageHeader = defineAsyncComponent(() => import('../../common/components/PageHeader.vue'))

// Lazy load modals
const LazyZoneFormModal = defineAsyncComponent(() => import('./components/ZoneFormModal.vue'))
const LazyZoneDeleteModal = defineAsyncComponent(() => import('./components/ZoneDeleteModal.vue'))
const UCheckbox = resolveComponent('UCheckbox')
const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')

const router = useRouter()
const overlay = useOverlay()
const table = useTemplateRef('table')

// Composables
const zonesStore = useZonesStore()

const {
  loadZones,
  loadCenters,
  create,
  update,
  remove,
  bulkDelete,
  handleSearch,
  setFilters,
  setSorts,
  setPage,
} = zonesStore

const {
  page,
  pageSize,
  total,
  zones,
  loading,
  filters: storeFilters,
  sorts: storeSorts,
} = storeToRefs(zonesStore)

// Table state
const selected = ref<ZoneDto[]>([])
const sorting = ref<Array<{ id: string; desc: boolean }>>([])
const activeFilters = ref<FilterCondition[]>([])
const columnFiltersState = reactive<Record<string, FilterCondition[]>>({})
const advancedFiltersGroup = ref<FilterGroup | undefined>(undefined)

// Search and filter state
const searchValue = ref('')

// Advanced filter state
const showAdvancedFilters = ref(false)

// Filterable columns configuration
const filterableColumns: FilterableColumn[] = [
  {
    field: 'code',
    label: 'Code',
    type: 'string',
    filterType: 'text',
    filterConfig: {
      placeholder: 'Enter zone code...',
    },
  },
  {
    field: 'name',
    label: 'Name',
    type: 'string',
    filterType: 'text',
    filterConfig: {
      placeholder: 'Enter zone name...',
    },
  },
  {
    field: 'centerName',
    label: 'Center',
    type: 'string',
    filterType: 'text',
    filterConfig: {
      placeholder: 'Enter center name...',
    },
  },
  {
    field: 'hasPolygon',
    label: 'Has Polygon',
    type: 'boolean',
    filterType: 'boolean',
  },
]

/**
 * Setup header component for table columns
 */
const setupHeader = ({
  column,
  config,
}: {
  column: any // eslint-disable-line @typescript-eslint/no-explicit-any
  config: {
    variant: 'ghost' | 'solid' | 'outline' | 'soft' | 'link'
    label: string
    class: string
    activeColor?: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
    inactiveColor?: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
    filterable?: boolean
  }
}) =>
  h(TableHeaderCell<ZoneDto>, {
    column,
    config,
    filterableColumns: filterableColumns,
    activeFilters: activeFilters.value,
    'onUpdate:filters': handleFiltersUpdate,
  })

// Filter and sort state

const getAllActiveFilters = (): FilterCondition[] | undefined => {
  if (!storeFilters.value || !storeFilters.value.conditions) return undefined
  return storeFilters.value.conditions.filter((item): item is FilterCondition => 'field' in item)
}

const getFilterStructure = (): string => {
  if (!storeFilters.value || !storeFilters.value.conditions) return ''

  const formatItem = (item: FilterCondition | FilterGroup): string => {
    if ('field' in item) {
      // It's a FilterCondition
      const condition = item as FilterCondition
      return `${getColumnLabel(condition.field)} ${getOperatorLabel(condition.operator)} ${condition.value}`
    } else if ('conditions' in item) {
      // It's a FilterGroup
      const group = item as FilterGroup
      const groupContent = group.conditions
        .map((subItem: FilterCondition | FilterGroup, subIndex: number) => {
          const itemStr = formatItem(subItem)
          // Add logic operator between items (except first item)
          return subIndex > 0 && subItem.logic ? `${subItem.logic} ${itemStr}` : itemStr
        })
        .join(' ')
      return `(${groupContent})`
    }
    return ''
  }

  return storeFilters.value.conditions
    .map((item: FilterCondition | FilterGroup, index: number) => {
      const itemStr = formatItem(item)
      // Add logic operator between items (except first item)
      return index > 0 && item.logic ? `${item.logic} ${itemStr}` : itemStr
    })
    .join(' ')
}

const getActiveFilterGroup = (): FilterGroup | undefined => {
  return storeFilters.value
}

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

const handleAdvancedFilterApply = (filterGroup: FilterGroup) => {
  advancedFiltersGroup.value =
    filterGroup && filterGroup.conditions.length > 0 ? filterGroup : undefined
  showAdvancedFilters.value = false
  updateStoreFilters()
}

const handleAdvancedFilterClear = () => {
  advancedFiltersGroup.value = undefined
  Object.keys(columnFiltersState).forEach((key) => {
    delete columnFiltersState[key]
  })
  activeFilters.value = []
  showAdvancedFilters.value = false
  setFilters(undefined)
  setPage(0)
  loadZones()
}

const clearFilters = () => {
  handleAdvancedFilterClear()
  if (searchTimeout) {
    clearTimeout(searchTimeout)
    searchTimeout = null
  }
  searchValue.value = ''
  handleSearch('')
}

/**
 * Handle filters update from column filters
 */
interface ColumnFilterUpdatePayload {
  columnId: string
  filters: FilterCondition[]
}

const updateStoreFilters = () => {
  const columnFilters = Object.values(columnFiltersState).flat()
  activeFilters.value = columnFilters

  const combinedConditions: (FilterCondition | FilterGroup)[] = []

  if (advancedFiltersGroup.value && advancedFiltersGroup.value.conditions.length > 0) {
    combinedConditions.push({
      logic: advancedFiltersGroup.value.logic,
      conditions: [...advancedFiltersGroup.value.conditions],
      id: advancedFiltersGroup.value.id,
    })
  }

  if (columnFilters.length > 0) {
    combinedConditions.push(...columnFilters.map((filter) => ({ ...filter })))
  }

  const combinedFilterGroup =
    combinedConditions.length > 0
      ? {
          logic: 'AND' as const,
          conditions: combinedConditions,
        }
      : undefined

  setFilters(combinedFilterGroup)
  setPage(0)
  loadZones()
}

const handleFiltersUpdate = ({ columnId, filters }: ColumnFilterUpdatePayload) => {
  if (filters.length > 0) {
    columnFiltersState[columnId] = filters.map((filter) => ({ ...filter }))
  } else {
    delete columnFiltersState[columnId]
  }

  updateStoreFilters()
}

// Table columns configuration
const columns: TableColumn<ZoneDto>[] = [
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
    accessorKey: 'actions',
    header: 'Actions',
    cell: ({ row }) => {
      const zone = row.original
      return h('div', { class: 'flex space-x-2' }, [
        h(UButton, {
          icon: 'i-heroicons-eye',
          size: 'sm',
          variant: 'ghost',
          title: 'View zone details',
          onClick: () => viewZoneDetail(zone),
        }),
        h(UButton, {
          icon: 'i-heroicons-pencil',
          size: 'sm',
          variant: 'ghost',
          title: 'Edit zone',
          onClick: () => openEditModal(zone),
        }),
        h(UButton, {
          icon: 'i-heroicons-trash',
          size: 'sm',
          variant: 'ghost',
          color: 'error',
          title: 'Delete zone',
          onClick: () => openDeleteModal(zone),
        }),
      ])
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
  },
  {
    accessorKey: 'name',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Name',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
  },
  {
    accessorKey: 'centerName',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Center',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
  },
  {
    accessorKey: 'hasPolygon',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Has Polygon',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
    cell: ({ row }) => {
      const hasPolygon = row.getValue('hasPolygon') as boolean
      const color = hasPolygon ? 'green' : 'gray'
      return h(UBadge, { class: 'capitalize', variant: 'soft', color }, () =>
        hasPolygon ? 'Yes' : 'No',
      )
    },
  },
  {
    accessorKey: 'id',
    header: 'ID',
  },
]

// Client-side filtering for simple mode
const filteredZones = computed(() => {
  let filtered = [...zones.value]

  // Apply search filter
  if (searchValue.value) {
    const search = searchValue.value.toLowerCase()
    filtered = filtered.filter(
      (zone) =>
        zone.code.toLowerCase().includes(search) ||
        zone.name.toLowerCase().includes(search) ||
        (zone.centerName && zone.centerName.toLowerCase().includes(search)),
    )
  }

  return filtered
})

/**
 * Get column label for sorting summary
 */
const getColumnLabel = (columnId: string): string => {
  const labelMap: Record<string, string> = {
    code: 'Code',
    name: 'Name',
    centerName: 'Center',
    hasPolygon: 'Has Polygon',
  }
  return labelMap[columnId] || columnId
}

// Removed getOperatorLabel as it's not used in this view

/**
 * Open create modal
 */
const openCreateModal = async () => {
  const modal = overlay.create(LazyZoneFormModal)
  const instance = modal.open({ mode: 'create' })
  const formData = await instance.result

  if (formData) {
    await create(formData)
  }
}

/**
 * Open edit modal
 */
const openEditModal = async (zone: ZoneDto) => {
  const modal = overlay.create(LazyZoneFormModal)
  const instance = modal.open({ mode: 'edit', zone })
  const formData = await instance.result

  if (formData) {
    await update(zone.zone_id, formData)
  }
}

/**
 * Open delete modal
 */
const openDeleteModal = async (zone: ZoneDto) => {
  const modal = overlay.create(LazyZoneDeleteModal)
  const instance = modal.open({ zoneName: zone.displayName })
  const confirmed = await instance.result

  if (confirmed) {
    await remove(zone.zone_id)
  }
}

/**
 * Handle bulk delete
 */
const handleBulkDelete = async () => {
  if (selected.value.length === 0) return

  const modal = overlay.create(LazyZoneDeleteModal)
  const instance = modal.open({ count: selected.value.length })
  const confirmed = await instance.result

  if (confirmed) {
    const ids = selected.value.map((zone) => zone.zone_id)
    await bulkDelete(ids)
    selected.value = []
  }
}

/**
 * Navigate to zone detail
 */
const viewZoneDetail = (zone: ZoneDto) => {
  router.push(`/zones/${zone.zone_id}`)
}

// Load data on mount
onMounted(async () => {
  await loadCenters()
  await loadZones()
})

// Watch for search changes with debounce
let searchTimeout: ReturnType<typeof setTimeout> | null = null
watch(searchValue, (newValue) => {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  searchTimeout = setTimeout(() => {
    handleSearch(newValue)
  }, 300)
})

const onSortingChange = (newSorting: SortingState): void => {
  // Convert sorting to store format
  const newSorts = newSorting.map((sort) => ({
    field: sort.id,
    direction: sort.desc ? ('desc' as const) : ('asc' as const),
  }))

  setSorts(newSorts)
  sorting.value = newSorting

  loadZones()
}

const clearSorting = () => {
  sorting.value = []
  setSorts([])
  loadZones()
}

// Watch for sorts changes and sync with sorting
watch(
  storeSorts,
  (newSorts: SortConfig[]) => {
    const newSorting = newSorts.map((sort: SortConfig) => ({
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
    <PageHeader title="Zones" description="Manage delivery zones and distribution areas">
      <template #actions>
        <UButton
          color="primary"
          variant="outline"
          icon="i-heroicons-map"
          @click="router.push('/zones/map')"
        >
          Map View
        </UButton>
        <UButton icon="i-heroicons-plus" @click="openCreateModal"> Add Zone </UButton>
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
            placeholder="Search zones..."
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
          @click="clearSorting"
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
        :data="filteredZones"
        :columns="columns"
        :loading="loading"
        :manual-sorting="true"
        enable-multi-sort
        @update:sorting="onSortingChange($event)"
      />

      <!-- Empty State -->
      <template v-if="!loading && filteredZones.length === 0">
        <div class="text-center py-12">
          <div class="mx-auto h-12 w-12 text-gray-400">
            <UIcon name="i-heroicons-map" class="h-12 w-12" />
          </div>
          <h3 class="mt-2 text-sm font-medium text-gray-900 dark:text-gray-100">No zones found</h3>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
            {{
              searchValue
                ? 'Try adjusting your search or filter criteria.'
                : 'Get started by creating a new zone.'
            }}
          </p>
          <div class="mt-6">
            <UButton v-if="!searchValue" icon="i-heroicons-plus" @click="openCreateModal">
              Add Zone
            </UButton>
            <UButton v-else variant="soft" @click="clearFilters"> Clear Filters </UButton>
          </div>
        </div>
      </template>
    </UCard>

    <!-- Pagination -->
    <div v-if="!loading && filteredZones.length > 0" class="mt-6 flex items-center justify-between">
      <div class="text-sm text-gray-700 dark:text-gray-300">
        Showing {{ page * pageSize + 1 }} to {{ Math.min((page + 1) * pageSize, total) }} of
        {{ total }} results
      </div>
      <UPagination
        :model-value="page + 1"
        :items-per-page="pageSize"
        :total="total"
        @update:model-value="(newPage: number) => setPage(newPage - 1)"
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
