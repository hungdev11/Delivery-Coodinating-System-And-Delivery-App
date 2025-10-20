<template>
  <UButton
    :variant="config.variant"
    :label="config.label"
    :class="config.class"
    :color="isSorted ? config.activeColor : config.inactiveColor"
    :icon="sortIcon"
    @click="handleSort"
  />
</template>

<script setup lang="ts" generic="TData">
import { computed } from 'vue'
import type { Column, RowData } from '@tanstack/table-core'

interface Props<TData extends RowData> {
  column: Column<TData>
  config: {
    variant: 'ghost' | 'solid' | 'outline' | 'soft' | 'link'
    label: string
    class: string
    activeColor?: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
    inactiveColor?: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
  }
}

const props = defineProps<Props<TData>>()

// Computed properties for sort state
const isSorted = computed(() => props.column.getIsSorted())

// Computed sort icon
const sortIcon = computed(() => {
  console.log(isSorted.value)
  if (isSorted.value === 'asc') return 'i-lucide-arrow-up-narrow-wide'
  if (isSorted.value === 'desc') return 'i-lucide-arrow-down-wide-narrow'
  return 'i-lucide-arrow-up-down'
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
</script>
