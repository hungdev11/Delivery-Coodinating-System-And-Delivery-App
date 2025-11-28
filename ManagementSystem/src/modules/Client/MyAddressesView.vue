<script setup lang="ts">
/**
 * My Addresses View
 * Client view for managing their own addresses
 */

import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import {
  getMyAddresses,
  deleteMyAddress,
  setMyPrimaryAddress,
  type UserAddressDto,
} from '@/modules/UserAddresses/api'
import { defineAsyncComponent } from 'vue'

const PageHeader = defineAsyncComponent(() => import('@/common/components/PageHeader.vue'))

const router = useRouter()
const toast = useToast()

const addresses = ref<UserAddressDto[]>([])
const loading = ref(false)

const loadAddresses = async () => {
  loading.value = true
  try {
    const response = await getMyAddresses()
    if (response.result) {
      addresses.value = response.result
    }
  } catch (error) {
    console.error('Failed to load addresses:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to load addresses',
      color: 'error',
    })
  } finally {
    loading.value = false
  }
}

const handleDelete = async (address: UserAddressDto) => {
  if (
    !confirm(`Are you sure you want to delete "${address.destinationDetails?.name || address.id}"?`)
  ) {
    return
  }

  try {
    await deleteMyAddress(address.id)
    toast.add({
      title: 'Success',
      description: 'Address deleted successfully',
      color: 'success',
    })
    await loadAddresses()
  } catch (error) {
    console.error('Failed to delete address:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to delete address',
      color: 'error',
    })
  }
}

const handleSetPrimary = async (address: UserAddressDto) => {
  try {
    await setMyPrimaryAddress(address.id)
    toast.add({
      title: 'Success',
      description: 'Primary address updated',
      color: 'success',
    })
    await loadAddresses()
  } catch (error) {
    console.error('Failed to set primary address:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to set primary address',
      color: 'error',
    })
  }
}

const goToCreateAddress = () => {
  router.push({ name: 'client-create-address' })
}

onMounted(() => {
  loadAddresses()
})
</script>

<template>
  <div class="container mx-auto px-2 md:px-4 py-4 md:py-6 space-y-4 md:space-y-6">
    <PageHeader title="Địa chỉ của tôi" description="Quản lý địa chỉ giao hàng">
      <template #actions>
        <UButton
          color="primary"
          icon="i-heroicons-plus"
          size="sm"
          class="md:size-md"
          @click="goToCreateAddress"
        >
          <span class="hidden sm:inline">Thêm địa chỉ</span>
          <span class="sm:hidden">Thêm</span>
        </UButton>
      </template>
    </PageHeader>

    <template v-if="loading">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3 md:gap-4">
        <USkeleton v-for="i in 3" :key="i" class="h-40 w-full rounded-lg" />
      </div>
    </template>

    <div v-else-if="addresses.length === 0" class="text-center py-12">
      <UIcon name="i-heroicons-map-pin" class="w-16 h-16 text-gray-400 mx-auto mb-4" />
      <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">Chưa có địa chỉ</h3>
      <p class="text-gray-500 mb-4">Thêm địa chỉ đầu tiên để nhận đơn hàng</p>
      <UButton color="primary" icon="i-heroicons-plus" @click="goToCreateAddress">
        Thêm địa chỉ
      </UButton>
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3 md:gap-4">
      <UCard
        v-for="address in addresses"
        :key="address.id"
        :class="['relative', address.isPrimary ? 'ring-2 ring-orange-500' : '']"
      >
        <template #header>
          <div class="flex items-start justify-between gap-2">
            <div class="flex-1 min-w-0">
              <h3 class="text-base md:text-lg font-semibold truncate">
                {{ address.destinationDetails?.name || 'Địa chỉ chưa đặt tên' }}
              </h3>
            </div>
            <UBadge
              v-if="address.isPrimary"
              color="primary"
              variant="soft"
              size="xs"
              class="flex-shrink-0"
            >
              Mặc định
            </UBadge>
          </div>
        </template>

        <div class="space-y-2 text-sm">
          <div
            v-if="address.destinationDetails?.addressText"
            class="text-gray-600 dark:text-gray-400 line-clamp-2"
          >
            <UIcon name="i-heroicons-map-pin" class="w-4 h-4 inline mr-1 flex-shrink-0" />
            {{ address.destinationDetails.addressText }}
          </div>
          <div class="text-gray-500 text-xs hidden md:block">
            Tọa độ: {{ address.destinationDetails?.lat?.toFixed(6) }},
            {{ address.destinationDetails?.lon?.toFixed(6) }}
          </div>
          <div v-if="address.tag" class="text-gray-500 text-xs">Nhãn: {{ address.tag }}</div>
          <div v-if="address.note" class="text-gray-500 text-xs line-clamp-1">
            Ghi chú: {{ address.note }}
          </div>
        </div>

        <template #footer>
          <div class="flex items-center justify-between gap-2">
            <UButton
              v-if="!address.isPrimary"
              variant="ghost"
              size="xs"
              class="md:size-sm"
              @click="handleSetPrimary(address)"
            >
              Đặt mặc định
            </UButton>
            <div v-else class="text-xs text-orange-600 dark:text-orange-400 font-medium">
              Địa chỉ mặc định
            </div>
            <UButton
              variant="ghost"
              color="error"
              size="xs"
              class="md:size-sm"
              icon="i-heroicons-trash"
              @click="handleDelete(address)"
            >
              <span class="hidden sm:inline">Xóa</span>
            </UButton>
          </div>
        </template>
      </UCard>
    </div>
  </div>
</template>
