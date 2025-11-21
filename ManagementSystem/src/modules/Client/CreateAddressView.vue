<script setup lang="ts">
/**
 * Create Address View
 * Client view for creating a new address using AddressPickerView
 */

import { useRouter } from 'vue-router'
import { onMounted, watch } from 'vue'
import AddressPickerView from '@/modules/Addresses/AddressPickerView.vue'
import { useAddresses } from '@/modules/Addresses/composables'
import { createMyAddress } from '@/modules/UserAddresses/api'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'

const router = useRouter()
const toast = useToast()
const currentUser = getCurrentUser()
const { success } = useAddresses()

// Watch for successful address creation
watch(success, async (newValue) => {
  if (newValue && currentUser?.id) {
    // Address was created in AddressPickerView
    // Now we need to add it to user's address list
    try {
      // The address picker creates a destination, we need to link it to user
      // This will be handled by the AddressPickerView's existing logic
      // Just navigate back after a short delay
      setTimeout(() => {
        router.push({ name: 'client-addresses' })
      }, 1000)
    } catch (error) {
      console.error('Failed to link address to user:', error)
    }
  }
})

onMounted(() => {
  // The AddressPickerView will handle address creation
  // We just need to watch for success and navigate back
})
</script>

<template>
  <div class="container mx-auto px-4 py-6">
    <div class="mb-4">
      <UButton
        variant="ghost"
        icon="i-heroicons-arrow-left"
        @click="router.push({ name: 'client-addresses' })"
      >
        Back to My Addresses
      </UButton>
    </div>
    <AddressPickerView />
  </div>
</template>
