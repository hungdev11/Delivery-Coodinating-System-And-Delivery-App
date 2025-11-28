import { defineStore } from 'pinia'
import { ref, computed, watch, readonly } from 'vue'

export interface SortConfig {
  key: string
  direction: 'asc' | 'desc'
}

export interface FilterConfig {
  key: string
  value: any
  operator:
    | 'equals'
    | 'contains'
    | 'startsWith'
    | 'endsWith'
    | 'greaterThan'
    | 'lessThan'
    | 'between'
}

export interface ColumnConfig {
  accessorKey: string
  header: string
  sortable?: boolean
  filterable?: boolean
  filterType?: 'text' | 'select' | 'number' | 'date' | 'boolean'
  filterOptions?: any[]
  class?: string
}

export interface DataTableState<T = any> {
  data: T[]
  loading: boolean
  page: number
  pageSize: number
  total: number
  searchQuery: string
  sorts: SortConfig[]
  filters: FilterConfig[]
  selectedIds: Set<string>
  rowKey: string
}

export const useDataTableStore = defineStore('dataTable', () => {
  // State
  const state = ref<DataTableState>({
    data: [],
    loading: false,
    page: 0,
    pageSize: 10,
    total: 0,
    searchQuery: '',
    sorts: [],
    filters: [],
    selectedIds: new Set(),
    rowKey: 'id',
  })

  // Getters
  const hasData = computed(() => state.value.data.length > 0)
  const hasSelection = computed(() => state.value.selectedIds.size > 0)
  const selectionCount = computed(() => state.value.selectedIds.size)
  const selectedRows = computed(() =>
    state.value.data.filter((row) => state.value.selectedIds.has(row[state.value.rowKey])),
  )
  const selectedIds = computed(() => Array.from(state.value.selectedIds))

  const totalPages = computed(() => Math.ceil(state.value.total / state.value.pageSize))

  const currentPageDisplay = computed(() => state.value.page + 1)

  const hasSorts = computed(() => state.value.sorts.length > 0)
  const hasFilters = computed(() => state.value.filters.length > 0)
  const hasSearch = computed(() => state.value.searchQuery.trim().length > 0)
  const hasActiveFilters = computed(() => hasSorts.value || hasFilters.value || hasSearch.value)

  // Actions
  const setData = (data: any[]) => {
    state.value.data = data
  }

  const setLoading = (loading: boolean) => {
    state.value.loading = loading
  }

  const setPage = (page: number) => {
    state.value.page = Math.max(0, page)
  }

  const setPageSize = (size: number) => {
    state.value.pageSize = Math.max(1, size)
    // Reset to first page when page size changes
    state.value.page = 0
  }

  const setTotal = (total: number) => {
    state.value.total = total
  }

  const setSearchQuery = (query: string) => {
    state.value.searchQuery = query
    // Reset to first page when search changes
    state.value.page = 0
  }

  const setRowKey = (key: string) => {
    state.value.rowKey = key
  }

  // Sorting
  const addSort = (key: string, direction: 'asc' | 'desc' = 'asc') => {
    // Remove existing sort for this key
    state.value.sorts = state.value.sorts.filter((sort) => sort.key !== key)

    // Add new sort
    state.value.sorts.push({ key, direction })

    // Reset to first page when sorting changes
    state.value.page = 0
  }

  const removeSort = (key: string) => {
    state.value.sorts = state.value.sorts.filter((sort) => sort.key !== key)
    state.value.page = 0
  }

  const toggleSort = (key: string) => {
    const existingSort = state.value.sorts.find((sort) => sort.key === key)

    if (!existingSort) {
      addSort(key, 'asc')
    } else if (existingSort.direction === 'asc') {
      addSort(key, 'desc')
    } else {
      removeSort(key)
    }
  }

  const clearSorts = () => {
    state.value.sorts = []
    state.value.page = 0
  }

  const getSortDirection = (key: string): 'asc' | 'desc' | null => {
    const sort = state.value.sorts.find((s) => s.key === key)
    return sort ? sort.direction : null
  }

  // Filtering
  const addFilter = (key: string, value: any, operator: FilterConfig['operator'] = 'contains') => {
    // Remove existing filter for this key
    state.value.filters = state.value.filters.filter((filter) => filter.key !== key)

    // Add new filter if value is not empty
    if (value !== null && value !== undefined && value !== '') {
      state.value.filters.push({ key, value, operator })
    }

    state.value.page = 0
  }

  const removeFilter = (key: string) => {
    state.value.filters = state.value.filters.filter((filter) => filter.key !== key)
    state.value.page = 0
  }

  const clearFilters = () => {
    state.value.filters = []
    state.value.page = 0
  }

  const clearSearch = () => {
    state.value.searchQuery = ''
    state.value.page = 0
  }

  const clearAllFilters = () => {
    clearFilters()
    clearSearch()
    clearSorts()
  }

  const getFilterValue = (key: string) => {
    const filter = state.value.filters.find((f) => f.key === key)
    return filter ? filter.value : null
  }

  // Selection
  const selectRow = (rowKey: string) => {
    state.value.selectedIds.add(rowKey)
  }

  const deselectRow = (rowKey: string) => {
    state.value.selectedIds.delete(rowKey)
  }

  const toggleRowSelection = (rowKey: string) => {
    if (state.value.selectedIds.has(rowKey)) {
      deselectRow(rowKey)
    } else {
      selectRow(rowKey)
    }
  }

  const selectAll = () => {
    state.value.data.forEach((row) => {
      state.value.selectedIds.add(row[state.value.rowKey])
    })
  }

  const deselectAll = () => {
    state.value.selectedIds.clear()
  }

  const toggleSelectAll = () => {
    if (hasSelection.value) {
      deselectAll()
    } else {
      selectAll()
    }
  }

  const isRowSelected = (rowKey: string) => {
    return state.value.selectedIds.has(rowKey)
  }

  // Bulk operations
  const deleteSelected = () => {
    const ids = selectedIds.value
    deselectAll()
    return ids
  }

  // Reset state
  const reset = () => {
    state.value = {
      data: [],
      loading: false,
      page: 0,
      pageSize: 10,
      total: 0,
      searchQuery: '',
      sorts: [],
      filters: [],
      selectedIds: new Set(),
      rowKey: 'id',
    }
  }

  // Watch for data changes to clear selection if needed
  watch(
    () => state.value.data,
    (newData) => {
      // Remove selections for rows that no longer exist
      const existingIds = new Set(newData.map((row) => row[state.value.rowKey]))
      const validSelections = new Set(
        [...state.value.selectedIds].filter((id) => existingIds.has(id)),
      )
      state.value.selectedIds = validSelections
    },
  )

  return {
    // State
    state: state,

    // Getters
    hasData,
    hasSelection,
    selectionCount,
    selectedRows,
    selectedIds,
    totalPages,
    currentPageDisplay,
    hasSorts,
    hasFilters,
    hasSearch,
    hasActiveFilters,

    // Data actions
    setData,
    setLoading,
    setTotal,
    setRowKey,

    // Pagination actions
    setPage,
    setPageSize,

    // Search actions
    setSearchQuery,
    clearSearch,

    // Sort actions
    addSort,
    removeSort,
    toggleSort,
    clearSorts,
    getSortDirection,

    // Filter actions
    addFilter,
    removeFilter,
    clearFilters,
    clearAllFilters,
    getFilterValue,

    // Selection actions
    selectRow,
    deselectRow,
    toggleRowSelection,
    selectAll,
    deselectAll,
    toggleSelectAll,
    isRowSelected,

    // Bulk actions
    deleteSelected,

    // Utility
    reset,
  }
})
