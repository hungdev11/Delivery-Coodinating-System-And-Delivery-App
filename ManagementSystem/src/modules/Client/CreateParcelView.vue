<script setup lang="ts">
/**
 * Create Parcel View
 * Client view for creating a new parcel using only their existing addresses
 */

import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { createParcel } from '@/modules/Parcels/api'
import { CreateParcelRequest } from '@/modules/Parcels/model.type'
import { getMyAddresses, type UserAddressDto } from '@/modules/UserAddresses/api'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'
import { defineAsyncComponent } from 'vue'

const PageHeader = defineAsyncComponent(() => import('@/common/components/PageHeader.vue'))

const router = useRouter()
const toast = useToast()
const currentUser = getCurrentUser()

const addresses = ref<UserAddressDto[]>([])
const loadingAddresses = ref(false)
const creating = ref(false)

const form = ref({
  code: '',
  senderId: '',
  receiverId: '',
  receiveFrom: '',
  sendTo: '',
  senderDestinationId: '',
  receiverDestinationId: '',
  weight: 0,
  value: 0,
  deliveryType: 'NORMAL' as const,
})

const loadAddresses = async () => {
  if (!currentUser?.id) return

  loadingAddresses.value = true
  try {
    const response = await getMyAddresses()
    if (response.result) {
      addresses.value = response.result
      // Set default receiver to current user
      form.value.receiverId = currentUser.id
      // Set default sender destination to primary address if available
      const primaryAddress = response.result.find((addr) => addr.isPrimary)
      if (primaryAddress) {
        form.value.senderDestinationId = primaryAddress.destinationId
        form.value.receiveFrom = primaryAddress.destinationDetails?.name || ''
      }
    }
  } catch (error) {
    console.error('Failed to load addresses:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to load addresses',
      color: 'error',
    })
  } finally {
    loadingAddresses.value = false
  }
}

const handleSenderAddressChange = (addressId: string) => {
  const address = addresses.value.find((addr) => addr.destinationId === addressId)
  if (address) {
    form.value.receiveFrom = address.destinationDetails?.name || ''
  }
}

const handleReceiverAddressChange = (addressId: string) => {
  const address = addresses.value.find((addr) => addr.destinationId === addressId)
  if (address) {
    form.value.sendTo = address.destinationDetails?.name || ''
  }
}

const handleSubmit = async () => {
  if (!form.value.senderDestinationId || !form.value.receiverDestinationId) {
    toast.add({
      title: 'Error',
      description: 'Please select both sender and receiver addresses',
      color: 'error',
    })
    return
  }

  if (!form.value.code.trim()) {
    toast.add({
      title: 'Error',
      description: 'Please enter parcel code',
      color: 'error',
    })
    return
  }

  creating.value = true
  try {
    const request = new CreateParcelRequest({
      code: form.value.code,
      senderId: form.value.senderId || currentUser?.id || '',
      receiverId: form.value.receiverId || currentUser?.id || '',
      receiveFrom: form.value.receiveFrom,
      sendTo: form.value.sendTo,
      senderDestinationId: form.value.senderDestinationId,
      receiverDestinationId: form.value.receiverDestinationId,
      weight: form.value.weight,
      value: form.value.value,
      deliveryType: form.value.deliveryType,
    })

    await createParcel(request)

    toast.add({
      title: 'Success',
      description: 'Parcel created successfully',
      color: 'success',
    })

    router.push({ name: 'client-parcels' })
  } catch (error) {
    console.error('Failed to create parcel:', error)
    toast.add({
      title: 'Error',
      description: error instanceof Error ? error.message : 'Failed to create parcel',
      color: 'error',
    })
  } finally {
    creating.value = false
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
    <PageHeader title="Create Parcel" description="Create a new parcel using your addresses">
      <template #actions>
        <UButton variant="ghost" icon="i-heroicons-arrow-left" @click="router.back()">
          Back
        </UButton>
      </template>
    </PageHeader>

    <UCard>
      <template #header>
        <h3 class="text-lg font-semibold">Parcel Information</h3>
      </template>

      <div class="space-y-4">
        <UFormField label="Parcel Code" required>
          <UInput v-model="form.code" placeholder="e.g., PARCEL-001" />
        </UFormField>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <UFormField label="Sender Address" required>
            <USelect
              v-model="form.senderDestinationId"
              :options="
                addresses.map((addr) => ({
                  label: `${addr.destinationDetails?.name || 'Unnamed'}${addr.isPrimary ? ' (Primary)' : ''}`,
                  value: addr.destinationId,
                }))
              "
              placeholder="Select sender address"
              @update:model-value="handleSenderAddressChange"
            />
          </UFormField>

          <UFormField label="Receiver Address" required>
            <USelect
              v-model="form.receiverDestinationId"
              :options="
                addresses.map((addr) => ({
                  label: `${addr.destinationDetails?.name || 'Unnamed'}${addr.isPrimary ? ' (Primary)' : ''}`,
                  value: addr.destinationId,
                }))
              "
              placeholder="Select receiver address"
              @update:model-value="handleReceiverAddressChange"
            />
          </UFormField>
        </div>

        <UAlert
          v-if="addresses.length === 0"
          color="warning"
          variant="soft"
          title="No addresses available"
          description="Please create at least one address before creating a parcel"
        >
          <template #actions>
            <UButton color="primary" size="sm" @click="goToCreateAddress"> Create Address </UButton>
          </template>
        </UAlert>

        <div class="flex items-center justify-between">
          <span class="text-sm text-gray-500">Don't have the address you need?</span>
          <UButton variant="ghost" size="sm" icon="i-heroicons-plus" @click="goToCreateAddress">
            Add New Address
          </UButton>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <UFormField label="Weight (kg)">
            <UInput v-model.number="form.weight" type="number" step="0.01" min="0" />
          </UFormField>

          <UFormField label="Value (VND)">
            <UInput v-model.number="form.value" type="number" step="1000" min="0" />
          </UFormField>
        </div>

        <UFormField label="Delivery Type">
          <USelect
            v-model="form.deliveryType"
            :options="[
              { label: 'URGENT', value: 'URGENT' },
              { label: 'EXPRESS', value: 'EXPRESS' },
              { label: 'FAST', value: 'FAST' },
              { label: 'NORMAL', value: 'NORMAL' },
              { label: 'ECONOMY', value: 'ECONOMY' },
            ]"
          />
        </UFormField>
      </div>

      <template #footer>
        <div class="flex items-center justify-end space-x-2">
          <UButton variant="ghost" @click="router.back()">Cancel</UButton>
          <UButton color="primary" :loading="creating" @click="handleSubmit">
            Create Parcel
          </UButton>
        </div>
      </template>
    </UCard>
  </div>
</template>
