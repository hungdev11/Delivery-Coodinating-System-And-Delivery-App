<script setup lang="ts">
/**
 * Parcel QR/Barcode Modal
 *
 * Modal for displaying parcel ID as QR code or barcode
 * Usage with useOverlay()
 */

import { computed } from 'vue'

interface Props {
  parcelId: string
  parcelCode: string
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: [] }>()

const qrCodeUrl = computed(() => {
  // Generate QR code URL using a QR code service or library
  // For now, we'll use a simple approach - display the ID prominently
  // You can integrate a QR code library like 'qrcode' or use an API service
  const baseUrl = import.meta.env.VITE_API_URL || ''
  return `${baseUrl}/v1/qr/generate?data=${encodeURIComponent(props.parcelId)}`
})

const handleClose = () => {
  emit('close')
}
</script>

<template>
  <UModal
    title="Parcel QR Code"
    description="Scan this code with DeliveryApp to view parcel details"
    :close="{ onClick: handleClose }"
    :ui="{ footer: 'justify-end' }"
  >
    <template #body>
      <div class="flex flex-col items-center space-y-4 p-6">
        <!-- Parcel Code Display -->
        <div class="text-center">
          <p class="text-sm text-gray-600 mb-2">Parcel Code</p>
          <p class="text-2xl font-bold font-mono">{{ parcelCode }}</p>
        </div>

        <!-- Parcel ID Display -->
        <div class="text-center">
          <p class="text-sm text-gray-600 mb-2">Parcel ID</p>
          <p class="text-lg font-mono break-all">{{ parcelId }}</p>
        </div>

        <!-- QR Code Image (if available) -->
        <div class="flex justify-center p-4 bg-white rounded-lg border-2 border-gray-200">
          <img
            v-if="qrCodeUrl"
            :src="qrCodeUrl"
            alt="QR Code"
            class="w-64 h-64"
            @error="(e) => (e.target.style.display = 'none')"
          />
          <div v-else class="w-64 h-64 flex items-center justify-center text-gray-400">
            <p>QR Code will be displayed here</p>
          </div>
        </div>

        <!-- Instructions -->
        <div class="text-center text-sm text-gray-500">
          <p>Open DeliveryApp and scan this code to view parcel details</p>
        </div>
      </div>
    </template>

    <template #footer>
      <UButton variant="outline" color="neutral" @click="handleClose"> Close </UButton>
    </template>
  </UModal>
</template>
