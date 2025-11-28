<script setup lang="ts">
/**
 * Parcel Form Modal
 *
 * Modal for creating/editing parcels
 * Usage with useOverlay()
 */

import { computed, ref, watch } from 'vue'
import type { ParcelDto, DeliveryType } from '../model.type'
import AddressPicker from './AddressPicker.vue'
import UserSelect from '@/common/components/UserSelect.vue'
import { getOrCreateAddress } from '@/modules/Addresses/api'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'

interface Props {
  parcel?: ParcelDto
  mode: 'create' | 'edit'
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: [result: typeof form.value | null] }>()
const toast = useToast()

// Form state
const form = ref({
  code: '',
  senderId: '',
  receiverId: '',
  deliveryType: 'NORMAL' as DeliveryType,
  receiveFrom: '',
  sendTo: '',
  weight: 0,
  value: 0,
  windowStart: '',
  windowEnd: '',
  senderDestinationId: '',
  receiverDestinationId: '',
})

// Address picker states
const receiveFromAddress = ref<{
  lat?: number
  lon?: number
  addressText?: string
}>({})

const sendToAddress = ref<{
  lat?: number
  lon?: number
  addressText?: string
}>({})

// Watch for prop changes and update form
watch(
  () => props.parcel,
  (newParcel) => {
    if (newParcel) {
      const parcelData = newParcel

      form.value = {
        code: parcelData.code || '',
        senderId: parcelData.senderId || '',
        receiverId: parcelData.receiverId || '',
        deliveryType: parcelData.deliveryType || 'NORMAL',
        receiveFrom: parcelData.receiveFrom || '',
        sendTo: parcelData.targetDestination || '',
        weight: parcelData.weight || 0,
        value: parcelData.value || 0,
        windowStart: parcelData.windowStart || '',
        windowEnd: parcelData.windowEnd || '',
        senderDestinationId: '',
        receiverDestinationId: '',
      }

      // Initialize address pickers
      receiveFromAddress.value = {
        lat: parcelData.lat,
        lon: parcelData.lon,
        addressText: parcelData.receiveFrom || undefined,
      }
      sendToAddress.value = {
        lat: parcelData.lat,
        lon: parcelData.lon,
        addressText: parcelData.targetDestination || undefined,
      }
    } else {
      // Reset for create mode
      receiveFromAddress.value = {}
      sendToAddress.value = {}
    }
  },
  { immediate: true },
)

// Watch address picker changes and update form
watch(
  () => receiveFromAddress.value,
  (newValue) => {
    if (newValue.addressText) {
      form.value.receiveFrom = newValue.addressText
    }
  },
  { deep: true },
)

watch(
  () => sendToAddress.value,
  (newValue) => {
    if (newValue.addressText) {
      form.value.sendTo = newValue.addressText
    }
  },
  { deep: true },
)

const submitting = ref(false)

const deliveryTypeOptions = [
  { label: 'Economy', value: 'ECONOMY' },
  { label: 'Normal', value: 'NORMAL' },
  { label: 'Fast', value: 'FAST' },
  { label: 'Express', value: 'EXPRESS' },
  { label: 'Urgent', value: 'URGENT' },
]

const isEditMode = computed(() => props.mode === 'edit')

