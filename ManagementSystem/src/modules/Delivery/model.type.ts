/**
 * Delivery Module Types
 *
 * DTOs and request/response models for delivery management
 */

import type { IApiResponse } from '@/common/types'
import type { FilterCondition, FilterGroup, SortConfig } from '@/common/types/filter'

/**
 * Core enums
 */
export type SessionStatus = 'IN_PROGRESS' | 'COMPLETED' | 'FAILED'
export type AssignmentStatus = 'IN_PROGRESS' | 'COMPLETED' | 'FAILED'

/**
 * Delivery Man (Shipper) DTO
 */
export class DeliveryManDto {
  id: string
  userId: string
  vehicleType: string
  capacityKg: number
  createdAt: string
  updatedAt: string
  // User information (from backend)
  username?: string
  email?: string
  firstName?: string
  lastName?: string
  phone?: string
  status?: string

  constructor(data: DeliveryManDto) {
    this.id = data.id
    this.userId = data.userId
    this.vehicleType = data.vehicleType
    this.capacityKg = data.capacityKg
    this.createdAt = data.createdAt
    this.updatedAt = data.updatedAt
    // User fields
    this.username = data.username
    this.email = data.email
    this.firstName = data.firstName
    this.lastName = data.lastName
    this.phone = data.phone
    this.status = data.status
  }

  get displayName(): string {
    if (this.firstName || this.lastName) {
      const fullName = `${this.firstName ?? ''} ${this.lastName ?? ''}`.trim()
      return fullName || this.username || 'Unknown shipper'
    }
    return this.username || 'Unknown shipper'
  }
}

export class CreateDeliveryManRequest {
  userId: string
  vehicleType: string
  capacityKg: number

  constructor(data: CreateDeliveryManRequest) {
    this.userId = data.userId
    this.vehicleType = data.vehicleType
    this.capacityKg = data.capacityKg
  }
}

export class UpdateDeliveryManRequest {
  vehicleType?: string
  capacityKg?: number

  constructor(data: UpdateDeliveryManRequest) {
    this.vehicleType = data.vehicleType
    this.capacityKg = data.capacityKg
  }
}

export interface PagedDeliveryManResponse {
  data: DeliveryManDto[]
  page: Paging
}

export type GetDeliveryManResponse = IApiResponse<PagedDeliveryManResponse>
export type GetDeliveryManDetailResponse = IApiResponse<DeliveryManDto>
export type CreateDeliveryManResponse = IApiResponse<DeliveryManDto>
export type UpdateDeliveryManResponse = IApiResponse<DeliveryManDto>

/**
 * Delivery Assignment DTO
 */
export class DeliveryAssignmentDto {
  id: string
  parcelId: string
  status: AssignmentStatus
  failReason?: string | null
  scanedAt: string
  updatedAt: string

  constructor(data: DeliveryAssignmentDto) {
    this.id = data.id
    this.parcelId = data.parcelId
    this.status = data.status
    this.failReason = data.failReason
    this.scanedAt = data.scanedAt
    this.updatedAt = data.updatedAt
  }

  get isCompleted(): boolean {
    return this.status === 'COMPLETED'
  }

  get isFailed(): boolean {
    return this.status === 'FAILED'
  }
}

/**
 * Delivery Session DTO
 */
export class DeliverySessionDto {
  id: string
  deliveryManId: string
  status: SessionStatus
  startTime: string
  endTime?: string | null
  totalTasks: number
  completedTasks: number
  failedTasks: number
  assignments?: DeliveryAssignmentDto[]

  constructor(data: DeliverySessionDto) {
    this.id = data.id
    this.deliveryManId = data.deliveryManId
    this.status = data.status
    this.startTime = data.startTime
    this.endTime = data.endTime
    this.totalTasks = data.totalTasks
    this.completedTasks = data.completedTasks
    this.failedTasks = data.failedTasks
    this.assignments = data.assignments?.map((assignment) => new DeliveryAssignmentDto(assignment))
  }

  get isActive(): boolean {
    return this.status === 'IN_PROGRESS'
  }
}

export interface PagedDeliverySessionResponse {
  data: DeliverySessionDto[]
  page: Paging
}

export type GetDeliverySessionsResponse = IApiResponse<PagedDeliverySessionResponse>
export type GetDeliverySessionDetailResponse = IApiResponse<DeliverySessionDto>

/**
 * Session detail extras
 */
export interface DeliverySessionSummary {
  session: DeliverySessionDto
  deliveryMan?: DeliveryManDto
}

export interface UpdateAssignmentStatusRequestPayload {
  assignmentStatus: AssignmentStatus
  parcelEvent: string
  failReason?: string | null
  routeInfo?: {
    distanceM?: number
    durationS?: number
    waypoints?: Array<{ lat: number; lon: number }>
  }
}

/**
 * Pagination info reused from users module
 */
export interface Paging {
  page: number
  size: number
  totalElements: number
  totalPages: number
  filters?: (FilterCondition | FilterGroup)[] | null
  sorts?: SortConfig[] | null
  selected?: string[]
}

/**
 * Assignment task response (from delivery assignment API)
 */
export interface DeliveryAssignmentTask {
  sessionId: string
  parcelId: string
  parcelCode?: string
  deliveryType?: string
  status: string
  deliveryManAssignedId: string
  deliveryManPhone?: string
  receiverName?: string
  receiverId?: string
  receiverPhone?: string
  deliveryLocation?: string
  value?: number
  weight?: number
  createdAt?: string
  completedAt?: string
  failReason?: string
  lat?: string | number | null
  lon?: string | number | null
}

export interface DeliveryAssignmentTaskResponse {
  content: DeliveryAssignmentTask[]
  pageNo: number
  pageSize: number
  totalElements: number
  totalPages: number
  last: boolean
}
