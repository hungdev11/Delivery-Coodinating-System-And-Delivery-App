<script setup lang="ts">
/**
 * User Detail View
 *
 * View for displaying user details
 */

import { ref, onMounted, computed, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import { PageHeader } from '@/common/components'
import { getUserById, deleteUser } from './api'
import { UserDto } from './model.type'

// Lazy load modals
const LazyUserFormModal = defineAsyncComponent(() => import('./components/UserFormModal.vue'))
const LazyUserDeleteModal = defineAsyncComponent(() => import('./components/UserDeleteModal.vue'))

const route = useRoute()
const router = useRouter()
const toast = useToast()
const overlay = useOverlay()

const userId = computed(() => route.params.id as string)
const user = ref<UserDto | null>(null)
const loading = ref(false)

/**
 * Load user details
 */
const loadUser = async () => {
  loading.value = true
  try {
    const response = await getUserById(userId.value)
    if (response.result) {
      user.value = new UserDto(response.result)
    }
  } catch (error) {
    console.error('Failed to load user:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to load user details',
      color: 'error',
    })
  } finally {
    loading.value = false
  }
}

/**
 * Open edit modal
 */
const openEditModal = async () => {
  if (!user.value) return

  const modal = overlay.create(LazyUserFormModal)
  const instance = modal.open({ mode: 'edit', user: user.value })
  const formData = await instance.result

  if (formData) {
    // Refresh user data after edit
    loadUser()
  }
}

/**
 * Open delete modal
 */
const openDeleteModal = async () => {
  if (!user.value) return

  const modal = overlay.create(LazyUserDeleteModal)
  const instance = modal.open({ userName: user.value.fullName })
  const confirmed = await instance.result

  if (confirmed) {
    try {
      await deleteUser(user.value.id)
      toast.add({
        title: 'Success',
        description: 'User deleted successfully',
        color: 'success',
      })
      router.push('/users')
    } catch (error) {
      console.error('Failed to delete user:', error)
    }
  }
}

/**
 * Get status color
 */
const getStatusColor = (status: string) => {
  const colorMap: Record<string, string> = {
    ACTIVE: 'green',
    INACTIVE: 'gray',
    SUSPENDED: 'red',
    PENDING: 'yellow',
  }
  return colorMap[status] || 'gray'
}

/**
 * Format date
 */
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

onMounted(() => {
  loadUser()
})
</script>

<template>
  <div class="container mx-auto px-4 py-6">
    <PageHeader
      :title="user?.fullName || 'User Details'"
      description="View and manage user information"
      show-back
    >
      <template #actions>
        <UButton variant="outline" icon="i-heroicons-pencil" @click="openEditModal"> Edit </UButton>
        <UButton color="error" variant="soft" icon="i-heroicons-trash" @click="openDeleteModal">
          Delete
        </UButton>
      </template>
    </PageHeader>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <UIcon name="i-heroicons-arrow-path" class="w-8 h-8 animate-spin text-primary-500" />
    </div>

    <div v-else-if="user" class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Main Info Card -->
      <UCard class="lg:col-span-2">
        <template #header>
          <h3 class="text-lg font-semibold">User Information</h3>
        </template>

        <div class="space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="text-sm font-medium text-gray-500">Username</label>
              <p class="mt-1 text-base">{{ user.username }}</p>
            </div>

            <div>
              <label class="text-sm font-medium text-gray-500">Email</label>
              <p class="mt-1 text-base">{{ user.email }}</p>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="text-sm font-medium text-gray-500">First Name</label>
              <p class="mt-1 text-base">{{ user.firstName }}</p>
            </div>

            <div>
              <label class="text-sm font-medium text-gray-500">Last Name</label>
              <p class="mt-1 text-base">{{ user.lastName }}</p>
            </div>
          </div>

          <div v-if="user.phone">
            <label class="text-sm font-medium text-gray-500">Phone</label>
            <p class="mt-1 text-base">{{ user.phone }}</p>
          </div>

          <div v-if="user.address">
            <label class="text-sm font-medium text-gray-500">Address</label>
            <p class="mt-1 text-base">{{ user.address }}</p>
          </div>

          <div v-if="user.identityNumber">
            <label class="text-sm font-medium text-gray-500">Identity Number</label>
            <p class="mt-1 text-base">{{ user.identityNumber }}</p>
          </div>

          <div>
            <label class="text-sm font-medium text-gray-500">Status</label>
            <div class="mt-1">
              <UBadge :color="getStatusColor(user.status)" variant="soft">
                {{ user.displayStatus }}
              </UBadge>
            </div>
          </div>
        </div>
      </UCard>

      <!-- Meta Info Card -->
      <UCard>
        <template #header>
          <h3 class="text-lg font-semibold">Meta Information</h3>
        </template>

        <div class="space-y-4">
          <div>
            <label class="text-sm font-medium text-gray-500">User ID</label>
            <p class="mt-1 text-sm font-mono break-all">{{ user.id }}</p>
          </div>

          <div v-if="user.keycloakId">
            <label class="text-sm font-medium text-gray-500">Keycloak ID</label>
            <p class="mt-1 text-sm font-mono break-all">{{ user.keycloakId }}</p>
          </div>

          <div>
            <label class="text-sm font-medium text-gray-500">Created At</label>
            <p class="mt-1 text-sm">{{ formatDate(user.createdAt) }}</p>
          </div>

          <div>
            <label class="text-sm font-medium text-gray-500">Updated At</label>
            <p class="mt-1 text-sm">{{ formatDate(user.updatedAt) }}</p>
          </div>
        </div>
      </UCard>
    </div>

    <div v-else class="text-center py-12">
      <p class="text-gray-500">User not found</p>
    </div>
  </div>
</template>
