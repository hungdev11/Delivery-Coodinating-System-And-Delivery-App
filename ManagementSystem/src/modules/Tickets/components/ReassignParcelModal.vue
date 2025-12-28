<script setup lang="ts">
/**
 * Reassign Parcel Modal
 *
 * Modal for reassigning parcels to different delivery assignments
 */

import { ref } from 'vue'
import type { TicketDto, ReassignParcelRequest } from '../model.type'

interface Props {
  ticket: TicketDto
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: [result: ReassignParcelRequest | null] }>()

const form = ref<ReassignParcelRequest>({
  deliveryAssignmentId: '',
  notes: '',
})

const submitting = ref(false)

const handleSubmit = async () => {
  if (!form.value.deliveryAssignmentId.trim()) {
    return
  }

  submitting.value = true
  try {
    emit('close', form.value)
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
    title="Reassign Parcel"
    description="Reassign parcel to a different delivery assignment"
    :close="{ onClick: handleCancel }"
  >
    <template #body>
      <div class="space-y-4">
        <div>
          <p class="text-sm text-gray-600 dark:text-gray-400">
            <strong>Ticket ID:</strong> {{ ticket.id }}
          </p>
          <p class="text-sm text-gray-600 dark:text-gray-400">
            <strong>Parcel ID:</strong> {{ ticket.parcelId }}
          </p>
          <p v-if="ticket.deliveryAssignmentId" class="text-sm text-gray-600 dark:text-gray-400">
            <strong>Current Assignment:</strong> {{ ticket.deliveryAssignmentId }}
          </p>
        </div>

        <UFormGroup label="New Delivery Assignment ID" name="deliveryAssignmentId" required>
          <UInput
            v-model="form.deliveryAssignmentId"
            placeholder="Enter delivery assignment ID"
          />
        </UFormGroup>

        <UFormGroup label="Notes" name="notes">
          <UTextarea
            v-model="form.notes"
            placeholder="Enter notes about the reassignment..."
            :rows="3"
          />
        </UFormGroup>
      </div>
    </template>

    <template #footer>
      <UButton variant="ghost" @click="handleCancel">Cancel</UButton>
      <UButton
        color="primary"
        :loading="submitting"
        :disabled="!form.deliveryAssignmentId.trim()"
        @click="handleSubmit"
      >
        Reassign
      </UButton>
    </template>
  </UModal>
</template>
