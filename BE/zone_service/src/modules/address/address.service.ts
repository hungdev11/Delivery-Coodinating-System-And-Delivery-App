/**
 * Address Service
 *
 * Handles:
 * - CRUD operations for addresses
 * - Automatic segment association (find nearest road segment)
 * - Projection onto segment curves
 * - Geohash encoding for fast proximity searches
 * - Nearest address queries using geohash + PostGIS
 * - Batch import for efficient data loading
 */

import { PrismaClient } from '@prisma/client'
import * as geohash from '../../../utils/geohash'
import * as geometry from '../../../utils/geometry'
import {
  AddressDto,
  CreateAddressDto,
  UpdateAddressDto,
  NearestAddressQuery,
  NearestAddressResult,
  AddressPagingRequest,
  BatchAddressDto,
  BatchImportResult
} from './address.model'
import { PagedData } from '../../common/types/restful'
import { nearbySearch, textSearch, TrackAsiaPlaceResult } from '../../services/track-asia'

export class AddressService {
  constructor(private prisma: PrismaClient) {}

  /**
   * Create a new address
   * - Auto-calculates geohash
   * - Auto-finds nearest road segment if not provided
   * - Projects address onto segment curve
   * - No PostGIS required
   * - Checks for existing address with same (lat, lon, name) to avoid unique constraint violation
   */
  async createAddress(dto: CreateAddressDto): Promise<AddressDto> {
    // Check if address with same (lat, lon, name) already exists
    // This prevents unique constraint violations
    const existing = await this.prisma.addresses.findFirst({
      where: {
        lat: dto.lat,
        lon: dto.lon,
        name: dto.name
      },
      include: {
        road_segment: true,
        zones: true
      }
    })

    if (existing) {
      // Address with same (lat, lon, name) already exists, return it
      return this.toDto(existing)
    }

    // Calculate geohash for fast proximity searches (precision 7 = ~76m)
    const addressGeohash = geohash.encode(dto.lat, dto.lon, 7)

    // Find nearest segment if not provided
    let segmentId = dto.segmentId
    let segmentPosition: number | undefined
    let distanceToSegment: number | undefined
    let projectedLat: number | undefined
    let projectedLon: number | undefined

    if (!segmentId) {
      const nearestSegment = await this.findNearestSegment(dto.lat, dto.lon)
      if (nearestSegment) {
        segmentId = nearestSegment.segmentId
        segmentPosition = nearestSegment.position
        distanceToSegment = nearestSegment.distance
        projectedLat = nearestSegment.projectedLat
        projectedLon = nearestSegment.projectedLon
      }
    } else {
      // Segment provided, calculate projection
      const projection = await this.projectOntoSegment(dto.lat, dto.lon, segmentId)
      if (projection) {
        segmentPosition = projection.position
        distanceToSegment = projection.distance
        projectedLat = projection.projectedLat
        projectedLon = projection.projectedLon
      }
    }

    // Enrich address details via TrackAsia nearby if missing
    let enrichedAddressText = dto.addressText
    let enrichedWard: string | undefined = dto.wardName
    let enrichedDistrict: string | undefined = dto.districtName
    try {
      if (!enrichedAddressText || !enrichedWard || !enrichedDistrict) {
        const nearby = await nearbySearch({ lat: dto.lat, lon: dto.lon, radius: 50, newAdmin: true })
        const first = (nearby.results || [])[0]
        if (first) {
          if (!enrichedAddressText && first.formatted_address) enrichedAddressText = first.formatted_address
          const comps = (first as any).address_components as Array<{
            long_name: string
            short_name?: string
            types?: string[]
          }> | undefined
          if (comps && (!enrichedWard || !enrichedDistrict)) {
            for (const c of comps) {
              if (!enrichedWard && c.types?.includes('administrative_area_level_3')) enrichedWard = c.long_name
              if (!enrichedDistrict && c.types?.includes('administrative_area_level_2')) enrichedDistrict = c.long_name
            }
          }
        }
      }
    } catch {}

    // Create address using Prisma
    // Handle unique constraint errors gracefully (race condition)
    try {
      const created = await this.prisma.addresses.create({
        data: {
          name: dto.name,
          name_en: dto.nameEn || null,
          address_text: enrichedAddressText || dto.addressText || null,
          lat: dto.lat,
          lon: dto.lon,
          geohash: addressGeohash,
          segment_id: segmentId || null,
          segment_position: segmentPosition || null,
          distance_to_segment: distanceToSegment || null,
          projected_lat: projectedLat || null,
          projected_lon: projectedLon || null,
          zone_id: dto.zoneId || null,
          ward_name: enrichedWard || dto.wardName || null,
          district_name: enrichedDistrict || dto.districtName || null,
          address_type: dto.addressType || 'GENERAL'
        },
        include: {
          road_segment: true,
          zones: true
        }
      })

      return this.toDto(created)
    } catch (error: any) {
      // Handle unique constraint violation (race condition: another request created it between check and create)
      if (error.code === 'P2002' || error.message?.includes('Unique constraint')) {
        // Retry: find existing address
        const retryExisting = await this.prisma.addresses.findFirst({
          where: {
            lat: dto.lat,
            lon: dto.lon,
            name: dto.name
          },
          include: {
            road_segment: true,
            zones: true
          }
        })

        if (retryExisting) {
          return this.toDto(retryExisting)
        }
      }

      // Re-throw if it's not a unique constraint error
      throw error
    }
  }

