<script setup lang="ts">
/**
 * Address Form Modal
 * 
 * Modal for creating/editing user addresses with map picker
 * Usage with useOverlay()
 */

import { ref, computed, watch } from 'vue'
import { defineAsyncComponent } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { getOrCreateAddress } from '@/modules/Addresses/api'
import type { UserAddressDto } from '../api'

const AddressPicker = defineAsyncComponent(
  () => import('@/modules/Parcels/components/AddressPicker.vue'),
)

interface Props {
  address?: UserAddressDto
  mode: 'create' | 'edit'
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: [result: any] }>()

const toast = useToast()

// Form state
const form = ref({
  name: '',
  addressText: '',
  lat: 0,
  lon: 0,
  tag: 'Home',
  note: '',
  isPrimary: false,
})

// Address picker model value
const addressPickerValue = ref<{
  lat?: number
  lon?: number
  addressText?: string
}>({})

// Watch for prop changes and update form
watch(
  () => props.address,
  (newAddress) => {
    if (newAddress) {
      form.value = {
        name: newAddress.destinationDetails?.name || '',
        addressText: newAddress.destinationDetails?.addressText || newAddress.note || '',
        lat: newAddress.destinationDetails?.lat || 0,
        lon: newAddress.destinationDetails?.lon || 0,
        tag: newAddress.tag || 'Home',
        note: newAddress.note || '',
        isPrimary: newAddress.isPrimary || false,
      }
      
      // Set address picker value
      if (form.value.lat && form.value.lon) {
        addressPickerValue.value = {
          lat: form.value.lat,
          lon: form.value.lon,
          addressText: form.value.addressText,
        }
      }
    }
  },
  { immediate: true },
)

// Watch address picker changes
watch(
  () => addressPickerValue.value,
  (newValue) => {
    if (newValue.lat && newValue.lon) {
      form.value.lat = newValue.lat
      form.value.lon = newValue.lon
      if (newValue.addressText && !form.value.addressText) {
        form.value.addressText = newValue.addressText
      }
    }
  },
  { deep: true },
)

const submitting = ref(false)

const tagOptions = [
  { label: 'Nhà', value: 'Home' },
  { label: 'Cơ quan', value: 'Work' },
  { label: 'Khác', value: 'Other' },
]

const isEditMode = computed(() => props.mode === 'edit')

const handleSubmit = async () => {
  if (!form.value.name.trim()) {
    toast.add({
      title: 'Lỗi',
      description: 'Vui lòng nhập tên địa chỉ',
      color: 'error',
    })
    return
  }

  if (!form.value.lat || !form.value.lon) {
    toast.add({
      title: 'Lỗi',
      description: 'Vui lòng chọn vị trí trên bản đồ',
      color: 'error',
    })
    return
  }

  submitting.value = true
  try {
    // First, get or create destination in zone-service
    const destinationResponse = await getOrCreateAddress({
      name: form.value.name.trim(),
      addressText: form.value.addressText.trim() || undefined,
      lat: form.value.lat,
      lon: form.value.lon,
    })

    if (!destinationResponse || !destinationResponse.result || !destinationResponse.result.id) {
      throw new Error('Failed to create destination')
    }

    // Return form data with destinationId
    emit('close', {
      destinationId: destinationResponse.result.id,
      note: form.value.note.trim() || undefined,
      tag: form.value.tag,
      isPrimary: form.value.isPrimary,
    })
  } catch (error: any) {
    console.error('Failed to submit address:', error)
    toast.add({
      title: 'Lỗi',
      description: error.message || 'Failed to save address',
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
    :title="isEditMode ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ mới'"
    :description="isEditMode ? 'Cập nhật thông tin địa chỉ' : 'Thêm địa chỉ giao hàng mới'"
    :close="{ onClick: handleCancel }"
    :ui="{
      width: 'sm:max-w-2xl md:max-w-3xl',
      footer: 'justify-end',
    }"
  >
    <template #body>
      <form @submit.prevent="handleSubmit" class="space-y-4">
        <UFormField label="Tên địa chỉ" name="name" required>
          <UInput
            class="w-full"
            v-model="form.name"
            placeholder="Ví dụ: Nhà riêng, Cơ quan, ..."
          />
        </UFormField>

        <UFormField label="Vị trí" name="location" required>
          <AddressPicker
            v-model="addressPickerValue"
            label=""
            placeholder="Chọn vị trí trên bản đồ hoặc tìm kiếm địa chỉ"
          />
        </UFormField>

        <div class="grid grid-cols-2 gap-4">
          <UFormField label="Nhãn" name="tag">
            <USelect class="w-full" v-model="form.tag" :items="tagOptions" />
          </UFormField>

          <UFormField label="Đặt làm mặc định" name="isPrimary">
            <div class="flex items-center h-10">
              <USwitch v-model="form.isPrimary" />
            </div>
          </UFormField>
        </div>

        <UFormField label="Ghi chú" name="note">
          <UTextarea
            class="w-full"
            v-model="form.note"
            placeholder="Ghi chú thêm về địa chỉ (tùy chọn)"
            :rows="3"
          />
        </UFormField>
      </form>
    </template>

    <template #footer>
      <UButton :disabled="submitting" variant="outline" color="neutral" @click="handleCancel">
        Hủy
      </UButton>
      <UButton :loading="submitting" @click="handleSubmit">
        {{ isEditMode ? 'Cập nhật' : 'Tạo mới' }}
      </UButton>
    </template>
  </UModal>
</template>
