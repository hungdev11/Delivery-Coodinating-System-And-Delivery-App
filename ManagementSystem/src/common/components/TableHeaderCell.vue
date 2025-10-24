<template>
  <div class="flex items-center gap-1">
    <!-- Sort Button -->
    <UButton
      :variant="config.variant"
      :label="config.label"
      :class="config.class"
      :color="isSorted ? config.activeColor : config.inactiveColor"
      :icon="sortIcon"
      @click="handleSort"
    />

    <!-- Filter Button (if column is filterable) -->
    <ColumnFilter
      v-if="isFilterable"
      :column="currentFilterableColumn"
      :active-filters="currentColumnFilters"
      @update:filters="handleFilterUpdate"
    />
  </div>
</template>

<script setup lang="ts" generic="TData">
import { computed } from 'vue'
import type { Column, RowData } from '@tanstack/table-core'
import type { FilterableColumn, FilterCondition } from '@/common/types/filter'
import ColumnFilter from './filters/ColumnFilter.vue'

interface Props<TData extends RowData> {
  column: Column<TData>
  config: {
    variant: 'ghost' | 'solid' | 'outline' | 'soft' | 'link'
    label: string
    class: string
    activeColor?: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
    inactiveColor?: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
    filterable?: boolean
  }
  filterableColumns?: FilterableColumn[]
  activeFilters?: FilterCondition[] | undefined
}

interface Emits {
  (e: 'update:filters', filters: FilterCondition[]): void
}

const props = defineProps<Props<TData>>()
const emit = defineEmits<Emits>()

// Computed properties for sort state
const isSorted = computed(() => props.column.getIsSorted())

// Computed sort icon
const sortIcon = computed(() => {
  if (isSorted.value === 'asc') return 'i-lucide-arrow-up-narrow-wide'
  if (isSorted.value === 'desc') return 'i-lucide-arrow-down-wide-narrow'
  return 'i-lucide-arrow-up-down'
})

// Filter-related computed properties
const isFilterable = computed(() => {
  return props.config.filterable !== false && !!props.filterableColumns
})

// Get the filterable column config for current column
const currentFilterableColumn = computed(() => {
  if (!props.filterableColumns) return undefined
  return props.filterableColumns.find(col => col.field === props.column.id)
})

// Get filters for current column only
const currentColumnFilters = computed(() => {
  if (!props.activeFilters) return undefined
  return props.activeFilters.filter(filter => filter.field === props.column.id)
})

// Handle sort click
const handleSort = () => {
  if (isSorted.value === 'asc') {
    props.column.toggleSorting(true, true)
  } else if (isSorted.value === 'desc') {
    props.column.clearSorting()
  } else {
    props.column.toggleSorting(false, true)
  }
}

// Handle filter update
const handleFilterUpdate = (filters: FilterCondition[]) => {
  emit('update:filters', filters)
}
</script>
