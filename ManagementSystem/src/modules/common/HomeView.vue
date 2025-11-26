<script setup lang="ts">
import { onMounted, computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getUserRoles, getCurrentUser } from '@/common/guards/roleGuard.guard'
import { getMyAddresses } from '@/modules/UserAddresses/api'
import { getParcelsV2 } from '@/modules/Parcels/api'
import { defineAsyncComponent } from 'vue'
import type { FilterGroup } from '@/common/types/filter'

const PageHeader = defineAsyncComponent(() => import('@/common/components/PageHeader.vue'))

const router = useRouter()
const currentUser = getCurrentUser()
const userRoles = computed(() => getUserRoles())
const isAdmin = computed(() => userRoles.value.includes('ADMIN'))
const isClient = computed(() => userRoles.value.includes('CLIENT'))

// Client dashboard data
const clientStats = ref({
  totalParcels: 0,
  pendingParcels: 0,
  deliveredParcels: 0,
  totalAddresses: 0,
})

const loading = ref(false)

const loadClientStats = async () => {
  if (!isClient.value || !currentUser?.id) return

  loading.value = true
  try {
    // Load addresses count
    const addressesResponse = await getMyAddresses()
    const addressesCount = addressesResponse.result?.length || 0

    // Load parcels count
    const filterGroup: FilterGroup = {
      logic: 'AND',
      conditions: [
        {
          field: 'receiverId',
          operator: 'eq',
          value: currentUser.id,
          logic: 'AND',
        },
      ],
    }

    const parcelsResponse = await getParcelsV2({
      filters: filterGroup,
      page: 0,
      size: 1,
    })

    const totalParcels = parcelsResponse.result?.page.totalElements || 0

    // Count by status
    const allParcelsResponse = await getParcelsV2({
      filters: filterGroup,
      page: 0,
      size: 1000, // Get all to count status
    })

    const parcels = allParcelsResponse.result?.data || []
    const pendingParcels = parcels.filter(
      (p) => p.status === 'IN_WAREHOUSE' || p.status === 'ON_ROUTE',
    ).length
    const deliveredParcels = parcels.filter(
      (p) => p.status === 'DELIVERED' || p.status === 'SUCCEEDED',
    ).length

    clientStats.value = {
      totalParcels,
      pendingParcels,
      deliveredParcels,
      totalAddresses: addressesCount,
    }
  } catch (error) {
    console.error('Failed to load client stats:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  document.title = 'Hệ thống quản lý đơn hàng'
  if (isClient.value) {
    loadClientStats()
  }
})
</script>

<template>
  <div class="container mx-auto px-4 py-6 space-y-6">
    <!-- Admin Dashboard -->
    <template v-if="isAdmin">
      <PageHeader
        title="Admin Dashboard"
        description="System overview and management"
      />

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-500">Total Users</span>
              <UIcon name="i-heroicons-user-group" class="w-5 h-5 text-gray-400" />
            </div>
          </template>
          <div class="text-3xl font-semibold text-gray-900 dark:text-gray-100">
            —
          </div>
          <div class="text-xs text-gray-500 mt-2">
            <UButton
              variant="ghost"
              size="xs"
              @click="router.push({ name: 'users' })"
            >
              View All →
            </UButton>
          </div>
        </UCard>

        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-500">Total Parcels</span>
              <UIcon name="i-heroicons-cube" class="w-5 h-5 text-gray-400" />
            </div>
          </template>
          <div class="text-3xl font-semibold text-gray-900 dark:text-gray-100">
            —
          </div>
          <div class="text-xs text-gray-500 mt-2">
            <UButton
              variant="ghost"
              size="xs"
              @click="router.push({ name: 'parcels' })"
            >
              View All →
            </UButton>
          </div>
        </UCard>

        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-500">Active Shippers</span>
              <UIcon name="i-heroicons-truck" class="w-5 h-5 text-gray-400" />
            </div>
          </template>
          <div class="text-3xl font-semibold text-gray-900 dark:text-gray-100">
            —
          </div>
          <div class="text-xs text-gray-500 mt-2">
            <UButton
              variant="ghost"
              size="xs"
              @click="router.push({ name: 'delivery-shippers' })"
            >
              View All →
            </UButton>
          </div>
        </UCard>

        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-500">Zones</span>
              <UIcon name="i-heroicons-map" class="w-5 h-5 text-gray-400" />
            </div>
          </template>
          <div class="text-3xl font-semibold text-gray-900 dark:text-gray-100">
            —
          </div>
          <div class="text-xs text-gray-500 mt-2">
            <UButton
              variant="ghost"
              size="xs"
              @click="router.push({ name: 'zones' })"
            >
              View All →
            </UButton>
          </div>
        </UCard>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">Quick Actions</h3>
          </template>
          <div class="space-y-2">
            <UButton
              color="primary"
              variant="soft"
              block
              icon="i-heroicons-cube"
              @click="router.push({ name: 'parcels' })"
            >
              Manage Parcels
            </UButton>
            <UButton
              color="primary"
              variant="soft"
              block
              icon="i-heroicons-user-group"
              @click="router.push({ name: 'users' })"
            >
              Manage Users
            </UButton>
            <UButton
              color="primary"
              variant="soft"
              block
              icon="i-heroicons-truck"
              @click="router.push({ name: 'delivery-shippers' })"
            >
              Manage Shippers
            </UButton>
          </div>
        </UCard>

        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">System Information</h3>
          </template>
          <div class="space-y-2 text-sm text-gray-600 dark:text-gray-400">
            <p>Welcome to the Admin Dashboard</p>
            <p>Use the sidebar to navigate to different management sections.</p>
          </div>
        </UCard>
      </div>
    </template>

    <!-- Client Dashboard -->
    <template v-else-if="isClient">
      <PageHeader
        title="My Dashboard"
        description="Overview of your parcels and addresses"
      />

      <USkeleton v-if="loading" class="h-32 w-full" />

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-500">Total Parcels</span>
              <UIcon name="i-heroicons-cube" class="w-5 h-5 text-gray-400" />
            </div>
          </template>
          <div class="text-3xl font-semibold text-gray-900 dark:text-gray-100">
            {{ clientStats.totalParcels }}
          </div>
          <div class="text-xs text-gray-500 mt-2">
            <UButton
              variant="ghost"
              size="xs"
              @click="router.push({ name: 'client-parcels' })"
            >
              View All →
            </UButton>
          </div>
        </UCard>

        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-500">Pending</span>
              <UIcon name="i-heroicons-clock" class="w-5 h-5 text-yellow-400" />
            </div>
          </template>
          <div class="text-3xl font-semibold text-yellow-600 dark:text-yellow-400">
            {{ clientStats.pendingParcels }}
          </div>
          <div class="text-xs text-gray-500 mt-2">In warehouse or on route</div>
        </UCard>

        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-500">Delivered</span>
              <UIcon name="i-heroicons-check-circle" class="w-5 h-5 text-green-400" />
            </div>
          </template>
          <div class="text-3xl font-semibold text-green-600 dark:text-green-400">
            {{ clientStats.deliveredParcels }}
          </div>
          <div class="text-xs text-gray-500 mt-2">Successfully delivered</div>
        </UCard>

        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-500">My Addresses</span>
              <UIcon name="i-heroicons-map-pin" class="w-5 h-5 text-gray-400" />
            </div>
          </template>
          <div class="text-3xl font-semibold text-gray-900 dark:text-gray-100">
            {{ clientStats.totalAddresses }}
          </div>
          <div class="text-xs text-gray-500 mt-2">
            <UButton
              variant="ghost"
              size="xs"
              @click="router.push({ name: 'client-addresses' })"
            >
              Manage →
            </UButton>
          </div>
        </UCard>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">Quick Actions</h3>
          </template>
          <div class="space-y-2">
            <UButton
              color="primary"
              variant="soft"
              block
              icon="i-heroicons-plus"
              @click="router.push({ name: 'client-create-parcel' })"
            >
              Create New Parcel
            </UButton>
            <UButton
              color="primary"
              variant="soft"
              block
              icon="i-heroicons-map-pin"
              @click="router.push({ name: 'client-create-address' })"
            >
              Add New Address
            </UButton>
            <UButton
              color="primary"
              variant="soft"
              block
              icon="i-heroicons-cube"
              @click="router.push({ name: 'client-parcels' })"
            >
              View My Parcels
            </UButton>
          </div>
        </UCard>

        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">Welcome</h3>
          </template>
          <div class="space-y-2 text-sm text-gray-600 dark:text-gray-400">
            <p>Welcome back, {{ currentUser?.firstName || currentUser?.username || 'User' }}!</p>
            <p>Manage your parcels, addresses, and profile from here.</p>
          </div>
        </UCard>
      </div>
    </template>

    <!-- Default Dashboard (for other roles) -->
    <template v-else>
      <PageHeader
        title="Dashboard"
        description="Welcome to the system"
      />

      <div class="space-y-6">
        <p class="text-gray-600">
          Welcome to the Dashboard
        </p>
      </div>
    </template>
  </div>
</template>

<style scoped>
.home-theme-1 {
  padding: 20px;
  background-color: #f0f8ff;
  border: 1px solid #d4e8ff;
  border-radius: 8px;
}
</style>
