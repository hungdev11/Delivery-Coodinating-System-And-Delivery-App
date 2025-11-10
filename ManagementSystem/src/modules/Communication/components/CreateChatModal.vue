<script setup lang="ts">
/**
 * Create Chat Modal
 *
 * Modal for creating a new chat conversation with a user
 * Usage with useOverlay()
 */

import { ref, computed } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { getUserById } from '../../Users/api'

interface Props {
  currentUserId: string
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: [result: { partnerId: string } | null] }>()

const toast = useToast()
const partnerIdInput = ref('')
const searching = ref(false)
const partnerInfo = ref<{ id: string; name: string } | null>(null)

const submitting = ref(false)

/**
 * Search for user by ID
 */
const searchUser = async () => {
  if (!partnerIdInput.value.trim()) {
    partnerInfo.value = null
    return
  }

  searching.value = true
  try {
    const response = await getUserById(partnerIdInput.value.trim())
    if (response.result) {
      partnerInfo.value = {
        id: response.result.id,
        name: response.result.fullName || response.result.username,
      }
    } else {
      partnerInfo.value = null
      toast.add({
        title: 'User not found',
        description: 'Please check the user ID',
        color: 'warning',
      })
    }
  } catch (error) {
    console.error('Failed to search user:', error)
    partnerInfo.value = null
    toast.add({
      title: 'Error',
      description: 'Failed to search user',
      color: 'error',
    })
  } finally {
    searching.value = false
  }
}

const handleSubmit = async () => {
  if (!partnerIdInput.value.trim()) {
    toast.add({
      title: 'Error',
      description: 'Please enter a user ID',
      color: 'error',
    })
    return
  }

  if (partnerIdInput.value.trim() === props.currentUserId) {
    toast.add({
      title: 'Error',
      description: 'You cannot chat with yourself',
      color: 'error',
    })
    return
  }

  submitting.value = true
  try {
    emit('close', { partnerId: partnerIdInput.value.trim() })
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  emit('close', null)
}

const canSubmit = computed(() => {
  return partnerIdInput.value.trim() && partnerIdInput.value.trim() !== props.currentUserId
})
</script>

<template>
  <UModal
    title="Start New Conversation"
    description="Enter a user ID to start chatting"
    :close="{ onClick: handleCancel }"
    :ui="{ footer: 'justify-end' }"
  >
    <template #body>
      <div class="space-y-4">
        <UFormField label="User ID" name="partnerId" required>
          <div class="flex gap-2">
            <UInput
              class="flex-1"
              v-model="partnerIdInput"
              placeholder="Enter user ID"
              @blur="searchUser"
              @keyup.enter="searchUser"
            />
            <UButton
              :loading="searching"
              icon="i-heroicons-magnifying-glass"
              @click="searchUser"
            >
              Search
            </UButton>
          </div>
        </UFormField>

        <div v-if="partnerInfo" class="p-3 bg-green-50 dark:bg-green-900/20 rounded-lg">
          <div class="flex items-center space-x-2">
            <UIcon name="i-heroicons-check-circle" class="text-green-600" />
            <div>
              <p class="text-sm font-medium text-gray-900 dark:text-gray-100">
                {{ partnerInfo.name }}
              </p>
              <p class="text-xs text-gray-500">ID: {{ partnerInfo.id }}</p>
            </div>
          </div>
        </div>
      </div>
    </template>

    <template #footer>
      <UButton :disabled="submitting" variant="outline" color="neutral" @click="handleCancel">
        Cancel
      </UButton>
      <UButton :loading="submitting" :disabled="!canSubmit" @click="handleSubmit">
        Start Chat
      </UButton>
    </template>
  </UModal>
</template>