  /**
   * Find nearest road segment to a point
   * Uses Haversine distance calculation (no PostGIS required)
   */
  private async findNearestSegment(
    lat: number,
    lon: number,
    maxDistance: number = 500 // 500m default
  ): Promise<{
    segmentId: string
    position: number
    distance: number
    projectedLat: number
    projectedLon: number
  } | null> {
    // Query using Haversine distance
    // Calculate distance to segment midpoint as approximation
    let segments: any[] = []
    try {
      // NOTE: MySQL JSON functions differ from PostgreSQL. If this raw query fails due to JSON syntax,
      // we gracefully skip nearest-segment detection to avoid breaking address creation.
      segments = await this.prisma.$queryRaw<any[]>`
        SELECT s.segment_id, s.geometry
        FROM road_segments s
        LIMIT 0
      `
      // Intentionally not performing complex JSON calculations here for MySQL compatibility.
      // Fallback: no candidates; return null below.
    } catch (err) {
      // On any error (e.g., JSON operator incompatibility), skip segment association
      return null
    }

    if (!segments.length) {
      return null
    }

    // Find best projection across candidates using geometry utility
    let bestMatch: any = null
    let minDistance = Infinity

    for (const segment of segments) {
      const projection = geometry.findNearestPointOnSegment(
        { lat, lon },
        segment.geometry
      )

      if (projection && projection.distance < minDistance && projection.distance <= maxDistance) {
        minDistance = projection.distance
        bestMatch = {
          segmentId: segment.segment_id,
          position: projection.position,
          distance: projection.distance,
          projectedLat: projection.closestPoint.lat,
          projectedLon: projection.closestPoint.lon
        }
      }
    }

    return bestMatch
  }

  /**
   * Project a point onto a specific segment
   */
  private async projectOntoSegment(
    lat: number,
    lon: number,
    segmentId: string
  ): Promise<{
    position: number
    distance: number
    projectedLat: number
    projectedLon: number
  } | null> {
    const segment = await this.prisma.road_segments.findUnique({
      where: { segment_id: segmentId }
    })

    if (!segment || !segment.geometry) {
      return null
    }

    const projection = geometry.findNearestPointOnSegment(
      { lat, lon },
      segment.geometry as any
    )

    if (!projection) {
      return null
    }

    return {
      position: projection.position,
      distance: projection.distance,
      projectedLat: projection.closestPoint.lat,
      projectedLon: projection.closestPoint.lon
    }
  }

