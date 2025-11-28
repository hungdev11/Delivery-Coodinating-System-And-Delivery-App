<script setup lang="ts">
/**
 * Table Filters Component
 *
 * Shared component for table filters, sorting, and search
 * - Fixed size (no layout shift)
 * - No hide button (always visible, uses disabled state)
 * - Responsive for Android
 */

import type { FilterCondition } from '@/common/types/filter'
import type { SortingState } from '@tanstack/table-core'
import { useResponsiveStore } from '@/common/store/responsive.store'
import { computed, onMounted, ref } from 'vue'
import SortingDrawer from './SortingDrawer.vue'

interface Props {
  searchValue?: string
  searchPlaceholder?: string
  activeFilters?: FilterCondition[] | undefined
  filterStructure?: string
  sorting?: SortingState | Array<{ id: string; desc: boolean }>
  getColumnLabel?: (columnId: string) => string
  showAdvancedFilters?: boolean
  // Bulk actions props
  selectedCount?: number
  totalCount?: number
  onBulkExport?: () => void
  onBulkDelete?: () => void
  // Sortable columns for Android sorting select
  sortableColumns?: Array<{ id: string; label: string }>
}

interface Emits {
  (e: 'update:searchValue', value: string): void
  (e: 'update:showAdvancedFilters', value: boolean): void
  (e: 'clearFilters'): void
  (e: 'clearSorting'): void
  (e: 'openAdvancedFilters'): void
  (e: 'bulkExport'): void
  (e: 'bulkDelete'): void
  (e: 'update:sorting', value: SortingState | Array<{ id: string; desc: boolean }>): void
}

const props = withDefaults(defineProps<Props>(), {
  searchValue: '',
  searchPlaceholder: 'Search...',
  activeFilters: undefined,
  filterStructure: '',
  sorting: () => [],
  getColumnLabel: (id: string) => id,
  showAdvancedFilters: false,
  selectedCount: 0,
  totalCount: 0,
  onBulkExport: undefined,
  onBulkDelete: undefined,
  sortableColumns: () => [],
})

const emit = defineEmits<Emits>()

const responsiveStore = useResponsiveStore()

// Ensure resize listener is attached
onMounted(() => {
  responsiveStore.attachResizeListener()
})

// Handle search input
const handleSearchInput = (value: string) => {
  emit('update:searchValue', value)
}

// Handle clear filters
const handleClearFilters = () => {
  emit('clearFilters')
}

// Handle clear sorting
const handleClearSorting = () => {
  emit('clearSorting')
}

// Sorting drawer
const showSortingDrawer = ref(false)

// Handle update sorting from drawer
const handleSortingUpdate = (newSorting: SortingState | Array<{ id: string; desc: boolean }>) => {
  emit('update:sorting', newSorting)
}

// Handle open sorting drawer
const handleOpenSortingDrawer = () => {
  showSortingDrawer.value = true
}

// Handle open advanced filters
const handleOpenAdvancedFilters = () => {
  emit('openAdvancedFilters')
}

// Check if has active filters
const hasActiveFilters = computed(() => {
  return props.activeFilters && props.activeFilters.length > 0
})

// Check if has sorting
const hasSorting = computed(() => {
  return props.sorting && props.sorting.length > 0
})

// Check if has selection for bulk actions
const hasSelection = computed(() => props.selectedCount > 0)

// Handle bulk export
const handleBulkExport = () => {
  if (props.onBulkExport) {
    props.onBulkExport()
  } else {
    emit('bulkExport')
  }
}

// Handle bulk delete
const handleBulkDelete = () => {
  if (props.onBulkDelete) {
    props.onBulkDelete()
  } else {
    emit('bulkDelete')
  }
}
</script>

