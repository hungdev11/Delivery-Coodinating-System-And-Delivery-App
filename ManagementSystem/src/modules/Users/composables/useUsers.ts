/**
 * useUsers Composable
 *
 * Business logic for user management
 */

import { ref } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { getUsers, createUser, updateUser, deleteUser } from '../api'
import { UserDto, CreateUserRequest, UpdateUserRequest } from '../model.type'

export function useUsers() {
  const toast = useToast()

  const users = ref<UserDto[]>([])
  const loading = ref(false)
  const page = ref(0)
  const pageSize = ref(10)
  const total = ref(0)
  const searchQuery = ref('')

  /**
   * Load users
   */
  const loadUsers = async () => {
    loading.value = true
    try {
      const response = await getUsers({
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

  return {
    users,
    loading,
    page,
    pageSize,
    total,
    searchQuery,
    loadUsers,
    create,
    update,
    remove,
    bulkDelete,
    handlePageChange,
    handleSearch,
  }
}
