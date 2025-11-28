<script setup lang="ts">
/**
 * Proposal Config View
 *
 * Admin view for managing proposal configurations
 */

import { onMounted, ref } from 'vue'
import { useProposals } from './composables'
import type { ProposalConfigDTO, ProposalTypeConfig } from './model.type'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'

const overlay = useOverlay()
const { proposalConfigs, loading, loadProposalConfigs, createConfig, updateConfig, deleteConfig } =
  useProposals()

const editingConfig = ref<ProposalTypeConfig | null>(null)
const showForm = ref(false)

/**
 * Load proposal configs on mount
 */
onMounted(async () => {
  await loadProposalConfigs()
})

/**
 * Handle create config
 */
const handleCreate = () => {
  editingConfig.value = null
  showForm.value = true
}

/**
 * Handle edit config
 */
const handleEdit = (config: ProposalTypeConfig) => {
  editingConfig.value = config
  showForm.value = true
}

/**
 * Handle delete config
 */
const handleDelete = async (config: ProposalTypeConfig) => {
  if (confirm(`Are you sure you want to delete proposal config "${config.type}"?`)) {
    await deleteConfig(config.id)
  }
}

/**
 * Handle save config
 */
const handleSave = async (data: ProposalConfigDTO) => {
  if (editingConfig.value) {
    await updateConfig(editingConfig.value.id, data)
  } else {
    await createConfig(data)
  }
  showForm.value = false
  editingConfig.value = null
}

/**
 * Handle cancel
 */
const handleCancel = () => {
  showForm.value = false
  editingConfig.value = null
}
</script>

<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-semibold">Proposal Configurations</h1>
      <UButton icon="i-heroicons-plus" @click="handleCreate"> Create Config </UButton>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-12">
      <UIcon name="i-heroicons-arrow-path" class="animate-spin text-2xl" />
    </div>

    <div v-else-if="proposalConfigs.length === 0" class="text-center py-12">
      <p class="text-gray-500">No proposal configurations found</p>
    </div>

    <div v-else class="space-y-4">
      <UCard v-for="config in proposalConfigs" :key="config.id" class="p-4">
        <div class="flex items-center justify-between">
          <div class="flex-1">
            <h3 class="font-semibold text-lg">{{ config.type }}</h3>
            <p class="text-sm text-gray-500 mt-1">{{ config.description || 'No description' }}</p>
            <div class="mt-2 flex items-center space-x-4 text-sm">
              <span class="text-gray-600">
                <strong>Required Role:</strong> {{ config.requiredRole }}
              </span>
              <span class="text-gray-600">
                <strong>Action Type:</strong> {{ config.actionType }}
              </span>
            </div>
          </div>
          <div class="flex items-center space-x-2">
            <UButton
              icon="i-heroicons-pencil"
              variant="ghost"
              size="sm"
              @click="handleEdit(config)"
            >
              Edit
            </UButton>
            <UButton
              icon="i-heroicons-trash"
              variant="ghost"
              color="red"
              size="sm"
              @click="handleDelete(config)"
            >
              Delete
            </UButton>
          </div>
        </div>
      </UCard>
    </div>

    <!-- Form Modal -->
    <UModal v-model="showForm">
        <template #header>
          <h3 class="text-lg font-semibold">
            {{ editingConfig ? 'Edit Proposal Config' : 'Create Proposal Config' }}
          </h3>
        </template>

        <template #body>
          <div class="space-y-4">
            <UFormField label="Type" required>
              <USelect
                :model-value="editingConfig?.type || ''"
                :items="['CONFIRM_REFUSAL', 'POSTPONE_REQUEST', 'DELAY_ORDER_RECEIVE']"
                placeholder="Select type"
              />
            </UFormField>

            <UFormField label="Required Role" required>
              <UInput
                :model-value="editingConfig?.requiredRole || ''"
                placeholder="e.g., ADMIN, CLIENT, SHIPPER"
              />
            </UFormField>

            <UFormField label="Action Type" required>
              <USelect
                :model-value="editingConfig?.actionType || ''"
                :items="['ACCEPT_DECLINE', 'DATE_PICKER', 'TEXT_INPUT', 'CHOICE']"
                placeholder="Select action type"
              />
            </UFormField>

            <UFormField label="Template (JSON)" required>
              <UTextarea
                :model-value="editingConfig?.template || ''"
                placeholder='{"title": "Confirm Refusal", "message": "Are you sure?"}'
                rows="5"
              />
            </UFormField>

            <UFormField label="Description">
              <UInput
                :model-value="editingConfig?.description || ''"
                placeholder="Optional description"
              />
            </UFormField>
          </div>
        </template>

        <template #footer>
          <div class="flex justify-end space-x-2">
            <UButton variant="ghost" @click="handleCancel"> Cancel </UButton>
            <UButton @click="handleSave"> Save </UButton>
          </div>
        </template>
    </UModal>
  </div>
</template>
