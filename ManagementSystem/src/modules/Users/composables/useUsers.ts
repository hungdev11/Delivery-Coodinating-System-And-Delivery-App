/**
 * useUsers Composable
 *
 * Business logic for user management
 */

import { ref } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { getUsers, createUser, updateUser, deleteUser } from '../api'
import { UserDto, CreateUserRequest, UpdateUserRequest } from '../model.type'
import type { FilterGroup, SortConfig, FilterableColumn } from '@/common/types/filter'
import { createEmptyFilterGroup } from '@/common/utils/query-builder'

export function useUsers() {
  const toast = useToast()

  const users = ref<UserDto[]>([])
  const loading = ref(false)
  const page = ref(0)
  const pageSize = ref(10)
  const total = ref(0)
  const searchQuery = ref('')

  // New filter/sort state
  const filters = ref<FilterGroup>(createEmptyFilterGroup())
  const sorts = ref<SortConfig[]>([])
  const useAdvancedSearch = ref(true)

  /**
   * Load users
   */
  const loadUsers = async () => {
    loading.value = true
    try {
      const response = await getUsers({
        filters: filters.value.conditions.length > 0 ? filters.value : undefined,
        sorts: sorts.value.length > 0 ? sorts.value : undefined,
        page: page.value,
        size: pageSize.value,
        search: searchQuery.value || undefined,
      })

      if (response.result) {
        users.value = response.result.data.map((u) => new UserDto(u))
        total.value = response.result.page.totalElements
      }
    } catch (error) {
      console.error('Failed to load users:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to load users',
        color: 'error',
      })
    } finally {
      loading.value = false
    }
  }

  /**
   * Create user
   */
  const create = async (data: CreateUserRequest) => {
    try {
      const request = new CreateUserRequest(data)
      await createUser(request)

      toast.add({
        title: 'Success',
        description: 'User created successfully',
        color: 'success',
      })

      loadUsers()
      return true
    } catch (error) {
      console.error('Failed to create user:', error)
      return false
    }
  }

  /**
   * Update user
   */
  const update = async (id: string, data: UpdateUserRequest) => {
    try {
      const request = new UpdateUserRequest(data)
      await updateUser(id, request)

      toast.add({
        title: 'Success',
        description: 'User updated successfully',
        color: 'success',
      })

      loadUsers()
      return true
    } catch (error) {
      console.error('Failed to update user:', error)
      return false
    }
  }

  /**
   * Delete user
   */
  const remove = async (id: string) => {
    try {
      await deleteUser(id)

      toast.add({
        title: 'Success',
        description: 'User deleted successfully',
        color: 'success',
      })

      loadUsers()
      return true
    } catch (error) {
      console.error('Failed to delete user:', error)
      return false
    }
  }

  /**
   * Bulk delete users
   */
  const bulkDelete = async (ids: string[]) => {
    try {
      await Promise.all(ids.map((id) => deleteUser(id)))

      toast.add({
        title: 'Success',
        description: `${ids.length} user(s) deleted successfully`,
        color: 'success',
      })

      loadUsers()
      return true
    } catch (error) {
      console.error('Failed to bulk delete users:', error)
      return false
    }
  }

  /**
   * Handle page change
   */
  const handlePageChange = (newPage: number) => {
    page.value = newPage
    loadUsers()
  }

  /**
   * Handle search
   */
  const handleSearch = (query: string) => {
    searchQuery.value = query
    page.value = 0
    loadUsers()
  }

  /**
   * Update filters
   */
  const updateFilters = (newFilters: FilterGroup) => {
    filters.value = newFilters
    page.value = 0
    loadUsers()
  }

  /**
   * Update sorts
   */
  const updateSorts = (newSorts: SortConfig[]) => {
    sorts.value = newSorts
    console.log('newSorts', newSorts)
    if (useAdvancedSearch.value) {
      loadUsers() // Reload data when using advanced search
    }
  }

  /**
   * Clear all filters
   */
  const clearFilters = () => {
    filters.value = createEmptyFilterGroup()
    page.value = 0
    loadUsers()
  }

  /**
   * Clear all sorts
   */
  const clearSorts = () => {
    sorts.value = []
    loadUsers()
  }

  /**
   * Toggle advanced search mode
   */
  const toggleAdvancedSearch = () => {
    useAdvancedSearch.value = !useAdvancedSearch.value
    if (!useAdvancedSearch.value) {
      // Clear filters when switching to simple mode
      clearFilters()
      clearSorts()
    }
    loadUsers()
  }

  /**
   * Get filterable columns configuration
   */
  const getFilterableColumns = (): FilterableColumn[] => {
    return [
      {
        field: 'username',
        label: 'Username',
        type: 'string',
        caseSensitive: false,
        filterable: true,
        filterType: 'text',
        filterConfig: {
          placeholder: 'Enter username...',
        },
      },
      {
        field: 'email',
        label: 'Email',
        type: 'string',
        caseSensitive: false,
        filterable: true,
        filterType: 'text',
        filterConfig: {
          placeholder: 'Enter email address...',
        },
      },
      {
        field: 'firstName',
        label: 'First Name',
        type: 'string',
        caseSensitive: false,
        filterable: true,
        filterType: 'text',
        filterConfig: {
          placeholder: 'Enter first name...',
        },
      },
      {
        field: 'lastName',
        label: 'Last Name',
        type: 'string',
        caseSensitive: false,
        filterable: true,
        filterType: 'text',
        filterConfig: {
          placeholder: 'Enter last name...',
        },
      },
      {
        field: 'phone',
        label: 'Phone',
        type: 'string',
        caseSensitive: false,
        filterable: true,
        filterType: 'text',
        filterConfig: {
          placeholder: 'Enter phone number...',
        },
      },
      {
        field: 'status',
        label: 'Status',
        type: 'enum',
        enumOptions: [
          { label: 'Active', value: 'ACTIVE' },
          { label: 'Inactive', value: 'INACTIVE' },
          { label: 'Suspended', value: 'SUSPENDED' },
          { label: 'Pending', value: 'PENDING' },
        ],
        filterable: true,
        filterType: 'select',
        filterConfig: {
          placeholder: 'Select status...',
          multiple: false,
        },
      },
      {
        field: 'createdAt',
        label: 'Created At',
        type: 'date',
        filterable: true,
        filterType: 'date',
        filterConfig: {
          placeholder: 'Select date...',
        },
      },
      {
        field: 'updatedAt',
        label: 'Updated At',
        type: 'date',
        filterable: true,
        filterType: 'date',
        filterConfig: {
          placeholder: 'Select date...',
        },
      },
      {
        field: 'age',
        label: 'Age',
        type: 'number',
        filterable: true,
        filterType: 'number',
        filterConfig: {
          placeholder: 'Enter age...',
          min: 0,
          max: 120,
          step: 1,
        },
      },
      {
        field: 'salary',
        label: 'Salary',
        type: 'number',
        filterable: true,
        filterType: 'range',
        filterConfig: {
          placeholder: 'Enter salary range...',
          min: 0,
          max: 1000000,
          step: 1000,
        },
      },
      {
        field: 'id',
        label: 'ID',
        type: 'string',
        filterable: false, // ID không nên filter được
      },
    ]
  }

  return {
    users,
    loading,
    page,
    pageSize,
    total,
    searchQuery,
    filters,
    sorts,
    useAdvancedSearch,
    loadUsers,
    create,
    update,
    remove,
    bulkDelete,
    handlePageChange,
    handleSearch,
    updateFilters,
    updateSorts,
    clearFilters,
    clearSorts,
    toggleAdvancedSearch,
    getFilterableColumns,
  }
}
