/**
 * Filter V2 Converter
 *
 * Converts between V1 FilterGroup format and V2 FilterGroupItemV2 format
 */

import type { FilterGroup, FilterCondition } from '../types/filter'
import type {
  FilterGroupItemV2,
  FilterConditionItemV2,
  FilterOperatorItemV2,
  FilterItemV2,
} from '../types/filter-v2'
import { FilterItemType } from '../types/filter-v2'

/**
 * Operator mapping from V1 to V2 format
 */
const operatorMapV1ToV2: Record<string, string> = {
  eq: 'EQUALS',
  equals: 'EQUALS',
  ne: 'NOT_EQUALS',
  not_equals: 'NOT_EQUALS',
  gt: 'GREATER_THAN',
  greater_than: 'GREATER_THAN',
  gte: 'GREATER_THAN_OR_EQUAL',
  greater_than_or_equal: 'GREATER_THAN_OR_EQUAL',
  lt: 'LESS_THAN',
  less_than: 'LESS_THAN',
  lte: 'LESS_THAN_OR_EQUAL',
  less_than_or_equal: 'LESS_THAN_OR_EQUAL',
  contains: 'CONTAINS',
  startsWith: 'STARTS_WITH',
  starts_with: 'STARTS_WITH',
  endsWith: 'ENDS_WITH',
  ends_with: 'ENDS_WITH',
  regex: 'REGEX',
  in: 'IN',
  notIn: 'NOT_IN',
  not_in: 'NOT_IN',
  between: 'BETWEEN',
  isNull: 'IS_NULL',
  is_null: 'IS_NULL',
  isNotNull: 'IS_NOT_NULL',
  is_not_null: 'IS_NOT_NULL',
}

/**
 * Convert V1 FilterGroup to V2 FilterGroupItemV2
 *
 * This converts the V1 nested structure with group-level logic
 * to V2 flat structure with pair-level operators
 */
export function convertV1ToV2Filter(v1Filter: FilterGroup): FilterGroupItemV2 {
  const items: FilterItemV2[] = []

  if (!v1Filter.conditions || v1Filter.conditions.length === 0) {
    return {
      type: FilterItemType.GROUP,
      items: [],
    }
  }

  // Process each condition/group
  v1Filter.conditions.forEach((condition, index) => {
    // Add operator before each condition except the first
    if (index > 0) {
      const operator: FilterOperatorItemV2 = {
        type: FilterItemType.OPERATOR,
        value: v1Filter.logic === 'OR' ? 'OR' : 'AND',
      }
      items.push(operator)
    }

    // Add the condition or nested group
    if ('field' in condition) {
      // It's a FilterCondition
      const v2Condition: FilterConditionItemV2 = {
        type: FilterItemType.CONDITION,
        field: condition.field,
        operator: operatorMapV1ToV2[condition.operator] || condition.operator.toUpperCase(),
        value: condition.value,
        caseSensitive: condition.caseSensitive,
      }
      items.push(v2Condition)
    } else {
      // It's a nested FilterGroup - recursively convert
      const nestedGroup = convertV1ToV2Filter(condition)
      items.push(nestedGroup)
    }
  })

  return {
    type: FilterItemType.GROUP,
    items,
  }
}

/**
 * Convert V2 FilterGroupItemV2 to V1 FilterGroup
 *
 * This converts V2 flat structure back to V1 nested structure
 * Note: This may lose some information if V2 has mixed AND/OR operations
 */
export function convertV2ToV1Filter(v2Filter: FilterGroupItemV2): FilterGroup {
  if (!v2Filter.items || v2Filter.items.length === 0) {
    return {
      logic: 'AND',
      conditions: [],
    }
  }

  const conditions: (FilterCondition | FilterGroup)[] = []
  let currentLogic: 'AND' | 'OR' = 'AND'

  // Parse items and group consecutive conditions with the same operator
  let i = 0
  while (i < v2Filter.items.length) {
    const item = v2Filter.items[i]

    if (item.type === FilterItemType.CONDITION) {
      const condition = item as FilterConditionItemV2
      const v1Condition: FilterCondition = {
        field: condition.field,
        operator: mapV2OperatorToV1(condition.operator),
        value: condition.value,
        caseSensitive: condition.caseSensitive,
      }
      conditions.push(v1Condition)
      i++
    } else if (item.type === FilterItemType.GROUP) {
      const nestedGroup = item as FilterGroupItemV2
      const v1NestedGroup = convertV2ToV1Filter(nestedGroup)
      conditions.push(v1NestedGroup)
      i++
    } else if (item.type === FilterItemType.OPERATOR) {
      const operator = item as FilterOperatorItemV2
      currentLogic = operator.value
      i++
      // If next item is a condition, we'll use this logic
      // For now, just continue
      if (i < v2Filter.items.length && v2Filter.items[i].type === FilterItemType.CONDITION) {
        // This operator applies to the next condition
        // We'll use currentLogic for grouping
        continue
      }
    }
  }

  return {
    logic: currentLogic,
    conditions,
  }
}

/**
 * Map V2 operator to V1 operator format
 */
function mapV2OperatorToV1(v2Operator: string): string {
  const reverseMap: Record<string, string> = {
    EQUALS: 'eq',
    NOT_EQUALS: 'ne',
    GREATER_THAN: 'gt',
    GREATER_THAN_OR_EQUAL: 'gte',
    LESS_THAN: 'lt',
    LESS_THAN_OR_EQUAL: 'lte',
    CONTAINS: 'contains',
    STARTS_WITH: 'startsWith',
    ENDS_WITH: 'endsWith',
    REGEX: 'regex',
    IN: 'in',
    NOT_IN: 'notIn',
    BETWEEN: 'between',
    IS_NULL: 'isNull',
    IS_NOT_NULL: 'isNotNull',
  }

  return reverseMap[v2Operator] || v2Operator.toLowerCase()
}

/**
 * Create empty V2 filter group
 */
export function createEmptyV2FilterGroup(): FilterGroupItemV2 {
  return {
    type: FilterItemType.GROUP,
    items: [],
  }
}
