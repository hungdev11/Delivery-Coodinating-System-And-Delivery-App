<script setup lang="ts">
/**
 * User Delete Modal
 *
 * Confirmation modal for deleting user(s)
 * Usage with useOverlay()
 */

interface Props {
  userName?: string
  count?: number
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: [confirmed: boolean] }>()

const submitting = ref(false)

const message = computed(() => {
  if (props.count && props.count > 1) {
    return `Are you sure you want to delete ${props.count} users? This action cannot be undone.`
  }
  return `Are you sure you want to delete ${props.userName}? This action cannot be undone.`
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
    title="Delete User"
    :description="message"
    :close="{ onClick: handleCancel }"
    :ui="{ footer: 'justify-end' }"
  >
    <template #body>
      <p class="text-gray-600">{{ message }}</p>
    </template>

    <template #footer>
      <UButton :disabled="submitting" variant="outline" color="neutral" @click="handleCancel">
        Cancel
      </UButton>
      <UButton :loading="submitting" color="error" @click="handleConfirm"> Delete </UButton>
    </template>
  </UModal>
</template>
