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

interface Props {
  parcel?: ParcelDto
  mode: 'create' | 'edit'
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: [result: typeof form.value | null] }>()

// Form state
const form = ref({
  code: '',
  senderId: '',
  receiverId: '',
  deliveryType: 'STANDARD' as DeliveryType,
  receiveFrom: '',
  sendTo: '',
  weight: 0,
  value: 0,
  windowStart: '',
  windowEnd: '',
  lat: undefined as number | undefined,
  lon: undefined as number | undefined,
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
        deliveryType: parcelData.deliveryType || 'STANDARD',
        receiveFrom: parcelData.receiveFrom || '',
        sendTo: parcelData.targetDestination || '',
        weight: parcelData.weight || 0,
        value: parcelData.value || 0,
        windowStart: parcelData.windowStart || '',
        windowEnd: parcelData.windowEnd || '',
        lat: parcelData.lat,
        lon: parcelData.lon,
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
    // Use receiveFrom coordinates for lat/lon if sendTo doesn't have them
    if (newValue.lat && newValue.lon && !sendToAddress.value.lat) {
      form.value.lat = newValue.lat
      form.value.lon = newValue.lon
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
    // Prefer sendTo coordinates for lat/lon (destination)
    if (newValue.lat && newValue.lon) {
      form.value.lat = newValue.lat
      form.value.lon = newValue.lon
    }
  },
  { deep: true },
)

const submitting = ref(false)

const deliveryTypeOptions = [
  { label: 'Standard', value: 'STANDARD' },
  { label: 'Express', value: 'EXPRESS' },
  { label: 'Same Day', value: 'SAME_DAY' },
]

const isEditMode = computed(() => props.mode === 'edit')

const handleSubmit = async () => {
  submitting.value = true
  try {
    // Emit close with form data
    emit('close', form.value)
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
    :ui="{ footer: 'justify-end' }"
  >
    <template #body>
      <form @submit.prevent="handleSubmit" class="space-y-4">
        <UFormField label="Code" name="code" required>
          <UInput class="w-full" v-model="form.code" placeholder="Enter parcel code" />
        </UFormField>

        <div class="grid grid-cols-2 gap-4">
          <UFormField label="Sender ID" name="senderId" required>
            <UInput class="w-full" v-model="form.senderId" placeholder="Enter sender ID" />
          </UFormField>

          <UFormField label="Receiver ID" name="receiverId" required>
            <UInput class="w-full" v-model="form.receiverId" placeholder="Enter receiver ID" />
          </UFormField>
        </div>

        <UFormField label="Delivery Type" name="deliveryType" required>
          <USelect class="w-full" v-model="form.deliveryType" :options="deliveryTypeOptions" />
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

        <div class="grid grid-cols-2 gap-4">
          <UFormField label="Latitude" name="lat">
            <UInput
              class="w-full"
              v-model.number="form.lat"
              type="number"
              step="0.000001"
              placeholder="Enter latitude"
            />
          </UFormField>

          <UFormField label="Longitude" name="lon">
            <UInput
              class="w-full"
              v-model.number="form.lon"
              type="number"
              step="0.000001"
              placeholder="Enter longitude"
            />
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
