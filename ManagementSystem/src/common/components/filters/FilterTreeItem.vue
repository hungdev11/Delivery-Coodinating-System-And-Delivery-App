<template>
  <div class="filter-tree-item">
    <!-- Condition Item -->
    <div v-if="item.type === 'condition'" class="condition-item">
      <div class="flex items-center gap-2 p-2 bg-gray-50 rounded border">
        <span class="text-sm font-medium">{{ getFieldLabel(item.field) }}</span>
        <span class="text-sm text-gray-600">{{ item.operator }}</span>
        <span class="text-sm">{{ item.value }}</span>

        <div class="flex gap-1 ml-auto">
          <UButton
            icon="i-heroicons-pencil"
            size="xs"
            variant="ghost"
            @click="editCondition"
          />
          <UButton
            icon="i-heroicons-trash"
            size="xs"
            variant="ghost"
            color="error"
            @click="removeItem"
          />
        </div>
      </div>
    </div>

    <!-- Group Item -->
    <div v-else-if="item.type === 'group'" class="group-item">
      <div class="group-header">
        <div class="flex items-center gap-2 p-2 bg-blue-50 rounded border border-blue-200">
          <span class="text-sm font-medium text-blue-700">
            Group ({{ (item as TreeGroup).items.length }} items)
          </span>

          <div class="flex gap-1 ml-auto">
            <UButton
              icon="i-heroicons-plus"
              size="xs"
              variant="ghost"
              @click="addCondition"
            />
            <UButton
              icon="i-heroicons-folder-plus"
              size="xs"
              variant="ghost"
              @click="addGroup"
            />
            <UButton
              icon="i-heroicons-pencil"
              size="xs"
              variant="ghost"
              @click="editGroup"
            />
            <UButton
              icon="i-heroicons-trash"
              size="xs"
              variant="ghost"
              color="error"
              @click="removeItem"
            />
          </div>
        </div>
      </div>

      <!-- Group Items -->
      <div class="group-items ml-4 mt-2">
        <draggable
          v-model="(item as TreeGroup).items"
          :group="{ name: 'filter-items', pull: true, put: true }"
          :animation="200"
          ghost-class="ghost"
          chosen-class="chosen"
          drag-class="drag"
          @add="onAddToGroup"
          class="space-y-2"
        >
          <template #item="{ element: subItem, index }">
            <div class="space-y-2">
              <!-- Logic Operator (except for first item) -->
              <div v-if="index > 0" class="flex items-center justify-center">
                <USelect
                  :model-value="subItem.logic || 'AND'"
                  :items="logicOptions"
                  size="sm"
                  class="w-20"
                  @update:model-value="updateLogic(subItem.id, $event)"
                />
              </div>

              <!-- Recursive Item -->
              <FilterTreeItem
                :item="subItem"
                :columns="columns"
                :depth="(depth || 0) + 1"
                @update:item="(updatedItem) => updateSubItem(subItem.id, updatedItem)"
                @remove:item="() => removeSubItem(subItem.id)"
                @add:condition="() => addSubCondition(subItem.id)"
                @add:group="() => addSubGroup(subItem.id)"
              />
            </div>
          </template>
        </draggable>

        <!-- Add Items to Group -->
        <div v-if="(item as TreeGroup).items.length === 0" class="flex gap-2 p-2 border-2 border-dashed border-gray-300 rounded">
          <UButton
            size="xs"
            variant="outline"
            icon="i-heroicons-plus"
            @click="addCondition"
          >
            Add Condition
          </UButton>
          <UButton
            size="xs"
            variant="outline"
            icon="i-heroicons-folder-plus"
            @click="addGroup"
          >
            Add Group
          </UButton>
        </div>

        <!-- Add More Items to Group -->
        <div v-else class="flex gap-2 p-2 border-2 border-dashed border-gray-300 rounded">
          <UButton
            size="xs"
            variant="outline"
            icon="i-heroicons-plus"
            @click="addCondition"
          >
            Add Condition
          </UButton>
          <UButton
            size="xs"
            variant="outline"
            icon="i-heroicons-folder-plus"
            @click="addGroup"
          >
            Add Group
          </UButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FilterableColumn } from '../../types/filter'
