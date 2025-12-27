/**
 * Parcel Module Types
 *
 * Type definitions for Parcel module
 */

import type { IApiResponse } from '@/common/types'
import type { FilterCondition, FilterGroup, SortConfig } from '@/common/types/filter'

/**
 * User Info DTO (nested object)
 */
export class UserInfoDto {
  id: string
  firstName?: string
  lastName?: string
  username?: string
  email?: string
  phone?: string
  address?: string // General address string

  constructor(data?: Partial<UserInfoDto>) {
    this.id = data?.id || ''
    this.firstName = data?.firstName
    this.lastName = data?.lastName
    this.username = data?.username
    this.email = data?.email
    this.phone = data?.phone
    this.address = data?.address
  }

  get fullName(): string {
    if (this.firstName && this.lastName) {
      return `${this.firstName} ${this.lastName}`
    } else if (this.firstName) {
      return this.firstName
    } else if (this.lastName) {
      return this.lastName
    } else if (this.username) {
      return this.username
    }
    return `User ${this.id.substring(0, Math.min(4, this.id.length))}`
  }
}

/**
 * Address Info DTO (nested object with coordinates)
 */
export class AddressInfoDto {
  id: string // UserAddress ID
  userId?: string
  destinationId?: string // ID in zone-service
  note?: string
  tag?: string
  isPrimary?: boolean
  lat?: number
  lon?: number
  zoneId?: string

  constructor(data?: Partial<AddressInfoDto>) {
    this.id = data?.id || ''
    this.userId = data?.userId
    this.destinationId = data?.destinationId
    this.note = data?.note
    this.tag = data?.tag
    this.isPrimary = data?.isPrimary
    this.lat = data?.lat
    this.lon = data?.lon
    this.zoneId = data?.zoneId
  }
}

/**
 * Parcel Data Transfer Object
 */
export class ParcelDto {
  id: string
  code: string
  senderId: string
  senderName?: string // Full name from UserSnapshot (backward compatibility)
  receiverId: string
  receiverName?: string // Full name from UserSnapshot (backward compatibility)
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
  zoneId?: string // Zone ID from receiver destination (for easy access)

  // New nested objects (from backend ParcelResponse)
  sender?: UserInfoDto
  receiver?: UserInfoDto
  senderAddress?: AddressInfoDto
  receiverAddress?: AddressInfoDto

  constructor(data: any) {
    this.id = data.id
    this.code = data.code
    this.senderId = data.senderId
    this.senderName = data.senderName
    this.receiverId = data.receiverId
    this.receiverName = data.receiverName
    this.receiverPhoneNumber = data.receiverPhoneNumber
    this.deliveryType = data.deliveryType
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

    // Map nested objects
    this.sender = data.sender ? new UserInfoDto(data.sender) : undefined
    this.receiver = data.receiver ? new UserInfoDto(data.receiver) : undefined
    this.senderAddress = data.senderAddress ? new AddressInfoDto(data.senderAddress) : undefined
    this.receiverAddress = data.receiverAddress ? new AddressInfoDto(data.receiverAddress) : undefined

    // Map receiveFrom and targetDestination from nested address objects or fallback to old fields
    // receiveFrom = sender address text (from sender.address or senderAddress.note)
    if (data.receiveFrom) {
      this.receiveFrom = data.receiveFrom
    } else if (this.sender?.address) {
      this.receiveFrom = this.sender.address
    } else if (this.senderAddress?.note) {
      this.receiveFrom = this.senderAddress.note
    } else {
      this.receiveFrom = ''
    }

    // targetDestination = receiver address text (from receiver.address or receiverAddress.note)
    if (data.targetDestination) {
      this.targetDestination = data.targetDestination
    } else if (this.receiver?.address) {
      this.targetDestination = this.receiver.address
    } else if (this.receiverAddress?.note) {
      this.targetDestination = this.receiverAddress.note
    } else {
      this.targetDestination = ''
    }

    // Update lat/lon from receiver address if not set
    if (!this.lat && this.receiverAddress?.lat != null) {
      this.lat = typeof this.receiverAddress.lat === 'number' ? this.receiverAddress.lat : parseFloat(this.receiverAddress.lat.toString())
    }
    if (!this.lon && this.receiverAddress?.lon != null) {
      this.lon = typeof this.receiverAddress.lon === 'number' ? this.receiverAddress.lon : parseFloat(this.receiverAddress.lon.toString())
    }

    // Update zoneId from receiver address if not set
    if (!this.zoneId && this.receiverAddress?.zoneId) {
      this.zoneId = this.receiverAddress.zoneId
    }

    // Backward compatibility: if nested objects exist but old fields don't, populate them
    if (!this.senderName && this.sender) {
      this.senderName = this.sender.fullName
    }
    if (!this.receiverName && this.receiver) {
      this.receiverName = this.receiver.fullName
    }
    if (!this.receiverPhoneNumber && this.receiver?.phone) {
      this.receiverPhoneNumber = this.receiver.phone
    }
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
export type DeliveryType = 'URGENT' | 'EXPRESS' | 'FAST' | 'NORMAL' | 'ECONOMY'

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
  senderDestinationId: string
  receiverDestinationId: string

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
    this.senderDestinationId = data.senderDestinationId
    this.receiverDestinationId = data.receiverDestinationId
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
