<script setup lang="ts">
/**
 * Users List View
 *
 * Main view for managing users with UTable and Nuxt UI v3 best practices
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
import { useUsers, useUserExport } from './composables'
import type { UserDto, UserStatus } from './model.type'
import { useTemplateRef } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import AdvancedFilterDrawer from '../../common/components/filters/AdvancedFilterDrawer.vue'
import TableFilters from '../../common/components/table/TableFilters.vue'
import type { SortingState, Column } from '@tanstack/table-core'
import type { FilterCondition, FilterGroup } from '../../common/types/filter'
import { createSortConfig } from '../../common/utils/query-builder'
import TableHeaderCell from '../../common/components/TableHeaderCell.vue'

// Type for table ref
interface TableRef {
  tableApi?: {
    getFilteredSelectedRowModel?: () => { rows?: Array<unknown> }
    getFilteredRowModel?: () => { rows?: Array<unknown> }
  }
}

// Dynamic imports to avoid TypeScript issues
const PageHeader = defineAsyncComponent(() => import('../../common/components/PageHeader.vue'))

// Lazy load modals
const LazyUserFormModal = defineAsyncComponent(() => import('./components/UserFormModal.vue'))
const LazyUserDeleteModal = defineAsyncComponent(() => import('./components/UserDeleteModal.vue'))
const UCheckbox = resolveComponent('UCheckbox')
const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')

const router = useRouter()
const overlay = useOverlay()
const table = useTemplateRef<TableRef | null>('table')

// Composables
const {
  users,
  loading,
  page,
  pageSize,
  total,
  filters,
  sorts,
  loadUsers,
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
} = useUsers()

const { exportUsers } = useUserExport()

// Handle page change from UPagination (1-indexed) to API (0-indexed)
const handlePaginationChange = (newPage: number) => {
  // UPagination uses 1-indexed pages, convert to 0-indexed for API
  handlePageChange(Math.max(newPage - 1, 0))
}

// Table state
const selected = ref<UserDto[]>([])
const sorting = ref<Array<{ id: string; desc: boolean }>>([])
const activeFilters = ref<FilterCondition[]>([])
const columnFiltersState = reactive<Record<string, FilterCondition[]>>({})
const advancedFiltersGroup = ref<FilterGroup | undefined>(undefined)

// Search and filter state
const searchValue = ref('')
const statusFilter = ref<UserStatus | ''>('')

// Advanced filter state
const showAdvancedFilters = ref(false)
const filterableColumns = computed(() => getFilterableColumns())

// Get sortable columns list from filterable columns (for consistency)
// Only include columns that are actually sortable (not arrays, objects, etc.)
const sortableColumnsList = computed(() => {
  // Fields that should not be sortable
  const excludeFields = ['id', 'select', 'actions', 'roles'] // roles is an array
  // Only include columns with simple types that can be sorted
  const sortableTypes = ['string', 'number', 'date', 'enum']

  return filterableColumns.value
    .filter(
      (col) =>
        !excludeFields.includes(col.field) &&
        col.field &&
        sortableTypes.includes(col.type || ''),
    )
    .map((col) => ({
      id: col.field,
      label: col.label,
    }))
})

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

type HeaderColumn = Column<UserDto, unknown>

const setupHeader = ({ column, config }: { column: HeaderColumn; config: HeaderConfig }) =>
  h(TableHeaderCell<UserDto>, {
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
const columns: TableColumn<UserDto>[] = [
  {
    accessorKey: 'select',
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
      const user = row.original
      return h('div', { class: 'flex space-x-2' }, [
        h(UButton, {
          icon: 'i-heroicons-eye',
          size: 'sm',
          variant: 'ghost',
          title: 'View user details',
          onClick: () => viewUserDetail(user),
        }),
        h(UButton, {
          icon: 'i-heroicons-pencil',
          size: 'sm',
          variant: 'ghost',
          title: 'Edit user',
          onClick: () => openEditModal(user),
        }),
        h(UButton, {
          icon: 'i-heroicons-trash',
          size: 'sm',
          variant: 'ghost',
          color: 'error',
          title: 'Delete user',
          onClick: () => openDeleteModal(user),
        }),
      ])
    },
  },
  {
    accessorKey: 'username',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Username',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
  },
  {
    accessorKey: 'fullName',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Full Name',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
  },
  {
    accessorKey: 'email',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Email',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
  },
  {
    accessorKey: 'phone',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Phone',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
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
      const status = row.getValue('status') as UserStatus
      const color = getStatusColor(status)
      const displayStatus = getDisplayStatus(status)
      return h(UBadge, { class: 'capitalize', variant: 'soft', color }, () => displayStatus)
    },
  },
  {
    accessorKey: 'roles',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Roles',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: false,
        },
      }),
    cell: ({ row }) => {
      const roles = row.getValue('roles') as string[]
      if (!roles || roles.length === 0) {
        return h('span', { class: 'text-gray-400' }, 'No roles')
      }
      return h(
        'div',
        { class: 'flex flex-wrap gap-1' },
        roles.map((role) =>
          h(UBadge, { variant: 'soft', color: 'primary', size: 'sm' }, () => role),
        ),
      )
    },
  },
  {
    accessorKey: 'id',
    header: 'ID',
  },
  {
    accessorKey: 'actions',
    header: 'Actions',
    cell: ({ row }) => {
      const user = row.original
      return h('div', { class: 'flex space-x-2' }, [
        h(UButton, {
          icon: 'i-heroicons-eye',
          size: 'sm',
          variant: 'ghost',
          title: 'View user details',
          onClick: () => viewUserDetail(user),
        }),
        h(UButton, {
          icon: 'i-heroicons-pencil',
          size: 'sm',
          variant: 'ghost',
          title: 'Edit user',
          onClick: () => openEditModal(user),
        }),
        h(UButton, {
          icon: 'i-heroicons-trash',
          size: 'sm',
          variant: 'ghost',
          color: 'error',
          title: 'Delete user',
          onClick: () => openDeleteModal(user),
        }),
      ])
    },
  },
]

const columnPinning = ref({
  left: ['select', 'actions'],
})

// Client-side filtering for simple mode
const filteredUsers = computed(() => {
  let filtered = [...users.value]

  // Apply search filter
  if (searchValue.value) {
    const search = searchValue.value.toLowerCase()
    filtered = filtered.filter(
      (user) =>
        user.username.toLowerCase().includes(search) ||
        user.email.toLowerCase().includes(search) ||
        user.firstName.toLowerCase().includes(search) ||
        user.lastName.toLowerCase().includes(search) ||
        (user.phone && user.phone.toLowerCase().includes(search)),
    )
  }

  // Apply status filter
  if (statusFilter.value) {
    filtered = filtered.filter((user) => user.status === statusFilter.value)
  }

  return filtered
})

/**
 * Open create modal
 */
