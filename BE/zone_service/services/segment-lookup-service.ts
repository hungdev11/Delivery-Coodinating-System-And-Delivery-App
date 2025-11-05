/**
 * Segment Lookup Service
 * 
 * Fast lookup service to find road segments using MySQL Spatial indexes.
 * Supports lookup by:
 * - segment_id (direct PK lookup)
 * - osm_way_id (indexed lookup)
 * - Geographic coordinates (point snapping with SPATIAL INDEX)
 * - Polyline/path (buffer + intersection with SPATIAL INDEX)
 */

import { PrismaClient, Prisma } from '@prisma/client';

const prisma = new PrismaClient();

export interface SegmentLookupResult {
  segment_id: string;
  osm_way_id?: bigint | null;
  name: string;
  road_type: string;
  length_meters: number;
  distance_meters?: number; // For geo lookups: distance from query point
  geometry?: any; // GeoJSON LineString
}

export interface PointSnapOptions {
  latitude: number;
  longitude: number;
  threshold_meters?: number; // Default 25m
  max_results?: number; // Default 5
}

export interface PolylineSnapOptions {
  coordinates: Array<[number, number]>; // [[lon, lat], ...]
  buffer_meters?: number; // Default 20m
  max_results?: number; // Default 50
}

export class SegmentLookupService {
  /**
   * Lookup segment by segment_id (fastest, O(1))
   */
  async getBySegmentId(segment_id: string): Promise<SegmentLookupResult | null> {
    const segment = await prisma.road_segments.findUnique({
      where: { segment_id },
      select: {
        segment_id: true,
        osm_way_id: true,
        name: true,
        road_type: true,
        length_meters: true,
        geometry: true,
      },
    });

    if (!segment) return null;

    return {
      segment_id: segment.segment_id,
      osm_way_id: segment.osm_way_id,
      name: segment.name,
      road_type: segment.road_type,
      length_meters: segment.length_meters,
      geometry: segment.geometry,
    };
  }

  /**
   * Lookup segments by osm_way_id (indexed)
   * One OSM way may map to multiple segments
   */
  async getByOsmWayId(osm_way_id: bigint | string): Promise<SegmentLookupResult[]> {
    const osmId = typeof osm_way_id === 'string' ? BigInt(osm_way_id) : osm_way_id;

    const segments = await prisma.road_segments.findMany({
      where: { osm_way_id: osmId },
      select: {
        segment_id: true,
        osm_way_id: true,
        name: true,
        road_type: true,
        length_meters: true,
        geometry: true,
      },
    });

    return segments.map(s => ({
      segment_id: s.segment_id,
      osm_way_id: s.osm_way_id,
      name: s.name,
      road_type: s.road_type,
      length_meters: s.length_meters,
      geometry: s.geometry,
    }));
  }

  /**
   * Snap point to nearest road segments (using SPATIAL INDEX)
   * Uses ST_Distance_Sphere for accurate distance calculation
   */
  async snapPoint(options: PointSnapOptions): Promise<SegmentLookupResult[]> {
    const { latitude, longitude, threshold_meters = 25, max_results = 5 } = options;

    // MySQL spatial query with ST_Distance_Sphere
    // Note: MySQL SRID 4326 uses (lon, lat) order for POINT
    const query = `
      SELECT 
        segment_id,
        osm_way_id,
        name,
        road_type,
        length_meters,
        geometry as geometry_json,
        ST_Distance_Sphere(
          geometry_linestring,
          ST_GeomFromText('POINT(${longitude} ${latitude})', 4326)
        ) as distance_meters
      FROM road_segments
      WHERE geometry_linestring IS NOT NULL
        AND ST_Distance_Sphere(
          geometry_linestring,
          ST_GeomFromText('POINT(${longitude} ${latitude})', 4326)
        ) <= ${threshold_meters}
      ORDER BY distance_meters ASC
      LIMIT ${max_results}
    `;

    const results = await prisma.$queryRawUnsafe<any[]>(query);

    return results.map(r => ({
      segment_id: r.segment_id,
      osm_way_id: r.osm_way_id ? BigInt(r.osm_way_id) : null,
      name: r.name,
      road_type: r.road_type,
      length_meters: r.length_meters,
      distance_meters: r.distance_meters,
      geometry: r.geometry_json,
    }));
  }

  /**
   * Find all segments intersecting a polyline/path (using SPATIAL INDEX)
   * Creates a buffer around the polyline and finds intersections
   */
  async snapPolyline(options: PolylineSnapOptions): Promise<SegmentLookupResult[]> {
    const { coordinates, buffer_meters = 20, max_results = 50 } = options;

    if (coordinates.length < 2) {
      throw new Error('Polyline must have at least 2 coordinates');
    }

    // Build WKT LineString: LINESTRING(lon lat, lon lat, ...)
    const wktPoints = coordinates.map(([lon, lat]) => `${lon} ${lat}`).join(', ');
    const wktLineString = `LINESTRING(${wktPoints})`;

    // MySQL spatial query: buffer polyline and find intersecting segments
    const query = `
      SELECT 
        segment_id,
        osm_way_id,
        name,
        road_type,
        length_meters,
        geometry as geometry_json,
        ST_Distance_Sphere(
          geometry_linestring,
          ST_GeomFromText('${wktLineString}', 4326)
        ) as distance_meters
      FROM road_segments
      WHERE geometry_linestring IS NOT NULL
        AND ST_Intersects(
          ST_Buffer(geometry_linestring, ${buffer_meters / 111320}),
          ST_Buffer(ST_GeomFromText('${wktLineString}', 4326), ${buffer_meters / 111320})
        )
      ORDER BY distance_meters ASC
      LIMIT ${max_results}
    `;

    const results = await prisma.$queryRawUnsafe<any[]>(query);

    return results.map(r => ({
      segment_id: r.segment_id,
      osm_way_id: r.osm_way_id ? BigInt(r.osm_way_id) : null,
      name: r.name,
      road_type: r.road_type,
      length_meters: r.length_meters,
      distance_meters: r.distance_meters,
      geometry: r.geometry_json,
    }));
  }

  /**
   * Find segments within a bounding box (for map viewport queries)
   */
  async getSegmentsInBBox(
    minLat: number,
    minLon: number,
    maxLat: number,
    maxLon: number,
    limit: number = 500
  ): Promise<SegmentLookupResult[]> {
    // Create bounding box polygon
    const bboxWKT = `POLYGON((
      ${minLon} ${minLat},
      ${maxLon} ${minLat},
      ${maxLon} ${maxLat},
      ${minLon} ${maxLat},
      ${minLon} ${minLat}
    ))`;

    const query = `
      SELECT 
        segment_id,
        osm_way_id,
        name,
        road_type,
        length_meters,
        geometry as geometry_json
      FROM road_segments
      WHERE geometry_linestring IS NOT NULL
        AND MBRIntersects(
          geometry_linestring,
          ST_GeomFromText('${bboxWKT}', 4326)
        )
      LIMIT ${limit}
    `;

    const results = await prisma.$queryRawUnsafe<any[]>(query);

    return results.map(r => ({
      segment_id: r.segment_id,
      osm_way_id: r.osm_way_id ? BigInt(r.osm_way_id) : null,
      name: r.name,
      road_type: r.road_type,
      length_meters: r.length_meters,
      geometry: r.geometry_json,
    }));
  }

  /**
   * Cleanup and disconnect
   */
  async disconnect() {
    await prisma.$disconnect();
  }
}

export const segmentLookupService = new SegmentLookupService();
