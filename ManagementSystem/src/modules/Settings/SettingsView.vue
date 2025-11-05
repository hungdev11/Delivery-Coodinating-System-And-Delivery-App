<script setup lang="ts">
/**
 * Settings View
 *
 * View for managing system settings with UTable and Nuxt UI v3 best practices
 */

import { onMounted, ref, watch, resolveComponent, h, defineAsyncComponent } from 'vue'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import { PageHeader } from '@/common/components'
import { SystemSettingDto } from './model.type'
import { useSettingsStore } from './stores/useSettingsStore'
import { useTemplateRef } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import type { SortingState } from '@tanstack/table-core'
import { storeToRefs } from 'pinia'
import TableHeaderCell from '@/common/components/TableHeaderCell.vue'
import ColumnFilter from '@/common/components/filters/ColumnFilter.vue'
import type { Column } from '@tanstack/table-core'
import type { FilterableColumn, FilterCondition } from '@/common/types/filter'

// Lazy load modals
const LazySettingFormModal = defineAsyncComponent(() => import('./components/SettingFormModal.vue'))
const LazySettingDeleteModal = defineAsyncComponent(() => import('./components/SettingDeleteModal.vue'))

const UCheckbox = resolveComponent('UCheckbox')
const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')

const overlay = useOverlay()
const table = useTemplateRef('table')

// Pinia store
const settingsStore = useSettingsStore()

// Setup header with filter and sort
const setupHeader = ({
  column,
  config,
}: {
  column: Column<SystemSettingDto>
  config: {
    variant: 'ghost' | 'solid' | 'outline' | 'soft' | 'link'
    label: string
    class: string
    activeColor?: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
    inactiveColor?: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
    filterable?: boolean
  }
}) => h(TableHeaderCell<SystemSettingDto>, {
  column,
  config,
  filterableColumns: filterableColumns,
  activeFilters: activeFilters.value,
  'onUpdate:filters': handleFiltersUpdate
})

// Destructure store state - use storeToRefs for reactive variables
const { settings, loading, searchValue, pagination } = storeToRefs(settingsStore)

// Local state
const selected = ref<SystemSettingDto[]>([])
const sorting = ref<Array<{ id: string; desc: boolean }>>([])
const visiblePasswords = ref<Set<string>>(new Set())
const activeFilters = ref<FilterCondition[]>([])

// Filterable columns configuration
const filterableColumns: FilterableColumn[] = [
  {
    field: 'key',
    label: 'Key',
    type: 'string',
    filterType: 'text',
    filterConfig: {
      placeholder: 'Enter setting key...',
    },
  },
  {
    field: 'description',
    label: 'Description',
    type: 'string',
    filterType: 'text',
    filterConfig: {
      placeholder: 'Enter description...',
    },
  },
  {
    field: 'value',
    label: 'Value',
    type: 'string',
    filterType: 'text',
    filterConfig: {
      placeholder: 'Enter setting value...',
    },
  },
  {
    field: 'type',
    label: 'Type',
    type: 'enum',
    filterType: 'select',
    enumOptions: [
      { label: 'String', value: 'STRING' },
      { label: 'Integer', value: 'INTEGER' },
      { label: 'Boolean', value: 'BOOLEAN' },
      { label: 'Double', value: 'DOUBLE' },
      { label: 'JSON', value: 'JSON' },
    ],
  },
  {
    field: 'group',
    label: 'Group',
    type: 'string',
    filterType: 'text',
    filterConfig: {
      placeholder: 'Enter group name...',
    },
  },
]

