import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

export interface RoadNodeResult {
  id: string;
  latitude: number;
  longitude: number;
  distance: number; // Distance in meters
}

export class RoadNodesService {
  /**
   * Find nearest road nodes to a location using Haversine formula
   * Returns nodes sorted by distance (closest first)
   */
  async findNearestNodes(
    lat: number,
    lon: number,
    radiusMeters: number = 100
  ): Promise<RoadNodeResult[]> {
    // Use Haversine formula to calculate distance
    // MySQL spatial query would be better but requires spatial index setup
    // For now, use a simple bounding box filter + Haversine calculation
    
    // Approximate degrees per meter (at equator)
    const degreesPerMeter = 1 / 111000;
    const latDelta = radiusMeters * degreesPerMeter;
    const lonDelta = radiusMeters * degreesPerMeter / Math.cos(lat * Math.PI / 180);

    // Get nodes within bounding box (faster than full scan)
    const nodes = await prisma.road_nodes.findMany({
      where: {
        lat: {
          gte: lat - latDelta,
          lte: lat + latDelta,
        },
        lon: {
          gte: lon - lonDelta,
          lte: lon + lonDelta,
        },
      },
    });

    // Calculate distance for each node and filter by radius
    const nodesWithDistance: RoadNodeResult[] = nodes
      .map((node) => {
        const distance = this.calculateHaversineDistance(
          lat,
          lon,
          node.lat,
          node.lon
        );
        return {
          id: node.node_id,
          latitude: node.lat,
          longitude: node.lon,
          distance,
        };
      })
      .filter((node) => node.distance <= radiusMeters)
      .sort((a, b) => a.distance - b.distance)
      .slice(0, 5); // Return top 5 nearest

    return nodesWithDistance;
  }

  /**
   * Calculate distance between two points using Haversine formula (in meters)
   */
  private calculateHaversineDistance(
    lat1: number,
    lon1: number,
    lat2: number,
    lon2: number
  ): number {
    const R = 6371000; // Earth radius in meters
    const lat1Rad = (lat1 * Math.PI) / 180;
    const lat2Rad = (lat2 * Math.PI) / 180;
    const deltaLat = ((lat2 - lat1) * Math.PI) / 180;
    const deltaLon = ((lon2 - lon1) * Math.PI) / 180;

    const a =
      Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
      Math.cos(lat1Rad) *
        Math.cos(lat2Rad) *
        Math.sin(deltaLon / 2) *
        Math.sin(deltaLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return R * c;
  }
}
