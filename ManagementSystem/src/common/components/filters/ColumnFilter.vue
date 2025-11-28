<template>
  <div class="column-filter">
    <UPopover v-model:open="open">
      <UButton
        icon="i-heroicons-funnel"
        :color="hasActiveFilters ? 'primary' : 'neutral'"
        :variant="hasActiveFilters ? 'solid' : 'ghost'"
        size="sm"
        :title="`Filter by ${column?.label || '(?)'}`"
      />

      <template #content>
        <div class="p-4 w-80">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-medium text-gray-900 dark:text-gray-100">
              Filter by {{ column?.label || '(?)' }}
            </h3>
            <UButton
              v-if="hasActiveFilters"
              icon="i-heroicons-x-mark"
              size="xs"
              variant="ghost"
              @click="clearFilters"
            />
          </div>

          <div class="space-y-3">
            <!-- Operator Selection -->
            <div>
              <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">
                Operator
              </label>
              <USelect
                v-model="currentOperator"
                :items="operatorOptions"
                placeholder="Select operator"
                size="sm"
                class="w-full"
              />
            </div>

            <!-- Value Input -->
            <div v-if="needsValue">
              <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">
                Value
              </label>

              <!-- Text input -->
              <UInput
                v-if="getFilterType() === 'text'"
                v-model="currentValue"
                :placeholder="column.filterConfig?.placeholder || getValuePlaceholder()"
                size="sm"
                class="w-full"
              />

              <!-- Number input -->
              <UInput
                v-else-if="getFilterType() === 'number'"
                v-model.number="currentValue"
                type="number"
                :placeholder="column.filterConfig?.placeholder || getValuePlaceholder()"
                :min="column.filterConfig?.min"
                :max="column.filterConfig?.max"
                :step="column.filterConfig?.step"
                size="sm"
                class="w-full"
              />

              <!-- Date input -->
              <UInput
                v-else-if="getFilterType() === 'date'"
                v-model="currentValue"
                type="date"
                :placeholder="column.filterConfig?.placeholder || getValuePlaceholder()"
                size="sm"
                class="w-full"
              />

              <!-- Date range picker -->
              <div v-else-if="getFilterType() === 'daterange'" class="space-y-2">
                <UCalendar
                  v-model="dateRangeValue"
                  range
                  :number-of-months="2"
                  size="sm"
                  class="w-full"
                />
              </div>

              <!-- Boolean select -->
              <USelect
                v-else-if="getFilterType() === 'boolean'"
                v-model="currentValue"
                :items="booleanOptions"
                placeholder="Select value"
                size="sm"
                class="w-full"
              />

              <!-- Select (single) -->
              <USelect
                v-else-if="getFilterType() === 'select' && !column.filterConfig?.multiple"
                v-model="currentValue"
                :items="getSelectOptions()"
                :placeholder="column.filterConfig?.placeholder || 'Select value'"
                size="sm"
                class="w-full"
              />

              <!-- Select (multiple) -->
              <USelect
                v-else-if="getFilterType() === 'select' && column.filterConfig?.multiple"
                v-model="currentValue"
                :items="getSelectOptions()"
                :placeholder="column.filterConfig?.placeholder || 'Select values'"
                multiple
                size="sm"
                class="w-full"
              />

              <!-- Range input -->
              <div v-else-if="getFilterType() === 'range'" class="space-y-2">
                <UInput
                  v-model="rangeValues[0]"
                  :type="column.type === 'date' ? 'date' : 'number'"
                  :placeholder="column.filterConfig?.min?.toString() || 'Min'"
                  size="sm"
                  class="w-full"
                />
                <UInput
                  v-model="rangeValues[1]"
                  :type="column.type === 'date' ? 'date' : 'number'"
                  :placeholder="column.filterConfig?.max?.toString() || 'Max'"
                  size="sm"
                  class="w-full"
                />
              </div>

              <!-- Array input (comma-separated) -->
              <UInput
                v-else-if="column.type === 'array'"
                v-model="arrayValueString"
                placeholder="Enter values separated by commas"
                size="sm"
                class="w-full"
              />
            </div>

            <!-- Between values input -->
            <div v-if="currentOperator === 'between'" class="space-y-2">
              <div>
                <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">
                  From
                </label>
                <UInput
                  v-model="betweenValues[0]"
                  :type="
                    column.type === 'date' ? 'date' : column.type === 'number' ? 'number' : 'text'
                  "
                  placeholder="From value"
                  size="sm"
                  class="w-full"
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">
                  To
                </label>
                <UInput
                  v-model="betweenValues[1]"
                  :type="
                    column.type === 'date' ? 'date' : column.type === 'number' ? 'number' : 'text'
                  "
                  placeholder="To value"
                  size="sm"
                  class="w-full"
                />
              </div>
            </div>

            <!-- Case sensitive toggle for string fields -->
            <div
              v-if="
                column.type === 'string' &&
                ['contains', 'startsWith', 'endsWith', 'regex'].includes(currentOperator)
              "
            >
              <UCheckbox v-model="caseSensitive" label="Case sensitive" size="sm" class="w-full" />
            </div>
          </div>

          <!-- Action buttons -->
          <div
            class="flex justify-end space-x-2 mt-4 pt-3 border-t border-gray-200 dark:border-gray-700"
          >
            <UButton variant="ghost" size="sm" @click="cancel"> Cancel </UButton>
            <UButton :disabled="!isValidFilter" size="sm" @click="applyFilter"> Apply </UButton>
          </div>
        </div>
      </template>
    </UPopover>
  </div>
