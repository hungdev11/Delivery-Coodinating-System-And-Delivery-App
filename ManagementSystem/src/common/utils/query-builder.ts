/**
 * Query Builder Utilities
 *
 * Converts FilterGroup and SortConfig to MongoDB-style queries
 * and provides serialization/deserialization for API communication
 */

import type {
  FilterGroup,
  FilterCondition,
  SortConfig,
  QueryPayload,
  MongoQuery,
  MongoSort,
  FilterValidationResult,
  FilterValidationError,
  FieldType,
  AllOperators
} from '../types/filter'

// ============================================================================
// MONGODB QUERY BUILDER
// ============================================================================

/**
 * Convert FilterGroup to MongoDB-style query object
 */
export function buildMongoQuery(group: FilterGroup): MongoQuery {
  if (!group.conditions || group.conditions.length === 0) {
    return {}
  }

  const mongoConditions = group.conditions.map(condition => {
    if ('field' in condition) {
      // It's a FilterCondition
      return buildMongoCondition(condition)
    } else {
      // It's a FilterGroup (recursive)
      return buildMongoQuery(condition)
    }
  })

  if (mongoConditions.length === 1) {
    return mongoConditions[0]
  }

  const operator = group.logic === 'AND' ? '$and' : '$or'
  return {
    [operator]: mongoConditions
  }
}

/**
 * Convert FilterCondition to MongoDB-style condition
 */
export function buildMongoCondition(condition: FilterCondition): MongoQuery {
  const { field, operator, value, caseSensitive } = condition

  switch (operator) {
    case 'eq':
      return { [field]: { $eq: value } }

    case 'ne':
      return { [field]: { $ne: value } }

    case 'contains':
      const containsOptions: any = { $regex: escapeRegex(value), $options: 'i' }
      if (caseSensitive) {
        delete containsOptions.$options
      }
      return { [field]: containsOptions }

    case 'startsWith':
      const startsWithOptions: any = { $regex: `^${escapeRegex(value)}`, $options: 'i' }
      if (caseSensitive) {
        delete startsWithOptions.$options
      }
      return { [field]: startsWithOptions }

    case 'endsWith':
      const endsWithOptions: any = { $regex: `${escapeRegex(value)}$`, $options: 'i' }
      if (caseSensitive) {
        delete endsWithOptions.$options
      }
      return { [field]: endsWithOptions }

    case 'regex':
      const regexOptions: any = { $regex: value, $options: 'i' }
      if (caseSensitive) {
        delete regexOptions.$options
      }
      return { [field]: regexOptions }

    case 'in':
      return { [field]: { $in: Array.isArray(value) ? value : [value] } }

    case 'notIn':
      return { [field]: { $nin: Array.isArray(value) ? value : [value] } }

    case 'gt':
      return { [field]: { $gt: value } }

    case 'gte':
      return { [field]: { $gte: value } }

    case 'lt':
      return { [field]: { $lt: value } }

    case 'lte':
      return { [field]: { $lte: value } }

    case 'between':
      if (!Array.isArray(value) || value.length !== 2) {
        throw new Error('Between operator requires array of exactly 2 values')
      }
      return { [field]: { $gte: value[0], $lte: value[1] } }

    case 'isNull':
      return { [field]: { $eq: null } }

    case 'isNotNull':
      return { [field]: { $ne: null } }

    case 'containsAny':
      return { [field]: { $in: Array.isArray(value) ? value : [value] } }

    case 'containsAll':
      return { [field]: { $all: Array.isArray(value) ? value : [value] } }

    case 'isEmpty':
      return { [field]: { $size: 0 } }

    case 'isNotEmpty':
      return { [field]: { $not: { $size: 0 } } }

    default:
      throw new Error(`Unsupported operator: ${operator}`)
  }
}

/**
 * Convert SortConfig array to MongoDB-style sort object
 */
export function buildMongoSort(sorts: SortConfig[]): MongoSort {
  const mongoSort: MongoSort = {}

  sorts.forEach(sort => {
    mongoSort[sort.field] = sort.direction === 'asc' ? 1 : -1
  })

  return mongoSort
}

// ============================================================================
// SERIALIZATION
// ============================================================================

/**
 * Serialize QueryPayload to JSON string for API communication
 */
export function serializeQuery(query: QueryPayload): string {
  return JSON.stringify(query, null, 0)
}

/**
 * Deserialize JSON string to QueryPayload
 */
export function deserializeQuery(json: string): QueryPayload {
  try {
    return JSON.parse(json)
  } catch (error) {
    throw new Error(`Failed to deserialize query: ${error}`)
  }
}

/**
 * Convert QueryPayload to URL query string (for GET requests)
 */
