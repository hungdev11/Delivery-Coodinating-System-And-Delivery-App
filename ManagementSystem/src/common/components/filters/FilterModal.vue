<template>
  <UModal v-model="isOpen" :ui="{ width: 'w-full sm:max-w-4xl' }">
    <UCard>
      <template #header>
        <div class="flex items-center justify-between">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100">Advanced Filters</h3>
          <UButton icon="i-heroicons-x-mark" variant="ghost" size="sm" @click="close" />
        </div>
      </template>

      <div class="space-y-6">
        <!-- Filter Builder Placeholder -->
        <div
          class="min-h-[400px] border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-lg p-8 text-center"
        >
          <div class="space-y-4">
            <UIcon name="i-heroicons-funnel" class="h-12 w-12 text-gray-400 mx-auto" />
            <h4 class="text-lg font-medium text-gray-900 dark:text-gray-100">
              Advanced Filter Builder
            </h4>
            <p class="text-sm text-gray-500 dark:text-gray-400">
              Drag-and-drop filter builder with nested groups will be implemented here.
            </p>
            <p class="text-xs text-gray-400 dark:text-gray-500">
              This will support complex queries like: (status = 'ACTIVE' AND (email contains
              '@gmail.com' OR email contains '@yahoo.com'))
            </p>
          </div>
        </div>

        <!-- Current Filters Display -->
        <div v-if="activeFilters.length > 0" class="space-y-2">
          <h4 class="text-sm font-medium text-gray-700 dark:text-gray-300">Current Filters:</h4>
          <div class="flex flex-wrap gap-2">
            <UBadge
              v-for="filter in activeFilters"
              :key="filter.id"
              color="blue"
              variant="soft"
              size="sm"
            >
              {{ getColumnLabel(filter.field) }} {{ getOperatorLabel(filter.operator) }}
              {{ formatValue(filter.value) }}
            </UBadge>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end space-x-2">
          <UButton variant="ghost" @click="close"> Cancel </UButton>
          <UButton variant="soft" color="neutral" @click="clearAll"> Clear All </UButton>
          <UButton @click="apply"> Apply Filters </UButton>
        </div>
      </template>
    </UCard>
  </UModal>
</template>

<script setup lang="ts" generic="TData">
import { computed } from 'vue'
import type { FilterableColumn, FilterCondition } from '@/common/types/filter'

interface Props {
  show: boolean
  columns: FilterableColumn[]
  activeFilters: FilterCondition[]
}

interface Emits {
  (e: 'update:show', value: boolean): void
  (e: 'apply', filters: FilterCondition[]): void
  (e: 'clear'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const isOpen = computed({
  get: () => props.show,
  set: (value) => emit('update:show', value),
})

function close() {
  emit('update:show', false)
}

function apply() {
  // For now, just pass through the current filters
  // In the full implementation, this would build the filter group from the UI
  emit('apply', props.activeFilters)
  close()
}

function clearAll() {
  emit('clear')
}

function getColumnLabel(field: string): string {
  const column = props.columns.find((col) => col.field === field)
  return column?.label || field
}

function getOperatorLabel(operator: string): string {
  const labels: Record<string, string> = {
    eq: '=',
    ne: '≠',
    contains: 'contains',
    startsWith: 'starts with',
    endsWith: 'ends with',
    regex: 'regex',
    in: 'in',
    notIn: 'not in',
    gt: '>',
    gte: '≥',
    lt: '<',
    lte: '≤',
    between: 'between',
    isNull: 'is null',
    isNotNull: 'is not null',
    containsAny: 'contains any',
    containsAll: 'contains all',
    isEmpty: 'is empty',
    isNotEmpty: 'is not empty',
  }
  return labels[operator] || operator
}

function formatValue(value: any): string {
  if (Array.isArray(value)) {
    return value.join(', ')
  }
  if (typeof value === 'boolean') {
    return value ? 'Yes' : 'No'
  }
  if (value === null || value === undefined) {
    return ''
  }
  return String(value)
}
</script>
