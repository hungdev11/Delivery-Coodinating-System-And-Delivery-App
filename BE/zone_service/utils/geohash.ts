/**
 * Geohash utility functions for fast spatial proximity searches
 *
 * Geohash encodes latitude/longitude into a short string of letters and digits.
 * Strings with a common prefix are spatially close together.
 *
 * Precision levels:
 * - 5: ±2.4km  (city-level)
 * - 6: ±610m   (neighborhood)
 * - 7: ±76m    (street-level) ✅ Default for addresses
 * - 8: ±19m    (building-level)
 * - 9: ±2.4m   (room-level)
 */

const BASE32 = '0123456789bcdefghjkmnpqrstuvwxyz'

/**
 * Encode latitude/longitude to geohash
 * @param lat Latitude (-90 to 90)
 * @param lon Longitude (-180 to 180)
 * @param precision Geohash length (default: 7 for ~76m precision)
 */
export function encode(lat: number, lon: number, precision: number = 7): string {
  let idx = 0
  let bit = 0
  let evenBit = true
  let geohash = ''

  let latMin = -90
  let latMax = 90
  let lonMin = -180
  let lonMax = 180

  while (geohash.length < precision) {
    if (evenBit) {
      // Longitude
      const lonMid = (lonMin + lonMax) / 2
      if (lon > lonMid) {
        idx = (idx << 1) + 1
        lonMin = lonMid
      } else {
        idx = idx << 1
        lonMax = lonMid
      }
    } else {
      // Latitude
      const latMid = (latMin + latMax) / 2
      if (lat > latMid) {
        idx = (idx << 1) + 1
        latMin = latMid
      } else {
        idx = idx << 1
        latMax = latMid
      }
    }

    evenBit = !evenBit

    if (++bit === 5) {
      geohash += BASE32[idx]
      bit = 0
      idx = 0
    }
  }

  return geohash
}

/**
 * Decode geohash to latitude/longitude bounds
 * @param geohash Geohash string
 * @returns Bounding box and center point
 */
export function decode(geohash: string): {
  lat: { min: number; max: number; mid: number }
  lon: { min: number; max: number; mid: number }
} {
  let evenBit = true
  let latMin = -90
  let latMax = 90
  let lonMin = -180
  let lonMax = 180

  for (let i = 0; i < geohash.length; i++) {
    const chr = geohash[i]
    if (!chr) {
      throw new Error(`Invalid geohash character at position ${i}`)
    }
    const idx = BASE32.indexOf(chr)

    if (idx === -1) {
      throw new Error(`Invalid geohash character: ${chr}`)
    }

    for (let n = 4; n >= 0; n--) {
      const bitN = (idx >> n) & 1

      if (evenBit) {
        // Longitude
        const lonMid = (lonMin + lonMax) / 2
        if (bitN === 1) {
          lonMin = lonMid
        } else {
          lonMax = lonMid
        }
      } else {
        // Latitude
        const latMid = (latMin + latMax) / 2
        if (bitN === 1) {
          latMin = latMid
        } else {
          latMax = latMid
        }
      }

      evenBit = !evenBit
    }
  }

  return {
    lat: { min: latMin, max: latMax, mid: (latMin + latMax) / 2 },
    lon: { min: lonMin, max: lonMax, mid: (lonMin + lonMax) / 2 }
  }
}

/**
 * Get all 8 neighboring geohash cells (N, NE, E, SE, S, SW, W, NW)
 * Used for proximity searches - query the target cell + 8 neighbors = 9 cells
 *
 * @param geohash Center geohash
 * @returns Array of 9 geohashes (center + 8 neighbors)
 */
export function getNeighbors(geohash: string): string[] {
  const n = neighbor(geohash, 'n')
  const s = neighbor(geohash, 's')
  const e = neighbor(geohash, 'e')
  const w = neighbor(geohash, 'w')

  const neighbors = [
    geohash, // Center
    n,  // North
    s,  // South
    e,  // East
    w,  // West
    n ? neighbor(n, 'e') : null, // NE
    n ? neighbor(n, 'w') : null, // NW
    s ? neighbor(s, 'e') : null, // SE
    s ? neighbor(s, 'w') : null  // SW
  ]

  return neighbors.filter(Boolean) as string[]
}

