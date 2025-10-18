<script setup lang="ts" generic="T extends Record<string, any>">
import { useDebounceFn } from '@vueuse/core';
import { computed, ref, watch, onMounted, nextTick, h } from 'vue'
import { useDataTableStore } from '../store/useDataTableStore'

/**
 * DataTable Component
 *
 * A wrapper around UTable with pagination, search, bulk actions, and loading states
 *
 * @example
 * ```vue
 * <DataTable
 *   :columns="columns"
 *   :rows="users"
 *   :loading="loading"
 *   :page="page"
 *   :total="total"
 *   selectable
 *   @update:page="handlePageChange"
 *   @bulk-delete="handleBulkDelete"
 * >
 *   <template #bulk-actions="{ selected }">
 *     <UButton @click="exportSelected(selected)">Export</UButton>
 *   </template>
 * </DataTable>
 * ```
 */

interface Column {
  accessorKey: string
  header: string
  cell?: (props: { row: any; getValue: () => any }) => any
  sortable?: boolean
  filterable?: boolean
  filterType?: 'text' | 'select' | 'number' | 'date' | 'boolean'
  filterOptions?: any[]
  class?: string
}

interface Props {
  /** Table columns */
  columns: Column[]
  /** Table rows */
  rows: T[]
  /** Loading state */
  loading?: boolean
  /** Current page (0-indexed) */
  page?: number
  /** Page size */
  pageSize?: number
  /** Total number of items */
  total?: number
  /** Enable search */
  searchable?: boolean
  /** Search placeholder */
  searchPlaceholder?: string
  /** Enable selection */
  selectable?: boolean
  /** Enable sorting */
  sortable?: boolean
  /** Enable filtering */
  filterable?: boolean
  /** Empty state message */
  emptyMessage?: string
  /** Row key for selection tracking */
  rowKey?: string
  /** Store key for persistence */
  storeKey?: string
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  page: 0,
  pageSize: 10,
  total: 0,
  searchable: false,
  searchPlaceholder: 'Search...',
  selectable: false,
  sortable: true,
  filterable: false,
  emptyMessage: 'No data available',
  rowKey: 'id',
  storeKey: 'default'
})

const emit = defineEmits<{
  'update:page': [page: number]
  'update:pageSize': [size: number]
  search: [query: string]
  sort: [sorts: { key: string; direction: 'asc' | 'desc' }[]]
  filter: [filters: { key: string; value: any; operator: string }[]]
  'selection-change': [rows: T[]]
  'row-click': [row: T]
  'bulk-delete': [ids: string[]]
}>()

// Initialize store
const store = useDataTableStore()

// Local search query for debouncing
const localSearchQuery = ref('')

// Sync props with store
watch(() => props.rows, (newRows) => {
  store.setData(newRows)
}, { immediate: true })

watch(() => props.loading, (loading) => {
  store.setLoading(loading)
}, { immediate: true })

watch(() => props.total, (total) => {
  store.setTotal(total)
}, { immediate: true })

watch(() => props.pageSize, (pageSize) => {
  store.setPageSize(pageSize)
}, { immediate: true })

watch(() => props.rowKey, (rowKey) => {
  store.setRowKey(rowKey)
}, { immediate: true })

// Handle search with debouncing
const handleSearch = useDebounceFn(() => {
  store.setSearchQuery(localSearchQuery.value)
  emit('search', localSearchQuery.value)
}, 300)

// Handle sorting
const handleSort = (key: string) => {
  if (!props.sortable) return

  store.toggleSort(key)
  emit('sort', store.state.sorts)
}

// Handle filtering
const handleFilter = (key: string, value: any, operator = 'contains') => {
  if (!props.filterable) return

  store.addFilter(key, value, operator)
  emit('filter', store.state.filters)
}

// Handle pagination
const currentPage = computed({
  get: () => store.currentPageDisplay,
  set: (value) => {
    store.setPage(value - 1) // Convert from 1-indexed to 0-indexed
    emit('update:page', store.state.page)
  }
})

// Handle bulk operations
const handleBulkDelete = () => {
  const ids = store.deleteSelected()
  emit('bulk-delete', ids)
}

// Watch for selection changes
watch(() => store.selectedRows, (selectedRows) => {
  emit('selection-change', selectedRows)
}, { deep: true })

