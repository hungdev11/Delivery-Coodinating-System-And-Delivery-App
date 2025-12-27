<script setup lang="ts">
/**
 * Delivery Shippers View
 *
 * Admin page for managing shippers (delivery men)
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
import { useShippers } from './composables'
import TableHeaderCell from '@/common/components/TableHeaderCell.vue'
import AdvancedFilterDrawer from '@/common/components/filters/AdvancedFilterDrawer.vue'
import TableFilters from '@/common/components/table/TableFilters.vue'
import type { DeliveryManDto } from './model.type'

const PageHeader = defineAsyncComponent(() => import('@/common/components/PageHeader.vue'))
const ShipperShiftManagementModal = defineAsyncComponent(
  () => import('./components/ShipperShiftManagementModal.vue'),
)

const overlay = useOverlay()
const router = useRouter()
const table = useTemplateRef('table')

const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')

const {
  shippers,
  loading,
  page,
  pageSize,
  total,
  filters,
  sorts,
  searchQuery,
  loadShippers,
  handleSearch,
  updateFilters,
  updateSorts,
  clearFilters,
  getFilterableColumns,
  handlePageChange,
} = useShippers()
const sorting = ref<Array<{ id: string; desc: boolean }>>([])
const activeFilters = ref<FilterCondition[]>([])
const columnFiltersState = reactive<Record<string, FilterCondition[]>>({})
const advancedFiltersGroup = ref<FilterGroup | undefined>(undefined)
const showAdvancedFilters = ref(false)
const filterableColumns = computed(() => getFilterableColumns())

type HeaderConfig = {
  variant: 'link' | 'solid' | 'outline' | 'soft' | 'ghost'
  label: string
  class: string
  activeColor?: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
  inactiveColor?: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
  filterable?: boolean
}

type HeaderColumn = Column<DeliveryManDto, unknown>

const setupHeader = ({ column, config }: { column: HeaderColumn; config: HeaderConfig }) =>
  h(TableHeaderCell<DeliveryManDto>, {
    column,
    config,
    filterableColumns: filterableColumns.value,
    activeFilters: activeFilters.value,
    'onUpdate:filters': handleFiltersUpdate,
  })

const getAllActiveFilters = (): FilterCondition[] | undefined => {
  if (!filters.value || !filters.value.conditions) return undefined
  const extract = (item: FilterCondition | FilterGroup): FilterCondition[] => {
    if ('field' in item) return [item]
    if ('conditions' in item) return item.conditions.flatMap(extract)
    return []
  }
  const allFilters = filters.value.conditions.flatMap(extract)
  return allFilters.length > 0 ? allFilters : undefined
}

const getFilterStructure = (): string => {
  if (!filters.value || !filters.value.conditions) return ''
  const formatItem = (item: FilterCondition | FilterGroup): string => {
    if ('field' in item) {
      const condition = item as FilterCondition
      return `${getColumnLabel(condition.field)} ${getOperatorLabel(condition.operator)} ${condition.value}`
    }
    const groupContent = item.conditions
      .map((subItem, index) => {
        const formatted = formatItem(subItem)
        return index > 0 && subItem.logic ? `${subItem.logic} ${formatted}` : formatted
      })
      .join(' ')
    return `(${groupContent})`
  }
  return filters.value.conditions
    .map((item, index) => {
      const formatted = formatItem(item)
      return index > 0 && item.logic ? `${item.logic} ${formatted}` : formatted
    })
    .join(' ')
}

const handleFiltersUpdate = ({
  columnId,
  filters,
}: {
  columnId: string
  filters: FilterCondition[]
}) => {
  if (filters.length > 0) {
    columnFiltersState[columnId] = filters.map((filter) => ({ ...filter }))
  } else {
    delete columnFiltersState[columnId]
  }

  const columnFilters = Object.values(columnFiltersState).flat()
  activeFilters.value = columnFilters

  if (columnFilters.length === 0 && !advancedFiltersGroup.value) {
    clearFilters()
    return
  }

  updateFilters({
    logic: 'AND',
    conditions: [
      ...(advancedFiltersGroup.value && advancedFiltersGroup.value.conditions.length > 0
        ? [advancedFiltersGroup.value]
        : []),
      ...columnFilters,
    ],
  })
}

const handleAdvancedFilterApply = (filterGroup: FilterGroup) => {
  advancedFiltersGroup.value =
    filterGroup && filterGroup.conditions.length > 0 ? filterGroup : undefined
  showAdvancedFilters.value = false
  updateFilters({
    logic: 'AND',
    conditions: [
      ...(advancedFiltersGroup.value ? [advancedFiltersGroup.value] : []),
      ...Object.values(columnFiltersState).flat(),
    ],
  })
}

const handleAdvancedFilterClear = () => {
  advancedFiltersGroup.value = undefined
  Object.keys(columnFiltersState).forEach((key) => delete columnFiltersState[key])
  activeFilters.value = []
  clearFilters()
  showAdvancedFilters.value = false
}

const columns = [
  {
    accessorKey: 'displayName',
    header: ({ column }: { column: HeaderColumn }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Shipper',
          class: '-mx-2.5',
          filterable: true,
        },
      }),
  },
  {
    accessorKey: 'vehicleType',
    header: ({ column }: { column: HeaderColumn }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Loại xe',
          class: '-mx-2.5',
          filterable: true,
        },
      }),
  },
  {
    accessorKey: 'capacityKg',
    header: ({ column }: { column: HeaderColumn }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Tải trọng (kg)',
          class: '-mx-2.5',
          filterable: true,
        },
      }),
  },
  {
    accessorKey: 'status',
    header: 'Trạng thái',
  },
  {
    accessorKey: 'createdAt',
    header: 'Ngày tạo',
  },
  {
    accessorKey: 'actions',
    header: 'Thao tác',
    cell: ({ row }: { row: { original: DeliveryManDto } }) => {
      const shipper = row.original
      return h('div', { class: 'flex gap-2' }, [
        h(UButton, {
          icon: 'i-heroicons-arrow-path',
          size: 'sm',
          variant: 'ghost',
          title: 'Quản lý ca làm việc',
          onClick: () => openShiftManagementModal(shipper),
        }),
      ])
    },
  },
]

const filteredShippers = computed(() => {
  let result = [...shippers.value]
  if (searchQuery.value) {
    const search = searchQuery.value.toLowerCase()
    result = result.filter((shipper) => {
      const matchName = shipper.displayName.toLowerCase().includes(search)
      const matchVehicle = shipper.vehicleType?.toLowerCase().includes(search)
      const matchEmail = shipper.email?.toLowerCase().includes(search) ?? false
      return matchName || matchVehicle || matchEmail
    })
  }
  return result
})

const formatCapacity = (capacity: number) =>
  new Intl.NumberFormat('en-US', { maximumFractionDigits: 2 }).format(capacity)

const formatDate = (value?: string) => {
  if (!value) return '—'
  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

const openShiftManagementModal = (shipper: DeliveryManDto) => {
  const modal = overlay.create(ShipperShiftManagementModal)
  modal.open({ shipper })
}

const onSortingChange = (newSorting: SortingState): void => {
  const newSorts: SortConfig[] = newSorting.map((sort) => ({
    field: sort.id,
    direction: sort.desc ? 'desc' : 'asc',
  }))
  updateSorts(newSorts)
  sorting.value = newSorting as Array<{ id: string; desc: boolean }>
}

// Handle clear sorting
const handleClearSorting = () => {
  sorting.value = []
  updateSorts([])
}

// Get column label for sorting summary
const getColumnLabel = (columnId: string): string => {
  const labelMap: Record<string, string> = {
    displayName: 'Shipper',
    vehicleType: 'Loại xe',
    capacityKg: 'Tải trọng',
    status: 'Trạng thái',
    createdAt: 'Ngày tạo',
  }
  return labelMap[columnId] || columnId
}

// Get operator label for display
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

const searchValue = ref('')
let searchTimeout: NodeJS.Timeout
watch(searchValue, (newValue) => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    handleSearch(newValue)
  }, 300)
})

onMounted(async () => {
  await loadShippers()
})
</script>

<template>
  <div class="container mx-auto px-2 sm:px-4 py-4 sm:py-6">
    <PageHeader title="Giao hàng" description="Quản lý Shipper và các phiên giao hàng của họ">
      <template #actions>
        <UButton
          variant="soft"
          color="primary"
          icon="i-heroicons-clipboard-document-list"
          size="sm"
          class="md:size-md"
          @click="router.push({ name: 'delivery-tasks' })"
        >
          <span class="hidden sm:inline">Quản lý nhiệm vụ</span>
          <span class="sm:hidden">Nhiệm vụ</span>
        </UButton>
        <UButton
          variant="soft"
          color="primary"
          icon="i-heroicons-calendar-days"
          size="sm"
          class="md:size-md"
          @click="router.push({ name: 'delivery-shift-calendar' })"
        >
          <span class="hidden sm:inline">Lịch ca làm việc</span>
          <span class="sm:hidden">Lịch</span>
        </UButton>
        <UButton
          variant="soft"
          color="primary"
          icon="i-heroicons-map"
          size="sm"
          class="md:size-md"
          @click="router.push('/zones/map/demo-routing')"
        >
          <span class="hidden sm:inline">Demo tuyến đường</span>
          <span class="sm:hidden">Tuyến đường</span>
        </UButton>
      </template>
    </PageHeader>

    <!-- Table Filters (includes Search + Filters + Sort) -->
    <TableFilters
      :search-value="searchValue"
      search-placeholder="Tìm kiếm Shipper..."
      :active-filters="getAllActiveFilters()"
      :filter-structure="getFilterStructure()"
      :sorting="sorting"
      :get-column-label="getColumnLabel"
      :sortable-columns="sortableColumnsList"
      @update:search-value="searchValue = $event"
      @update:sorting="onSortingChange"
      @clear-filters="handleAdvancedFilterClear"
      @clear-sorting="handleClearSorting"
      @open-advanced-filters="showAdvancedFilters = true"
    />

    <UCard>
      <UTable
        ref="table"
        :data="filteredShippers"
        :columns="columns"
        :loading="loading"
        :sorting="sorting"
        :manual-sorting="true"
        enable-multi-sort
        @update:sorting="onSortingChange"
        :ui="{
          empty: 'text-center py-12',
          root: 'h-[50vh]',
          thead: 'sticky top-0 bg-white dark:bg-gray-800',
        }"
      >
        <template #cell(displayName)="{ row }">
          <div class="flex flex-col gap-1">
            <div class="flex items-center gap-2">
              <span class="font-medium">{{ row.original.displayName }}</span>
              <UBadge
                v-if="row.original.hasActiveSession"
                variant="solid"
                color="success"
                class="text-xs"
              >
                ACTIVE
              </UBadge>
            </div>
            <span v-if="row.original.email" class="text-xs text-gray-500">{{ row.original.email }}</span>
            <span
              v-if="row.original.lastSessionStartTime"
              class="text-xs text-gray-400"
            >
              Phiên cuối:
              {{
                new Intl.DateTimeFormat('en-GB', {
                  dateStyle: 'short',
                  timeStyle: 'short',
                }).format(new Date(row.original.lastSessionStartTime))
              }}
            </span>
          </div>
        </template>

        <template #cell(vehicleType)="{ row }">
          {{ row.original.vehicleType || '—' }}
        </template>

        <template #cell(capacityKg)="{ row }">
          {{ formatCapacity(row.original.capacityKg) }} kg
        </template>

        <template #cell(status)="{ row }">
          <UBadge
            :color="
              row.original.status === 'ACTIVE'
                ? 'success'
                : row.original.status === 'SUSPENDED'
                  ? 'error'
                  : row.original.status === 'PENDING'
                    ? 'warning'
                    : 'neutral'
            "
            variant="soft"
            class="capitalize"
          >
            {{ row.original.status ? row.original.status.toLowerCase() : 'unknown' }}
          </UBadge>
        </template>

        <template #cell(createdAt)="{ row }">
          {{ formatDate(row.original.createdAt) }}
        </template>
      </UTable>

      <template v-if="!loading && filteredShippers.length === 0">
        <div class="text-center py-12">
          <div class="mx-auto h-12 w-12 text-gray-400">
            <UIcon name="i-heroicons-truck" class="h-12 w-12" />
          </div>
          <h3 class="mt-2 text-sm font-medium text-gray-900 dark:text-gray-100">
            Không tìm thấy Shipper
          </h3>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Thử điều chỉnh tiêu chí tìm kiếm hoặc lọc của bạn.
          </p>
        </div>
      </template>
    </UCard>

    <div
      v-if="!loading && filteredShippers.length > 0"
      class="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4"
    >
      <div class="text-sm text-gray-700 dark:text-gray-300 text-center sm:text-left">
        Hiển thị {{ page * pageSize + 1 }} đến {{ Math.min((page + 1) * pageSize, total) }} trong tổng số
        {{ total }} kết quả
      </div>
      <UPagination
        :model-value="page + 1"
        :items-per-page="pageSize"
        :total="total"
        @update:model-value="(newPage: number) => handlePageChange(newPage - 1)"
      />
    </div>

    <AdvancedFilterDrawer
      :show="showAdvancedFilters"
      :columns="filterableColumns"
      :active-filters="getAllActiveFilters()"
      :active-filter-group="advancedFiltersGroup"
      @apply="handleAdvancedFilterApply"
      @clear="handleAdvancedFilterClear"
      @update:show="showAdvancedFilters = $event"
    />
  </div>
</template>
