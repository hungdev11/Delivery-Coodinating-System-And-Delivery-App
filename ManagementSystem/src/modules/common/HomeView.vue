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
  document.title = 'DSS - Phân phối hàng hoá đầu cuối'
  if (isClient.value) {
    loadClientStats()
  }
})
</script>

<template>
  <div class="container mx-auto px-2 sm:px-4 py-4 sm:py-6 space-y-4 sm:space-y-6">
    <!-- Admin Dashboard -->
    <template v-if="isAdmin">
      <PageHeader title="Bảng điều khiển quản trị" description="Tổng quan và quản lý hệ thống" />

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-sm">Tổng người dùng</span>
              <UIcon name="i-heroicons-user-group" class="w-5 h-5" />
            </div>
          </template>
          <div class="text-3xl font-semibold">—</div>
          <div class="text-xs mt-2">
            <UButton variant="ghost" size="xs" @click="router.push({ name: 'users' })">
              Xem tất cả →
            </UButton>
          </div>
        </UCard>

        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-sm">Tổng đơn hàng</span>
              <UIcon name="i-heroicons-cube" class="w-5 h-5" />
            </div>
          </template>
          <div class="text-3xl font-semibold">—</div>
          <div class="text-xs mt-2">
            <UButton variant="ghost" size="xs" @click="router.push({ name: 'parcels' })">
              Xem tất cả →
            </UButton>
          </div>
        </UCard>

        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-sm">Shipper đang hoạt động</span>
              <UIcon name="i-heroicons-truck" class="w-5 h-5" />
            </div>
          </template>
          <div class="text-3xl font-semibold">—</div>
          <div class="text-xs mt-2">
            <UButton variant="ghost" size="xs" @click="router.push({ name: 'delivery-shippers' })">
              Xem tất cả →
            </UButton>
          </div>
        </UCard>

        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-sm">Khu vực</span>
              <UIcon name="i-heroicons-map" class="w-5 h-5" />
            </div>
          </template>
          <div class="text-3xl font-semibold">—</div>
          <div class="text-xs mt-2">
            <UButton variant="ghost" size="xs" @click="router.push({ name: 'zones' })">
              Xem tất cả →
            </UButton>
          </div>
        </UCard>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">Thao tác nhanh</h3>
          </template>
          <div class="space-y-2">
            <UButton
              color="primary"
              variant="soft"
              block
              icon="i-heroicons-cube"
              @click="router.push({ name: 'parcels' })"
            >
              Quản lý đơn hàng
            </UButton>
            <UButton
              color="primary"
              variant="soft"
              block
              icon="i-heroicons-user-group"
              @click="router.push({ name: 'users' })"
            >
              Quản lý người dùng
            </UButton>
            <UButton
              color="primary"
              variant="soft"
              block
              icon="i-heroicons-truck"
              @click="router.push({ name: 'delivery-shippers' })"
            >
              Quản lý Shipper
            </UButton>
          </div>
        </UCard>

        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">Thông tin hệ thống</h3>
          </template>
          <div class="space-y-2 text-sm">
            <p>Chào mừng đến Bảng điều khiển quản trị</p>
            <p>Sử dụng thanh bên để điều hướng đến các phần quản lý khác nhau.</p>
          </div>
        </UCard>
      </div>
    </template>

    <!-- Client Dashboard -->
    <template v-else-if="isClient">
      <PageHeader title="Bảng điều khiển của tôi" description="Tổng quan về đơn hàng và địa chỉ của bạn" />

      <USkeleton v-if="loading" class="h-32 w-full" />

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-sm">Tổng đơn hàng</span>
              <UIcon name="i-heroicons-cube" class="w-5 h-5" />
            </div>
          </template>
          <div class="text-3xl font-semibold">
            {{ clientStats.totalParcels }}
          </div>
          <div class="text-xs mt-2">
            <UButton variant="ghost" size="xs" @click="router.push({ name: 'client-parcels' })">
              Xem tất cả →
            </UButton>
          </div>
        </UCard>

        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-sm">Đang chờ</span>
              <UIcon name="i-heroicons-clock" class="w-5 h-5 text-warning-500" />
            </div>
          </template>
          <div class="text-3xl font-semibold text-warning-600 dark:text-warning-400">
            {{ clientStats.pendingParcels }}
          </div>
          <div class="text-xs mt-2">Trong kho hoặc đang vận chuyển</div>
        </UCard>

        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-sm">Đã giao</span>
              <UIcon name="i-heroicons-check-circle" class="w-5 h-5 text-success-500" />
            </div>
          </template>
          <div class="text-3xl font-semibold text-success-600 dark:text-success-400">
            {{ clientStats.deliveredParcels }}
          </div>
          <div class="text-xs mt-2">Đã giao thành công</div>
        </UCard>

        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-sm">Địa chỉ của tôi</span>
              <UIcon name="i-heroicons-map-pin" class="w-5 h-5" />
            </div>
          </template>
          <div class="text-3xl font-semibold">
            {{ clientStats.totalAddresses }}
          </div>
          <div class="text-xs mt-2">
            <UButton variant="ghost" size="xs" @click="router.push({ name: 'client-addresses' })">
              Quản lý →
            </UButton>
          </div>
        </UCard>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">Thao tác nhanh</h3>
          </template>
          <div class="space-y-2">
            <UButton
              color="primary"
              variant="soft"
              block
              icon="i-heroicons-plus"
              @click="router.push({ name: 'client-create-parcel' })"
            >
              Tạo đơn hàng mới
            </UButton>
            <UButton
              color="primary"
              variant="soft"
              block
              icon="i-heroicons-map-pin"
              @click="router.push({ name: 'client-create-address' })"
            >
              Thêm địa chỉ mới
            </UButton>
            <UButton
              color="primary"
              variant="soft"
              block
              icon="i-heroicons-cube"
              @click="router.push({ name: 'client-parcels' })"
            >
              Xem đơn hàng của tôi
            </UButton>
          </div>
        </UCard>

        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">Chào mừng</h3>
          </template>
          <div class="space-y-2 text-sm">
            <p>Chào mừng trở lại, {{ currentUser?.firstName || currentUser?.username || 'Người dùng' }}!</p>
            <p>Quản lý đơn hàng, địa chỉ và hồ sơ của bạn từ đây.</p>
          </div>
        </UCard>
      </div>
    </template>

    <!-- Default Dashboard (for other roles) -->
    <template v-else>
      <PageHeader title="Bảng điều khiển" description="Chào mừng đến hệ thống" />

      <div class="space-y-6">
        <p>Chào mừng đến Bảng điều khiển</p>
      </div>
    </template>
  </div>
</template>
