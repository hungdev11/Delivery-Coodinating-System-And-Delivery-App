<script setup lang="ts">
/**
 * Create Chat Modal
 *
 * Modal for creating a new chat conversation with a user
 * Usage with useOverlay()
 */

import { ref, computed } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import UserSelect from '@/common/components/UserSelect.vue'

interface Props {
  currentUserId: string
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: [result: { partnerId: string } | null] }>()

const toast = useToast()
const partnerIdInput = ref('')
const submitting = ref(false)

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
  return partnerIdInput.value && partnerIdInput.value.trim() !== props.currentUserId
})
</script>

<template>
  <UModal
    title="Start New Conversation"
    description="Enter a user ID to start chatting"
    :close="{ onClick: handleCancel }"
    :ui="{ width: 'sm:max-w-sm md:max-w-md', footer: 'justify-end' }"
  >
    <template #body>
      <div class="space-y-4">
        <UserSelect
          v-model="partnerIdInput"
          label="Select User"
          placeholder="Search user by ID or name..."
          :allow-seed-id="true"
          :searchable="true"
        />
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