export function queryToUrlParams(query: QueryPayload): URLSearchParams {
  const params = new URLSearchParams()

  if (query.page !== undefined) {
    params.set('page', query.page.toString())
  }

  if (query.size !== undefined) {
    params.set('size', query.size.toString())
  }

  if (query.search) {
    params.set('search', query.search)
  }

  if (query.filters) {
    params.set('filters', serializeQuery({ filters: query.filters }))
  }

  if (query.sorts && query.sorts.length > 0) {
    params.set('sorts', serializeQuery({ sorts: query.sorts }))
  }

  return params
}

// ============================================================================
// VALIDATION
// ============================================================================

/**
 * Validate FilterGroup for common issues
 */
export function validateFilters(group: FilterGroup): FilterValidationResult {
  const errors: FilterValidationError[] = []

  function validateNode(node: FilterGroup | FilterCondition, path: string = ''): void {
    if ('field' in node) {
      // It's a FilterCondition
      const condition = node as FilterCondition
      const fieldPath = path ? `${path}.${condition.field}` : condition.field

      // Validate field name
      if (!condition.field || condition.field.trim() === '') {
        errors.push({
          field: fieldPath,
          message: 'Field name is required',
          code: 'FIELD_REQUIRED'
        })
      }

      // Validate operator
      if (!condition.operator) {
        errors.push({
          field: fieldPath,
          message: 'Operator is required',
          code: 'OPERATOR_REQUIRED'
        })
      }

      // Validate value based on operator
      if (needsValue(condition.operator) && condition.value === undefined) {
        errors.push({
          field: fieldPath,
          message: 'Value is required for this operator',
          code: 'VALUE_REQUIRED'
        })
      }

      // Validate between operator
      if (condition.operator === 'between' && (!Array.isArray(condition.value) || condition.value.length !== 2)) {
        errors.push({
          field: fieldPath,
          message: 'Between operator requires exactly 2 values',
          code: 'BETWEEN_INVALID'
        })
      }

      // Validate array operators
      if (['in', 'notIn', 'containsAny', 'containsAll'].includes(condition.operator) && !Array.isArray(condition.value)) {
        errors.push({
          field: fieldPath,
          message: 'This operator requires an array value',
          code: 'ARRAY_REQUIRED'
        })
      }

    } else {
      // It's a FilterGroup
      const group = node as FilterGroup

      if (!group.conditions || group.conditions.length === 0) {
        errors.push({
          field: path || 'root',
          message: 'Filter group must have at least one condition',
          code: 'EMPTY_GROUP'
        })
      }

      group.conditions.forEach((condition, index) => {
        validateNode(condition, path ? `${path}[${index}]` : `[${index}]`)
      })
    }
  }

  validateNode(group)

  return {
    valid: errors.length === 0,
    errors
  }
}

/**
 * Check if operator requires a value
 */
function needsValue(operator: AllOperators): boolean {
  return !['isNull', 'isNotNull', 'isEmpty', 'isNotEmpty'].includes(operator)
}

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

/**
 * Escape special regex characters
 */
function escapeRegex(str: string): string {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/**
 * Create empty FilterGroup
 */
export function createEmptyFilterGroup(): FilterGroup {
  return {
    logic: 'AND',
    conditions: []
  }
}

/**
 * Create FilterCondition
 */
export function createFilterCondition(
  field: string,
  operator: AllOperators,
  value: any,
  caseSensitive?: boolean
): FilterCondition {
  return {
    field,
    operator,
    value,
    caseSensitive,
    id: generateId()
  }
}

/**
 * Create SortConfig
 */
export function createSortConfig(field: string, direction: 'asc' | 'desc'): SortConfig {
  return {
    field,
    direction
  }
}

/**
 * Generate unique ID for UI tracking
 */
function generateId(): string {
  return Math.random().toString(36).substr(2, 9)
}

/**
 * Get available operators for field type
 */
export function getOperatorsForType(type: FieldType): AllOperators[] {
  switch (type) {
    case 'string':
      return ['eq', 'ne', 'contains', 'startsWith', 'endsWith', 'regex', 'in', 'notIn', 'isNull', 'isNotNull']
    case 'number':
      return ['eq', 'ne', 'gt', 'gte', 'lt', 'lte', 'between', 'in', 'notIn', 'isNull', 'isNotNull']
    case 'date':
      return ['eq', 'ne', 'gt', 'gte', 'lt', 'lte', 'between', 'isNull', 'isNotNull']
    case 'boolean':
      return ['eq', 'ne', 'isNull', 'isNotNull']
    case 'array':
      return ['containsAny', 'containsAll', 'isEmpty', 'isNotEmpty', 'isNull', 'isNotNull']
    case 'enum':
      return ['eq', 'ne', 'in', 'notIn', 'isNull', 'isNotNull']
    default:
      return ['eq', 'ne', 'isNull', 'isNotNull']
  }
}

/**
 * Check if operator is valid for field type
 */
export function isValidOperatorForType(operator: AllOperators, type: FieldType): boolean {
  return getOperatorsForType(type).includes(operator)
}
