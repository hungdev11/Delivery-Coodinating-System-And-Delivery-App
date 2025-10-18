/**
 * Utility to parse OSM .poly files
 * Format reference: https://wiki.openstreetmap.org/wiki/Osmosis/Polygon_Filter_File_Format
 */

import { readFileSync } from 'fs';

export interface Polygon {
  name: string;
  coordinates: number[][][]; // GeoJSON MultiPolygon format
}

/**
 * Parse a .poly file and return coordinates in GeoJSON format
 */
export function parsePolyFile(filePath: string): Polygon {
  const content = readFileSync(filePath, 'utf-8');
  const lines = content.split('\n');

  if (lines.length === 0) {
    throw new Error('Empty poly file');
  }

  // First non-empty line is the polygon name
  const name = lines[0].trim();
  let currentPolygon: number[][] = [];
  const allPolygons: number[][][] = [];

  for (let i = 1; i < lines.length; i++) {
    const line = lines[i];
    const trimmed = line.trim();

    // Skip empty lines
    if (!trimmed) continue;

    // Check for END marker
    if (trimmed === 'END') {
      if (currentPolygon.length > 0) {
        // Close the polygon
        const first = currentPolygon[0];
        const last = currentPolygon[currentPolygon.length - 1];
        if (first[0] !== last[0] || first[1] !== last[1]) {
          currentPolygon.push([...first]);
        }
        allPolygons.push(currentPolygon);
        currentPolygon = [];
      }
      continue;
    }

    // Try to parse as coordinates (two numbers separated by spaces)
    const parts = trimmed.split(/\s+/);
    if (parts.length >= 2) {
      const num1 = parseFloat(parts[0]);
      const num2 = parseFloat(parts[1]);

      // If both are valid numbers, treat as coordinates
      if (!isNaN(num1) && !isNaN(num2)) {
        currentPolygon.push([num1, num2]);
      }
      // Otherwise it's a section name, skip it
    }
  }

  // Handle last polygon if not explicitly ended
  if (currentPolygon.length > 0) {
    const first = currentPolygon[0];
    const last = currentPolygon[currentPolygon.length - 1];
    if (first[0] !== last[0] || first[1] !== last[1]) {
      currentPolygon.push([...first]);
    }
    allPolygons.push(currentPolygon);
  }

  return {
    name,
    coordinates: allPolygons,
  };
}

/**
 * Convert polygon to GeoJSON format
 */
export function polyToGeoJSON(polygon: Polygon): any {
  if (polygon.coordinates.length === 1) {
    return {
      type: 'Polygon',
      coordinates: polygon.coordinates,
    };
  } else {
    return {
      type: 'MultiPolygon',
      coordinates: polygon.coordinates.map(ring => [ring]),
    };
  }
}

/**
 * Calculate centroid of a polygon
 */
export function calculateCentroid(coordinates: number[][]): { lat: number; lon: number } {
  let sumLat = 0;
  let sumLon = 0;
  let count = 0;

  for (const coord of coordinates) {
    sumLon += coord[0];
    sumLat += coord[1];
    count++;
  }

  return {
    lat: sumLat / count,
    lon: sumLon / count,
  };
}

/**
 * Parse a multi-polygon .poly file where each section represents a separate zone
 * Returns array of individual polygons with their names
 */
export interface NamedPolygon {
  name: string;
  coordinates: number[][];
}

export function parseMultiPolyFile(filePath: string): NamedPolygon[] {
  const content = readFileSync(filePath, 'utf-8');
  const lines = content.split('\n');

  const polygons: NamedPolygon[] = [];
  let currentName = '';
  let currentPolygon: number[][] = [];
  let inPolygon = false;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const trimmed = line.trim();

    // Skip empty lines
    if (!trimmed) continue;

    // Check for END marker
    if (trimmed === 'END') {
      if (currentPolygon.length > 0 && currentName) {
        // Close the polygon
        const first = currentPolygon[0];
        const last = currentPolygon[currentPolygon.length - 1];
        if (first[0] !== last[0] || first[1] !== last[1]) {
          currentPolygon.push([...first]);
        }
        
        polygons.push({
          name: currentName,
          coordinates: currentPolygon,
        });
        
        currentPolygon = [];
        inPolygon = false;
      }
      continue;
    }

    // Try to parse as coordinates (two numbers separated by spaces)
    const parts = trimmed.split(/\s+/);
    if (parts.length >= 2) {
      const num1 = parseFloat(parts[0]);
      const num2 = parseFloat(parts[1]);

      // If both are valid numbers, treat as coordinates
      if (!isNaN(num1) && !isNaN(num2)) {
        currentPolygon.push([num1, num2]);
        inPolygon = true;
      } else {
        // Not coordinates - might be a name or section marker
        // If we're not in a polygon yet and this looks like text, it's a name
        if (!inPolygon && trimmed && !trimmed.includes('area_')) {
          currentName = trimmed;
        }
      }
    } else if (!inPolygon && trimmed && !trimmed.includes('area_')) {
      // Single word or name
      currentName = trimmed;
    }
  }

  // Handle last polygon if not explicitly ended
  if (currentPolygon.length > 0 && currentName) {
    const first = currentPolygon[0];
    const last = currentPolygon[currentPolygon.length - 1];
    if (first[0] !== last[0] || first[1] !== last[1]) {
      currentPolygon.push([...first]);
    }
    polygons.push({
      name: currentName,
      coordinates: currentPolygon,
    });
  }

  return polygons;
}

/**
 * Calculate union boundary (bounding polygon) from multiple polygons
 * Returns a simple convex hull-like boundary that encompasses all polygons
 */
export function unionPolygons(polygons: NamedPolygon[]): number[][] {
  // Collect all points from all polygons
  const allPoints: number[][] = [];
  
  for (const polygon of polygons) {
    allPoints.push(...polygon.coordinates);
  }

  if (allPoints.length === 0) {
    return [];
  }

  // Find bounding box
  let minLon = Infinity;
  let maxLon = -Infinity;
  let minLat = Infinity;
  let maxLat = -Infinity;

  for (const [lon, lat] of allPoints) {
    minLon = Math.min(minLon, lon);
    maxLon = Math.max(maxLon, lon);
    minLat = Math.min(minLat, lat);
    maxLat = Math.max(maxLat, lat);
  }

  // Create a simple rectangular boundary
  // For more accurate union, we'd need a proper computational geometry library
  const boundary = [
    [minLon, minLat],
    [maxLon, minLat],
    [maxLon, maxLat],
    [minLon, maxLat],
    [minLon, minLat], // Close the polygon
  ];

  return boundary;
}

/**
 * Generate a zone code from Vietnamese ward name
 * Examples:
 *   "Phường Linh Xuân" -> "LX"
 *   "Phường Tam Phú" -> "TP"
 *   "Phường Long Bình" -> "LB"
 */
export function generateZoneCode(name: string): string {
  // Remove common prefixes
  let cleanName = name
    .replace(/^Phường\s+/i, '')
    .replace(/^Xã\s+/i, '')
    .replace(/^Thị trấn\s+/i, '')
    .trim();

  // Split into words
  const words = cleanName.split(/\s+/);

  // Take first letter of each word
  let code = words
    .map(word => word.charAt(0).toUpperCase())
    .join('');

  // If code is too long, take only first 2-3 letters
  if (code.length > 4) {
    code = code.substring(0, 3);
  }

  // If code is too short, pad or use first few chars of first word
  if (code.length < 2 && words.length > 0) {
    code = words[0].substring(0, 2).toUpperCase();
  }

  return code;
}
