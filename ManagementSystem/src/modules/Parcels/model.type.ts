/**
 * Parcel Module Types
 *
 * Type definitions for Parcel module
 */

import type { IApiResponse } from '@/common/types'
import type { FilterCondition, FilterGroup, SortConfig } from '@/common/types/filter'

/**
 * Parcel Data Transfer Object
 */
export class ParcelDto {
  id: string
  code: string
  senderId: string
  receiverId: string
  receiverPhoneNumber?: string
  deliveryType: DeliveryType
  receiveFrom: string
  targetDestination: string
  status: ParcelStatus
  weight: number
  value: number
  createdAt: string
  updatedAt: string
  deliveredAt?: string
  windowStart?: string
  windowEnd?: string
  lat?: number
  lon?: number

  constructor(data: ParcelDto) {
    this.id = data.id
    this.code = data.code
    this.senderId = data.senderId
    this.receiverId = data.receiverId
    this.receiverPhoneNumber = data.receiverPhoneNumber
    this.deliveryType = data.deliveryType
    this.receiveFrom = data.receiveFrom
    this.targetDestination = data.targetDestination
    this.status = data.status
    this.weight = data.weight
    this.value = data.value
    this.createdAt = data.createdAt
    this.updatedAt = data.updatedAt
    this.deliveredAt = data.deliveredAt
    this.windowStart = data.windowStart
    this.windowEnd = data.windowEnd
    this.lat = data.lat
    this.lon = data.lon
  }

  get displayStatus(): string {
    const statusMap: Record<ParcelStatus, string> = {
      IN_WAREHOUSE: 'In Warehouse',
      ON_ROUTE: 'On Route',
      DELIVERED: 'Delivered',
      SUCCEEDED: 'Succeeded',
      FAILED: 'Failed',
      DELAYED: 'Delayed',
      DISPUTE: 'Dispute',
      LOST: 'Lost',
    }
    return statusMap[this.status] || this.status
  }
}

/**
 * Parcel Status
 */
export type ParcelStatus =
  | 'IN_WAREHOUSE'
  | 'ON_ROUTE'
  | 'DELIVERED'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'DELAYED'
  | 'DISPUTE'
  | 'LOST'

/**
 * Delivery Type
 */
export type DeliveryType = 'STANDARD' | 'EXPRESS' | 'SAME_DAY'

/**
 * Parcel Event (for status changes)
 */
export type ParcelEvent =
  | 'DELIVERY_SUCCESSFUL'
  | 'CUSTOMER_RECEIVED'
  | 'CAN_NOT_DELIVERY'
  | 'CUSTOMER_REJECT'
  | 'POSTPONE'
  | 'CUSTOMER_CONFIRM_NOT_RECEIVED'
  | 'MISSUNDERSTANDING_DISPUTE'
  | 'FAULT_DISPUTE'

/**
 * Create Parcel Request
 */
export class CreateParcelRequest {
  code: string
  senderId: string
  receiverId: string
  deliveryType: DeliveryType
  receiveFrom: string
  sendTo: string
  weight: number
  value: number
  windowStart?: string
  windowEnd?: string
  lat?: number
  lon?: number

  constructor(data: CreateParcelRequest) {
    this.code = data.code
    this.senderId = data.senderId
    this.receiverId = data.receiverId
    this.deliveryType = data.deliveryType
    this.receiveFrom = data.receiveFrom
    this.sendTo = data.sendTo
    this.weight = data.weight
    this.value = data.value
    this.windowStart = data.windowStart
    this.windowEnd = data.windowEnd
    this.lat = data.lat
    this.lon = data.lon
  }
}

/**
 * Update Parcel Request
 */
export class UpdateParcelRequest {
  code?: string
  senderId?: string
  receiverId?: string
  deliveryType?: DeliveryType
  receiveFrom?: string
  sendTo?: string
  weight?: number
  value?: number
  windowStart?: string
  windowEnd?: string
  lat?: number
  lon?: number

  constructor(data: UpdateParcelRequest) {
    this.code = data.code
    this.senderId = data.senderId
    this.receiverId = data.receiverId
    this.deliveryType = data.deliveryType
    this.receiveFrom = data.receiveFrom
    this.sendTo = data.sendTo
    this.weight = data.weight
    this.value = data.value
    this.windowStart = data.windowStart
    this.windowEnd = data.windowEnd
    this.lat = data.lat
    this.lon = data.lon
  }
}

/**
 * Pagination Interface
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
 * Paginated Parcels Response
 */
export interface PagedParcelsResponse {
  data: ParcelDto[]
  page: Paging
}

/**
 * Parcel API Responses
 */
export type GetParcelsResponse = IApiResponse<PagedParcelsResponse>
export type GetParcelResponse = IApiResponse<ParcelDto>
export type CreateParcelResponse = IApiResponse<ParcelDto>
export type UpdateParcelResponse = IApiResponse<ParcelDto>
export type DeleteParcelResponse = IApiResponse<null>
export type ChangeStatusResponse = IApiResponse<ParcelDto>
