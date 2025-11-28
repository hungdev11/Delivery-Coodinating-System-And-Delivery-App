<script setup lang="ts">
/**
 * Sorting Drawer Component for Mobile
 *
 * Allows multiple column selection with order control using UDrawer
 */

import { ref, computed, watch } from 'vue'
import type { SortingState } from '@tanstack/table-core'
import { useResponsiveStore } from '@/common/store/responsive.store'

interface Props {
  modelValue: boolean
  sorting: SortingState | Array<{ id: string; desc: boolean }>
  sortableColumns: Array<{ id: string; label: string }>
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'update:sorting', value: SortingState | Array<{ id: string; desc: boolean }>): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const responsiveStore = useResponsiveStore()

// Drawer direction: top for mobile/Android, bottom for desktop
const drawerDirection = computed(() => {
  return responsiveStore.isMobile || responsiveStore.isAndroid ? 'top' : 'bottom'
})

// Local state for sorting selection (ordered list)
const selectedSorts = ref<Array<{ id: string; desc: boolean; order: number }>>([])

// Sync with modelValue to control drawer open/close
const isOpen = computed(() => props.modelValue)

// Initialize from props when drawer opens
watch(
  () => props.modelValue,
  (isOpen) => {
    if (isOpen) {
      // Load sorting when opening drawer
      if (props.sorting && props.sorting.length > 0) {
        selectedSorts.value = props.sorting.map((sort, index) => ({
          id: sort.id,
          desc: sort.desc,
          order: index + 1,
        }))
      } else {
        selectedSorts.value = []
      }
    }
    // Don't reset when closing - keep the state for next open
  },
)

// Also watch sorting changes while drawer is open
watch(
  () => props.sorting,
  (newSorting) => {
    // Only update if drawer is open
    if (props.modelValue) {
      if (newSorting && newSorting.length > 0) {
        selectedSorts.value = newSorting.map((sort, index) => ({
          id: sort.id,
          desc: sort.desc,
          order: index + 1,
        }))
      } else {
        selectedSorts.value = []
      }
    }
  },
  { deep: true },
)

// Available columns (excluding already selected ones)
const availableColumns = computed(() => {
  const selectedIds = selectedSorts.value.map((s) => s.id)
  return props.sortableColumns.filter((col) => !selectedIds.includes(col.id))
})

// Handle add column to sorting
const addColumn = (columnId: string) => {
  const column = props.sortableColumns.find((col) => col.id === columnId)
  if (column) {
    const newOrder = selectedSorts.value.length + 1
    selectedSorts.value.push({
      id: columnId,
      desc: false,
      order: newOrder,
    })
  }
}

// Handle remove column from sorting
const removeColumn = (columnId: string) => {
  selectedSorts.value = selectedSorts.value
    .filter((s) => s.id !== columnId)
    .map((s, index) => ({ ...s, order: index + 1 }))
}

// Handle toggle direction
const toggleDirection = (columnId: string) => {
  const sort = selectedSorts.value.find((s) => s.id === columnId)
  if (sort) {
    sort.desc = !sort.desc
  }
}

// Handle move up
const moveUp = (columnId: string) => {
  const index = selectedSorts.value.findIndex((s) => s.id === columnId)
  if (index > 0) {
    const temp = selectedSorts.value[index]
    selectedSorts.value[index] = selectedSorts.value[index - 1]
    selectedSorts.value[index - 1] = temp
    // Reorder
    selectedSorts.value = selectedSorts.value.map((s, i) => ({ ...s, order: i + 1 }))
  }
}

// Handle move down
const moveDown = (columnId: string) => {
  const index = selectedSorts.value.findIndex((s) => s.id === columnId)
  if (index < selectedSorts.value.length - 1) {
    const temp = selectedSorts.value[index]
    selectedSorts.value[index] = selectedSorts.value[index + 1]
    selectedSorts.value[index + 1] = temp
    // Reorder
    selectedSorts.value = selectedSorts.value.map((s, i) => ({ ...s, order: i + 1 }))
  }
}

// Handle close
const close = () => {
  emit('update:modelValue', false)
}