</template>

<script setup lang="ts" generic="TData">
import { computed, ref, watch } from 'vue'
import { CalendarDate } from '@internationalized/date'
import type { FilterableColumn, FilterCondition, AllOperators } from '@/common/types/filter'
import { getOperatorsForType } from '@/common/utils/query-builder'

interface Props<TData> {
  column: FilterableColumn
  activeFilters?: FilterCondition[] | undefined
}

interface Emits {
  (e: 'update:filters', filters: FilterCondition[]): void
}

const props = defineProps<Props<TData>>()
const emit = defineEmits<Emits>()

// Reactive state
const currentOperator = ref<AllOperators>('eq')
const currentValue = ref<
  string | number | boolean | string[] | number[] | boolean[] | null | undefined
>('')
const betweenValues = ref<
  [
    string | number | boolean | string[] | number[] | boolean[] | null | undefined,
    string | number | boolean | string[] | number[] | boolean[] | null | undefined,
  ]
>(['', ''])
const rangeValues = ref<
  [
    string | number | boolean | string[] | number[] | boolean[] | null | undefined,
    string | number | boolean | string[] | number[] | boolean[] | null | undefined,
  ]
>(['', ''])
const dateRangeValue = ref<{ start?: CalendarDate; end?: CalendarDate } | null>(null)
const arrayValueString = ref('')
const caseSensitive = ref(false)
const open = ref(false)

// Computed properties
const operatorOptions = computed(() => {
  const operators = getOperatorsForType(props.column.type)
  return operators.map((op) => ({
    label: getOperatorLabel(op),
    value: op,
  }))
})

const needsValue = computed(() => {
  return !['isNull', 'isNotNull', 'isEmpty', 'isNotEmpty'].includes(currentOperator.value)
})

const isValidFilter = computed(() => {
  if (!needsValue.value) return true

  if (currentOperator.value === 'between') {
    return betweenValues.value[0] !== '' && betweenValues.value[1] !== ''
  }

  if (['in', 'notIn', 'containsAny', 'containsAll'].includes(currentOperator.value)) {
    return arrayValueString.value.trim() !== ''
  }

  return (
    currentValue.value !== '' && currentValue.value !== null && currentValue.value !== undefined
  )
})

const hasActiveFilters = computed(() => {
  return (props.activeFilters?.length || 0) > 0
})

const booleanOptions = [
  { label: 'True', value: true },
  { label: 'False', value: false },
]

// Helper methods
function getFilterType(): string {
  return props.column.filterType || getDefaultFilterType()
}

function getDefaultFilterType(): string {
  switch (props.column.type) {
    case 'string':
      return 'text'
    case 'number':
      return 'number'
    case 'date':
      // For date fields, check if current operator supports range
      if (currentOperator.value === 'between') {
        return 'daterange'
      }
      return 'date'
    case 'boolean':
      return 'boolean'
    case 'enum':
      return 'select'
    case 'array':
      return 'text'
    default:
      return 'text'
  }
}

function getSelectOptions() {
  if (props.column.enumOptions) {
    return props.column.enumOptions
  }

  // Generate options based on field type
  switch (props.column.type) {
    case 'boolean':
      return booleanOptions
    default:
      return []
  }
}

// Watch for operator changes to reset value
watch(currentOperator, () => {
  if (currentOperator.value === 'between') {
    betweenValues.value = ['', '']
  } else if (getFilterType() === 'range') {
    rangeValues.value = ['', '']
  } else if (['in', 'notIn', 'containsAny', 'containsAll'].includes(currentOperator.value)) {
    arrayValueString.value = ''
  } else {
    currentValue.value = ''
  }
})