// Computed columns with sorting indicators
const columnsWithSorting = computed(() => {
  return props.columns.map(col => ({
    ...col,
    header: () => {
      const sortDirection = store.getSortDirection(col.accessorKey)
      const sortIcon = sortDirection ?
        (sortDirection === 'asc' ? 'i-heroicons-chevron-up' : 'i-heroicons-chevron-down') :
        'i-heroicons-chevron-up-down'

      return h('div', {
        class: 'flex items-center gap-2 cursor-pointer',
        onClick: () => handleSort(col.accessorKey)
      }, [
        h('span', col.header),
        h('UIcon', {
          name: sortIcon,
          class: 'w-4 h-4 text-gray-400'
        })
      ])
    }
  }))
})

/**
 * Component initialization
 */
onMounted(() => {
  // Ensure columns are properly initialized
  if (!props.columns || props.columns.length === 0) {
    console.warn('DataTable: No columns provided')
  }

  // Log column configuration for debugging
  console.debug('DataTable mounted with columns:', props.columns.map(col => ({
    accessorKey: col.accessorKey,
    header: col.header,
    sortable: col.sortable,
    filterable: col.filterable
  })))

  // Initialize store with current props
  store.setData(props.rows)
  store.setLoading(props.loading)
  store.setTotal(props.total)
  store.setPageSize(props.pageSize)
  store.setRowKey(props.rowKey)
})
</script>

<template>
  <div class="space-y-4">
    <!-- Search Bar -->
    <div class="flex justify-between items-center gap-4">
      <UInput
        v-if="searchable"
        v-model="localSearchQuery"
        icon="i-heroicons-magnifying-glass"
        :placeholder="searchPlaceholder"
        @input="handleSearch"
        class="flex-1 max-w-md"
      />
      <div class="flex items-center gap-2">
        <slot name="actions" />
      </div>
    </div>

    <!-- Bulk Actions Bar -->
    <div
      v-if="selectable && store.hasSelection"
      class="flex items-center justify-between p-3 bg-primary-50 border border-primary-200 rounded-lg"
    >
      <div class="flex items-center gap-3">
        <span class="text-sm font-medium text-primary-900">
          {{ store.selectionCount }} item{{ store.selectionCount !== 1 ? 's' : '' }} selected
        </span>
        <UButton size="xs" variant="ghost" @click="store.deselectAll"> Clear selection </UButton>
      </div>

      <div class="flex items-center gap-2">
        <!-- Custom bulk actions slot -->
        <slot name="bulk-actions" :selected="store.selectedIds" :count="store.selectionCount" />

        <!-- Default bulk delete action -->
        <UButton
          size="sm"
          color="error"
          variant="soft"
          icon="i-heroicons-trash"
          @click="handleBulkDelete"
        >
          Delete {{ store.selectionCount }} item{{ store.selectionCount !== 1 ? 's' : '' }}
        </UButton>
      </div>
    </div>

    <!-- Table -->
    <UTable
      :v-if="store.state.data.length > 0"
      :data="store.state.data"
      :columns="columnsWithSorting"
      :loading="store.state.loading"
      :ui="{ empty: emptyMessage }"
      @select="(e: Event, row: any) => emit('row-click', row)"
    >
      <template #empty>
        <div class="flex flex-col items-center justify-center h-32 text-gray-500">
          <UIcon name="i-heroicons-inbox" class="w-8 h-8 mb-2" />
          <p>{{ emptyMessage }}</p>
        </div>
      </template>

      <!-- Forward all slots to UTable -->
      <template v-for="(_, name) in $slots" #[name]="slotData">
        <slot :name="name" v-bind="slotData" />
      </template>
    </UTable>

    <!-- Pagination -->
    <div v-if="store.state.total > store.state.pageSize" class="flex justify-between items-center">
      <p class="text-sm text-gray-600">
        Showing {{ store.state.page * store.state.pageSize + 1 }} to
        {{ Math.min((store.state.page + 1) * store.state.pageSize, store.state.total) }} of {{ store.state.total }} results
      </p>

      <UPagination v-model="currentPage" :total="store.totalPages" :max="7" />
    </div>
  </div>
</template>