const openCreateModal = async () => {
  const modal = overlay.create(LazyUserFormModal)
  const instance = modal.open({ mode: 'create' })
  const formData = await instance.result

  if (formData) {
    await create(formData)
  }
}

/**
 * Open edit modal
 */
const openEditModal = async (user: UserDto) => {
  const modal = overlay.create(LazyUserFormModal)
  const instance = modal.open({ mode: 'edit', user })
  const formData = await instance.result

  if (formData) {
    await update(user.id, formData)
  }
}

/**
 * Open delete modal
 */
const openDeleteModal = async (user: UserDto) => {
  const modal = overlay.create(LazyUserDeleteModal)
  const instance = modal.open({ userName: user.fullName })
  const confirmed = await instance.result

  if (confirmed) {
    await remove(user.id)
  }
}

/**
 * Handle bulk delete
 */
const handleBulkDelete = async () => {
  if (selected.value.length === 0) return

  const modal = overlay.create(LazyUserDeleteModal)
  const instance = modal.open({ count: selected.value.length })
  const confirmed = await instance.result

  if (confirmed) {
    const ids = selected.value.map((user) => user.id)
    await bulkDelete(ids)
    selected.value = []
  }
}

/**
 * Handle bulk export
 */
const handleBulkExport = () => {
  if (selected.value.length === 0) return
  exportUsers(selected.value)
}

/**
 * Navigate to user detail
 */
const viewUserDetail = (user: UserDto) => {
  router.push(`/users/${user.id}`)
}

/**
 * Get status color
 */
const getStatusColor = (
  status: UserStatus,
): 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral' => {
  const colorMap: Record<
    UserStatus,
    'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
  > = {
    ACTIVE: 'success',
    INACTIVE: 'neutral',
    SUSPENDED: 'error',
    PENDING: 'warning',
  }
  return colorMap[status] || 'neutral'
}

/**
 * Get display status
 */
const getDisplayStatus = (status: UserStatus): string => {
  const statusMap: Record<UserStatus, string> = {
    ACTIVE: 'Active',
    INACTIVE: 'Inactive',
    SUSPENDED: 'Suspended',
    PENDING: 'Pending',
  }
  return statusMap[status] || status
}

/**
 * Get column label for sorting summary
 */