/**
 * Get adjacent geohash in given direction
 * @param geohash Source geohash
 * @param direction Direction: 'n', 's', 'e', 'w'
 */
function neighbor(geohash: string, direction: 'n' | 's' | 'e' | 'w'): string | null {
  const lastChar = geohash.slice(-1)
  const parent = geohash.slice(0, -1)
  const type = geohash.length % 2 === 0 ? 'even' : 'odd'

  // Lookup tables for neighbor calculations
  const neighbors: Record<string, Record<string, string>> = {
    n: {
      even: 'p0r21436x8zb9dcf5h7kjnmqesgutwvy',
      odd: 'bc01fg45238967deuvhjyznpkmstqrwx'
    },
    s: {
      even: '14365h7k9dcfesgujnmqp0r2twvyx8zb',
      odd: '238967debc01fg45kmstqrwxuvhjyznp'
    },
    e: {
      even: 'bc01fg45238967deuvhjyznpkmstqrwx',
      odd: 'p0r21436x8zb9dcf5h7kjnmqesgutwvy'
    },
    w: {
      even: '238967debc01fg45kmstqrwxuvhjyznp',
      odd: '14365h7k9dcfesgujnmqp0r2twvyx8zb'
    }
  }

  const borders: Record<string, Record<string, string>> = {
    n: { even: 'prxz', odd: 'bcfguvyz' },
    s: { even: '028b', odd: '0145hjnp' },
    e: { even: 'bcfguvyz', odd: 'prxz' },
    w: { even: '0145hjnp', odd: '028b' }
  }

  const border = borders[direction]?.[type]
  const neighborChars = neighbors[direction]?.[type]

  if (!border || !neighborChars) {
    return null
  }

  if (border.includes(lastChar) && parent) {
    const parentNeighbor = neighbor(parent, direction)
    if (!parentNeighbor) return null
    return parentNeighbor + BASE32[neighborChars.indexOf(lastChar)]
  } else {
    return parent + BASE32[neighborChars.indexOf(lastChar)]
  }
}

/**
 * Calculate bounding box for a list of geohash cells
 * Useful for debugging and visualization
 */
export function getBoundingBox(geohashes: string[]): {
  minLat: number
  maxLat: number
  minLon: number
  maxLon: number
} {
  let minLat = Infinity
  let maxLat = -Infinity
  let minLon = Infinity
  let maxLon = -Infinity

  for (const gh of geohashes) {
    const bounds = decode(gh)
    minLat = Math.min(minLat, bounds.lat.min)
    maxLat = Math.max(maxLat, bounds.lat.max)
    minLon = Math.min(minLon, bounds.lon.min)
    maxLon = Math.max(maxLon, bounds.lon.max)
  }

  return { minLat, maxLat, minLon, maxLon }
}

/**
 * Validate geohash string
 */
export function isValid(geohash: string): boolean {
  if (!geohash || typeof geohash !== 'string') return false
  return /^[0-9bcdefghjkmnpqrstuvwxyz]+$/.test(geohash)
}

/**
 * Get approximate error distance for a geohash precision level
 * @param precision Geohash length
 * @returns Approximate error in meters
 */
export function getPrecisionError(precision: number): number {
  const errors: Record<number, number> = {
    1: 5000000, // ±5000km
    2: 630000,  // ±630km
    3: 78000,   // ±78km
    4: 20000,   // ±20km
    5: 2400,    // ±2.4km
    6: 610,     // ±610m
    7: 76,      // ±76m (street-level)
    8: 19,      // ±19m
    9: 2.4,     // ±2.4m
    10: 0.6,    // ±60cm
    11: 0.074,  // ±7.4cm
    12: 0.019   // ±1.9cm
  }
  return errors[precision] || 76
}
