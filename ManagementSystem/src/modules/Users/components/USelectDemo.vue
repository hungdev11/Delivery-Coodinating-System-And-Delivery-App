<template>
  <div class="p-4 space-y-4">
    <h2 class="text-xl font-bold">USelect Demo</h2>
    
    <div>
      <label class="block text-sm font-medium mb-2">Simple Select:</label>
      <USelect 
        v-model="selectedValue" 
        :options="options" 
        placeholder="Select an option"
      />
      <p class="text-sm text-gray-600 mt-2">Selected: {{ selectedValue }}</p>
    </div>

    <div>
      <label class="block text-sm font-medium mb-2">Advanced Filter Demo:</label>
      <UButton
        color="primary"
        @click="showDrawer = true"
      >
        Open Advanced Filter Drawer
      </UButton>

      <AdvancedFilterDrawer
        :show="showDrawer"
        :columns="filterableColumns"
        :active-filters="[]"
        @apply="handleFilterApply"
        @clear="handleFilterClear"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AdvancedFilterDrawer from '../../../common/components/filters/AdvancedFilterDrawer.vue'
import type { FilterableColumn, FilterGroup } from '../../../common/types/filter'

const selectedValue = ref('')
const showDrawer = ref(false)

const options = [
  { label: 'Backlog', value: 'backlog' },
  { label: 'Todo', value: 'todo' },
  { label: 'In Progress', value: 'in-progress' },
  { label: 'Done', value: 'done' }
]

const filterableColumns: FilterableColumn[] = [
  {
    field: 'username',
    label: 'Username',
    type: 'string',
    filterable: true,
    filterType: 'text',
    filterConfig: {
      placeholder: 'Enter username...',
    },
  },
  {
    field: 'email',
    label: 'Email',
    type: 'string',
    filterable: true,
    filterType: 'text',
    filterConfig: {
      placeholder: 'Enter email address...',
    },
  },
  {
    field: 'status',
    label: 'Status',
    type: 'enum',
    enumOptions: [
      { label: 'Active', value: 'ACTIVE' },
      { label: 'Inactive', value: 'INACTIVE' },
      { label: 'Suspended', value: 'SUSPENDED' },
      { label: 'Pending', value: 'PENDING' },
    ],
    filterable: true,
    filterType: 'select',
    filterConfig: {
      placeholder: 'Select status...',
      multiple: false,
    },
  },
]

function handleFilterApply(filterGroup: FilterGroup) {
  console.log('Applied filters:', filterGroup)
  showDrawer.value = false
}

function handleFilterClear() {
  console.log('Cleared filters')
  showDrawer.value = false
}
</script>
