<script setup lang="ts">
/**
 * Profile View
 * Client view for managing personal information
 */

import { ref, onMounted, computed } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { getCurrentUserV2, updateUser } from '@/modules/Users/api'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'
import { defineAsyncComponent } from 'vue'
import type { UpdateUserRequest } from '@/modules/Users/model.type'

const PageHeader = defineAsyncComponent(() => import('@/common/components/PageHeader.vue'))

const toast = useToast()
const currentUser = getCurrentUser()

const loading = ref(false)
const saving = ref(false)
const user = ref<any>(null)

const form = ref({
  firstName: '',
  lastName: '',
  email: '',
  username: '',
})

const loadProfile = async () => {
  if (!currentUser?.id) return

  loading.value = true
  try {
    const response = await getCurrentUserV2()
    if (response.result) {
      user.value = response.result
      form.value = {
        firstName: response.result.firstName || '',
        lastName: response.result.lastName || '',
        email: response.result.email || '',
        username: response.result.username || '',
      }
    }
  } catch (error) {
    console.error('Failed to load profile:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to load profile',
      color: 'error',
    })
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  if (!currentUser?.id) return

  saving.value = true
  try {
    const updateRequest: UpdateUserRequest = {
      firstName: form.value.firstName,
      lastName: form.value.lastName,
      email: form.value.email,
    }

    await updateUser(currentUser.id, updateRequest)

    toast.add({
      title: 'Success',
      description: 'Profile updated successfully',
      color: 'success',
    })

    await loadProfile()
  } catch (error) {
    console.error('Failed to update profile:', error)
    toast.add({
      title: 'Error',
      description: error instanceof Error ? error.message : 'Failed to update profile',
      color: 'error',
    })
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<template>
  <div class="container mx-auto px-4 py-6 space-y-6">
    <PageHeader
      title="My Profile"
      description="Manage your personal information"
    />

    <USkeleton v-if="loading" class="h-64 w-full" />

    <UCard v-else>
      <template #header>
        <h3 class="text-lg font-semibold">Personal Information</h3>
      </template>

      <div class="space-y-4">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <UFormField label="First Name">
            <UInput v-model="form.firstName" placeholder="Enter first name" />
          </UFormField>

          <UFormField label="Last Name">
            <UInput v-model="form.lastName" placeholder="Enter last name" />
          </UFormField>
        </div>

        <UFormField label="Username" disabled>
          <UInput v-model="form.username" placeholder="Username" />
          <template #description>
            Username cannot be changed
          </template>
        </UFormField>

        <UFormField label="Email">
          <UInput v-model="form.email" type="email" placeholder="Enter email" />
        </UFormField>

      </div>

      <template #footer>
        <div class="flex items-center justify-end">
          <UButton color="primary" :loading="saving" @click="handleSave">
            Save Changes
          </UButton>
        </div>
      </template>
    </UCard>
  </div>
</template>