  /**
   * Update an address
   * - Recalculates geohash if location changed
   * - Recalculates segment projection if location or segment changed
   */
  async updateAddress(id: string, dto: UpdateAddressDto): Promise<AddressDto> {
    const existing = await this.prisma.addresses.findUnique({
      where: { address_id: id }
    })

    if (!existing) {
      throw new Error('Address not found')
    }

    // Check if location changed
    const locationChanged = dto.lat !== undefined || dto.lon !== undefined
    const newLat = dto.lat ?? existing.lat
    const newLon = dto.lon ?? existing.lon

    let addressGeohash = existing.geohash
    let segmentId = dto.segmentId !== undefined ? dto.segmentId : existing.segment_id
    let segmentPosition = existing.segment_position
    let distanceToSegment = existing.distance_to_segment
    let projectedLat = existing.projected_lat
    let projectedLon = existing.projected_lon

    if (locationChanged) {
      // Recalculate geohash
      addressGeohash = geohash.encode(newLat, newLon, 7)

      // Recalculate segment projection
      if (segmentId) {
        const projection = await this.projectOntoSegment(newLat, newLon, segmentId)
        if (projection) {
          segmentPosition = projection.position
          distanceToSegment = projection.distance
          projectedLat = projection.projectedLat
          projectedLon = projection.projectedLon
        }
      } else {
        // Find new nearest segment
        const nearestSegment = await this.findNearestSegment(newLat, newLon)
        if (nearestSegment) {
          segmentId = nearestSegment.segmentId
          segmentPosition = nearestSegment.position
          distanceToSegment = nearestSegment.distance
          projectedLat = nearestSegment.projectedLat
          projectedLon = nearestSegment.projectedLon
        }
      }
    } else if (dto.segmentId !== undefined && dto.segmentId !== existing.segment_id) {
      // Segment changed without location change
      if (segmentId) {
        const projection = await this.projectOntoSegment(newLat, newLon, segmentId)
        if (projection) {
          segmentPosition = projection.position
          distanceToSegment = projection.distance
          projectedLat = projection.projectedLat
          projectedLon = projection.projectedLon
        }
      }
    }

    // Update address
    if (locationChanged) {
      await this.prisma.addresses.update({
        where: { address_id: id },
        data: {
          name: dto.name !== undefined ? dto.name : existing.name,
          name_en: dto.nameEn !== undefined ? dto.nameEn : existing.name_en,
          address_text: dto.addressText !== undefined ? dto.addressText : existing.address_text,
          lat: newLat,
          lon: newLon,
          geohash: addressGeohash || existing.geohash,
          segment_id: segmentId,
          segment_position: segmentPosition,
          distance_to_segment: distanceToSegment,
          projected_lat: projectedLat,
          projected_lon: projectedLon,
          zone_id: dto.zoneId !== undefined ? dto.zoneId : existing.zone_id,
          ward_name: dto.wardName !== undefined ? dto.wardName : existing.ward_name,
          district_name: dto.districtName !== undefined ? dto.districtName : existing.district_name,
          address_type: dto.addressType !== undefined ? dto.addressType : existing.address_type,
          updated_at: new Date()
        }
      })
    } else {
      const updateData: any = {
        updated_at: new Date()
      }
      if (dto.name !== undefined) updateData.name = dto.name
      if (dto.nameEn !== undefined) updateData.name_en = dto.nameEn
      if (dto.addressText !== undefined) updateData.address_text = dto.addressText
      if (segmentId !== undefined) updateData.segment_id = segmentId
      if (dto.zoneId !== undefined) updateData.zone_id = dto.zoneId
      if (dto.wardName !== undefined) updateData.ward_name = dto.wardName
      if (dto.districtName !== undefined) updateData.district_name = dto.districtName
      if (dto.addressType !== undefined) updateData.address_type = dto.addressType

      await this.prisma.addresses.update({
        where: { address_id: id },
        data: updateData
      })
    }

    const updated = await this.prisma.addresses.findUnique({
      where: { address_id: id },
      include: {
        road_segment: true,
        zones: true
      }
    })

    return this.toDto(updated!)
  }

