<script setup lang="ts">
/**
 * User Form Modal
 *
 * Modal for creating/editing users
 * Usage with useOverlay()
 */

import { computed, ref, watch } from 'vue';
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
watch(() => props.user, (newUser) => {
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
}, { immediate: true })

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
    :title="isEditMode ? 'Edit User' : 'Create User'"
    :description="isEditMode ? 'Update user information' : 'Create a new user account'"
    :close="{ onClick: handleCancel }"
    :ui="{ footer: 'justify-end' }"
  >
    <template #body>
      <form @submit.prevent="handleSubmit" class="space-y-4">
        <UFormField v-if="!isEditMode" label="Username" name="username" required>
          <UInput v-model="form.username" placeholder="Enter username" />
        </UFormField>

        <UFormField label="Email" name="email" required>
          <UInput v-model="form.email" type="email" placeholder="Enter email" />
        </UFormField>

        <div class="grid grid-cols-2 gap-4">
          <UFormField label="First Name" name="firstName" required>
            <UInput v-model="form.firstName" placeholder="Enter first name" />
          </UFormField>

          <UFormField label="Last Name" name="lastName" required>
            <UInput v-model="form.lastName" placeholder="Enter last name" />
          </UFormField>
        </div>

        <UFormField label="Phone" name="phone">
          <UInput v-model="form.phone" placeholder="Enter phone number" />
        </UFormField>

        <UFormField label="Address" name="address">
          <UTextarea v-model="form.address" placeholder="Enter address" />
        </UFormField>

        <UFormField label="Identity Number" name="identityNumber">
          <UInput v-model="form.identityNumber" placeholder="Enter identity number" />
        </UFormField>

        <UFormField v-if="!isEditMode" label="Password" name="password">
          <UInput v-model="form.password" type="password" placeholder="Enter password" />
        </UFormField>

        <UFormField v-if="isEditMode" label="Status" name="status" required>
          <USelect v-model="form.status" :options="statusOptions" />
        </UFormField>
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
