<script setup lang="ts">
/**
 * Text Input Modal Component
 *
 * Modal for text input proposals
 */

import { ref } from 'vue'

interface Props {
  title: string
  placeholder?: string
}

withDefaults(defineProps<Props>(), {
  placeholder: 'Enter reason...',
})

const emit = defineEmits<{
  close: [result: { text: string } | null]
}>()

const inputValue = ref('')

/**
 * Handle confirm
 */
const handleConfirm = () => {
  if (!inputValue.value.trim()) {
    alert('Please enter a reason')
    return
  }
  emit('close', { text: inputValue.value.trim() })
}

/**
 * Handle cancel
 */
const handleCancel = () => {
  emit('close', null)
}
</script>

<template>
  <UModal :title="title" :ui="{ width: 'sm:max-w-sm md:max-w-md' }">
    <template #body>
      <div class="space-y-4 p-4">
        <p class="text-sm text-gray-600">Please enter the reason:</p>
        <UInput v-model="inputValue" :placeholder="placeholder" />
        <div class="flex justify-end space-x-2">
          <UButton variant="ghost" @click="handleCancel"> Cancel </UButton>
          <UButton @click="handleConfirm"> Send </UButton>
        </div>
      </div>
    </template>
  </UModal>
</template>
