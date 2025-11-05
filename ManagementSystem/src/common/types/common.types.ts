/**
 * Common Type Definitions
 *
 * Shared types used across the application
 */

/**
 * Base API Response wrapper
 * @deprecated Use IApiResponse from '@/common/types/http' instead
 */
export interface BaseResponse<T = any> {
  result?: T
  message?: string
}

/**
 * Pagination information
 */
export interface Paging {
  page: number
  size: number
  totalElements: number
  totalPages: number
  filters?: Filter[]
  sorts?: Sort[]
  selected?: string[]
}

/**
 * Paginated data wrapper
 */
export interface PagedData<T> {
  data: T[]
  page: Paging
}

/**
 * Filter definition
 */
export interface Filter {
  field: string
  operator: 'eq' | 'ne' | 'gt' | 'gte' | 'lt' | 'lte' | 'like' | 'in' | 'notIn'
  value: any
}

/**
 * Sort definition
 */
export interface Sort {
  field: string
  direction: 'asc' | 'desc'
}

/**
 * Pagination request parameters
 */
export interface PagingRequest {
  page?: number
  size?: number
  search?: string
  filters?: Filter[]
  sorts?: Sort[]
  selected?: string[]
}

/**
 * User DTO
 */
export interface UserDto {
  id: string
  keycloakId?: string
  username: string
  email: string
  firstName: string
  lastName: string
  phone?: string
  address?: string
  identityNumber?: string
  status: UserStatus
  createdAt: string
  updatedAt: string
}

/**
 * Create User Request
 */
export interface CreateUserRequest {
  username: string
  email: string
  firstName: string
  lastName: string
  phone?: string
  address?: string
  identityNumber?: string
  password?: string
}

/**
 * Update User Request
 */
export interface UpdateUserRequest {
  email?: string
  firstName?: string
  lastName?: string
  phone?: string
  address?: string
  identityNumber?: string
  status?: UserStatus
}

/**
 * User Status
 */
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING'

/**
 * User Role
 */
export type UserRole = 'ADMIN' | 'MANAGER' | 'STAFF' | 'SHIPPER' | 'CLIENT'

/**
 * Login Request
 */
export interface LoginRequest {
  username: string
  password: string
  type?: 'BACKEND' | 'FRONTEND'
}

/**
 * Login Response
 */
export interface LoginResponse {
  message: string
  access_token: string
  expires_in: number
  refresh_expires_in: number
  refresh_token: string
  token_type: string
  user: UserDto
}

/**
 * Refresh Token Request
 */
export interface RefreshTokenRequest {
  refreshToken: string
}

/**
 * Zone DTO
 */
export interface ZoneDto {
  id: string
  code: string
  name: string
  polygon: GeoJSONPolygon | null
  centerId: string
  centerCode?: string
  centerName?: string
}

/**
 * Create Zone Request
 */
export interface CreateZoneRequest {
  code: string
  name: string
  polygon?: GeoJSONPolygon
  centerId: string
}

/**
 * Update Zone Request
 */
export interface UpdateZoneRequest {
  code?: string
  name?: string
  polygon?: GeoJSONPolygon
  centerId?: string
}

/**
 * GeoJSON Polygon
 */
export interface GeoJSONPolygon {
  type: 'Polygon'
  coordinates: number[][][]
}

/**
 * Center DTO
 */
export interface CenterDto {
  id: string
  code: string
  name: string
  address?: string
  location?: GeoJSONPoint
}

/**
 * GeoJSON Point
 */
export interface GeoJSONPoint {
  type: 'Point'
  coordinates: [number, number] // [longitude, latitude]
}

/**
 * Settings DTO
 */
export interface SystemSettingDto {
  key: string
  group: string
  description?: string
  type: SettingType
  value: string
  level: number
  isReadOnly: boolean
  displayMode: DisplayMode
  createdAt: string
  updatedAt: string
  updatedBy?: string
}

/**
 * Setting Type
 */
export type SettingType = 'STRING' | 'INTEGER' | 'BOOLEAN' | 'DOUBLE' | 'JSON'

/**
 * Display Mode
 */
export type DisplayMode = 'TEXT' | 'NUMBER' | 'BOOLEAN' | 'SELECT' | 'TEXTAREA' | 'JSON'

/**
 * Create/Update Setting Request
 */
export interface UpsertSettingRequest {
  key: string
  group: string
  description?: string
  type: SettingType
  value: string
  level?: number
  isReadOnly?: boolean
  displayMode?: DisplayMode
}

/**
 * Address DTO
 */
export interface AddressDto {
  id: string
  fullAddress: string
  location: GeoJSONPoint
  city?: string
  district?: string
  ward?: string
  street?: string
}

/**
 * Table Column Definition
 */
export interface TableColumn<T = any> {
  key: string
  label: string
  sortable?: boolean
  class?: string
  headerClass?: string
  render?: (row: T) => string | number | null
}

/**
 * Form Field Definition
 */
export interface FormField {
  name: string
  label: string
  type: 'text' | 'email' | 'password' | 'number' | 'select' | 'textarea' | 'checkbox' | 'switch'
  placeholder?: string
  required?: boolean
  disabled?: boolean
  help?: string
  options?: SelectOption[]
  validation?: ValidationRule[]
}

/**
 * Select Option
 */
export interface SelectOption {
  label: string
  value: any
  disabled?: boolean
}

/**
 * Validation Rule
 */
export interface ValidationRule {
  type: 'required' | 'email' | 'minLength' | 'maxLength' | 'pattern' | 'custom'
  value?: any
  message: string
}

/**
 * API Error
 */
export interface ApiError {
  message: string
  status?: number
  code?: string
  details?: any
}

/**
 * Loading State
 */
export interface LoadingState {
  isLoading: boolean
  error: ApiError | null
}

/**
 * Action Button Definition
 */
export interface ActionButton {
  label: string
  icon?: string
  color?: 'primary' | 'neutral' | 'success' | 'warning' | 'error'
  variant?: 'solid' | 'outline' | 'soft' | 'ghost' | 'link'
  onClick: () => void | Promise<void>
  disabled?: boolean
  loading?: boolean
}

/**
 * Breadcrumb Item
 */
export interface BreadcrumbItem {
  label: string
  to?: string
  icon?: string
}

/**
 * Tab Item
 */
export interface TabItem {
  key: string
  label: string
  icon?: string
  disabled?: boolean
}

/**
 * Modal Config
 */
export interface ModalConfig {
  title: string
  description?: string
  confirmText?: string
  cancelText?: string
  onConfirm: () => void | Promise<void>
  onCancel?: () => void
}
