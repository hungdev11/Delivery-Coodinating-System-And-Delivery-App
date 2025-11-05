/**
 * Routing Helper Utilities
 *
 * Helper functions for routing calculations and formatting
 */

import type { Route, Waypoint } from '@/modules/Zones/routing.type'

/**
 * Format distance in meters to human-readable format
 */
export function formatDistance(meters: number): string {
  if (meters < 1000) {
    return `${Math.round(meters)} m`
  }
  return `${(meters / 1000).toFixed(1)} km`
}

/**
 * Format duration in seconds to human-readable format
 */
export function formatDuration(seconds: number): string {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)

  if (hours > 0) {
    return `${hours}h ${minutes}m`
  }
  return `${minutes}m`
}

/**
 * Format speed in m/s to km/h
 */
export function formatSpeed(metersPerSecond: number): string {
  const kmh = metersPerSecond * 3.6
  return `${Math.round(kmh)} km/h`
}

/**
 * Parse GeoJSON geometry from route to coordinates array
 */
export function parseRouteGeometry(geometryString: string): [number, number][] {
  try {
    const geometry = JSON.parse(geometryString)
    if (geometry.type === 'LineString' && Array.isArray(geometry.coordinates)) {
      return geometry.coordinates as [number, number][]
    }
    return []
  } catch (error) {
    console.error('Failed to parse route geometry:', error)
    return []
  }
}

/**
 * Calculate bounds for waypoints
 */
export function calculateBounds(
  waypoints: Waypoint[]
): [[number, number], [number, number]] | null {
  if (waypoints.length === 0) {
    return null
  }

  let minLng = Infinity
  let maxLng = -Infinity
  let minLat = Infinity
  let maxLat = -Infinity

  for (const wp of waypoints) {
    minLng = Math.min(minLng, wp.lon)
    maxLng = Math.max(maxLng, wp.lon)
    minLat = Math.min(minLat, wp.lat)
    maxLat = Math.max(maxLat, wp.lat)
  }

  // Add padding
  const lngPadding = (maxLng - minLng) * 0.1
  const latPadding = (maxLat - minLat) * 0.1

  return [
    [minLng - lngPadding, minLat - latPadding],
    [maxLng + lngPadding, maxLat + latPadding],
  ]
}

/**
 * Get congestion level color
 */
export function getCongestionColor(level: string): string {
  const colors: Record<string, string> = {
    FREE_FLOW: '#10b981', // green
    NORMAL: '#3b82f6', // blue
    SLOW: '#f59e0b', // amber
    CONGESTED: '#f97316', // orange
    BLOCKED: '#ef4444', // red
  }
  return colors[level] || '#6b7280'
}

/**
 * Get congestion level label
 */
export function getCongestionLabel(level: string): string {
  const labels: Record<string, string> = {
    FREE_FLOW: 'Free Flow',
    NORMAL: 'Normal',
    SLOW: 'Slow',
    CONGESTED: 'Congested',
    BLOCKED: 'Blocked',
  }
  return labels[level] || 'Unknown'
}