<template>
  <!-- Desktop/Tablet: Fixed height container - 2 lines max -->
  <div v-if="!responsiveStore.isMobile" class="mb-6 space-y-2 min-h-[88px]">
    <!-- Line 1: Search (50%) + Bulk Selected + Bulk Actions (50%) -->
    <div class="flex flex-col sm:flex-row items-start sm:items-center gap-2 h-10">
      <!-- Search Input - 50% width -->
      <div class="w-full sm:w-[50%] sm:flex-shrink-0">
        <UInput
          :model-value="searchValue"
          :placeholder="searchPlaceholder"
          icon="i-heroicons-magnifying-glass"
          size="md"
          @update:model-value="handleSearchInput"
        />
      </div>

      <!-- Bulk Selected Info + Actions - Compact, 50% width -->
      <div class="w-full sm:w-[50%] flex items-center justify-end gap-2 min-h-[40px]">
        <span class="text-xs text-gray-600 dark:text-gray-400 whitespace-nowrap">
          <template v-if="hasSelection">
            {{ selectedCount }} of {{ totalCount }} selected
          </template>
          <template v-else> No rows selected </template>
        </span>
        <UButton
          size="xs"
          variant="soft"
          icon="i-heroicons-arrow-down-tray"
          :disabled="!hasSelection"
          @click="handleBulkExport"
          title="Export selected"
        >
          <span class="hidden sm:inline">Export</span>
        </UButton>
        <UButton
          size="xs"
          variant="soft"
          color="error"
          icon="i-heroicons-trash"
          :disabled="!hasSelection"
          @click="handleBulkDelete"
          title="Delete selected"
        >
          <span class="hidden sm:inline">Delete</span>
        </UButton>
      </div>
    </div>

    <!-- Line 2: Filters + Sort -->
    <div
      class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 min-h-[32px]"
    >
      <!-- Active Filters Display -->
      <div class="flex items-center gap-2 flex-wrap flex-1">
        <span class="text-xs text-gray-600 dark:text-gray-400">Active filters:</span>
        <div class="flex items-center gap-1 flex-wrap">
          <UBadge
            v-if="hasActiveFilters && filterStructure"
            color="primary"
            variant="soft"
            size="xs"
            class="max-w-xs truncate"
          >
            {{ filterStructure }}
          </UBadge>
          <span v-else class="text-xs text-gray-400 dark:text-gray-500">None</span>
        </div>
        <UButton
          variant="ghost"
          size="xs"
          color="neutral"
          icon="i-heroicons-x-mark"
          :disabled="!hasActiveFilters"
          @click="handleClearFilters"
          title="Clear all filters"
        />
        <!-- Advanced Filters Button -->
        <UButton
          variant="soft"
          color="primary"
          icon="i-heroicons-cog-6-tooth"
          size="xs"
          @click="handleOpenAdvancedFilters"
        >
          <span class="hidden sm:inline">Filters</span>
          <span class="sm:hidden">Filters</span>
        </UButton>
      </div>

      <!-- Sorting Summary -->
      <div class="flex items-center gap-2 flex-wrap flex-1">
        <span class="text-xs text-gray-600 dark:text-gray-400">Sorted by:</span>
        <div class="flex items-center gap-1 flex-wrap">
          <template v-if="hasSorting">
            <UBadge
              v-for="sort in sorting"
              :key="sort.id"
              :color="(sort as any).desc ? 'error' : 'success'"
              variant="soft"
              size="xs"
              class="max-w-xs truncate"
            >
              {{ getColumnLabel(sort.id) }}
              <UIcon
                :name="
                  (sort as any).desc
                    ? 'i-lucide-arrow-down-wide-narrow'
                    : 'i-lucide-arrow-up-narrow-wide'
                "
                class="ml-1 w-3 h-3"
              />
            </UBadge>
          </template>
          <span v-else class="text-xs text-gray-400 dark:text-gray-500">None</span>
        </div>
        <UButton
          variant="ghost"
          size="xs"
          color="neutral"
          icon="i-heroicons-x-mark"
          :disabled="!hasSorting"
          @click="handleClearSorting"
          title="Clear all sorting"
        />
        <!-- Sorting Button -->
        <UButton
          variant="soft"
          color="primary"
          icon="i-lucide-arrow-up-down"
          size="xs"
          @click="handleOpenSortingDrawer"
        >
          <span class="hidden sm:inline">Sort</span>
          <span class="sm:hidden">Sort</span>
        </UButton>
      </div>
    </div>
  </div>

  <!-- Mobile: Line by line layout -->
  <div v-else class="mb-6 space-y-3">
    <!-- Line 1: Search -->
    <div class="w-full">
      <UInput
        :model-value="searchValue"
        :placeholder="searchPlaceholder"
        icon="i-heroicons-magnifying-glass"
        size="lg"
        class="text-base py-3 w-full"
        @update:model-value="handleSearchInput"
      />
    </div>

    <!-- Line 2: Filters -->
    <div class="flex items-center gap-3 flex-wrap w-full">
      <span class="text-sm text-gray-600 dark:text-gray-400">Active filters:</span>
      <div class="flex items-center gap-2 flex-wrap flex-1">
        <UBadge
          v-if="hasActiveFilters && filterStructure"
          color="primary"
          variant="soft"
          size="sm"
          class="max-w-xs truncate"
        >
          {{ filterStructure }}
        </UBadge>
        <span v-else class="text-sm text-gray-400 dark:text-gray-500">None</span>
        <UButton
          variant="ghost"
          size="sm"
          color="neutral"
          icon="i-heroicons-x-mark"
          :disabled="!hasActiveFilters"
          @click="handleClearFilters"
          title="Clear all filters"
        />
        <!-- Advanced Filters Button -->
        <UButton
          variant="soft"
          color="primary"
          icon="i-heroicons-cog-6-tooth"
          size="xs"
          @click="handleOpenAdvancedFilters"
        >
          <span class="text-base font-medium">Filters</span>
        </UButton>
      </div>
    </div>

    <!-- Line 3: Sort -->
    <div class="flex items-center gap-3 flex-wrap w-full">
      <span class="text-sm text-gray-600 dark:text-gray-400">Sorted by:</span>
      <div class="flex items-center gap-2 flex-wrap flex-1">
        <template v-if="hasSorting">
          <UBadge
            v-for="sort in sorting"
            :key="sort.id"
            :color="(sort as any).desc ? 'error' : 'success'"
            variant="soft"
            size="sm"
            class="max-w-xs truncate"
          >
            {{ getColumnLabel(sort.id) }}
            <UIcon
              :name="
                (sort as any).desc
                  ? 'i-lucide-arrow-down-wide-narrow'
                  : 'i-lucide-arrow-up-narrow-wide'
              "
              class="ml-1 w-4 h-4"
            />
          </UBadge>
        </template>
        <span v-else class="text-sm text-gray-400 dark:text-gray-500">None</span>
        <UButton
          variant="ghost"
          size="sm"
          color="neutral"
          icon="i-heroicons-x-mark"
          :disabled="!hasSorting"
          @click="handleClearSorting"
          title="Clear all sorting"
        />
        <!-- Sorting Button -->
        <UButton
          variant="soft"
          color="primary"
          icon="i-lucide-arrow-up-down"
          size="xs"
          @click="handleOpenSortingDrawer"
        >
          <span class="text-base font-medium">Sort</span>
        </UButton>
      </div>
    </div>

    <!-- Line 4: Bulk Selected + Actions -->
    <div class="flex items-center justify-between gap-3 w-full">
      <span class="text-sm text-gray-600 dark:text-gray-400 whitespace-nowrap">
        <template v-if="hasSelection"> {{ selectedCount }} / {{ totalCount }} selected </template>
        <template v-else> No rows selected </template>
      </span>
      <div class="flex items-center gap-2">
        <UButton
          size="sm"
          variant="soft"
          icon="i-heroicons-arrow-down-tray"
          :disabled="!hasSelection"
          @click="handleBulkExport"
          title="Export selected"
        >
          Export
        </UButton>
        <UButton
          size="sm"
          variant="soft"
          color="error"
          icon="i-heroicons-trash"
          :disabled="!hasSelection"
          @click="handleBulkDelete"
          title="Delete selected"
        >
          Delete
        </UButton>
      </div>
    </div>
  </div>

  <!-- Sorting Drawer -->
  <SortingDrawer
    :model-value="showSortingDrawer"
    :sorting="sorting"
    :sortable-columns="sortableColumns"
    @update:model-value="showSortingDrawer = $event"
    @update:sorting="handleSortingUpdate"
  />
</template>
