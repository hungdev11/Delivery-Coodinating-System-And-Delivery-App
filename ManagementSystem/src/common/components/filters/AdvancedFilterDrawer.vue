<template>
  <div>
    <UDrawer
      :open="isOpen"
      :direction="drawerDirection"
      :dismissible="false"
      :handle="false"
      :ui="{ header: 'flex items-center justify-between' }"
      @close="close"
    >
      <template #header>
        <div class="flex items-center justify-between w-full">
          <h2 class="text-highlighted font-semibold text-lg">Advanced Filters</h2>
          <div class="flex items-center gap-2">
            <UButton
              variant="soft"
              color="primary"
              size="sm"
              icon="i-heroicons-plus"
              @click="showConditionPopup = true"
            >
              Add Condition
            </UButton>
            <UButton
              variant="soft"
              color="primary"
              size="sm"
              icon="i-heroicons-folder-plus"
              @click="addGroup"
            >
              Add Group
            </UButton>
            <UButton color="neutral" variant="ghost" icon="i-heroicons-x-mark" @click="close" />
          </div>
        </div>
      </template>

      <template #body>
        <div class="space-y-6">
          <!-- Condition Pool Section -->
          <div class="border-b pb-4">
            <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
              Available Conditions
            </h3>
            <draggable
              v-model="availableConditions"
              :group="{ name: 'filter-items', pull: true, put: false }"
              :animation="200"
              ghost-class="ghost"
              chosen-class="chosen"
              drag-class="drag"
              @start="onDragStart"
              @end="onDragEnd"
              @remove="onRemove"
              @add="onAddToAvailable"
              class="grid grid-cols-2 gap-2"
            >
              <template #item="{ element: condition }">
                <div
                  class="p-2 bg-blue-50 dark:bg-blue-900/20 rounded border border-blue-200 dark:border-blue-700 cursor-move"
                >
                  <div class="text-xs font-medium text-blue-700 dark:text-blue-300">
                    {{ getColumnLabel(condition.field) }}
                  </div>
                  <div class="text-xs text-blue-600 dark:text-blue-400">
                    {{ getOperatorLabel(condition.operator) }} {{ formatValue(condition.value) }}
                  </div>
                </div>
              </template>
            </draggable>
            <UButton
              v-if="availableConditions.length === 0"
              variant="soft"
              color="primary"
              size="sm"
              icon="i-heroicons-plus"
              @click="showConditionPopup = true"
              class="w-full mt-2"
            >
              Create First Condition
            </UButton>
          </div>

          <!-- Filter Builder Section -->
          <div class="border-b pb-4">
            <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
              Filter Builder
            </h3>
            <div
              class="min-h-[200px] p-4 border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-lg"
              @drop="handleDrop($event, null)"
              @dragover.prevent
              @dragenter.prevent
            >
              <div
                v-if="filterTree.length === 0"
                class="text-center text-gray-500 dark:text-gray-400"
              >
                <div class="mb-4">Drag conditions here to start building your filter</div>
                <div class="flex gap-2 justify-center">
                  <UButton
                    variant="outline"
                    icon="i-heroicons-plus"
                    @click="showConditionPopup = true"
                  >
                    Add Condition
                  </UButton>
                  <UButton variant="outline" icon="i-heroicons-folder-plus" @click="addGroup">
                    Add Group
                  </UButton>
                </div>
              </div>

              <!-- Filter Tree Render -->
              <draggable
                v-else
                v-model="filterTree"
                :group="{ name: 'filter-items', pull: true, put: true }"
                :animation="200"
                ghost-class="ghost"
                chosen-class="chosen"
                drag-class="drag"
                @start="onDragStart"
                @end="onDragEnd"
                @add="onAdd"
                @remove="onRemoveFromFilter"
                class="space-y-2"
              >
                <template #item="{ element: item, index }">
                  <div class="space-y-2">
                    <!-- Logic Operator (except for first item) -->
                    <div v-if="index > 0" class="flex items-center justify-center">
                      <USelect
                        :model-value="item.logic || 'AND'"
                        :items="logicOptions"
                        size="sm"
                        class="w-20"
                        @update:model-value="(value) => updateItemLogic(item.id, value as string)"
                      />
                    </div>

                    <!-- Render Item (Condition or Group) -->
                    <FilterTreeItem
                      :item="item"
                      :columns="columns"
                      @update:item="(updatedItem: any) => updateTreeItem(item.id, updatedItem)"
                      @remove:item="() => removeTreeItem(item.id)"
                      @add:group="() => addNestedGroup(item.id)"
                    />
                  </div>
                </template>
              </draggable>
            </div>
          </div>

          <!-- Preview Section -->
          <div v-if="filterTree.length > 0" class="border-t pt-4">
            <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Filter Preview:
            </h3>
            <div
              class="text-sm text-gray-600 dark:text-gray-400 bg-gray-100 dark:bg-gray-700 p-3 rounded"
            >
              <pre class="whitespace-pre-wrap">{{ formatFilterTree() }}</pre>
            </div>
          </div>
        </div>
      </template>

      <template #footer>
        <div class="flex justify-between">
          <div class="flex gap-2">
            <UButton variant="soft" color="neutral" @click="clearAllFilters"> Clear All </UButton>
            <UButton variant="ghost" @click="close"> Cancel </UButton>
          </div>
          <UButton color="primary" @click="applyFilters"> Apply Filters </UButton>
        </div>
      </template>
    </UDrawer>

    <!-- Condition Creation Popup -->
    <UModal
      v-model:open="showConditionPopup"
      title="Create Filter Condition"
      description="Create a new filter condition to add to your filter tree"
    >
      <template #body>
        <div class="space-y-4">
          <!-- Field Selection -->
          <div>
            <label class="block text-sm font-medium mb-2">Field</label>
            <USelect
              class="w-full"
              v-model="newCondition.field"
              :items="fieldOptions"
              placeholder="Select field"
            />
          </div>

          <!-- Operator Selection -->
          <div>
            <label class="block text-sm font-medium mb-2">Operator</label>
            <USelect
              class="w-full"
              v-model="newCondition.operator"
              :items="getOperatorOptions(newCondition.field)"
              placeholder="Select operator"
            />
          </div>

          <!-- Value Input -->
          <div v-if="needsValue(newCondition.operator)">
            <label class="block text-sm font-medium mb-2">Value</label>
            <UInput
              class="w-full"
              v-model="newCondition.value"
              :placeholder="getPlaceholder(newCondition.field)"
            />
          </div>
        </div>
      </template>

      <template #footer="{ close }">
        <UButton label="Cancel" color="neutral" variant="outline" @click="close" />
        <UButton label="Create" color="primary" @click="createCondition" />
      </template>
    </UModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { FilterableColumn, FilterCondition, FilterGroup } from '../../types/filter'