// Methods
function getOperatorLabel(operator: AllOperators): string {
  const labels: Record<AllOperators, string> = {
    eq: 'Equals',
    ne: 'Not equals',
    contains: 'Contains',
    startsWith: 'Starts with',
    endsWith: 'Ends with',
    regex: 'Regex',
    in: 'In list',
    notIn: 'Not in list',
    gt: 'Greater than',
    gte: 'Greater than or equal',
    lt: 'Less than',
    lte: 'Less than or equal',
    between: 'Between',
    isNull: 'Is null',
    isNotNull: 'Is not null',
    containsAny: 'Contains any',
    containsAll: 'Contains all',
    isEmpty: 'Is empty',
    isNotEmpty: 'Is not empty',
  }
  return labels[operator] || operator
}

function getValuePlaceholder(): string {
  if (currentOperator.value === 'between') return ''

  const placeholders: Record<string, string> = {
    contains: 'Enter text to search for...',
    startsWith: 'Enter starting text...',
    endsWith: 'Enter ending text...',
    regex: 'Enter regular expression...',
    in: 'Enter values separated by commas...',
    notIn: 'Enter values separated by commas...',
    containsAny: 'Enter values separated by commas...',
    containsAll: 'Enter values separated by commas...',
  }

  return placeholders[currentOperator.value] || 'Enter value...'
}

function clearFilters() {
  emit('update:filters', [])
}

function cancel() {
  // Reset to current active filters
  if (props.activeFilters && props.activeFilters.length > 0) {
    const firstFilter = props.activeFilters[0]
    currentOperator.value = firstFilter.operator
    currentValue.value = firstFilter.value
    caseSensitive.value = firstFilter.caseSensitive || false
  }
  open.value = false
}

function applyFilter() {
  if (!isValidFilter.value) return

  let value: string | number | boolean | string[] | number[] | boolean[] | null | undefined =
    currentValue.value

  if (currentOperator.value === 'between') {
    value = [betweenValues.value[0], betweenValues.value[1]]
  } else if (getFilterType() === 'range') {
    value = [rangeValues.value[0], rangeValues.value[1]]
  } else if (getFilterType() === 'daterange' && dateRangeValue.value) {
    // Handle date range from UCalendar
    const startDate = dateRangeValue.value.start
    const endDate = dateRangeValue.value.end
    if (startDate && endDate) {
      // Convert CalendarDate to ISO string format
      const startStr = `${startDate.year}-${String(startDate.month).padStart(2, '0')}-${String(startDate.day).padStart(2, '0')}`
      const endStr = `${endDate.year}-${String(endDate.month).padStart(2, '0')}-${String(endDate.day).padStart(2, '0')}`
      value = [startStr, endStr]
    } else if (startDate) {
      const startStr = `${startDate.year}-${String(startDate.month).padStart(2, '0')}-${String(startDate.day).padStart(2, '0')}`
      value = [startStr]
    }
  } else if (['in', 'notIn', 'containsAny', 'containsAll'].includes(currentOperator.value)) {
    value = arrayValueString.value
      .split(',')
      .map((v) => v.trim())
      .filter((v) => v !== '')
  } else if (
    getFilterType() === 'select' &&
    props.column.filterConfig?.multiple &&
    Array.isArray(currentValue.value)
  ) {
    value = currentValue.value
  }

  const newFilter: FilterCondition = {
    field: props.column.field,
    operator: currentOperator.value,
    value,
    caseSensitive: caseSensitive.value,
    id: `filter_${Date.now()}`,
  }

  // Replace all existing filters for this column with the new one
  const otherFilters = (props.activeFilters || []).filter((f) => f.field !== props.column.field)
  emit('update:filters', [...otherFilters, newFilter])
  open.value = false
}

// Watch for operator changes to reset date range
watch(currentOperator, (newOperator) => {
  if (getFilterType() === 'daterange' && newOperator !== 'between') {
    dateRangeValue.value = null
  }
})

// Initialize with existing filter if any
if (props.activeFilters && props.activeFilters.length > 0) {
  const firstFilter = props.activeFilters[0]
  currentOperator.value = firstFilter.operator
  currentValue.value = firstFilter.value
  caseSensitive.value = firstFilter.caseSensitive || false

  // Initialize date range if it's a between filter
  if (
    firstFilter.operator === 'between' &&
    Array.isArray(firstFilter.value) &&
    firstFilter.value.length >= 2
  ) {
    try {
      const startDate = new Date(firstFilter.value[0])
      const endDate = new Date(firstFilter.value[1])
      dateRangeValue.value = {
        start: new CalendarDate(
          startDate.getFullYear(),
          startDate.getMonth() + 1,
          startDate.getDate(),
        ),
        end: new CalendarDate(endDate.getFullYear(), endDate.getMonth() + 1, endDate.getDate()),
      }
    } catch (error) {
      console.warn('Failed to parse date range values:', error)
    }
  }
}
</script>

<style scoped>
.column-filter {
  @apply inline-block;
}
</style>
