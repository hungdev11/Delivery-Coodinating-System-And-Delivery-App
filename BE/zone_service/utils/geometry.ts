/**
 * Geometry utility functions for spatial calculations
 *
 * Key operations:
 * - Distance calculation (Haversine formula)
 * - Point-to-line-segment projection
 * - Closest point on curve
 */

export interface Point {
  lat: number
  lon: number
}

export interface LineSegment {
  start: Point
  end: Point
}

const EARTH_RADIUS_METERS = 6371000 // Earth's radius in meters

/**
 * Calculate distance between two points using Haversine formula
 * Accurate for short distances (< 1000km)
 *
 * @param p1 First point
 * @param p2 Second point
 * @returns Distance in meters
 */
export function haversineDistance(p1: Point, p2: Point): number {
  const lat1 = toRadians(p1.lat)
  const lat2 = toRadians(p2.lat)
  const deltaLat = toRadians(p2.lat - p1.lat)
  const deltaLon = toRadians(p2.lon - p1.lon)

  const a =
    Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
    Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2)

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

  return EARTH_RADIUS_METERS * c
}

/**
 * Project a point onto a line segment and find the closest point
 *
 * Returns:
 * - closestPoint: The point on the line segment closest to the input point
 * - distance: Perpendicular distance from point to line segment (meters)
 * - position: Position along segment (0.0 = start, 1.0 = end)
 *
 * Algorithm:
 * 1. Convert lat/lon to approximate Cartesian (for short distances)
 * 2. Find projection using dot product
 * 3. Clamp position to [0, 1] to stay on segment
 * 4. Calculate perpendicular distance
 */
export function projectPointToSegment(
  point: Point,
  segment: LineSegment
): {
  closestPoint: Point
  distance: number
  position: number // 0.0 to 1.0
} {
  const { start, end } = segment

  // Convert to approximate Cartesian coordinates (meters)
  // For short distances, this is accurate enough
  const pointX = longitudeToMeters(point.lon, point.lat)
  const pointY = latitudeToMeters(point.lat)
  const startX = longitudeToMeters(start.lon, start.lat)
  const startY = latitudeToMeters(start.lat)
  const endX = longitudeToMeters(end.lon, end.lat)
  const endY = latitudeToMeters(end.lat)

  // Vector from start to end
  const dx = endX - startX
  const dy = endY - startY

  // If start and end are the same point
  if (dx === 0 && dy === 0) {
    return {
      closestPoint: start,
      distance: haversineDistance(point, start),
      position: 0
    }
  }

  // Calculate position along segment using dot product
  // t = 0 means closest point is at start
  // t = 1 means closest point is at end
  // 0 < t < 1 means closest point is between start and end
  const t = Math.max(0, Math.min(1, ((pointX - startX) * dx + (pointY - startY) * dy) / (dx * dx + dy * dy)))

  // Calculate closest point on segment
  const closestX = startX + t * dx
  const closestY = startY + t * dy

  // Convert back to lat/lon
  const closestLat = metersToLatitude(closestY)
  const closestLon = metersToLongitude(closestX, closestLat)

  const closestPoint: Point = { lat: closestLat, lon: closestLon }

  // Calculate distance
  const distance = haversineDistance(point, closestPoint)

  return {
    closestPoint,
    distance,
    position: t
  }
}

/**
 * Project a point onto a polyline (multiple connected segments)
 * Returns the closest point across all segments
 *
 * @param point Point to project
 * @param polyline Array of points forming a polyline
 */
export function projectPointToPolyline(
  point: Point,
  polyline: Point[]
): {
  closestPoint: Point
  distance: number
  segmentIndex: number // Which segment (0 = first segment)
  position: number // Position along that segment (0.0 to 1.0)
  totalPosition: number // Position along entire polyline (0.0 to 1.0)
} | null {
  if (polyline.length < 2) {
    return null
  }

  let minDistance = Infinity
  let bestResult: any = null
  let totalLength = 0
  const segmentLengths: number[] = []

  // Calculate segment lengths first
  for (let i = 0; i < polyline.length - 1; i++) {
    const length = haversineDistance(polyline[i], polyline[i + 1])
    segmentLengths.push(length)
    totalLength += length
  }

  let cumulativeLength = 0

  // Find closest segment
  for (let i = 0; i < polyline.length - 1; i++) {
    const segment: LineSegment = {
      start: polyline[i],
      end: polyline[i + 1]
    }

    const result = projectPointToSegment(point, segment)

    if (result.distance < minDistance) {
      minDistance = result.distance

      // Calculate position along entire polyline
      const positionInSegment = result.position * segmentLengths[i]
      const totalPosition = (cumulativeLength + positionInSegment) / totalLength

      bestResult = {
        closestPoint: result.closestPoint,
        distance: result.distance,
        segmentIndex: i,
        position: result.position,
        totalPosition: isNaN(totalPosition) ? 0 : totalPosition
      }
    }

    cumulativeLength += segmentLengths[i]
  }

  return bestResult
}

/**
 * Find the nearest point on a road segment geometry to a given address
 * Road segment geometry is stored as GeoJSON LineString
 *
 * @param point Address point
 * @param segmentGeometry GeoJSON LineString geometry from road_segments.geometry
 */
export function findNearestPointOnSegment(
  point: Point,
  segmentGeometry: { type: 'LineString'; coordinates: [number, number][] }
): {
  closestPoint: Point
  distance: number
  position: number
} | null {
  if (segmentGeometry.type !== 'LineString' || !segmentGeometry.coordinates?.length) {
    return null
  }

  // Convert GeoJSON coordinates [lon, lat] to Point {lat, lon}
  const polyline: Point[] = segmentGeometry.coordinates.map(([lon, lat]) => ({ lat, lon }))

  const result = projectPointToPolyline(point, polyline)

  if (!result) {
    return null
  }

  return {
    closestPoint: result.closestPoint,
    distance: result.distance,
    position: result.totalPosition
  }
}

// ========== Helper Functions ==========

function toRadians(degrees: number): number {
  return degrees * (Math.PI / 180)
}

function toDegrees(radians: number): number {
  return radians * (180 / Math.PI)
}

/**
 * Convert latitude to meters (from equator)
 */
function latitudeToMeters(lat: number): number {
  return lat * (Math.PI / 180) * EARTH_RADIUS_METERS
}

/**
 * Convert longitude to meters at a given latitude
 */
function longitudeToMeters(lon: number, lat: number): number {
  return lon * (Math.PI / 180) * EARTH_RADIUS_METERS * Math.cos(toRadians(lat))
}

/**
 * Convert meters to latitude
 */
function metersToLatitude(meters: number): number {
  return toDegrees(meters / EARTH_RADIUS_METERS)
}

/**
 * Convert meters to longitude at a given latitude
 */
function metersToLongitude(meters: number, lat: number): number {
  return toDegrees(meters / (EARTH_RADIUS_METERS * Math.cos(toRadians(lat))))
}

/**
 * Calculate bounding box around a point with a given radius
 * Useful for spatial queries
 *
 * @param center Center point
 * @param radiusMeters Radius in meters
 * @returns Bounding box {minLat, maxLat, minLon, maxLon}
 */
export function getBoundingBox(
  center: Point,
  radiusMeters: number
): {
  minLat: number
  maxLat: number
  minLon: number
  maxLon: number
} {
  const latDelta = metersToLatitude(radiusMeters)
  const lonDelta = metersToLongitude(radiusMeters, center.lat)

  return {
    minLat: center.lat - latDelta,
    maxLat: center.lat + latDelta,
    minLon: center.lon - lonDelta,
    maxLon: center.lon + lonDelta
  }
}
