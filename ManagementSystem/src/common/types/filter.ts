/**
 * Advanced Filter/Sort System Types
 *
 * Comprehensive type definitions for flexible filtering and sorting
 * that work across frontend (Vue/TanStack Table) and backend (Node.js/Spring Boot)
 */

// ============================================================================
// OPERATOR TYPES
// ============================================================================

export type StringOperator =
  | 'eq'           // equals
  | 'ne'           // not equals
  | 'contains'     // contains substring
  | 'startsWith'   // starts with
  | 'endsWith'     // ends with
  | 'regex'        // regular expression
  | 'in'           // in array
  | 'notIn'        // not in array
  | 'isNull'       // is null
  | 'isNotNull'    // is not null

export type NumberOperator =
  | 'eq'           // equals
  | 'ne'           // not equals
  | 'gt'           // greater than
  | 'gte'          // greater than or equal
  | 'lt'           // less than
  | 'lte'          // less than or equal
  | 'between'      // between two values
  | 'in'           // in array
  | 'notIn'        // not in array
  | 'isNull'       // is null
  | 'isNotNull'    // is not null

export type DateOperator =
  | 'eq'           // equals
  | 'ne'           // not equals
  | 'gt'           // greater than
  | 'gte'          // greater than or equal
  | 'lt'           // less than
  | 'lte'          // less than or equal
  | 'between'      // between two dates
  | 'isNull'       // is null
  | 'isNotNull'    // is not null

export type ArrayOperator =
  | 'containsAny'  // contains any of the values
  | 'containsAll'  // contains all values
  | 'isEmpty'      // array is empty
  | 'isNotEmpty'   // array is not empty

export type AllOperators = StringOperator | NumberOperator | DateOperator | ArrayOperator

// ============================================================================
// FILTER TYPES
// ============================================================================

export interface FilterCondition {
  field: string
  operator: AllOperators
  value: any
  caseSensitive?: boolean
  id?: string // for UI tracking
}

export interface FilterGroup {
  logic: 'AND' | 'OR'
  conditions: (FilterCondition | FilterGroup)[]
  id?: string // for UI tracking
}

export type FilterNode = FilterCondition | FilterGroup

// ============================================================================
// SORT TYPES
// ============================================================================

export interface SortConfig {
  field: string
  direction: 'asc' | 'desc'
  priority?: number // for multi-sort ordering
}

// ============================================================================
// QUERY PAYLOAD
// ============================================================================

export interface QueryPayload {
  filters?: FilterGroup
  sorts?: SortConfig[]
  page?: number
  size?: number
  search?: string // global search (optional, for backward compatibility)
}

// ============================================================================
// COLUMN DEFINITIONS
// ============================================================================

export type FieldType = 'string' | 'number' | 'date' | 'boolean' | 'array' | 'enum'

export interface FilterableColumn {
  field: string
  label: string
  type: FieldType
  operators?: AllOperators[] // specific operators for this column
  enumOptions?: { label: string; value: string | number | boolean }[]
  caseSensitive?: boolean // default case sensitivity for string fields
  filterable?: boolean // whether this column can be filtered (default: true)
  filterType?: 'text' | 'select' | 'date' | 'daterange' | 'number' | 'boolean' | 'range' | 'custom' // specific filter UI type
  filterConfig?: {
    placeholder?: string
    multiple?: boolean // for select filters
    min?: number // for number/date ranges
    max?: number // for number/date ranges
    step?: number // for number inputs
    format?: string // for date inputs
    customComponent?: string // for custom filter components
  }
}

// ============================================================================
// UI STATE TYPES
// ============================================================================

export interface FilterUIState {
  mode: 'simple' | 'advanced'
  activeFilters: FilterGroup
  showAdvanced: boolean
  columnFilters: Record<string, FilterCondition[]> // field -> conditions
}

export interface SortUIState {
  sorts: SortConfig[]
  multiSortEnabled: boolean
}

// ============================================================================
// API RESPONSE TYPES
// ============================================================================

export interface FilteredResponse<T> {
  data: T[]
  page: {
    page: number
    size: number
    totalElements: number
    totalPages: number
    filters: FilterGroup | null
    sorts: SortConfig[]
  }
}

// ============================================================================
// UTILITY TYPES
// ============================================================================

export type OperatorByType<T extends FieldType> =
  T extends 'string' ? StringOperator :
  T extends 'number' ? NumberOperator :
  T extends 'date' ? DateOperator :
  T extends 'array' ? ArrayOperator :
  AllOperators

export interface FilterPreset {
  id: string
  name: string
  description?: string
  filters: FilterGroup
  icon?: string
}

// ============================================================================
// VALIDATION TYPES
// ============================================================================

export interface FilterValidationError {
  field: string
  message: string
  code: string
}

export interface FilterValidationResult {
  valid: boolean
  errors: FilterValidationError[]
}

// ============================================================================
// MONGODB QUERY TYPES
// ============================================================================

export interface MongoQuery {
  [key: string]: any
}

export interface MongoSort {
  [field: string]: 1 | -1
}

// ============================================================================
// BACKEND INTEGRATION TYPES
// ============================================================================

export interface BackendQueryConfig {
  provider: 'mongodb' | 'prisma' | 'typeorm' | 'jpa' | 'sequelize'
  caseSensitiveDefault: boolean
  dateFormat: string
  timezone: string
}

export interface QueryParseResult<T = any> {
  where: T
  orderBy: any
  pagination: {
    skip: number
    take: number
  }
}
