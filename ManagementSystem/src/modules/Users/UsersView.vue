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
  resolveComponent,
  h,
  reactive,
} from 'vue'
import { useRouter } from 'vue-router'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import { useUsers, useUserExport } from './composables'
import type { UserDto, UserStatus } from './model.type'
import { useTemplateRef } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import TableHeaderCell from '@/common/components/TableHeaderCell.vue'
import FilterBar from '@/common/components/filters/FilterBar.vue'
import type { Column, SortingState } from '@tanstack/table-core'
import type { FilterableColumn, FilterCondition } from '@/common/types/filter'
import { createSortConfig } from '@/common/utils/query-builder'

const sorting2 = reactive<SortingState>([])

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
const table = useTemplateRef('table')

const setupHeader = ({
  column,
  config,
  filterableColumn,
  activeFilters,
}: {
  column: Column<UserDto>
  config: {
    variant: 'ghost' | 'solid' | 'outline' | 'soft' | 'link'
    label: string
    class: string
    activeColor?: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
    inactiveColor?: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
  }
  filterableColumn?: FilterableColumn
  activeFilters?: FilterCondition[]
}) =>
  h(TableHeaderCell<UserDto>, {
    column,
    config,
    filterableColumn,
    activeFilters,
    onUpdateFilters: (newFilters: FilterCondition[]) => {
      // Handle filter updates for this column
      const field = filterableColumn?.field
      if (field) {
        const otherFilters = filters.value.conditions.filter(
          (f) => 'field' in f && f.field !== field,
        ) as FilterCondition[]
        const allFilters = [...otherFilters, ...newFilters]
        updateFilters({
          logic: 'AND',
          conditions: allFilters,
        })
      }
    },
  })

// Composables
const {
  users,
  loading,
  page,
  pageSize,
  total,
  filters,
  sorts,
  useAdvancedSearch,
  loadUsers,
  create,
  update,
  remove,
  bulkDelete,
  handleSearch,
  updateFilters,
  updateSorts,
  clearFilters,
  toggleAdvancedSearch,
  getFilterableColumns,
} = useUsers()

const { exportUsers } = useUserExport()

// Table state
const selected = ref<UserDto[]>([])
const sorting = ref<Array<{ id: string; desc: boolean }>>([])

// Search and filter state
const searchValue = ref('')
const statusFilter = ref<UserStatus | ''>('')

// Advanced filter state
const showAdvancedFilters = ref(false)
const filterableColumns = computed(() => getFilterableColumns())

// Helper function to get active filters for a specific field
const getActiveFiltersForField = (field: string): FilterCondition[] | undefined => {
  const fieldFilters = filters.value.conditions.filter(
    (c) => 'field' in c && c.field === field,
  ) as FilterCondition[]
  return fieldFilters.length > 0 ? fieldFilters : undefined
}

// Helper function to get all active filters
const getAllActiveFilters = (): FilterCondition[] | undefined => {
  const allFilters = filters.value.conditions.filter((c) => 'field' in c) as FilterCondition[]
  return allFilters.length > 0 ? allFilters : undefined
}

// Helper function to get filterable column for a specific field
const getFilterableColumnForField = (field: string): FilterableColumn | undefined => {
  return filterableColumns.value.find((col) => col.field === field && col.filterable !== false)
}

// Table columns configuration
const columns: TableColumn<UserDto>[] = [
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
        },
        filterableColumn: getFilterableColumnForField('username'),
        activeFilters: getActiveFiltersForField('username'),
      }),
  },
  {
    accessorKey: 'fullName',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Fullname',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
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
        },
        filterableColumn: getFilterableColumnForField('email'),
        activeFilters: getActiveFiltersForField('email'),
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
        },
        filterableColumn: getFilterableColumnForField('phone'),
        activeFilters: getActiveFiltersForField('phone'),
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
        },
        filterableColumn: getFilterableColumnForField('status'),
        activeFilters: getActiveFiltersForField('status'),
      }),
    cell: ({ row }) => {
      const status = row.getValue('status') as UserStatus
      const color = getStatusColor(status)
      const displayStatus = getDisplayStatus(status)
      return h(UBadge, { class: 'capitalize', variant: 'soft', color }, () => displayStatus)
    },
  },
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
        },
        filterableColumn: getFilterableColumnForField('id'),
        activeFilters: getActiveFiltersForField('id'),
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
]