const handleSubmit = async () => {
  // Stage 1: Get or create destinations for sender and receiver
  if (!receiveFromAddress.value.lat || !receiveFromAddress.value.lon) {
    toast.add({
      title: 'Error',
      description: 'Please select sender address coordinates (Receive From)',
      color: 'error',
    })
    return
  }

  if (!sendToAddress.value.lat || !sendToAddress.value.lon) {
    toast.add({
      title: 'Error',
      description: 'Please select receiver address coordinates (Send To)',
      color: 'error',
    })
    return
  }

  submitting.value = true
  try {
    // Get or create sender destination
    const senderResponse = await getOrCreateAddress({
      name: form.value.receiveFrom || 'Sender Address',
      addressText: form.value.receiveFrom,
      lat: receiveFromAddress.value.lat,
      lon: receiveFromAddress.value.lon,
    })

    if (!senderResponse.result?.id) {
      toast.add({
        title: 'Error',
        description: 'Failed to get or create sender destination',
        color: 'error',
      })
      return
    }

    // Get or create receiver destination
    const receiverResponse = await getOrCreateAddress({
      name: form.value.sendTo || 'Receiver Address',
      addressText: form.value.sendTo,
      lat: sendToAddress.value.lat,
      lon: sendToAddress.value.lon,
    })

    if (!receiverResponse.result?.id) {
      toast.add({
        title: 'Error',
        description: 'Failed to get or create receiver destination',
        color: 'error',
      })
      return
    }

    // Stage 2: Set destination IDs and emit form data
    form.value.senderDestinationId = senderResponse.result.id
    form.value.receiverDestinationId = receiverResponse.result.id

    // Emit close with form data (now includes destination IDs)
    emit('close', form.value)
  } catch (error: unknown) {
    console.error('Failed to get or create destinations:', error)
    const errorMessage = error instanceof Error ? error.message : 'Failed to process addresses'
    toast.add({
      title: 'Error',
      description: errorMessage,
      color: 'error',
    })
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  emit('close', null)
}
</script>

<template>
  <UModal
    :title="isEditMode ? 'Edit Parcel' : 'Create Parcel'"
    :description="isEditMode ? 'Update parcel information' : 'Create a new parcel'"
    :close="{ onClick: handleCancel }"
    :ui="{ content: 'sm:max-w-md md:max-w-lg', footer: 'justify-end' }"
  >
    <template #body>
      <form @submit.prevent="handleSubmit" class="space-y-4">
        <UFormField label="Code" name="code" required>
          <UInput class="w-full" v-model="form.code" placeholder="Enter parcel code" />
        </UFormField>

        <div class="grid grid-cols-2 gap-4">
          <UserSelect
            v-model="form.senderId"
            label="Sender"
            placeholder="Search sender by ID or name..."
            :allow-seed-id="true"
            :searchable="true"
          />

          <UserSelect
            v-model="form.receiverId"
            label="Receiver"
            placeholder="Search receiver by ID or name..."
            :allow-seed-id="true"
            :searchable="true"
          />
        </div>

        <UFormField label="Delivery Type" name="deliveryType" required>
          <USelect class="w-full" v-model="form.deliveryType" :items="deliveryTypeOptions" />
        </UFormField>

        <AddressPicker
          v-model="receiveFromAddress"
          label="Receive From"
          :placeholder="form.receiveFrom || 'Enter receive address or pick on map'"
        />

        <AddressPicker
          v-model="sendToAddress"
          label="Send To"
          :placeholder="form.sendTo || 'Enter destination address or pick on map'"
        />

        <div class="grid grid-cols-2 gap-4">
          <UFormField label="Weight (kg)" name="weight" required>
            <UInput
              class="w-full"
              v-model.number="form.weight"
              type="number"
              step="0.1"
              placeholder="Enter weight"
            />
          </UFormField>

          <UFormField label="Value" name="value" required>
            <UInput
              class="w-full"
              v-model.number="form.value"
              type="number"
              step="0.01"
              placeholder="Enter value"
            />
          </UFormField>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <UFormField label="Window Start" name="windowStart">
            <UInput class="w-full" v-model="form.windowStart" type="time" />
          </UFormField>

          <UFormField label="Window End" name="windowEnd">
            <UInput class="w-full" v-model="form.windowEnd" type="time" />
          </UFormField>
        </div>
      </form>
    </template>

    <template #footer>
      <UButton :disabled="submitting" variant="outline" color="neutral" @click="handleCancel">
        Cancel
      </UButton>
      <UButton :loading="submitting" @click="handleSubmit">
        {{ isEditMode ? 'Update' : 'Create' }}
      </UButton>
    </template>
  </UModal>
</template>
