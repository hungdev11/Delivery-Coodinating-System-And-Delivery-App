/**
 * Utilities for finding road intersections and generating segments
 */

export interface Point {
  lat: number;
  lon: number;
  nodeId: string;
}

export interface Road {
  roadId: string;
  name: string;
  coordinates: Array<[number, number]>; // [lon, lat]
  nodeIds: string[];
}

export interface Intersection {
  nodeId: string;
  lat: number;
  lon: number;
  roads: string[]; // Road IDs that intersect here
}

/**
 * Find intersections between roads
 * This is a simplified version - production would use spatial indexing
 */
export function findIntersections(roads: Road[]): Intersection[] {
  const nodeMap = new Map<string, { lat: number; lon: number; roads: Set<string> }>();

  // Build map of all nodes and which roads use them
  for (const road of roads) {
    for (let i = 0; i < road.nodeIds.length; i++) {
      const nodeId = road.nodeIds[i];
      const coord = road.coordinates[i];
      if (!nodeId || !coord) continue;
      const [lon, lat] = coord;

      if (!nodeMap.has(nodeId)) {
        nodeMap.set(nodeId, {
          lat,
          lon,
          roads: new Set(),
        });
      }

      nodeMap.get(nodeId)!.roads.add(road.roadId);
    }
  }

  // Find nodes where multiple roads meet
  const intersections: Intersection[] = [];

  for (const [nodeId, data] of nodeMap.entries()) {
    // Intersection if used by 2+ roads, or at start/end of a road
    if (data.roads.size >= 2) {
      intersections.push({
        nodeId,
        lat: data.lat,
        lon: data.lon,
        roads: Array.from(data.roads),
      });
    } else if (data.roads.size === 1) {
      // Also add start/end points of roads
      const roadId = Array.from(data.roads)[0];
      const road = roads.find(r => r.roadId === roadId);
      if (road) {
        const nodeIndex = road.nodeIds.indexOf(nodeId);
        if (nodeIndex === 0 || nodeIndex === road.nodeIds.length - 1) {
          intersections.push({
            nodeId,
            lat: data.lat,
            lon: data.lon,
            roads: Array.from(data.roads),
          });
        }
      }
    }
  }

  return intersections;
}

/**
 * Generate road segments from roads and intersections
 */
export interface Segment {
  roadId: string;
  fromNodeId: string;
  toNodeId: string;
  coordinates: Array<[number, number]>;
}

export function generateSegments(roads: Road[], intersections: Intersection[]): Segment[] {
  const segments: Segment[] = [];
  const intersectionNodeIds = new Set(intersections.map(i => i.nodeId));

  for (const road of roads) {
    let segmentStart = 0;
    const firstCoord = road.coordinates[0];
    if (!firstCoord) continue;
    let segmentCoords: Array<[number, number]> = [firstCoord];

    for (let i = 1; i < road.nodeIds.length; i++) {
      const nodeId = road.nodeIds[i];
      const coord = road.coordinates[i];
      if (!nodeId || !coord) continue;
      segmentCoords.push(coord);

      // Create segment when we hit an intersection or end of road
      if (intersectionNodeIds.has(nodeId) || i === road.nodeIds.length - 1) {
        const fromNodeId = road.nodeIds[segmentStart];
        if (!fromNodeId) continue;

        segments.push({
          roadId: road.roadId,
          fromNodeId,
          toNodeId: nodeId,
          coordinates: [...segmentCoords],
        });

        // Start new segment
        segmentStart = i;
        segmentCoords = [coord];
      }
    }
  }

  return segments;
}

/**
 * Find roads with duplicate names (for deduplication)
 */
export function findDuplicateRoads(roads: Road[]): Map<string, Road[]> {
  const nameMap = new Map<string, Road[]>();

  for (const road of roads) {
    const normalizedName = road.name.toLowerCase().trim();
    if (!nameMap.has(normalizedName)) {
      nameMap.set(normalizedName, []);
    }
    nameMap.get(normalizedName)!.push(road);
  }

  // Filter to only duplicates
  const duplicates = new Map<string, Road[]>();
  for (const [name, roadList] of nameMap.entries()) {
    if (roadList.length > 1) {
      duplicates.set(name, roadList);
    }
  }

  return duplicates;
}

/**
 * Check if two road segments are connected (within threshold distance)
 */
export function areSegmentsConnected(
  seg1End: [number, number],
  seg2Start: [number, number],
  thresholdMeters: number = 50
): boolean {
  const [lon1, lat1] = seg1End;
  const [lon2, lat2] = seg2Start;

  const distance = calculateDistance(lat1, lon1, lat2, lon2);
  return distance <= thresholdMeters;
}

/**
 * Calculate distance between two coordinates (Haversine formula)
 */
function calculateDistance(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const R = 6371e3; // Earth radius in meters
  const φ1 = (lat1 * Math.PI) / 180;
  const φ2 = (lat2 * Math.PI) / 180;
  const Δφ = ((lat2 - lat1) * Math.PI) / 180;
  const Δλ = ((lon2 - lon1) * Math.PI) / 180;

  const a =
    Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
    Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return R * c;
}

/**
 * Merge connected road segments with the same name
 */
export function mergeConnectedRoads(roads: Road[], thresholdMeters: number = 50): Road[] {
  const duplicateGroups = findDuplicateRoads(roads);
  const mergedRoads: Road[] = [];
  const processedIds = new Set<string>();

  for (const [_name, roadGroup] of duplicateGroups.entries()) {
    // Try to merge roads in this group
    const merged = mergeRoadGroup(roadGroup, thresholdMeters);
    mergedRoads.push(...merged);

    for (const road of roadGroup) {
      processedIds.add(road.roadId);
    }
  }

  // Add roads that weren't duplicates
  for (const road of roads) {
    if (!processedIds.has(road.roadId)) {
      mergedRoads.push(road);
    }
  }

  return mergedRoads;
}

/**
 * Merge a group of roads with the same name
 */
function mergeRoadGroup(roads: Road[], thresholdMeters: number): Road[] {
  if (roads.length <= 1) return roads;

  const merged: Road[] = [];
  const used = new Set<string>();

  for (const road of roads) {
    if (used.has(road.roadId)) continue;

    // Try to find roads that connect to this one
    let currentRoad = road;
    let extended = true;

    while (extended) {
      extended = false;

      for (const otherRoad of roads) {
        if (used.has(otherRoad.roadId) || otherRoad.roadId === currentRoad.roadId) {
          continue;
        }

        // Check if they connect
        const currentEnd = currentRoad.coordinates[currentRoad.coordinates.length - 1];
        const otherStart = otherRoad.coordinates[0];

        if (currentEnd && otherStart && areSegmentsConnected(currentEnd, otherStart, thresholdMeters)) {
          // Merge them
          currentRoad = {
            ...currentRoad,
            coordinates: [...currentRoad.coordinates, ...otherRoad.coordinates.slice(1)],
            nodeIds: [...currentRoad.nodeIds, ...otherRoad.nodeIds.slice(1)],
          };
          used.add(otherRoad.roadId);
          extended = true;
        }
      }
    }

    merged.push(currentRoad);
    used.add(road.roadId);
  }

  return merged;
}