// For simple mode, we still need client-side filtering
const filteredUsers = computed(() => {
  if (useAdvancedSearch.value) {
    // Server-side filtering - return users as-is
    return users.value
  }

  // Client-side filtering for simple mode
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

// Status options for filter
const statusOptions = [
  { label: 'All Status', value: '' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Inactive', value: 'INACTIVE' },
  { label: 'Suspended', value: 'SUSPENDED' },
  { label: 'Pending', value: 'PENDING' },
]

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

// Watch for sorts changes and sync with sorting (only when not in advanced mode)
watch(
  sorts,
  (newSorts) => {
    if (!useAdvancedSearch.value) {
      const newSorting = newSorts.map((sort) => ({
        id: sort.field,
        desc: sort.direction === 'desc',
      }))
      sorting.value = newSorting
    }
  },
  { deep: true },
)
</script>

<template>
  <div class="container mx-auto px-4 py-6">
    <PageHeader title="Users" description="Manage system users">
      <template #actions>
        <UButton icon="i-heroicons-plus" @click="openCreateModal"> Add User </UButton>
      </template>
    </PageHeader>

    <!-- Filters and Search -->
    <div class="mb-6 space-y-4">
      <!-- Advanced Filter Bar -->
      <FilterBar
        v-if="useAdvancedSearch"
        :columns="filterableColumns"
        :active-filters="getAllActiveFilters()"
        :show-advanced="showAdvancedFilters"
        @update:filters="updateFilters"
      />

      <!-- Simple Search (when not using advanced mode) -->
      <div v-else class="flex flex-col sm:flex-row gap-4">
        <!-- Search Input -->
        <div class="flex-1">
          <UInput
            v-model="searchValue"
            placeholder="Search users..."
            icon="i-heroicons-magnifying-glass"
            size="lg"
          />
        </div>

        <!-- Status Filter -->
        <div class="w-full sm:w-48">
          <USelect
            v-model="statusFilter"
            :options="statusOptions"
            placeholder="Filter by status"
            size="lg"
          />
        </div>

        <!-- Clear Filters -->
        <UButton
          variant="soft"
          color="neutral"
          icon="i-heroicons-x-mark"
          @click="clearFilters"
          :disabled="!searchValue && !statusFilter"
        >
          Clear
        </UButton>
      </div>

      <!-- Toggle Advanced Mode -->
      <div class="flex justify-end">
        <UButton
          :variant="useAdvancedSearch ? 'solid' : 'soft'"
          :color="useAdvancedSearch ? 'primary' : 'neutral'"
          :icon="useAdvancedSearch ? 'i-heroicons-funnel' : 'i-heroicons-funnel-simple'"
          @click="toggleAdvancedSearch"
        >
          {{ useAdvancedSearch ? 'Advanced Mode' : 'Simple Mode' }}
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
      v-if="table && table?.tableApi?.getFilteredSelectedRowModel()?.rows?.length > 0"
      class="mb-4 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg"
    >
      <div class="flex items-center justify-between">
        <span class="text-sm text-gray-600 dark:text-gray-400">
          {{ table?.tableApi?.getFilteredSelectedRowModel()?.rows?.length }} of
          {{ table?.tableApi?.getFilteredRowModel()?.rows?.length }} row(s) selected.
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
        :data="useAdvancedSearch ? users : filteredUsers"
        :columns="columns"
        :loading="loading"
        :manual-sorting="true"
        @update:sorting="onSortingChange($event)"
      />

      <!-- Empty State -->
      <template v-if="!loading && (useAdvancedSearch ? users : filteredUsers).length === 0">
        <div class="text-center py-12">
          <div class="mx-auto h-12 w-12 text-gray-400">
            <UIcon name="i-heroicons-users" class="h-12 w-12" />
          </div>
          <h3 class="mt-2 text-sm font-medium text-gray-900 dark:text-gray-100">No users found</h3>
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

    <!-- Pagination -->
    <div
      v-if="!loading && (useAdvancedSearch ? users : filteredUsers).length > 0"
      class="mt-6 flex items-center justify-between"
    >
      <div class="text-sm text-gray-700 dark:text-gray-300">
        Showing {{ page * pageSize + 1 }} to {{ Math.min((page + 1) * pageSize, total) }} of
        {{ total }} results
      </div>
      <UPagination v-model="page" :items-per-page="pageSize" :total="total" />
    </div>
  </div>
</template>
