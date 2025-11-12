<script setup lang="ts">
/**
 * Proposal Menu Component
 *
 * Popover menu for selecting proposal types
 */

import type { ProposalTypeConfig } from '../model.type'
import { ref } from 'vue';

interface Props {
  availableConfigs: ProposalTypeConfig[]
  loading: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  select: [config: ProposalTypeConfig]
}>()

const showMenu = ref(false)

/**
 * Handle proposal selection
 */
const handleSelect = (config: ProposalTypeConfig) => {
  emit('select', config)
  showMenu.value = false
}
</script>

<template>
  <UPopover v-model:open="showMenu">
    <UButton
      icon="i-heroicons-paper-clip"
      variant="ghost"
      :disabled="props.availableConfigs.length === 0 || props.loading"
    />
    <template #content>
      <div class="p-2 min-w-[200px]">
        <div
          v-if="props.availableConfigs.length === 0"
          class="p-2 text-sm text-gray-500"
        >
          No proposals available
        </div>
        <div v-else class="space-y-1">
          <UButton
            v-for="config in props.availableConfigs"
            :key="config.id"
            block
            variant="ghost"
            class="justify-start"
            @click="handleSelect(config)"
          >
            {{ config.description || config.type }}
          </UButton>
        </div>
      </div>
    </template>
  </UPopover>
</template>
