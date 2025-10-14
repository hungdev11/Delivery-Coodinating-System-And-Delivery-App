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