// Table columns configuration
const columns: TableColumn<SystemSettingDto>[] = [
  {
    id: 'select',
    header: ({ table }) =>
      h(UCheckbox, {
        modelValue: table.getIsSomePageRowsSelected()
          ? 'indeterminate'
          : table.getIsAllPageRowsSelected(),
        'onUpdate:modelValue': (value: boolean | 'indeterminate') =>
          table.toggleAllPageRowsSelected(!!value),
        'aria-label': 'Select all',
      }),
    cell: ({ row }) =>
      h(UCheckbox, {
        modelValue: row.getIsSelected(),
        'onUpdate:modelValue': (value: boolean | 'indeterminate') => row.toggleSelected(!!value),
        'aria-label': 'Select row',
      }),
  },
  {
    accessorKey: 'key',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Key',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
  },
  {
    accessorKey: 'description',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Description',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
    cell: ({ row }) => {
      const description = row.getValue('description') as string
      return h('span', {
        class: 'text-sm text-gray-600 dark:text-gray-400 max-w-xs truncate',
        title: description
      }, description || '-')
    },
  },
  {
    accessorKey: 'value',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Value',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
    cell: ({ row }) => {
      const setting = row.original
      return h('div', { class: 'flex items-center space-x-2' }, [
        h('span', {
          class: 'text-sm font-mono bg-gray-100 dark:bg-gray-700 px-2 py-1 rounded max-w-xs truncate',
          title: setting.value
        }, getDisplayValue(setting)),
        ...(setting.displayMode === 'PASSWORD' ? [
          h(UButton, {
            icon: 'i-heroicons-eye',
            size: 'xs',
            variant: 'ghost',
            title: 'Show password',
            onClick: () => togglePasswordVisibility(setting.key)
          })
        ] : [])
      ])
    },
  },
  {
    accessorKey: 'type',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Type',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
    cell: ({ row }) => {
      const type = row.getValue('type') as string
      const color = getTypeColor(type)
      return h(UBadge, { class: 'capitalize', variant: 'soft', color }, () => type)
    },
  },
  {
    accessorKey: 'group',
    header: ({ column }) =>
      setupHeader({
        column,
        config: {
          variant: 'ghost',
          label: 'Group',
          class: '-mx-2.5',
          activeColor: 'primary',
          inactiveColor: 'neutral',
          filterable: true,
        },
      }),
  },
  {
    accessorKey: 'actions',
    header: 'Actions',
    cell: ({ row }) => {
      const setting = row.original
      return h('div', { class: 'flex space-x-2' }, [
        h(UButton, {
          icon: 'i-heroicons-pencil',
          size: 'sm',
          variant: 'ghost',
          title: 'Edit setting',
          disabled: setting.isReadOnly,
          onClick: () => openEditModal(setting),
        }),
        h(UButton, {
          icon: 'i-heroicons-trash',
          size: 'sm',
          variant: 'ghost',
          color: 'error',
          title: 'Delete setting',
          disabled: setting.isReadOnly,
          onClick: () => openDeleteModal(setting),
        }),
      ])
    },
  },
]

/**
 * Get column label for sorting summary
 */
const getColumnLabel = (columnId: string): string => {
  const labelMap: Record<string, string> = {
    key: 'Key',
    description: 'Description',
    value: 'Value',
    type: 'Type',
    group: 'Group',
    displayMode: 'Display Mode',
    isReadOnly: 'Read Only',
  }
  return labelMap[columnId] || columnId
}


/**
 * Event handlers
 */
const handleSearch = () => {
  settingsStore.setSearch(searchValue.value)
  loadSettings()
}

const onSortingChange = (newSorting: SortingState) => {
  const sorts = newSorting.map((s) => ({
    field: s.id,
    direction: s.desc ? ('desc' as const) : ('asc' as const),
  }))
  settingsStore.setSorting(sorts)
  sorting.value = newSorting
  // loadSettings() will be called by the watcher
}

const handlePageChange = (page: number) => {
  settingsStore.setPage(page - 1)
  loadSettings() // Use loadSettings to include filters
}

/**
 * Load settings using store
 */
const loadSettings = async () => {
  await settingsStore.loadSettings({
    filters: activeFilters.value.length > 0 ? {
      logic: 'AND',
      conditions: activeFilters.value
    } : undefined
  })
}

/**
 * Open create modal
 */
const openCreateModal = async () => {
  const modal = overlay.create(LazySettingFormModal)
  const instance = modal.open({ mode: 'create' })
  const formData = await instance.result

  if (formData) {
    await settingsStore.saveSetting(formData)
    await loadSettings()
  }
}

/**
 * Open edit modal
 */
