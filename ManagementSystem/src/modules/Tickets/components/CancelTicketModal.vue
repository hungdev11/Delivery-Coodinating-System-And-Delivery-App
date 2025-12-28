<script setup lang="ts">
/**
 * Cancel Ticket Modal
 *
 * Modal for cancelling tickets
 */

import { ref } from 'vue'
import type { TicketDto } from '../model.type'

interface Props {
  ticket: TicketDto
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: [result: boolean] }>()

const handleConfirm = () => {
  emit('close', true)
}

const handleCancel = () => {
  emit('close', false)
}
</script>

<template>
  <UModal
    title="Cancel Ticket"
    description="Are you sure you want to cancel this ticket?"
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
          <p v-if="ticket.description" class="text-sm text-gray-600 dark:text-gray-400 mt-2">
            <strong>Description:</strong> {{ ticket.description }}
          </p>
        </div>

        <UAlert
          color="warning"
          variant="soft"
          title="Warning"
          description="This action cannot be undone. The ticket will be marked as cancelled."
        />
      </div>
    </template>

    <template #footer>
      <UButton variant="ghost" @click="handleCancel">Cancel</UButton>
      <UButton color="error" @click="handleConfirm">Cancel Ticket</UButton>
    </template>
  </UModal>
</template>
