<template>
  <div class="flex items-center gap-3 w-full">
    <!-- Logic Selector (for multiple conditions) -->
    <USelect
      v-if="showLogic"
      :model-value="condition.logic || 'AND'"
      :items="logicOptions"
      size="sm"
      class="w-20"
      @update:model-value="(value) => updateCondition({ ...condition, logic: value })"
    />

    <!-- Field Selector -->
    <USelect
      :model-value="condition.field || ''"
      :items="fieldOptions"
      placeholder="Select field"
      size="sm"
      class="min-w-32"
      @update:model-value="
        (value) => updateCondition({ ...condition, field: value as string, value: '' })
      "
    />

    <!-- Operator Selector -->
    <USelect
      :model-value="condition.operator || 'eq'"
      :items="operatorOptions"
      placeholder="Operator"
      size="sm"
      class="min-w-24"
      @update:model-value="
        (value) => updateCondition({ ...condition, operator: value as any, value: '' })
      "
    />

    <!-- Value Input -->
    <div class="flex-1">
      <!-- Text Input -->
      <UInput
        v-if="isTextInput"
        :model-value="condition.value || ''"
        :placeholder="getPlaceholder()"
        size="sm"
        @update:model-value="(value) => updateCondition({ ...condition, value })"
      />

      <!-- Number Input -->
      <UInput
        v-else-if="isNumberInput"
        :model-value="condition.value || ''"
        type="number"
        :placeholder="getPlaceholder()"
        size="sm"
        @update:model-value="(value) => updateCondition({ ...condition, value: Number(value) })"
      />

      <!-- Date Input -->
      <UInput
        v-else-if="isDateInput"
        :model-value="condition.value || ''"
        type="date"
        :placeholder="getPlaceholder()"
        size="sm"
        @update:model-value="(value) => updateCondition({ ...condition, value })"
      />

      <!-- Select Input -->
      <USelect
        v-else-if="isSelectInput"
        :model-value="condition.value || ''"
        :items="getSelectOptions()"
        :placeholder="getPlaceholder()"
        size="sm"
        :multiple="isMultiSelect"
        @update:model-value="(value) => updateCondition({ ...condition, value })"
      />

      <!-- Range Input (for between operator) -->
      <div v-else-if="isRangeInput" class="flex items-center gap-2">
        <UInput
          :model-value="getRangeValue(0)"
          type="number"
          placeholder="Min"
          size="sm"
          class="w-24"
          @update:model-value="(value) => updateRangeValue(0, value)"
        />
        <span class="text-sm text-gray-500">to</span>
        <UInput
          :model-value="getRangeValue(1)"
          type="number"
          placeholder="Max"
          size="sm"
          class="w-24"
          @update:model-value="(value) => updateRangeValue(1, value)"
        />
      </div>

      <!-- No Value Input (for null/empty operators) -->
      <div v-else class="text-sm text-gray-500 italic">No value needed</div>
    </div>

    <!-- Remove Button -->
    <UButton
      variant="ghost"
      color="error"
      size="sm"
      icon="i-heroicons-trash"
      @click="$emit('remove:condition')"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FilterableColumn, FilterCondition } from '../../types/filter'

interface Props {
  condition: FilterCondition
  columns: FilterableColumn[]
  showLogic?: boolean
}

interface Emits {
  (e: 'update:condition', condition: FilterCondition): void
  (e: 'remove:condition'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// Computed properties
const selectedColumn = computed(() => {
  return props.columns.find((col) => col.field === props.condition.field)
})

const fieldOptions = computed(() => {
  return props.columns
    .filter((col) => col.filterable !== false)
    .map((col) => ({
      label: col.label,
      value: col.field,
    }))
})

const operatorOptions = computed(() => {
  const column = selectedColumn.value
  if (!column) return []

  const operators = getOperatorsForType(column.type)
  return operators.map((op) => ({
    label: getOperatorLabel(op),
    value: op,
  }))
})

const isTextInput = computed(() => {
  const column = selectedColumn.value
  if (!column) return false

  return (
    (column.type === 'string' &&
      ['eq', 'ne', 'contains', 'startsWith', 'endsWith', 'regex'].includes(
        props.condition.operator,
      )) ||
    (column.type === 'number' &&
      ['eq', 'ne', 'gt', 'gte', 'lt', 'lte'].includes(props.condition.operator))
  )
})

const isNumberInput = computed(() => {
  const column = selectedColumn.value
  if (!column) return false

  return (
    column.type === 'number' &&
    ['eq', 'ne', 'gt', 'gte', 'lt', 'lte'].includes(props.condition.operator)
  )
})

const isDateInput = computed(() => {
  const column = selectedColumn.value
  if (!column) return false

  return (
    column.type === 'date' &&
    ['eq', 'ne', 'gt', 'gte', 'lt', 'lte'].includes(props.condition.operator)
  )
})

const isSelectInput = computed(() => {
  const column = selectedColumn.value
  if (!column) return false

  return column.type === 'enum' || ['in', 'notIn'].includes(props.condition.operator)
})

const isMultiSelect = computed(() => {
  return ['in', 'notIn', 'containsAny', 'containsAll'].includes(props.condition.operator)
})

const isRangeInput = computed(() => {
  return props.condition.operator === 'between'
})

// Methods
function updateCondition(condition: FilterCondition) {
  emit('update:condition', condition)
}

function getPlaceholder(): string {
  const column = selectedColumn.value
  if (!column) return 'Select field first'

  return column.filterConfig?.placeholder || `Enter ${column.label.toLowerCase()}...`
}

function getSelectOptions() {
  const column = selectedColumn.value
  if (!column) return []

  if (column.type === 'enum' && column.enumOptions) {
    return column.enumOptions
  }

  // For in/notIn operators, return field options for multi-select
  if (['in', 'notIn'].includes(props.condition.operator)) {
    return fieldOptions.value
  }

  return []
}

function getRangeValue(index: number) {
  if (Array.isArray(props.condition.value)) {
    return props.condition.value[index] || ''
  }
  return ''
}

function updateRangeValue(index: number, value: string) {
  const currentValue = Array.isArray(props.condition.value) ? [...props.condition.value] : ['', '']
  currentValue[index] = value
  updateCondition({ ...props.condition, value: currentValue })
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

const logicOptions = [
  { label: 'AND', value: 'AND' },
  { label: 'OR', value: 'OR' },
]
</script>
