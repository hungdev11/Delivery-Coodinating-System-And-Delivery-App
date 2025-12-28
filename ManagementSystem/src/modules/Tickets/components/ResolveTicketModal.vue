<script setup lang="ts">
/**
 * Resolve Ticket Modal
 *
 * Modal for resolving tickets
 */

import { ref } from 'vue'
import type { TicketDto } from '../model.type'

interface Props {
  ticket: TicketDto
}

defineProps<Props>()
const emit = defineEmits<{ close: [result: { resolutionNotes?: string } | null] }>()

const resolutionNotes = ref('')
const submitting = ref(false)

const handleSubmit = async () => {
  submitting.value = true
  try {
    emit('close', { resolutionNotes: resolutionNotes.value })
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
    title="Resolve Ticket"
    description="Resolve this ticket and add resolution notes"
    :close="{ onClick: handleCancel }"
  >
    <template #body>
      <div class="space-y-4">
        <div>
          <p class="text-sm text-gray-600 dark:text-gray-400">
            <strong>Ticket ID:</strong> {{ ticket.id }}
          </p>
          <p class="text-sm text-gray-600 dark:text-gray-400">
            <strong>Type:</strong> {{ ticket.type }}
          </p>
          <p class="text-sm text-gray-600 dark:text-gray-400">
            <strong>Parcel ID:</strong> {{ ticket.parcelId }}
          </p>
        </div>

        <UFormGroup label="Resolution Notes" name="resolutionNotes">
          <UTextarea
            v-model="resolutionNotes"
            placeholder="Enter resolution notes..."
            :rows="5"
          />
        </UFormGroup>
      </div>
    </template>

    <template #footer>
      <UButton variant="ghost" @click="handleCancel">Cancel</UButton>
      <UButton color="success" :loading="submitting" @click="handleSubmit">
        Resolve
      </UButton>
    </template>
  </UModal>
</template>
