/**
 * Address Models and DTOs
 */

import { AddressType } from '@prisma/client'
import { PagingRequest } from '../../common/types/restful'

/**
 * Address DTO (returned to client)
 */
export interface AddressDto {
  id: string
  name: string
  nameEn?: string | null
  addressText?: string | null
  lat: number
  lon: number
  geohash?: string | null

  // Road segment association
  segmentId?: string | null
  segmentName?: string | null // Denormalized from segment
  roadType?: string | null // Denormalized from segment

  // Projection onto segment
  segmentPosition?: number | null // 0.0 to 1.0
  distanceToSegment?: number | null // meters
  projectedLat?: number | null
  projectedLon?: number | null

  // Zone information (nested object)
  zone?: {
    id: string
    code: string
    name: string
      center?: {
        id: string
        code?: string | null
        name?: string | null
        address?: string | null
        lat?: number | null
        lon?: number | null
        polygon?: any | null
      } | null
  } | null
  
  // Legacy fields for backward compatibility (zoneId, zoneName from zone relation)
  zoneId?: string | null
  zoneName?: string | null
  wardName?: string | null
  districtName?: string | null

  // Address type
  addressType: AddressType

  // Metadata
  createdAt: Date
  updatedAt: Date
}

/**
 * Create address request
 */
export interface CreateAddressDto {
  name: string
  nameEn?: string
  addressText?: string

  // Required location
  lat: number
  lon: number

  // Optional segment association (can be auto-calculated)
  segmentId?: string

  // Optional zone (can be auto-calculated)
  zoneId?: string
  wardName?: string
  districtName?: string

  // Address type
  addressType?: AddressType
}

/**
 * Update address request
 */
export interface UpdateAddressDto {
  name?: string
  nameEn?: string | null
  addressText?: string | null

  // Location update (will recalculate geohash and segment projection)
  lat?: number
  lon?: number

  // Manual segment association override
  segmentId?: string | null

  // Zone information
  zoneId?: string | null
  wardName?: string | null
  districtName?: string | null

  // Address type
  addressType?: AddressType
}

/**
 * Nearest address query request
 */
export interface NearestAddressQuery {
  lat: number
  lon: number
  limit?: number // Default: 10, Max: 100
  maxDistance?: number // Maximum distance in meters (default: 5000m = 5km)
  addressType?: AddressType // Filter by type
  segmentId?: string // Filter by segment
  zoneId?: string // Filter by zone
}

/**
 * Nearest address result
 */
export interface NearestAddressResult extends AddressDto {
  distance: number // Distance from query point (meters)
  bearing?: number // Bearing from query point (0-360 degrees)
}

/**
 * Address paging request
 */
export class AddressPagingRequest extends PagingRequest {
  search?: string // Search by name or address text
  addressType?: AddressType
  segmentId?: string
  zoneId?: string
  wardName?: string
  districtName?: string

  constructor(params: any) {
    super()
    this.search = params?.search
    this.addressType = params?.addressType
    this.segmentId = params?.segmentId
    this.zoneId = params?.zoneId
    this.wardName = params?.wardName
    this.districtName = params?.districtName
    this.page = params?.page ? parseInt(params.page) : 0
    this.size = params?.size ? parseInt(params.size) : 20
  }
}

/**
 * Batch address import request
 */
export interface BatchAddressDto {
  addresses: CreateAddressDto[]
  autoCalculateSegments?: boolean // Auto-find nearest segment (default: true)
  autoCalculateZones?: boolean // Auto-find containing zone (default: true)
}

/**
 * Batch import result
 */
export interface BatchImportResult {
  total: number
  successful: number
  failed: number
  errors: Array<{
    index: number
    name: string
    error: string
  }>
  addresses: AddressDto[]
}