// Handle apply
const handleApply = () => {
  const newSorting = selectedSorts.value.map((s) => ({ id: s.id, desc: s.desc }))
  emit('update:sorting', newSorting)
  close()
}

// Handle cancel
const handleCancel = () => {
  // Reset to original sorting
  if (props.sorting && props.sorting.length > 0) {
    selectedSorts.value = props.sorting.map((sort, index) => ({
      id: sort.id,
      desc: sort.desc,
      order: index + 1,
    }))
  } else {
    selectedSorts.value = []
  }
  close()
}

// Handle clear
const handleClear = () => {
  selectedSorts.value = []
  emit('update:sorting', [])
  close()
}
</script>

<template>
  <div>
    <UDrawer
      :open="isOpen"
      :direction="drawerDirection"
      :dismissible="false"
      :handle="false"
      title="Sorting"
      description="Select columns to sort data in order. You can select multiple columns and sort them in order of priority."
      :ui="{ header: 'flex items-center justify-between' }"
      @close="close"
    >
    <template #header>
      <div class="flex items-center justify-between w-full">
        <h2 class="text-highlighted font-semibold text-lg">Sorting</h2>
        <UButton color="neutral" variant="ghost" icon="i-heroicons-x-mark" @click="close" />
      </div>
    </template>

    <template #body>
      <div class="space-y-4">
        <!-- Selected Sorts (Ordered) -->
        <div v-if="selectedSorts.length > 0" class="space-y-3">
          <h4 class="text-base font-semibold text-gray-700 dark:text-gray-300">Sorting order:</h4>
          <div class="space-y-3">
            <div
              v-for="sort in selectedSorts.sort((a, b) => a.order - b.order)"
              :key="sort.id"
              class="flex items-center gap-3 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg"
            >
              <div class="flex-shrink-0 w-8 h-8 rounded-full bg-primary-500 text-white flex items-center justify-center text-sm font-semibold">
                {{ sort.order }}
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-base font-medium text-gray-900 dark:text-gray-100">
                  {{ sortableColumns.find((col) => col.id === sort.id)?.label || sort.id }}
                </p>
              </div>
              <div class="flex items-center gap-2">
                <UButton
                  size="sm"
                  variant="soft"
                  :color="sort.desc ? 'error' : 'success'"
                  class="px-3"
                  @click="toggleDirection(sort.id)"
                >
                  <UIcon
                    :name="sort.desc ? 'i-lucide-arrow-down-wide-narrow' : 'i-lucide-arrow-up-narrow-wide'"
                    class="w-5 h-5"
                  />
                </UButton>
                <UButton
                  size="sm"
                  variant="ghost"
                  icon="i-lucide-arrow-up"
                  :disabled="sort.order === 1"
                  @click="moveUp(sort.id)"
                />
                <UButton
                  size="sm"
                  variant="ghost"
                  icon="i-lucide-arrow-down"
                  :disabled="sort.order === selectedSorts.length"
                  @click="moveDown(sort.id)"
                />
                <UButton
                  size="sm"
                  variant="ghost"
                  color="error"
                  icon="i-heroicons-trash"
                  @click="removeColumn(sort.id)"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Available Columns -->
        <div v-if="availableColumns.length > 0" class="space-y-3">
          <h4 class="text-base font-semibold text-gray-700 dark:text-gray-300">Select column:</h4>
          <USelect
            :model-value="undefined"
            :items="availableColumns.map((col) => ({ label: col.label, value: col.id }))"
            placeholder="Select column to add..."
            size="lg"
            class="text-base"
            @update:model-value="(value) => value !== undefined && addColumn(value as string)"
          />
        </div>

        <div v-else-if="selectedSorts.length === 0" class="text-center py-12 text-gray-500">
          <p class="text-base">No columns selected</p>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-between gap-2">
        <UButton variant="ghost" color="error" @click="handleClear" :disabled="selectedSorts.length === 0">
          Clear all
        </UButton>
        <div class="flex gap-2">
          <UButton variant="outline" @click="handleCancel"> Cancel </UButton>
          <UButton @click="handleApply"> Apply </UButton>
        </div>
      </div>
    </template>
    </UDrawer>
  </div>
</template>
