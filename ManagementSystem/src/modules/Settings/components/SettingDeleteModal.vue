<script setup lang="ts">
/**
 * Setting Delete Modal
 *
 * Modal for confirming setting deletion
 */

import { computed } from 'vue'

interface Props {
  settingName?: string
  count?: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  result: [boolean]
}>()

// Determine if this is a bulk delete
const isBulkDelete = computed(() => props.count !== undefined)

// Get the appropriate message
const message = computed(() => {
  if (isBulkDelete.value) {
    return `Are you sure you want to delete ${props.count} setting(s)? This action cannot be undone.`
  }
  return `Are you sure you want to delete the setting "${props.settingName}"? This action cannot be undone.`
})

// Confirm deletion
const handleConfirm = () => {
  emit('result', true)
}

// Cancel deletion
const handleCancel = () => {
  emit('result', false)
}
</script>

<template>
  <UModal
    :title="isBulkDelete ? 'Delete Settings' : 'Delete Setting'"
    :description="
      isBulkDelete ? 'Delete multiple settings at once' : 'Confirm deletion of this setting'
    "
    :close="{ onClick: handleCancel }"
    :ui="{ width: 'sm:max-w-sm md:max-w-md', footer: 'justify-end space-x-2' }"
  >
    <template #body>
      <div class="space-y-4">
        <div class="flex items-center space-x-3">
          <div class="flex-shrink-0">
            <UIcon name="i-heroicons-exclamation-triangle" class="h-6 w-6 text-red-600" />
          </div>
          <p class="text-sm text-gray-700 dark:text-gray-300">
            {{ message }}
          </p>
        </div>
      </div>
    </template>

    <template #footer>
      <UButton variant="outline" color="neutral" @click="handleCancel"> Cancel </UButton>
      <UButton color="red" @click="handleConfirm">
        {{ isBulkDelete ? 'Delete All' : 'Delete' }}
      </UButton>
    </template>
  </UModal>
</template>
