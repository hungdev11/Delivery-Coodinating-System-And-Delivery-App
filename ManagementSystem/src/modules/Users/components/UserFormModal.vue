<script setup lang="ts">
/**
 * User Form Modal
 *
 * Modal for creating/editing users
 * Usage with useOverlay()
 */

import { computed, ref, watch } from 'vue'
import type { UserDto, UserStatus } from '../model.type'

interface Props {
  user?: UserDto
  mode: 'create' | 'edit'
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: [result: any] }>()

// Form state
const form = ref({
  username: '',
  email: '',
  firstName: '',
  lastName: '',
  phone: '',
  address: '',
  identityNumber: '',
  password: '',
  status: 'ACTIVE' as UserStatus,
})

// Watch for prop changes and update form
watch(
  () => props.user,
  (newUser) => {
    if (newUser) {
      // Extract the actual user data from the table row object
      const userData = newUser.original || newUser

      form.value = {
        username: userData.username || '',
        email: userData.email || '',
        firstName: userData.firstName || '',
        lastName: userData.lastName || '',
        phone: userData.phone || '',
        address: userData.address || '',
        identityNumber: userData.identityNumber || '',
        password: '',
        status: userData.status || 'ACTIVE',
      }
    }
  },
  { immediate: true },
)

const submitting = ref(false)

const statusOptions = [
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Inactive', value: 'INACTIVE' },
  { label: 'Suspended', value: 'SUSPENDED' },
  { label: 'Pending', value: 'PENDING' },
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
    :title="isEditMode ? 'Chỉnh sửa người dùng' : 'Tạo người dùng'"
    :description="isEditMode ? 'Cập nhật thông tin người dùng' : 'Tạo tài khoản người dùng mới'"
    :close="{ onClick: handleCancel }"
    :ui="{ width: 'sm:max-w-md md:max-w-lg', footer: 'justify-end' }"
  >
    <template #body>
      <form @submit.prevent="handleSubmit" class="space-y-4">
        <UFormField v-if="!isEditMode" label="Username" name="username" required>
          <UInput class="w-full" v-model="form.username" placeholder="Enter username" />
        </UFormField>

        <UFormField label="Email" name="email" required>
          <UInput class="w-full" v-model="form.email" type="email" placeholder="Enter email" />
        </UFormField>

        <div class="grid grid-cols-2 gap-4">
          <UFormField label="First Name" name="firstName" required>
            <UInput class="w-full" v-model="form.firstName" placeholder="Enter first name" />
          </UFormField>

          <UFormField label="Last Name" name="lastName" required>
            <UInput class="w-full" v-model="form.lastName" placeholder="Enter last name" />
          </UFormField>
        </div>

        <UFormField label="Phone" name="phone">
          <UInput class="w-full" v-model="form.phone" placeholder="Enter phone number" />
        </UFormField>

        <UFormField label="Address" name="address">
          <UTextarea class="w-full" v-model="form.address" placeholder="Enter address" />
        </UFormField>

        <UFormField label="Identity Number" name="identityNumber">
          <UInput
            class="w-full"
            v-model="form.identityNumber"
            placeholder="Enter identity number"
          />
        </UFormField>

        <UFormField v-if="!isEditMode" label="Password" name="password">
          <UInput
            class="w-full"
            v-model="form.password"
            type="password"
            placeholder="Enter password"
          />
        </UFormField>

        <UFormField v-if="isEditMode" label="Status" name="status" required>
          <USelect class="w-full" v-model="form.status" :items="statusOptions" />
        </UFormField>
      </form>
    </template>

    <template #footer>
      <UButton :disabled="submitting" variant="outline" color="neutral" @click="handleCancel">
        Hủy
      </UButton>
      <UButton :loading="submitting" @click="handleSubmit">
        {{ isEditMode ? 'Cập nhật' : 'Tạo' }}
      </UButton>
    </template>
  </UModal>
</template>
