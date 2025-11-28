<script setup lang="ts">
/**
 * Bulk Actions Component
 *
 * Shared component for bulk actions on selected table rows
 * - Fixed size (no layout shift)
 * - Hidden on Android (mobile-first)
 * - Always visible on desktop (even when no selection, shows disabled state)
 */

import { computed } from 'vue'
import { useResponsiveStore } from '@/common/store/responsive.store'

interface Props {
  selectedCount?: number
  totalCount?: number
  onExport?: () => void
  onDelete?: () => void
}

interface Emits {
  (e: 'export'): void
  (e: 'delete'): void
}

const props = withDefaults(defineProps<Props>(), {
  selectedCount: 0,
  totalCount: 0,
  onExport: undefined,
  onDelete: undefined,
})

const emit = defineEmits<Emits>()

const responsiveStore = useResponsiveStore()

// Check if has selection
const hasSelection = computed(() => props.selectedCount > 0)

// Handle export
const handleExport = () => {
  if (hasSelection.value && props.onExport) {
    props.onExport()
  } else {
    emit('export')
  }
}

// Handle delete
const handleDelete = () => {
  if (hasSelection.value && props.onDelete) {
    props.onDelete()
  } else {
    emit('delete')
  }
}
</script>

<template>
  <!-- Fixed height container - Hidden on mobile, visible on desktop/tablet -->
  <div
    v-if="!responsiveStore.isMobile"
    class="mb-4 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg min-h-[60px] flex items-center"
    :class="{ 'opacity-50': !hasSelection }"
  >
    <div class="flex items-center justify-between w-full">
      <span class="text-sm text-gray-600 dark:text-gray-400">
        <template v-if="hasSelection">
          {{ selectedCount }} of {{ totalCount }} row(s) selected.
        </template>
        <template v-else>
          No rows selected.
        </template>
      </span>
      <div class="flex space-x-2">
        <UButton
          size="sm"
          variant="soft"
          icon="i-heroicons-arrow-down-tray"
          :disabled="!hasSelection"
          @click="handleExport"
        >
          Export
        </UButton>
        <UButton
          size="sm"
          variant="soft"
          color="error"
          icon="i-heroicons-trash"
          :disabled="!hasSelection"
          @click="handleDelete"
        >
          Delete
        </UButton>
      </div>
    </div>
  </div>
</template>
