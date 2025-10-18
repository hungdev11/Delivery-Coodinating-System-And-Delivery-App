<script setup lang="ts">
/**
 * Users List View
 *
 * Main view for managing users with proper Vue 3 + Nuxt UI v3 patterns
 */

import { onMounted, defineAsyncComponent, ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import { useUsers, useUserExport } from './composables'
import type { UserDto } from './model.type'
// Dynamic imports to avoid TypeScript issues
const PageHeader = defineAsyncComponent(() => import('../../common/components/PageHeader.vue'))
const DataTable = defineAsyncComponent(() => import('../../common/components/DataTable.vue'))

// Lazy load modals
const LazyUserFormModal = defineAsyncComponent(() => import('./components/UserFormModal.vue'))
const LazyUserDeleteModal = defineAsyncComponent(() => import('./components/UserDeleteModal.vue'))

const router = useRouter()
const overlay = useOverlay()

// Composables
const {
  users,
  loading,
  page,
  pageSize,
  total,
  loadUsers,
  create,
  update,
  remove,
  bulkDelete,
  handlePageChange,
  handleSearch,
} = useUsers()

const { exportUsers } = useUserExport()

// Table columns with sorting and filtering capabilities
const columns = [
  {
    accessorKey: 'username',
    header: 'Username',
    sortable: true,
    filterable: true,
    filterType: 'text'
  },
  {
    accessorKey: 'fullName',
    header: 'Full Name',
    sortable: true,
    filterable: true,
    filterType: 'text'
  },
  {
    accessorKey: 'email',
    header: 'Email',
    sortable: true,
    filterable: true,
    filterType: 'text'
  },
  {
    accessorKey: 'phone',
    header: 'Phone',
    sortable: true,
    filterable: true,
    filterType: 'text'
  },
  {
    accessorKey: 'status',
    header: 'Status',
    sortable: true,
    filterable: true,
    filterType: 'select',
    filterOptions: [
      { label: 'Active', value: 'ACTIVE' },
      { label: 'Inactive', value: 'INACTIVE' },
      { label: 'Suspended', value: 'SUSPENDED' },
      { label: 'Pending', value: 'PENDING' }
    ]
  },
  {
    accessorKey: 'actions',
    header: 'Actions',
    sortable: false,
    filterable: false
  },
]

// Table rows - make it computed to react to users changes
const tableRows = computed(() => {
  return users.value.map((user) => ({
    ...user,
    fullName: `${user.firstName || ''} ${user.lastName || ''}`.trim() || 'N/A',
  }))
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
  console.log('UsersView: viewUserDetail', user)
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
const handleBulkDelete = async (ids: string[]) => {
  const modal = overlay.create(LazyUserDeleteModal)
  const instance = modal.open({ count: ids.length })
  const confirmed = await instance.result

  if (confirmed) {
    await bulkDelete(ids)
  }
}

/**
 * Handle bulk export
 */
const handleBulkExport = (ids: string[]) => {
  const selectedUsers = users.value.filter((u) => ids.includes(u.id))
  exportUsers(selectedUsers)
}

/**
 * Navigate to user detail
 */
const viewUserDetail = (user: UserDto) => {
  console.log('UsersView: viewUserDetail', user)
  router.push(`/users/${user.id}`)
}

/**
 * Get status color
 */
const getStatusColor = (status: string): 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral' => {
  const colorMap: Record<string, 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'> = {
    ACTIVE: 'success',
    INACTIVE: 'neutral',
    SUSPENDED: 'error',
    PENDING: 'warning',
  }
  return colorMap[status] || 'neutral'
}

/**
 * Handle sorting
 */
const handleSort = (sorts: { key: string; direction: 'asc' | 'desc' }[]) => {
  console.log('UsersView: Sort changed:', sorts)
  // Here you would typically call the API with sort parameters
  // For now, we'll just log the sort changes
}

/**
 * Handle filtering
 */
const handleFilter = (filters: { key: string; value: any; operator: string }[]) => {
  console.log('UsersView: Filter changed:', filters)
  // Here you would typically call the API with filter parameters
  // For now, we'll just log the filter changes
}

/**
 * Helper functions to handle row typing
 */
const getRowStatus = (row: Record<string, unknown>): string => (row?.status as string) || ''
const getRowUser = (row: Record<string, unknown>): UserDto => row as unknown as UserDto

// Load users on mount
onMounted(async () => {
  console.log('UsersView: Component mounted, loading users...')
  await loadUsers()
  console.log('UsersView: Users loaded:', users.value.length)
})

// Watch for users changes to ensure tableRows is updated
watch(users, (newUsers) => {
  console.log('UsersView: Users changed, updating table rows:', newUsers.length)
}, { immediate: true })
</script>

<template>
  <div class="container mx-auto px-4 py-6">
    <PageHeader title="Users" description="Manage system users">
      <template #actions>
        <UButton icon="i-heroicons-plus" @click="openCreateModal"> Add User </UButton>
      </template>
    </PageHeader>

    <DataTable
      :columns="columns"
      :rows="tableRows"
      :loading="loading"
      :page="page"
      :page-size="pageSize"
      :total="total"
      searchable
      selectable
      sortable
      filterable
      search-placeholder="Search users..."
      empty-message="No users found"
      store-key="users-table"
      @update:page="handlePageChange"
      @search="handleSearch"
      @sort="handleSort"
      @filter="handleFilter"
      @bulk-delete="handleBulkDelete"
    >
      <!-- Custom bulk actions -->
      <template #bulk-actions="{ selected, count }">
        <UButton
          size="sm"
          variant="soft"
          icon="i-heroicons-arrow-down-tray"
          @click="handleBulkExport(selected)"
        >
          Export {{ count }}
        </UButton>
      </template>

      <!-- Status Column -->
      <template #status-cell="{ row: { original } }">
        <UBadge :color="getStatusColor(getRowStatus(original))" variant="soft">
          {{ getRowStatus(original) }}
        </UBadge>
      </template>

      <!-- Actions Column -->
      <template #actions-cell="{ row: { original } }">
        <div class="flex space-x-2">
          <UButton
            icon="i-heroicons-eye"
            size="sm"
            variant="ghost"
            @click="viewUserDetail(getRowUser(original))"
          />
          <UButton icon="i-heroicons-pencil" size="sm" variant="ghost" @click="openEditModal(getRowUser(original))" />
          <UButton
            icon="i-heroicons-trash"
            size="sm"
            variant="ghost"
            color="error"
            @click="openDeleteModal(getRowUser(original))"
          />
        </div>
      </template>
    </DataTable>
  </div>
</template>
