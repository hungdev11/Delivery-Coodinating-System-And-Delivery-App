<script setup lang="ts">
/**
 * My Addresses View
 * Client view for managing their own addresses
 */

import { ref, onMounted, computed } from 'vue'
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
  if (!confirm(`Are you sure you want to delete "${address.destinationDetails?.name || address.id}"?`)) {
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
  <div class="container mx-auto px-4 py-6 space-y-6">
    <PageHeader
      title="My Addresses"
      description="Manage your delivery addresses"
    >
      <template #actions>
        <UButton color="primary" icon="i-heroicons-plus" @click="goToCreateAddress">
          Add Address
        </UButton>
      </template>
    </PageHeader>

    <USkeleton v-if="loading" class="h-64 w-full" />

    <div v-else-if="addresses.length === 0" class="text-center py-12">
      <UIcon name="i-heroicons-map-pin" class="w-16 h-16 text-gray-400 mx-auto mb-4" />
      <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">
        No addresses yet
      </h3>
      <p class="text-gray-500 mb-4">Create your first address to start receiving parcels</p>
      <UButton color="primary" icon="i-heroicons-plus" @click="goToCreateAddress">
        Add Address
      </UButton>
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <UCard
        v-for="address in addresses"
        :key="address.id"
        :class="[
          'relative',
          address.isPrimary ? 'ring-2 ring-primary-500' : '',
        ]"
      >
        <template #header>
          <div class="flex items-center justify-between">
            <div class="flex items-center space-x-2">
              <h3 class="text-lg font-semibold">
                {{ address.destinationDetails?.name || 'Unnamed Address' }}
              </h3>
              <UBadge v-if="address.isPrimary" color="primary" variant="soft" size="sm">
                Primary
              </UBadge>
            </div>
          </div>
        </template>

        <div class="space-y-2 text-sm">
          <div v-if="address.destinationDetails?.addressText" class="text-gray-600 dark:text-gray-400">
            <UIcon name="i-heroicons-map-pin" class="w-4 h-4 inline mr-1" />
            {{ address.destinationDetails.addressText }}
          </div>
          <div class="text-gray-500 text-xs">
            Coordinates: {{ address.destinationDetails?.lat?.toFixed(6) }},
            {{ address.destinationDetails?.lon?.toFixed(6) }}
          </div>
          <div v-if="address.tag" class="text-gray-500 text-xs">
            Tag: {{ address.tag }}
          </div>
          <div v-if="address.note" class="text-gray-500 text-xs">
            Note: {{ address.note }}
          </div>
        </div>

        <template #footer>
          <div class="flex items-center justify-between">
            <UButton
              v-if="!address.isPrimary"
              variant="ghost"
              size="sm"
              @click="handleSetPrimary(address)"
            >
              Set as Primary
            </UButton>
            <div v-else class="text-xs text-primary-600 dark:text-primary-400 font-medium">
              Primary Address
            </div>
            <UButton
              variant="ghost"
              color="error"
              size="sm"
              icon="i-heroicons-trash"
              @click="handleDelete(address)"
            >
              Delete
            </UButton>
          </div>
        </template>
      </UCard>
    </div>
  </div>
</template>