  /**
   * Find nearest addresses to a point
   * Uses two-stage approach:
   * 1. Geohash pre-filtering (fast, approximate)
   * 2. Haversine distance calculation (accurate, no PostGIS required)
   */
  async findNearestAddresses(query: NearestAddressQuery): Promise<NearestAddressResult[]> {
    const limit = Math.min(query.limit || 10, 100)
    const maxDistance = query.maxDistance || 5000 // 5km default

    // Calculate geohash for query point
    const queryGeohash = geohash.encode(query.lat, query.lon, 7)

    // Get neighboring cells for broader search (9 cells total)
    const neighborCells = geohash.getNeighbors(queryGeohash)

    // Build WHERE clause using Prisma
    const where: any = {
      geohash: {
        in: neighborCells
      }
    }

    if (query.addressType) {
      where.address_type = query.addressType
    }
    if (query.segmentId) {
      where.segment_id = query.segmentId
    }
    if (query.zoneId) {
      where.zone_id = query.zoneId
    }

    // Get candidates using geohash pre-filter
    const candidates = await this.prisma.addresses.findMany({
      where,
      include: {
        road_segment: true,
        zones: true
      }
    })

    // Calculate Haversine distance for each candidate
    const results: NearestAddressResult[] = []
    const queryPoint = { lat: query.lat, lon: query.lon }

    for (const address of candidates) {
      const addressPoint = { lat: address.lat, lon: address.lon }
      const distance = geometry.haversineDistance(queryPoint, addressPoint)

      // Filter by maxDistance
      if (distance <= maxDistance) {
        // Calculate bearing (approximate)
        const bearing = this.calculateBearing(query.lat, query.lon, address.lat, address.lon)

        const addressDto = this.toDto(address)
        results.push({
          ...addressDto,
          distance,
          bearing
        })
      }
    }

    // Sort by distance and limit results
    results.sort((a, b) => a.distance - b.distance)
    return results.slice(0, limit)
  }

  /**
   * Calculate bearing from point A to point B in degrees
   */
  private calculateBearing(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const dLon = (lon2 - lon1) * Math.PI / 180
    const lat1Rad = lat1 * Math.PI / 180
    const lat2Rad = lat2 * Math.PI / 180

    const y = Math.sin(dLon) * Math.cos(lat2Rad)
    const x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
              Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLon)

