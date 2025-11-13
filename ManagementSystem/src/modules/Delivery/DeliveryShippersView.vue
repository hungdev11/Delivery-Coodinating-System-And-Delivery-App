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
import type { DeliveryManDto } from './model.type'

const PageHeader = defineAsyncComponent(() => import('@/common/components/PageHeader.vue'))
const ShipperSessionsModal = defineAsyncComponent(
  () => import('./components/ShipperSessionsModal.vue'),
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

const setupHeader = ({
  column,
  config,
}: {
  column: HeaderColumn
  config: HeaderConfig
}) =>
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
      return `${item.field} ${item.operator} ${item.value}`
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

const handleFiltersUpdate = ({ columnId, filters }: { columnId: string; filters: FilterCondition[] }) => {
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
  advancedFiltersGroup.value = filterGroup && filterGroup.conditions.length > 0 ? filterGroup : undefined
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
          label: 'Vehicle Type',
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
          label: 'Capacity (kg)',
          class: '-mx-2.5',
          filterable: true,
        },
      }),
  },
  {
    accessorKey: 'status',
    header: 'Status',
  },
  {
    accessorKey: 'createdAt',
    header: 'Created At',
  },
  {
    accessorKey: 'actions',
    header: 'Actions',
    cell: ({ row }: { row: { original: DeliveryManDto } }) => {
      const shipper = row.original
      return h('div', { class: 'flex gap-2' }, [
        h(UButton, {
          icon: 'i-heroicons-arrow-path',
          size: 'sm',
          variant: 'ghost',
          title: 'View sessions',
          onClick: () => openSessionsModal(shipper),
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

const openSessionsModal = (shipper: DeliveryManDto) => {
  const modal = overlay.create(ShipperSessionsModal)
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
  <div class="container mx-auto px-4 py-6">
    <PageHeader title="Delivery" description="Manage shippers and their delivery sessions">
      <template #actions>
        <UButton
          variant="soft"
          color="primary"
          icon="i-heroicons-map"
          @click="router.push('/zones/map/demo-routing')"
        >
          Demo Routing
        </UButton>
      </template>
    </PageHeader>

    <div class="mb-6 space-y-4">
      <div class="flex flex-col sm:flex-row gap-4">
        <div class="flex-1">
          <UInput
            v-model="searchValue"
            placeholder="Search shippers..."
            icon="i-heroicons-magnifying-glass"
            size="lg"
          />
        </div>
      </div>

      <div class="flex justify-between items-center">
        <div
          v-if="getAllActiveFilters() && getAllActiveFilters()!.length > 0"
          class="flex items-center gap-2"
        >
          <span class="text-sm text-gray-600 dark:text-gray-400">Active filters:</span>
          <UBadge color="primary" variant="soft" size="sm" class="max-w-md truncate">
            {{ getFilterStructure() }}
          </UBadge>
          <UButton
            variant="ghost"
            size="xs"
            color="neutral"
            icon="i-heroicons-x-mark"
            @click="handleAdvancedFilterClear"
            title="Clear all filters"
          />
        </div>

        <UButton
          variant="soft"
          color="primary"
          icon="i-heroicons-funnel"
          @click="showAdvancedFilters = true"
        >
          Advanced Filters
        </UButton>
      </div>
    </div>

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
      >
        <template #cell(displayName)="{ row }">
          <div class="flex flex-col">
            <span class="font-medium">{{ row.original.displayName }}</span>
            <span class="text-xs text-gray-500">{{ row.original.email || 'No email' }}</span>
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
          <h3 class="mt-2 text-sm font-medium text-gray-900 dark:text-gray-100">No shippers found</h3>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Try adjusting your search or filter criteria.
          </p>
        </div>
      </template>
    </UCard>

    <div v-if="!loading && filteredShippers.length > 0" class="mt-6 flex items-center justify-between">
      <div class="text-sm text-gray-700 dark:text-gray-300">
        Showing {{ page * pageSize + 1 }} to {{ Math.min((page + 1) * pageSize, total) }} of
        {{ total }} results
      </div>
      <UPagination v-model="page" :items-per-page="pageSize" :total="total" />
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
