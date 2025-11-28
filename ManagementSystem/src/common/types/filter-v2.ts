/**
 * V2 Filter System Types
 *
 * Enhanced filter system that supports operations between each pair of conditions
 * Unlike V1, V2 uses a flat list of items with operators between pairs
 */

export enum FilterItemType {
  CONDITION = 'condition',
  OPERATOR = 'operator',
  GROUP = 'group',
}

/**
 * Base filter item interface
 */
export interface FilterItemV2 {
  type: FilterItemType
}

/**
 * Filter condition item
 */
export interface FilterConditionItemV2 extends FilterItemV2 {
  type: FilterItemType.CONDITION
  field: string
  operator: string // e.g., 'EQUALS', 'CONTAINS', 'GREATER_THAN_OR_EQUAL'
  value: any
  caseSensitive?: boolean
}

/**
 * Filter operator item (AND/OR)
 */
export interface FilterOperatorItemV2 extends FilterItemV2 {
  type: FilterItemType.OPERATOR
  value: 'AND' | 'OR'
}

/**
 * Filter group item (contains nested items)
 */
export interface FilterGroupItemV2 extends FilterItemV2 {
  type: FilterItemType.GROUP
  items: FilterItemV2[]
}

/**
 * Type guard for filter condition
 */
export function isFilterCondition(item: FilterItemV2): item is FilterConditionItemV2 {
  return item.type === FilterItemType.CONDITION
}

/**
 * Type guard for filter operator
 */
export function isFilterOperator(item: FilterItemV2): item is FilterOperatorItemV2 {
  return item.type === FilterItemType.OPERATOR
}

/**
 * Type guard for filter group
 */
export function isFilterGroup(item: FilterItemV2): item is FilterGroupItemV2 {
  return item.type === FilterItemType.GROUP
}
