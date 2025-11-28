<script setup lang="ts">
/**
 * Proposal Config Delete Modal
 *
 * Confirmation modal for deleting proposal configuration
 * Usage with useOverlay()
 */

import { computed, ref } from 'vue'
import type { ProposalTypeConfig } from '../model.type'

interface Props {
  config?: ProposalTypeConfig
  configType?: string
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: [confirmed: boolean] }>()

const submitting = ref(false)

const message = computed(() => {
  if (props.config) {
    return `Are you sure you want to delete the proposal configuration "${props.config.type}"? This action cannot be undone.`
  }
  if (props.configType) {
    return `Are you sure you want to delete the proposal configuration "${props.configType}"? This action cannot be undone.`
  }
  return 'Are you sure you want to delete this proposal configuration? This action cannot be undone.'
})

const handleConfirm = async () => {
  submitting.value = true
  try {
    emit('close', true)
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  emit('close', false)
}
</script>

<template>
  <UModal
    title="Delete Proposal Configuration"
    :description="message"
    :close="{ onClick: handleCancel }"
    :ui="{ content: 'sm:max-w-sm md:max-w-md', footer: 'justify-end' }"
  >
    <template #body>
      <p class="text-gray-600 dark:text-gray-400">{{ message }}</p>
    </template>

    <template #footer>
      <UButton :disabled="submitting" variant="outline" color="neutral" @click="handleCancel">
        Cancel
      </UButton>
      <UButton :loading="submitting" color="error" @click="handleConfirm"> Delete </UButton>
    </template>
  </UModal>
</template>
