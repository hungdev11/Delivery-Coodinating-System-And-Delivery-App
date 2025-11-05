<template>
  <div class="filter-bar space-y-4">
    <!-- Active Filters Display -->
    <div v-if="(activeFilters?.length || 0) > 0" class="mb-4">
      <div class="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400 mb-2">
        <span>Active filters:</span>
        <UButton
          variant="ghost"
          size="xs"
          color="neutral"
          icon="i-heroicons-x-mark"
          @click="clearAllFilters"
          title="Clear all filters"
        />
      </div>

      <div class="flex flex-wrap gap-2">
        <UBadge
          v-for="filter in (activeFilters || [])"
          :key="filter.id"
          :color="getFilterColor(filter)"
          variant="soft"
          size="sm"
          class="flex items-center gap-1"
        >
          <span class="font-medium">{{ getColumnLabel(filter.field) }}</span>
          <span class="text-xs">{{ getOperatorLabel(filter.operator) }}</span>
          <span v-if="filter.value !== undefined" class="text-xs">
            {{ formatFilterValue(filter) }}
          </span>
          <UButton
            icon="i-heroicons-x-mark"
            size="xs"
            variant="ghost"
            color="neutral"
            @click="removeFilter(filter.id || '')"
            class="ml-1"
          />
        </UBadge>
      </div>
    </div>

    <!-- Column Filters -->
    <div class="flex flex-wrap gap-2">
      <div v-for="column in filterableColumns" :key="column.field" class="flex items-center gap-2">
        <span class="text-sm font-medium text-gray-700 dark:text-gray-300">
          {{ column.label }}:
        </span>
        <ColumnFilter
          :column="column"
          :active-filters="getColumnFilters(column.field)"
          @update:filters="(filters) => updateColumnFilters(column.field, filters)"
        />
      </div>
    </div>

    <!-- Advanced Filter Toggle -->
    <div class="mt-4 flex items-center justify-between">
      <UButton
        variant="soft"
        color="primary"
        icon="i-heroicons-cog-6-tooth"
        @click="toggleAdvanced"
      >
        {{ showAdvanced ? 'Hide' : 'Show' }} Advanced Filters
      </UButton>

      <div v-if="(activeFilters?.length || 0) > 0" class="text-xs text-gray-500 dark:text-gray-400">
        {{ activeFilters?.length || 0 }} filter{{ (activeFilters?.length || 0) === 1 ? '' : 's' }} active
      </div>
    </div>

    <!-- Advanced Filter Drawer -->
    <AdvancedFilterDrawer
      :show="showAdvancedModal"
      :columns="columns"
      :active-filters="activeFilters || []"
      @apply="handleAdvancedApply"
      @clear="handleAdvancedClear"
    />
  </div>
</template>

<script setup lang="ts" generic="TData">
import { computed, ref } from 'vue'
import type { FilterableColumn, FilterCondition, FilterGroup } from '../../types/filter'
import { createEmptyFilterGroup } from '../../utils/query-builder'
import ColumnFilter from './ColumnFilter.vue'
import AdvancedFilterDrawer from './AdvancedFilterDrawer.vue'

interface Props {
  columns: FilterableColumn[]
  activeFilters?: FilterCondition[] | undefined
  showAdvanced?: boolean
}

interface Emits {
  (e: 'update:filters', filters: FilterGroup): void
  (e: 'toggle:advanced'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// Local state
const columnFilters = ref<Record<string, FilterCondition[]>>({})
const showAdvancedModal = ref(false)

// Computed properties
const filterableColumns = computed(() => {
  return props.columns.filter((col) => col.type !== 'enum' && col.filterable !== false)
})

// Methods
function getColumnLabel(field: string): string {
  const column = filterableColumns.value.find((col) => col.field === field)
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

function formatFilterValue(filter: FilterCondition): string {
  if (filter.operator === 'between' && Array.isArray(filter.value)) {
    return `${filter.value[0]} - ${filter.value[1]}`
  }

  if (
    ['in', 'notIn', 'containsAny', 'containsAll'].includes(filter.operator) &&
    Array.isArray(filter.value)
  ) {
    return filter.value.join(', ')
  }

  if (typeof filter.value === 'boolean') {
    return filter.value ? 'Yes' : 'No'
  }

  if (filter.value === null || filter.value === undefined) {
    return ''
  }

  return String(filter.value)
}

type ColorSet = 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'

function getFilterColor(filter: FilterCondition): ColorSet {
  const colors: Record<string, ColorSet> = {
    eq: 'primary',
    ne: 'error',
    contains: 'info',
    startsWith: 'info',
    endsWith: 'info',
    regex: 'warning',
    in: 'success',
    notIn: 'error',
    gt: 'warning',
    gte: 'warning',
    lt: 'warning',
    lte: 'warning',
    between: 'info',
    isNull: 'neutral',
    isNotNull: 'neutral',
    containsAny: 'success',
    containsAll: 'success',
    isEmpty: 'neutral',
    isNotEmpty: 'neutral',
  }
  return colors[filter.operator] || 'neutral'
}

function getColumnFilters(field: string): FilterCondition[] {
  return columnFilters.value[field] || []
}

function updateColumnFilters(field: string, filters: FilterCondition[]) {
  if (filters.length === 0) {
    delete columnFilters.value[field]
  } else {
    columnFilters.value[field] = filters
  }

  // Emit combined filters as FilterGroup
  const allFilters = Object.values(columnFilters.value).flat()
  const filterGroup: FilterGroup = {
    logic: 'AND',
    conditions: allFilters,
  }

  emit('update:filters', filterGroup)
}

function removeFilter(filterId: string) {
  // Find and remove the filter from columnFilters
  for (const [field, filters] of Object.entries(columnFilters.value)) {
    const index = filters.findIndex((f) => f.id === filterId)
    if (index !== -1) {
      filters.splice(index, 1)
      if (filters.length === 0) {
        delete columnFilters.value[field]
      }
      break
    }
  }

  // Emit updated filters
  const allFilters = Object.values(columnFilters.value).flat()
  const filterGroup: FilterGroup = {
    logic: 'AND',
    conditions: allFilters,
  }

  emit('update:filters', filterGroup)
}

function clearAllFilters() {
  columnFilters.value = {}
  emit('update:filters', createEmptyFilterGroup())
}

function toggleAdvanced() {
  showAdvancedModal.value = true
  emit('toggle:advanced')
}

function handleAdvancedApply(filterGroup: FilterGroup) {
  emit('update:filters', filterGroup)
  showAdvancedModal.value = false
}

function handleAdvancedClear() {
  emit('update:filters', createEmptyFilterGroup())
  showAdvancedModal.value = false
}

// Initialize columnFilters from props
function initializeColumnFilters() {
  const filtersByColumn: Record<string, FilterCondition[]> = Object.create(null)

  (props.activeFilters || []).forEach((filter: FilterCondition) => {
    if (!filtersByColumn[filter.field]) {
      filtersByColumn[filter.field] = []
    }
    filtersByColumn[filter.field].push(filter)
  })

  columnFilters.value = filtersByColumn
}

// Initialize on mount
initializeColumnFilters()
</script>