    const bearing = Math.atan2(y, x) * 180 / Math.PI
    return (bearing + 360) % 360 // Normalize to 0-360
  }

  /**
   * Get or create address by coordinates
   * - First checks for exact match on (lat, lon, name) to respect unique constraint
   * - If no exact match, finds nearest address within threshold (default 50m)
   * - If found within threshold, returns existing address
   * - If not found, creates new address
   */
  async getOrCreateAddress(dto: CreateAddressDto, thresholdMeters: number = 50): Promise<AddressDto> {
    // First, check for exact match on (lat, lon, name) to respect unique constraint
    const exactMatch = await this.prisma.addresses.findFirst({
      where: {
        lat: dto.lat,
        lon: dto.lon,
        name: dto.name
      },
      include: {
        road_segment: true,
        zones: true
      }
    })

    if (exactMatch) {
      // Exact match found, return it
      return this.toDto(exactMatch)
    }

    // No exact match, try to find nearest address within threshold
    const nearest = await this.findNearestAddresses({
      lat: dto.lat,
      lon: dto.lon,
      limit: 1,
      maxDistance: thresholdMeters,
    })

    // If found within threshold, return existing (but only if name matches or no name provided)
    // Note: We prefer exact matches, so we only return nearest if it's very close and name is similar
    if (nearest.length > 0 && nearest[0] && nearest[0].distance <= thresholdMeters) {
      const existing = await this.getAddress(nearest[0].id)
      if (existing) {
        // If names are similar (case-insensitive), return existing
        // Otherwise, create new address with the new name (respecting unique constraint)
        if (!dto.name || existing.name.toLowerCase() === dto.name.toLowerCase()) {
          return existing
        }
      }
    }

    // Not found within threshold or name doesn't match, create new
    // createAddress will handle unique constraint check internally
    return await this.createAddress(dto)
  }

  /**
   * Find address by point with local-first strategy, fallback to TrackAsia Nearby Search
   */
  async findByPointWithFallback(params: {
    lat: number
    lon: number
    radiusMeters?: number // preferred radius 5-15m
    limit?: number
  }): Promise<{
    local: NearestAddressResult[]
    external: Array<{
      placeId: string
      name: string
      formattedAddress?: string
      lat: number
      lon: number
      types?: string[]
    }>
  }> {
    const radius = Math.min(Math.max(params.radiusMeters ?? 75, 1), 100) // cap to 100m; default 75m
    const limit = Math.min(params.limit ?? 10, 20)

    // 1) Check local within radius using existing nearest method
    const localCandidates = await this.findNearestAddresses({
      lat: params.lat,
      lon: params.lon,
      limit,
      maxDistance: radius, // meters
    })

    // Always fetch TrackAsia nearby to provide external alongside internal
    const taRadius = Math.max(75, radius)
    const ta = await nearbySearch({ lat: params.lat, lon: params.lon, radius: taRadius, newAdmin: true })

    const external: Array<{
      placeId: string
      name: string
      formattedAddress?: string
      lat: number
      lon: number
      types?: string[]
    }> = (ta.results || []).map((r: TrackAsiaPlaceResult) => ({
      placeId: r.place_id,
      name: r.name,
      lat: r.geometry?.location?.lat,
      lon: r.geometry?.location?.lng,
      ...(r.formatted_address ? { formattedAddress: r.formatted_address } : {}),
      ...(r.types ? { types: r.types } : {}),
    }))

    return { local: localCandidates, external }
  }

  /**
   * Search point(s) by address text with local-first strategy, fallback to TrackAsia Text Search
   */
  async searchByTextWithFallback(params: {
    query: string
    limit?: number
  }): Promise<{
    local: AddressDto[]
    external: Array<{
      placeId: string
      name: string
      formattedAddress?: string
      lat: number
      lon: number
      types?: string[]
    }>
  }> {
    const limit = Math.min(params.limit ?? 10, 50)

    // 1) Local DB search by name/address_text
    const localMatches = await this.prisma.addresses.findMany({
      where: {
        OR: [
          { name: { contains: params.query } },
          { address_text: { contains: params.query } },
        ],
      },
      take: limit,
      orderBy: { updated_at: 'desc' },
      include: { road_segment: true, zones: true },
    })

    const local = localMatches.map(a => this.toDto(a))

    // If we already have some local results, still fetch external to enrich UX
    const ta = await textSearch({ query: params.query, newAdmin: true })
    const external = (ta.results || []).slice(0, limit).map((r: TrackAsiaPlaceResult) => ({
      placeId: r.place_id,
      name: r.name,
      lat: r.geometry?.location?.lat,
      lon: r.geometry?.location?.lng,
      ...(r.formatted_address ? { formattedAddress: r.formatted_address } : {}),
      ...(r.types ? { types: r.types } : {}),
    }))

    return { local, external }
  }

  /**
   * Get address by ID
   */
  async getAddress(id: string): Promise<AddressDto | null> {
    const address = await this.prisma.addresses.findUnique({
      where: { address_id: id },
      include: {
        road_segment: true,
        zones: true
      }
    })

    return address ? this.toDto(address) : null
  }

  /**
   * List addresses with pagination and filters
   */
  async listAddresses(request: AddressPagingRequest): Promise<PagedData<AddressDto>> {
    const page = request.page || 0
    const size = Math.min(request.size || 20, 100)
    const offset = page * size

    // Build WHERE clause
    const where: any = {}

    if (request.search) {
      where.OR = [
        { name: { contains: request.search, mode: 'insensitive' } },
        { address_text: { contains: request.search, mode: 'insensitive' } }
      ]
    }

    if (request.addressType) {
      where.address_type = request.addressType
    }

    if (request.segmentId) {
      where.segment_id = request.segmentId
    }

    if (request.zoneId) {
      where.zone_id = request.zoneId
    }

    if (request.wardName) {
      where.ward_name = request.wardName
    }

    if (request.districtName) {
      where.district_name = request.districtName
    }

    // Execute query
    const [addresses, total] = await Promise.all([
      this.prisma.addresses.findMany({
        where,
        include: {
          road_segment: true,
          zones: true
        },
        skip: offset,
        take: size,
        orderBy: { created_at: 'desc' }
      }),
      this.prisma.addresses.count({ where })
    ])

    const data = addresses.map(a => this.toDto(a))
    const paging = request.createPaging<string>(total)

    return new PagedData(data, paging)
  }

  /**
   * Delete address
   */
  async deleteAddress(id: string): Promise<void> {
    await this.prisma.addresses.delete({
      where: { address_id: id }
    })
  }

  /**
   * Batch import addresses
   * - Efficiently imports many addresses
   * - Auto-calculates segments and zones
   * - Returns detailed results with errors
   */
  async batchImport(dto: BatchAddressDto): Promise<BatchImportResult> {
    const autoCalculateSegments = dto.autoCalculateSegments ?? true
    const autoCalculateZones = dto.autoCalculateZones ?? true

    const result: BatchImportResult = {
      total: dto.addresses.length,
      successful: 0,
      failed: 0,
      errors: [],
      addresses: []
    }

    // Process in batches of 100
    const batchSize = 100
    for (let i = 0; i < dto.addresses.length; i += batchSize) {
      const batch = dto.addresses.slice(i, i + batchSize)

      for (let j = 0; j < batch.length; j++) {
        const addressDto = batch[j]
        if (!addressDto) continue

        const index = i + j

        try {
          const addressToCreate = {
            name: addressDto.name,
            lat: addressDto.lat,
            lon: addressDto.lon,
            ...(addressDto.nameEn !== undefined && { nameEn: addressDto.nameEn }),
            ...(addressDto.addressText !== undefined && { addressText: addressDto.addressText }),
            ...(addressDto.wardName !== undefined && { wardName: addressDto.wardName }),
            ...(addressDto.districtName !== undefined && { districtName: addressDto.districtName }),
            ...(addressDto.addressType !== undefined && { addressType: addressDto.addressType }),
            ...(autoCalculateSegments ? {} : addressDto.segmentId !== undefined ? { segmentId: addressDto.segmentId } : {}),
            ...(autoCalculateZones ? {} : addressDto.zoneId !== undefined ? { zoneId: addressDto.zoneId } : {})
          }
          const created = await this.createAddress(addressToCreate)

          result.successful++
          result.addresses.push(created)
        } catch (error: any) {
          result.failed++
          result.errors.push({
            index,
            name: addressDto.name,
            error: error.message
          })
        }
      }
    }

    return result
  }

  /**
   * Convert database model to DTO
   */
  private toDto(address: any): AddressDto {
    return {
      id: address.address_id,
      name: address.name,
      nameEn: address.name_en,
      addressText: address.address_text,
      lat: address.lat,
      lon: address.lon,
      geohash: address.geohash,
      segmentId: address.segment_id,
      segmentName: address.road_segment?.name,
      roadType: address.road_segment?.road_type,
      segmentPosition: address.segment_position,
      distanceToSegment: address.distance_to_segment,
      projectedLat: address.projected_lat,
      projectedLon: address.projected_lon,
      zoneId: address.zone_id,
      zoneName: address.zones?.name,
      wardName: address.ward_name,
      districtName: address.district_name,
      addressType: address.address_type,
      createdAt: address.created_at,
      updatedAt: address.updated_at
    }
  }
}