const getColumnLabel = (columnId: string): string => {
  const labelMap: Record<string, string> = {
    username: 'Username',
    fullName: 'Full Name',
    email: 'Email',
    phone: 'Phone',
    status: 'Status',
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

// Load users on mount
onMounted(async () => {
  await loadUsers()
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
  return table.value.tableApi.getFilteredSelectedRowModel()?.rows?.length || 0
})

const totalCount = computed((): number => {
  if (!table.value?.tableApi?.getFilteredRowModel) return 0
  return table.value.tableApi.getFilteredRowModel()?.rows?.length || 0
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
</script>

<template>
  <div class="container mx-auto px-2 sm:px-4 py-4 sm:py-6">
    <PageHeader title="Users" description="Manage system users">
      <template #actions>
        <UButton icon="i-heroicons-plus" size="sm" class="md:size-md" @click="openCreateModal">
          <span class="hidden sm:inline">Add User</span>
          <span class="sm:hidden">Add</span>
        </UButton>
      </template>
    </PageHeader>

    <!-- Table Filters (includes Search + Bulk Actions + Filters + Sort) -->
    <TableFilters
      :search-value="searchValue"
      search-placeholder="Search users..."
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

    <!-- Desktop Table View -->
    <div class="hidden md:block">
      <UCard>
        <UTable
          ref="table"
          :column-pinning="columnPinning"
          :sorting="sorting"
          :data="filteredUsers"
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
        <template v-if="!loading && filteredUsers.length === 0">
          <div class="text-center py-12">
            <div class="mx-auto h-12 w-12 text-gray-400">
              <UIcon name="i-heroicons-users" class="h-12 w-12" />
            </div>
            <h3 class="mt-2 text-sm font-medium text-gray-900 dark:text-gray-100">
              No users found
            </h3>
            <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
              {{
                searchValue || statusFilter
                  ? 'Try adjusting your search or filter criteria.'
                  : 'Get started by creating a new user.'
              }}
            </p>
            <div class="mt-6">
              <UButton
                v-if="!searchValue && !statusFilter"
                icon="i-heroicons-plus"
                @click="openCreateModal"
              >
                Add User
              </UButton>
              <UButton v-else variant="soft" @click="clearFilters"> Clear Filters </UButton>
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
      <template v-else-if="filteredUsers.length === 0">
        <UCard>
          <div class="text-center py-12">
            <div class="mx-auto h-12 w-12 text-gray-400">
              <UIcon name="i-heroicons-users" class="h-12 w-12" />
            </div>
            <h3 class="mt-2 text-sm font-medium text-gray-900 dark:text-gray-100">
              No users found
            </h3>
            <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
              {{
                searchValue || statusFilter
                  ? 'Try adjusting your search or filter criteria.'
                  : 'Get started by creating a new user.'
              }}
            </p>
            <div class="mt-6">
              <UButton
                v-if="!searchValue && !statusFilter"
                icon="i-heroicons-plus"
                @click="openCreateModal"
              >
                Add User
              </UButton>
              <UButton v-else variant="soft" @click="clearFilters"> Clear Filters </UButton>
            </div>
          </div>
        </UCard>
      </template>
      <template v-else>
        <UCard v-for="user in filteredUsers" :key="user.id" class="overflow-hidden">
          <div class="space-y-3">
            <!-- Header: Username and Status -->
            <div class="flex items-center justify-between">
              <div class="flex-1 min-w-0">
                <h3 class="text-base font-semibold text-gray-900 dark:text-gray-100 truncate">
                  {{ user.fullName || user.username }}
                </h3>
                <p class="text-sm text-gray-500 dark:text-gray-400 truncate">
                  @{{ user.username }}
                </p>
              </div>
              <UBadge
                :color="
                  user.status === 'ACTIVE'
                    ? 'success'
                    : user.status === 'INACTIVE'
                      ? 'neutral'
                      : 'error'
                "
                variant="soft"
                size="sm"
              >
                {{ user.status }}
              </UBadge>
            </div>

            <!-- Info Grid -->
            <div class="grid grid-cols-1 gap-2 text-sm">
              <div>
                <span class="text-gray-500 dark:text-gray-400">Email:</span>
                <p class="font-medium text-gray-900 dark:text-gray-100 truncate">
                  {{ user.email || 'N/A' }}
                </p>
              </div>
              <div v-if="user.phone">
                <span class="text-gray-500 dark:text-gray-400">Phone:</span>
                <p class="font-medium text-gray-900 dark:text-gray-100">{{ user.phone }}</p>
              </div>
              <div v-if="user.roles && user.roles.length > 0">
                <span class="text-gray-500 dark:text-gray-400">Roles:</span>
                <div class="flex flex-wrap gap-1 mt-1">
                  <UBadge
                    v-for="role in user.roles"
                    :key="role"
                    color="primary"
                    variant="soft"
                    size="xs"
                  >
                    {{ role }}
                  </UBadge>
                </div>
              </div>
            </div>

            <!-- Actions -->
            <div
              class="flex items-center justify-end gap-2 pt-2 border-t border-gray-200 dark:border-gray-700"
            >
              <UButton
                icon="i-heroicons-eye"
                size="sm"
                variant="ghost"
                @click="viewUserDetail(user)"
              >
                View
              </UButton>
              <UButton
                icon="i-heroicons-pencil"
                size="sm"
                variant="ghost"
                @click="openEditModal(user)"
              >
                Edit
              </UButton>
              <UButton
                icon="i-heroicons-trash"
                size="sm"
                variant="ghost"
                color="error"
                @click="openDeleteModal(user)"
              >
                Delete
              </UButton>
            </div>
          </div>
        </UCard>
      </template>
    </div>

    <!-- Pagination -->
    <div
      v-if="!loading && filteredUsers.length > 0"
      class="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4"
    >
      <div class="text-sm text-gray-700 dark:text-gray-300 text-center sm:text-left">
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
  </div>
</template>
