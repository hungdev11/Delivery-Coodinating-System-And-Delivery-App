/**
 * Zone Module Types
 *
 * Type definitions for Zone module
 */

import type { IApiResponse } from '@/common/types'

/**
 * GeoJSON Polygon
 */
export interface GeoJSONPolygon {
  type: 'Polygon'
  coordinates: number[][][]
}

/**
 * GeoJSON Point
 */
export interface GeoJSONPoint {
  type: 'Point'
  coordinates: [number, number] // [longitude, latitude]
}

/**
 * Zone Data Transfer Object
 */
export class ZoneDto {
  zone_id: string
  code: string
  name: string
  polygon: GeoJSONPolygon | null
  centerId: string
  centerCode?: string
  centerName?: string

  constructor(data: any) {
    // Map API response fields to DTO fields
    this.zone_id = data.zone_id
    this.code = data.code
    this.name = data.name
    this.polygon = data.polygon
    this.centerId = data.center_id || data.centerId
    this.centerCode = data.center_code || data.centerCode
    this.centerName = data.center_name || data.centerName
  }

  get displayName(): string {
    return `${this.code} - ${this.name}`
  }

  get hasPolygon(): boolean {
    return this.polygon !== null
  }

  get centerDisplayName(): string {
    return this.centerName || this.centerCode || 'Unknown Center'
  }
}

/**
 * Center Data Transfer Object
 */
export class CenterDto {
  center_id: string
  code: string
  name: string
  address?: string
  location?: GeoJSONPoint

  constructor(data: CenterDto) {
    this.center_id = data.center_id
    this.code = data.code
    this.name = data.name
    this.address = data.address
    this.location = data.location
  }

  get displayName(): string {
    return `${this.code} - ${this.name}`
  }
}

/**
 * Create Zone Request
 */
export class CreateZoneRequest {
  code: string
  name: string
  polygon?: GeoJSONPolygon
  centerId: string

  constructor(data: CreateZoneRequest) {
    this.code = data.code
    this.name = data.name
    this.polygon = data.polygon
    this.centerId = data.centerId
  }
}

/**
 * Update Zone Request
 */
export class UpdateZoneRequest {
  code?: string
  name?: string
  polygon?: GeoJSONPolygon
  centerId?: string

  constructor(data: UpdateZoneRequest) {
    this.code = data.code
    this.name = data.name
    this.polygon = data.polygon
    this.centerId = data.centerId
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
  filters?: any[]
  sorts?: any[]
  selected?: string[]
}

/**
 * Paginated Zones Response (Updated for new API)
 */
export interface PagedZonesResponse {
  data: ZoneDto[]
  page: Paging
}

/**
 * Zone API Responses
 */
export type GetZonesResponse = IApiResponse<PagedZonesResponse>
export type GetZoneResponse = IApiResponse<ZoneDto>
export type CreateZoneResponse = IApiResponse<ZoneDto>
export type UpdateZoneResponse = IApiResponse<ZoneDto>
export type DeleteZoneResponse = IApiResponse<null>
export type GetCentersResponse = IApiResponse<CenterDto[]>