const openEditModal = async (setting: SystemSettingDto) => {
  const modal = overlay.create(LazySettingFormModal)
  const instance = modal.open({ mode: 'edit', setting })
  const formData = await instance.result

  if (formData) {
    await settingsStore.saveSetting(formData)
    await loadSettings()
  }
}

/**
 * Open delete modal
 */
const openDeleteModal = async (setting: SystemSettingDto) => {
  const modal = overlay.create(LazySettingDeleteModal)
  const instance = modal.open({ settingName: setting.key })
  const confirmed = await instance.result

  if (confirmed) {
    await settingsStore.removeSetting(setting.group, setting.key)
    await loadSettings()
  }
}

/**
 * Handle bulk delete
 */
const handleBulkDelete = async () => {
  if (selected.value.length === 0) return

  const modal = overlay.create(LazySettingDeleteModal)
  const instance = modal.open({ count: selected.value.length })
  const confirmed = await instance.result

  if (confirmed) {
    const keys = selected.value.map((setting) => setting.key)
    await settingsStore.bulkDelete(keys)
    await loadSettings()
    selected.value = []
  }
}

/**
 * Get type badge color
 */
const getTypeColor = (type: string) => {
  const colorMap: Record<string, string> = {
    STRING: 'blue',
    INTEGER: 'green',
    BOOLEAN: 'purple',
    DOUBLE: 'yellow',
    JSON: 'gray',
  }
  return colorMap[type] || 'gray'
}

/**
 * Get display mode color
 */
const getDisplayModeColor = (displayMode: string) => {
  const colorMap: Record<string, string> = {
    TEXT: 'blue',
    PASSWORD: 'red',
    CODE: 'purple',
    NUMBER: 'green',
    TOGGLE: 'yellow',
    TEXTAREA: 'indigo',
    URL: 'cyan',
    EMAIL: 'pink',
  }
  return colorMap[displayMode] || 'gray'
}

/**
 * Get display mode label
 */
const getDisplayModeLabel = (displayMode: string) => {
  const labelMap: Record<string, string> = {
    TEXT: 'Text',
    PASSWORD: 'Password',
    CODE: 'Code',
    NUMBER: 'Number',
    TOGGLE: 'Toggle',
    TEXTAREA: 'Textarea',
    URL: 'URL',
    EMAIL: 'Email',
  }
  return labelMap[displayMode] || displayMode
}

/**
 * Get display value based on display mode
 */
const getDisplayValue = (setting: SystemSettingDto) => {
  if (setting.displayMode === 'PASSWORD') {
    if (visiblePasswords.value.has(setting.key)) {
      return setting.value
    }
    // Show asterisks based on actual value length
    return '*'.repeat(Math.max(setting.value.length, 4)) // Minimum 4 asterisks
  }

  if (setting.displayMode === 'CODE' || setting.displayMode === 'TEXTAREA') {
    // Show only first line for multi-line values
    return setting.value.split('\n')[0] + (setting.value.includes('\n') ? '...' : '')
  }

  return setting.value
}

/**
 * Toggle password visibility
 */
const togglePasswordVisibility = (key: string) => {
  if (visiblePasswords.value.has(key)) {
    visiblePasswords.value.delete(key)
  } else {
    visiblePasswords.value.add(key)
  }
}

/**
 * Handle filters update from column filters
 */
const handleFiltersUpdate = (filters: FilterCondition[]) => {
  activeFilters.value = filters
  loadSettings()
  console.log('Filters updated:', filters)
}

/**
 * Watchers
 */
watch(searchValue, () => {
  const timeout = setTimeout(() => {
    handleSearch()
  }, 300)
  return () => clearTimeout(timeout)
})

// Watch for sorts changes and sync with sorting
watch(
  () => settingsStore.sorting,
  (newSorts) => {
    const newSorting = newSorts.map((sort: { field: string; direction: string }) => ({
      id: sort.field,
      desc: sort.direction === 'desc',
    }))
    sorting.value = newSorting
    loadSettings() // Move loadSettings here to avoid double fetch
  },
  { deep: true },
)

// Load data on mount
onMounted(async () => {
  await loadSettings()
})
</script>

