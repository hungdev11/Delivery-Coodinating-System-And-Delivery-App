<script setup lang="ts">
/**
 * Proposal Config View
 *
 * Admin view for managing proposal configurations
 */

import { onMounted } from 'vue'
import { useProposals } from './composables'
import type { ProposalTypeConfig } from './model.type'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import { defineAsyncComponent } from 'vue'

const LazyProposalConfigFormModal = defineAsyncComponent(
  () => import('./components/ProposalConfigFormModal.vue'),
)
const LazyProposalConfigDeleteModal = defineAsyncComponent(
  () => import('./components/ProposalConfigDelete.vue'),
)
const PageHeader = defineAsyncComponent(() => import('@/common/components/PageHeader.vue'))

const overlay = useOverlay()
const { proposalConfigs, loading, loadProposalConfigs, createConfig, updateConfig, deleteConfig } =
  useProposals()

/**
 * Load proposal configs on mount
 */
onMounted(async () => {
  await loadProposalConfigs()
})

/**
 * Handle create config
 */
const handleCreate = async () => {
  const modal = overlay.create(LazyProposalConfigFormModal)
  const instance = modal.open({
    mode: 'create',
  })
  const result = await instance.result
  if (result) {
    await createConfig(result)
  }
}

/**
 * Handle edit config
 */
const handleEdit = async (config: ProposalTypeConfig) => {
  const modal = overlay.create(LazyProposalConfigFormModal)
  const instance = modal.open({
    mode: 'edit',
    config: config,
  })
  const result = await instance.result
  if (result) {
    await updateConfig(config.id, result)
  }
}

/**
 * Handle delete config
 */
const handleDelete = async (config: ProposalTypeConfig) => {
  const modal = overlay.create(LazyProposalConfigDeleteModal)
  const instance = modal.open({
    config: config,
  })
  const result = await instance.result
  if (result) {
    await deleteConfig(config.id)
  }
}
</script>

<template>
  <div class="container mx-auto px-2 sm:px-4 py-4 sm:py-6">
    <PageHeader title="Proposal Configurations" description="Manage proposal type configurations">
      <template #actions>
        <UButton icon="i-heroicons-plus" size="sm" class="md:size-md" @click="handleCreate">
          <span class="hidden sm:inline">Create Config</span>
          <span class="sm:hidden">Create</span>
        </UButton>
      </template>
    </PageHeader>

    <div v-if="loading" class="flex items-center justify-center py-12">
      <UIcon name="i-heroicons-arrow-path" class="animate-spin text-2xl" />
    </div>

    <div v-else-if="proposalConfigs.length === 0" class="text-center py-12">
      <div class="mx-auto h-12 w-12 text-gray-400">
        <UIcon name="i-heroicons-cog-6-tooth" class="h-12 w-12" />
      </div>
      <h3 class="mt-2 text-sm font-medium text-gray-900 dark:text-gray-100">
        No configurations found
      </h3>
      <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
        Get started by creating a new proposal configuration.
      </p>
      <div class="mt-6">
        <UButton icon="i-heroicons-plus" @click="handleCreate"> Add Configuration </UButton>
      </div>
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <UCard v-for="config in proposalConfigs" :key="config.id" class="overflow-hidden">
        <template #header>
          <div class="flex items-center justify-between">
            <h3 class="font-semibold text-lg">{{ config.type }}</h3>
            <UBadge color="primary" variant="soft" size="sm">
              {{ config.requiredRole }}
            </UBadge>
          </div>
        </template>

        <div class="space-y-3">
          <p class="text-sm text-gray-600 dark:text-gray-400">
            {{ config.description || 'No description' }}
          </p>

          <div class="space-y-2 text-sm">
            <div class="flex items-center justify-between">
              <span class="text-gray-500 dark:text-gray-400">Creation Action:</span>
              <UBadge color="primary" variant="outline" size="xs">
                {{ config.creationActionType }}
              </UBadge>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-gray-500 dark:text-gray-400">Response Action:</span>
              <UBadge color="secondary" variant="outline" size="xs">
                {{ config.responseActionType }}
              </UBadge>
            </div>
            <div v-if="config.defaultTimeoutMinutes" class="flex items-center justify-between">
              <span class="text-gray-500 dark:text-gray-400">Timeout:</span>
              <span class="font-medium">{{ config.defaultTimeoutMinutes }} min</span>
            </div>
          </div>
        </div>

        <template #footer>
          <div class="flex items-center justify-end gap-2">
            <UButton
              icon="i-heroicons-pencil"
              variant="ghost"
              size="sm"
              @click="handleEdit(config)"
            >
              <span class="hidden sm:inline">Edit</span>
              <span class="sm:hidden">Edit</span>
            </UButton>
            <UButton
              icon="i-heroicons-trash"
              variant="ghost"
              color="error"
              size="sm"
              @click="handleDelete(config)"
            >
              <span class="hidden sm:inline">Delete</span>
              <span class="sm:hidden">Delete</span>
            </UButton>
          </div>
        </template>
      </UCard>
    </div>
  </div>
</template>
