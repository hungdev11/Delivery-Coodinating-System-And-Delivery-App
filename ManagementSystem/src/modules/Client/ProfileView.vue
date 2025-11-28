<script setup lang="ts">
/**
 * Profile View
 * Client view for managing personal information
 */

import { ref, onMounted } from 'vue'
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
  <div class="container mx-auto px-2 md:px-4 py-4 md:py-6 space-y-4 md:space-y-6">
    <PageHeader title="Hồ sơ của tôi" description="Quản lý thông tin cá nhân" />

    <USkeleton v-if="loading" class="h-64 w-full rounded-lg" />

    <UCard v-else>
      <template #header>
        <div class="flex items-center gap-4">
          <!-- Avatar -->
          <div
            class="w-16 h-16 md:w-20 md:h-20 bg-orange-500 rounded-full flex items-center justify-center flex-shrink-0"
          >
            <span class="text-2xl md:text-3xl font-bold text-white">
              {{
                form.firstName && form.lastName
                  ? form.firstName.charAt(0) + form.lastName.charAt(0)
                  : form.username?.charAt(0)?.toUpperCase() || 'U'
              }}
            </span>
          </div>
          <div class="min-w-0">
            <h3 class="text-lg md:text-xl font-semibold text-gray-900 truncate">
              {{
                form.firstName && form.lastName
                  ? `${form.firstName} ${form.lastName}`
                  : form.username || 'Người dùng'
              }}
            </h3>
            <p class="text-sm text-gray-500 truncate">{{ form.email || 'Chưa có email' }}</p>
          </div>
        </div>
      </template>

      <div class="space-y-4">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <UFormField label="Họ">
            <UInput v-model="form.firstName" placeholder="Nhập họ" />
          </UFormField>

          <UFormField label="Tên">
            <UInput v-model="form.lastName" placeholder="Nhập tên" />
          </UFormField>
        </div>

        <UFormField label="Tên đăng nhập">
          <UInput v-model="form.username" placeholder="Tên đăng nhập" disabled />
          <template #description>
            <span class="text-xs">Không thể thay đổi tên đăng nhập</span>
          </template>
        </UFormField>

        <UFormField label="Email">
          <UInput v-model="form.email" type="email" placeholder="Nhập email" />
        </UFormField>
      </div>

      <template #footer>
        <div class="flex items-center justify-end">
          <UButton color="primary" :loading="saving" @click="handleSave"> Lưu thay đổi </UButton>
        </div>
      </template>
    </UCard>
  </div>
</template>
