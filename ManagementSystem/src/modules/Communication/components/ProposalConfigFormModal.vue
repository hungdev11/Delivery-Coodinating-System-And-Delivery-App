<script setup lang="ts">
/**
 * Proposal Config Form Modal
 *
 * Modal for creating/editing proposal configurations
 * Usage with useOverlay()
 */

import { ref, computed, watch } from 'vue'
import type {
  ProposalConfigDTO,
  ProposalTypeConfig,
  ProposalType,
  ProposalActionType,
} from '../model.type'

interface Props {
  mode: 'create' | 'edit'
  config?: ProposalTypeConfig
}

const props = defineProps<Props>()
const emit = defineEmits<{
  result: [ProposalConfigDTO | null]
}>()

// Form data
const formData = ref<ProposalConfigDTO>({
  type: 'CONFIRM_REFUSAL' as ProposalType,
  requiredRole: '',
  actionType: 'ACCEPT_DECLINE' as ProposalActionType,
  template: '',
  description: '',
})

// Proposal types
const proposalTypes: ProposalType[] = ['CONFIRM_REFUSAL', 'POSTPONE_REQUEST', 'DELAY_ORDER_RECEIVE']

// Action types
const actionTypes: ProposalActionType[] = ['ACCEPT_DECLINE', 'DATE_PICKER', 'TEXT_INPUT', 'CHOICE']

// Initialize form data
watch(
  () => props.config,
  (config) => {
    if (config && props.mode === 'edit') {
      formData.value = {
        type: config.type,
        requiredRole: config.requiredRole,
        actionType: config.creationActionType || 'ACCEPT_DECLINE', // Use creationActionType from config
        template: '', // Template not in ProposalTypeConfig, leave empty or fetch separately
        description: config.description || '',
      }
    } else {
      // Reset form for create mode
      formData.value = {
        type: 'CONFIRM_REFUSAL' as ProposalType,
        requiredRole: '',
        actionType: 'ACCEPT_DECLINE' as ProposalActionType,
        template: '',
        description: '',
      }
    }
  },
  { immediate: true },
)

// Form validation
const isValid = computed(() => {
  return (
    formData.value.type.trim() !== '' &&
    formData.value.requiredRole.trim() !== '' &&
    formData.value.actionType.trim() !== ''
  )
})

// Handle submit
const handleSubmit = () => {
  if (isValid.value) {
    emit('result', { ...formData.value })
  }
}

// Handle cancel
const handleCancel = () => {
  emit('result', null)
}

// Format template as JSON example
const templatePlaceholder = computed(() => {
  if (formData.value.type === 'POSTPONE_REQUEST') {
    return '{"title": "Postpone Request", "message": "Please select new date"}'
  }
  if (formData.value.type === 'DELAY_ORDER_RECEIVE') {
    return '{"title": "Delay Order Receive", "message": "Please provide reason"}'
  }
  return '{"title": "Confirm Refusal", "message": "Are you sure?"}'
})
</script>

<template>
  <UModal
    :title="mode === 'create' ? 'Create Proposal Configuration' : 'Edit Proposal Configuration'"
    :description="
      mode === 'create' ? 'Configure a new proposal type' : 'Update the proposal configuration'
    "
    :close="{ onClick: handleCancel }"
    :ui="{ content: 'sm:max-w-md md:max-w-lg', footer: 'justify-end' }"
  >
    <template #body>
      <form @submit.prevent="handleSubmit" class="space-y-4">
        <UFormField label="Type" required>
          <USelect
            v-model="formData.type"
            :items="proposalTypes.map((t) => ({ label: t, value: t }))"
            placeholder="Select proposal type"
            class="w-full"
          />
        </UFormField>

        <UFormField label="Required Role" required>
          <UInput
            v-model="formData.requiredRole"
            placeholder="e.g., ADMIN, CLIENT, SHIPPER"
            class="w-full"
          />
        </UFormField>

        <UFormField label="Action Type" required>
          <USelect
            v-model="formData.actionType"
            :items="actionTypes.map((t) => ({ label: t, value: t }))"
            placeholder="Select action type"
            class="w-full"
          />
        </UFormField>

        <UFormField label="Template (JSON)" required>
          <UTextarea
            v-model="formData.template"
            :placeholder="templatePlaceholder"
            :rows="5"
            class="w-full font-mono text-sm"
          />
          <template #help>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              JSON template for the proposal. Example: {{ templatePlaceholder }}
            </p>
          </template>
        </UFormField>

        <UFormField label="Description">
          <UInput
            v-model="formData.description"
            placeholder="Optional description"
            class="w-full"
          />
        </UFormField>
      </form>
    </template>

    <template #footer>
      <UButton variant="outline" color="neutral" @click="handleCancel"> Cancel </UButton>
      <UButton :disabled="!isValid" @click="handleSubmit"> Save </UButton>
    </template>
  </UModal>
</template>