import FilterTreeItem from './FilterTreeItem.vue'
import draggable from 'vuedraggable'
import { useResponsiveStore } from '@/common/store/responsive.store'

interface Props {
  show: boolean
  columns: FilterableColumn[]
  activeFilters?: FilterCondition[]
  activeFilterGroup?: FilterGroup
}

interface Emits {
  (e: 'update:show', value: boolean): void
  (e: 'apply', filterGroup: FilterGroup): void
  (e: 'clear'): void
}

// Tree item types
interface TreeCondition extends FilterCondition {
  id: string
  type: 'condition'
  logic?: 'AND' | 'OR'
}

interface TreeGroup {
  id: string
  type: 'group'
  logic?: 'AND' | 'OR'
  items: (TreeCondition | TreeGroup)[]
}

type TreeItem = TreeCondition | TreeGroup

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const responsiveStore = useResponsiveStore()

// Drawer direction: top for mobile/Android, bottom for desktop
const drawerDirection = computed(() => {
  return responsiveStore.isMobile || responsiveStore.isAndroid ? 'top' : 'bottom'
})

// State
const availableConditions = ref<TreeCondition[]>([])
const filterTree = ref<TreeItem[]>([])
const showConditionPopup = ref(false)
const nextId = ref(1)

// New condition form
const newCondition = ref({
  field: '',
  operator: 'eq',
  value: '',
})

// Computed
const isOpen = computed({
  get: () => props.show,
  set: (value) => emit('update:show', value),
})

const logicOptions = [
  { label: 'AND', value: 'AND' },
  { label: 'OR', value: 'OR' },
]