import draggable from 'vuedraggable'

// Types
interface TreeCondition {
  id: string
  type: 'condition'
  field: string
  operator: string
  value: string
  logic?: 'AND' | 'OR'
}

interface TreeGroup {
  id: string
  type: 'group'
  items: TreeItem[]
  logic?: 'AND' | 'OR'
}

type TreeItem = TreeCondition | TreeGroup

// Props
const props = defineProps<{
  item: TreeItem
  columns: FilterableColumn[]
  depth?: number
}>()

// Emits
const emit = defineEmits<{
  'update:item': [item: TreeItem]
  'remove:item': []
  'add:condition': []
  'add:group': []
  'add:item': [{ groupId: string, item: any }]
}>()

// Computed
const logicOptions = computed(() => [
  { label: 'AND', value: 'AND' },
  { label: 'OR', value: 'OR' }
])

// Utility function
function generateId(): string {
  return Math.random().toString(36).substr(2, 9)
}

// Methods
function getFieldLabel(field: string) {
  const column = props.columns.find(col => col.field === field)
  return column?.label || field
}

function editCondition() {
  // TODO: Implement edit condition
  console.log('Edit condition:', props.item)
}

function removeItem() {
  emit('remove:item')
}

function addCondition() {
  // Create a new condition and add it to this group
  const newCondition: TreeCondition = {
    id: generateId(),
    type: 'condition',
    field: '',
    operator: '',
    value: '',
    logic: 'AND'
  }

  if (props.item.type === 'group') {
    const updatedGroup = {
      ...props.item,
      items: [...props.item.items, newCondition]
    }
    emit('update:item', updatedGroup)
  }
}

function addGroup() {
  // Create a new group and add it to this group
  const newGroup: TreeGroup = {
    id: generateId(),
    type: 'group',
    items: [],
    logic: 'AND'
  }

  if (props.item.type === 'group') {
    const updatedGroup = {
      ...props.item,
      items: [...props.item.items, newGroup]
    }
    emit('update:item', updatedGroup)
  }
}

function editGroup() {
  // TODO: Implement edit group
  console.log('Edit group:', props.item)
}

function updateLogic(itemId: string, logic: string) {
  if (props.item.type === 'group') {
    const updatedGroup = {
      ...props.item,
      items: props.item.items.map(item =>
        item.id === itemId ? { ...item, logic: logic as 'AND' | 'OR' } : item
      )
    }
    emit('update:item', updatedGroup)
  }
}

function updateSubItem(itemId: string, updatedItem: TreeItem) {
  if (props.item.type === 'group') {
    const updatedGroup = {
      ...props.item,
      items: props.item.items.map(item =>
        item.id === itemId ? updatedItem : item
      )
    }
    emit('update:item', updatedGroup)
  }
}

function removeSubItem(itemId: string) {
  if (props.item.type === 'group') {
    const updatedGroup = {
      ...props.item,
      items: props.item.items.filter(item => item.id !== itemId)
    }
    emit('update:item', updatedGroup)
  }
}

function addSubCondition(parentId: string) {
  // TODO: Implement add sub condition
  console.log('Add sub condition to:', parentId)
}

function addSubGroup(parentId: string) {
  // TODO: Implement add sub group
  console.log('Add sub group to:', parentId)
}

function onAddToGroup(event: any) {
  console.log('Item added to group:', event)
  // Emit event to parent to handle the addition
  emit('add:item', { groupId: props.item.id, item: event.item })
}
</script>

<style scoped>
.filter-tree-item {
  position: relative;
}

.condition-item {
  position: relative;
}

.group-item {
  position: relative;
}

.group-header {
  position: relative;
}

.group-items {
  position: relative;
}

/* Drag and Drop Styles */
.ghost {
  opacity: 0.5;
  background-color: #f3f4f6;
  border: 2px dashed #d1d5db;
}

.chosen {
  background-color: #dbeafe;
  border-color: #93c5fd;
}

.drag {
  background-color: #bfdbfe;
  border-color: #60a5fa;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
}
</style>