<template>
  <div class="container mx-auto px-4 py-6">
    <PageHeader title="Settings" description="Manage system configuration settings">
      <template #actions>
        <UButton icon="i-heroicons-plus" @click="openCreateModal"> Add Setting </UButton>
      </template>
    </PageHeader>

    <!-- Search -->
    <div class="mb-6">
      <div class="flex flex-col sm:flex-row gap-4">
        <!-- Search Input -->
        <div class="flex-1">
          <UInput
            v-model="searchValue"
            placeholder="Search settings..."
            icon="i-heroicons-magnifying-glass"
            size="lg"
          />
    </div>
    </div>

      <!-- Sorting Summary -->
      <div
        v-if="sorting.length > 0"
        class="mt-4 flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400"
      >
        <span>Sorted by:</span>
        <div class="flex items-center gap-1">
          <UBadge
            v-for="sort in sorting"
            :key="sort.id"
            :color="sort.desc ? 'error' : 'success'"
            variant="soft"
            size="sm"
          >
            {{ getColumnLabel(sort.id) }}
            <UIcon
              :name="
                sort.desc
                  ? 'i-lucide-arrow-down-wide-narrow'
                  : 'i-lucide-arrow-up-narrow-wide'
              "
              class="ml-1"
            />
              </UBadge>
            </div>
        <UButton
          variant="ghost"
          size="xs"
          color="neutral"
          icon="i-heroicons-x-mark"
          @click="settingsStore.setSorting([])"
          title="Clear all sorting"
        />
      </div>
            </div>

    <!-- Bulk Actions -->
    <div
      v-if="
        table &&
        table?.tableApi?.getFilteredSelectedRowModel()?.rows &&
        (table?.tableApi?.getFilteredSelectedRowModel()?.rows?.length || 0) > 0
      "
      class="mb-4 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg"
    >
      <div class="flex items-center justify-between">
        <span class="text-sm text-gray-600 dark:text-gray-400">
          {{ table?.tableApi?.getFilteredSelectedRowModel()?.rows?.length || 0 }} of
          {{ table?.tableApi?.getFilteredRowModel()?.rows?.length || 0 }} row(s) selected.
        </span>
        <div class="flex space-x-2">
              <UButton
                size="sm"
            variant="soft"
            color="error"
            icon="i-heroicons-trash"
            @click="handleBulkDelete"
          >
            Delete
          </UButton>
        </div>
      </div>
            </div>

    <!-- Table -->
    <UCard>
      <UTable
        ref="table"
        :sorting="sorting"
        :data="settings"
        :columns="columns"
        :loading="loading"
        :manual-sorting="true"
        enable-multi-sort
        @update:sorting="onSortingChange($event)"
      />

      <!-- Empty State -->
      <template v-if="!loading && settings.length === 0">
        <div class="text-center py-12">
          <div class="mx-auto h-12 w-12 text-gray-400">
            <UIcon name="i-heroicons-cog-6-tooth" class="h-12 w-12" />
            </div>
          <h3 class="mt-2 text-sm font-medium text-gray-900 dark:text-gray-100">
            No settings found
          </h3>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
            {{
              searchValue
                ? 'Try adjusting your search or filter criteria.'
                : 'Get started by creating a new setting.'
            }}
          </p>
          <div class="mt-6">
            <UButton v-if="!searchValue" icon="i-heroicons-plus" @click="openCreateModal">
              Add Setting
            </UButton>
            <UButton v-else variant="soft" @click="searchValue = ''"> Clear Filters </UButton>
          </div>
        </div>
      </template>
      </UCard>

    <!-- Pagination -->
    <div v-if="settings.length > 0" class="mt-6 flex items-center justify-between">
      <div class="text-sm text-gray-700 dark:text-gray-300">
        Showing {{ pagination.page * pagination.size + 1 }}-{{
          Math.min((pagination.page + 1) * pagination.size, pagination.totalElements)
        }}
        of {{ pagination.totalElements }} results
      </div>
      <UPagination
        v-model="pagination.page"
        :page-count="pagination.size"
        :total="pagination.totalElements"
        @update:page="handlePageChange"
      />
    </div>

  </div>
</template>