const fieldOptions = computed(() => {
  return props.columns.map((col) => ({
    label: col.label,
    value: col.field,
  }))
})

// Methods
function generateId(): string {
  return `item-${nextId.value++}`
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

function formatValue(value: unknown): string {
  if (Array.isArray(value)) {
    return `[${value.join(', ')}]`
  }
  if (typeof value === 'boolean') {
    return value ? 'Yes' : 'No'
  }
  if (value === null || value === undefined) {
    return ''
  }
  return String(value)
}

function getOperatorOptions(field: string): Array<{ label: string; value: string }> {
  const column = props.columns.find((col) => col.field === field)
  if (!column) return []

  const operators = getOperatorsForType(column.type)
  return operators.map((op) => ({
    label: getOperatorLabel(op),
    value: op,
  }))
}

function getOperatorsForType(type: string): string[] {
  switch (type) {
    case 'string':
      return ['eq', 'ne', 'contains', 'startsWith', 'endsWith', 'regex', 'isEmpty', 'isNotEmpty']
    case 'number':
      return ['eq', 'ne', 'gt', 'gte', 'lt', 'lte', 'between', 'isNull', 'isNotNull']
    case 'date':
      return ['eq', 'ne', 'gt', 'gte', 'lt', 'lte', 'between', 'isNull', 'isNotNull']
    case 'enum':
      return ['eq', 'ne', 'in', 'notIn']
    case 'boolean':
      return ['eq', 'ne', 'isNull', 'isNotNull']
    default:
      return ['eq', 'ne', 'isNull', 'isNotNull']
  }
}

function needsValue(operator: string): boolean {
  return !['isNull', 'isNotNull', 'isEmpty', 'isNotEmpty'].includes(operator)
}

function getPlaceholder(field: string): string {
  const column = props.columns.find((col) => col.field === field)
  return column?.filterConfig?.placeholder || `Enter ${getColumnLabel(field).toLowerCase()}...`
}

// Drag and Drop
function handleDragStart(event: DragEvent, condition: TreeCondition) {
  if (event.dataTransfer) {
    event.dataTransfer.setData(
      'application/json',
      JSON.stringify({
        type: 'condition',
        data: condition,
      }),
    )
  }
}

function handleDrop(event: DragEvent, parentId: string | null) {
  event.preventDefault()

  if (!event.dataTransfer) return

  try {
    const dragData = JSON.parse(event.dataTransfer.getData('application/json'))

    if (dragData.type === 'condition') {
      const condition = dragData.data as TreeCondition

      // Remove from available conditions if it exists there
      const availableIndex = availableConditions.value.findIndex((c) => c.id === condition.id)
      if (availableIndex !== -1) {
        availableConditions.value.splice(availableIndex, 1)
      }

      // Add to filter tree
      if (parentId) {
        addItemToGroup(parentId, condition)
      } else {
        filterTree.value.push(condition)
      }
    }
  } catch (error) {
    console.error('Failed to handle drop:', error)
  }
}

function addItemToGroup(groupId: string, item: TreeItem) {
  function findGroup(items: TreeItem[], id: string): TreeGroup | null {
    for (const item of items) {
      if (item.id === id && item.type === 'group') {
        return item as TreeGroup
      }
      if (item.type === 'group') {
        const found = findGroup((item as TreeGroup).items, id)
        if (found) return found
      }
    }
    return null
  }

  const group = findGroup(filterTree.value, groupId)
  if (group) {
    group.items.push(item)
  }
}

// Condition Creation
function createCondition() {
  if (!newCondition.value.field || !newCondition.value.operator) return

  const condition: TreeCondition = {
    id: generateId(),
    type: 'condition',
    field: newCondition.value.field,
    operator: newCondition.value.operator as any,
    value: newCondition.value.value,
    logic: 'AND',
  }

  availableConditions.value.push(condition)

  // Reset form
  newCondition.value = {
    field: '',
    operator: 'eq',
    value: '',
  }

  showConditionPopup.value = false
}

// Tree Management
function updateItemLogic(itemId: string, logic: string) {
  function updateLogic(items: TreeItem[], id: string) {
    for (const item of items) {
      if (item.id === id) {
        item.logic = logic as 'AND' | 'OR'
        return
      }
      if (item.type === 'group') {
        updateLogic((item as TreeGroup).items, id)
      }
    }
  }

  updateLogic(filterTree.value, itemId)
}

function updateTreeItem(itemId: string, updatedItem: any) {
  function updateItem(items: TreeItem[], id: string) {
    const index = items.findIndex((item) => item.id === id)
    if (index !== -1) {
      items[index] = updatedItem
      return
    }
    for (const item of items) {
      if (item.type === 'group') {
        updateItem((item as TreeGroup).items, id)
      }
    }
  }

  updateItem(filterTree.value, itemId)
}

function removeTreeItem(itemId: string) {
  function removeItem(items: TreeItem[], id: string): boolean {
    const index = items.findIndex((item) => item.id === id)
    if (index !== -1) {
      const removedItem = items[index]
      items.splice(index, 1)

      // If it's a condition, add it back to available conditions
      if (removedItem.type === 'condition') {
        availableConditions.value.push(removedItem as TreeCondition)
      }
      return true
    }

    for (const item of items) {
      if (item.type === 'group') {
        if (removeItem((item as TreeGroup).items, id)) {
          return true
        }
      }
    }
    return false
  }

  removeItem(filterTree.value, itemId)
}

function addNestedGroup(parentId: string) {
  const newGroup: TreeGroup = {
    id: generateId(),
    type: 'group',
    logic: 'AND',
    items: [],
  }

  addItemToGroup(parentId, newGroup)
}

function addGroup() {
  const newGroup: TreeGroup = {
    id: generateId(),
    type: 'group',
    logic: 'AND',
    items: [],
  }

  filterTree.value.push(newGroup)
}

function addConditionToGroup(groupId: string, condition: TreeCondition) {
  function addToGroup(items: TreeItem[], id: string) {
    for (const item of items) {
      if (item.id === id && item.type === 'group') {
        ;(item as TreeGroup).items.push(condition)
        return
      }
      if (item.type === 'group') {
        addToGroup((item as TreeGroup).items, id)
      }
    }
  }

  addToGroup(filterTree.value, groupId)
}

// Drag and Drop Events
function onDragStart(event: any) {
  console.log('Drag started:', event)
}

function onDragEnd(event: any) {
  console.log('Drag ended:', event)
}

function onAdd(event: any) {
  console.log('Item added to filter tree:', event)

  // Check if the item was dragged from available conditions
  if (event.from && event.from.classList && event.from.classList.contains('grid')) {
    // This means it was dragged from available conditions
    const addedElement = event.item.__vueParentComponent?.ctx.element
    if (addedElement && addedElement.type === 'condition') {
      // Find and remove from available conditions
      const index = availableConditions.value.findIndex((c) => c.id === addedElement.id)
      if (index !== -1) {
        availableConditions.value.splice(index, 1)
      }
    }
  }
}

function onRemove(event: any) {
  console.log('Item removed from available conditions:', event)
  // This will be handled automatically by Vue.Draggable
}

function onAddToAvailable(event: any) {
  console.log('Item added to available conditions:', event)
  // This will be handled automatically by Vue.Draggable
}

function onRemoveFromFilter(event: any) {
  console.log('Item removed from filter tree:', event)

  // Check if the item was moved to available conditions
  if (event.to && event.to.classList && event.to.classList.contains('grid')) {
    // This means it was moved to available conditions
    const removedElement = event.item.__vueParentComponent?.ctx.element
    if (removedElement && removedElement.type === 'condition') {
      // Add back to available conditions
      availableConditions.value.push(removedElement)
    }
  }
}

// Filter Operations
function clearAllFilters() {
  // Move all conditions back to available conditions
  function collectConditions(items: TreeItem[]): TreeCondition[] {
    const conditions: TreeCondition[] = []
    for (const item of items) {
      if (item.type === 'condition') {
        conditions.push(item as TreeCondition)
      } else if (item.type === 'group') {
        conditions.push(...collectConditions((item as TreeGroup).items))
      }
    }
    return conditions
  }

  availableConditions.value.push(...collectConditions(filterTree.value))
  filterTree.value = []
}

function applyFilters() {
  // Convert tree to FilterGroup
  function treeToFilterGroup(items: TreeItem[]): FilterGroup {
    if (items.length === 0) {
      return { logic: 'AND', conditions: [] }
    }

    if (items.length === 1) {
      const item = items[0]
      if (item.type === 'condition') {
        return {
          logic: 'AND',
          conditions: [
            {
              field: item.field,
              operator: item.operator,
              value: item.value,
              caseSensitive: item.caseSensitive,
            },
          ],
        }
      } else {
        return treeToFilterGroup(item.items)
      }
    }

    // Convert all items to FilterCondition or FilterGroup
    const allItems: (FilterCondition | FilterGroup)[] = []

    for (let i = 0; i < items.length; i++) {
      const item = items[i]
      if (item.type === 'condition') {
        // Convert TreeCondition to FilterCondition
        const condition: FilterCondition = {
          field: item.field,
          operator: item.operator,
          value: item.value,
          caseSensitive: item.caseSensitive,
        }
        allItems.push(condition)
      } else {
        // Convert TreeGroup to FilterGroup
        const group: FilterGroup = {
          logic: item.logic || 'AND',
          conditions: treeToFilterGroup(item.items).conditions,
        }
        allItems.push(group)
      }
    }

    // Determine the logic for the group based on the first item's logic
    const groupLogic = items[1]?.logic || 'AND'

    return {
      logic: groupLogic,
      conditions: allItems,
    }
  }

  const filterGroup = treeToFilterGroup(filterTree.value)
  emit('apply', filterGroup)
  close()
}

function close() {
  emit('update:show', false)
}

function formatFilterTree(): string {
  function formatItem(item: TreeItem): string {
    if (item.type === 'condition') {
      const condition = item as TreeCondition
      const fieldLabel = getColumnLabel(condition.field)
      const operatorLabel = getOperatorLabel(condition.operator)
      const valueLabel = formatValue(condition.value)
      return `${fieldLabel} ${operatorLabel} ${valueLabel}`
    } else {
      const group = item as TreeGroup
      const items = group.items.map(formatItem).join(` ${group.logic} `)
      return `(${items})`
    }
  }

  return filterTree.value
    .map((item, index) => {
      const itemStr = formatItem(item)
      return index > 0 ? `${item.logic} ${itemStr}` : itemStr
    })
    .join(' ')
}

// Initialize from props when modal opens
watch(
  () => props.show,
  (show) => {
    if (show) {
      // Load active filters when opening
      if (props.activeFilterGroup) {
        // Convert FilterGroup to tree structure
        filterTree.value = convertFilterGroupToTree(props.activeFilterGroup)
      } else if (props.activeFilters && props.activeFilters.length > 0) {
        // Convert active filters to available conditions
        availableConditions.value = props.activeFilters.map((filter) => ({
          ...filter,
          id: generateId(),
          type: 'condition' as const,
        }))

        // Convert to filter tree structure
        filterTree.value = convertFiltersToTree(props.activeFilters)
      }
    }
    // Don't reset when closing - keep the state
  },
)

// Convert FilterGroup to tree structure
function convertFilterGroupToTree(filterGroup: FilterGroup): TreeItem[] {
  const result: TreeItem[] = []

  // Add conditions
  if (filterGroup.conditions) {
    filterGroup.conditions.forEach((condition) => {
      if ('field' in condition) {
        // It's a FilterCondition
        result.push({
          ...condition,
          id: generateId(),
          type: 'condition' as const,
        })
      } else {
        // It's a FilterGroup, convert to TreeGroup
        const treeGroup: TreeGroup = {
          id: generateId(),
          type: 'group',
          logic: condition.logic,
          items: convertFilterGroupToTree(condition),
        }
        result.push(treeGroup)
      }
    })
  }

  // Nested groups are already handled in the conditions loop above
  // since FilterGroup.conditions can contain both FilterCondition and FilterGroup

  return result
}

// Convert flat filters to tree structure
function convertFiltersToTree(filters: FilterCondition[]): TreeItem[] {
  // For now, just convert to flat conditions
  // This can be enhanced to detect nested structure from the filters
  return filters.map((filter) => ({
    ...filter,
    id: generateId(),
    type: 'condition' as const,
  }))
}
</script>

<style scoped>
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
