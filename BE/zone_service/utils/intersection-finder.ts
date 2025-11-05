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
 * SIMPLIFIED: Every node becomes an intersection point for segment creation
 * This ensures: nodes count ≈ segments count (N nodes → N-1 segments per road)
 */
export function findIntersections(roads: Road[]): Intersection[] {
  const intersections: Intersection[] = [];
  const nodeMap = new Map<string, { lat: number; lon: number; roads: Set<string> }>();

  // Build map of ALL nodes from all roads
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

  // CRITICAL CHANGE: Mark EVERY node as an intersection
  // This creates segments between every consecutive node pair
  // Result: N nodes → (N-1) segments, matching theory!
  for (const [nodeId, data] of nodeMap.entries()) {
          intersections.push({
            nodeId,
            lat: data.lat,
            lon: data.lon,
            roads: Array.from(data.roads),
          });
        }

  console.log(`   Found ${intersections.length} intersection points (all nodes)`);

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
  
  // CRITICAL: Maximum nodes per segment to prevent OSRM issues
  // If a road segment has too many nodes, OSRM may reject it or perform poorly
  const MAX_NODES_PER_SEGMENT = 50;

  for (const road of roads) {
    if (road.nodeIds.length < 2 || road.coordinates.length < 2) continue;
    
    const firstCoord = road.coordinates[0];
    if (!firstCoord) continue;
    
    let segmentStart = 0;
    let segmentCoords: Array<[number, number]> = [firstCoord];

    for (let i = 1; i < road.nodeIds.length; i++) {
      const nodeId = road.nodeIds[i];
      const coord = road.coordinates[i];
      if (!nodeId || !coord) continue;
      
      segmentCoords.push(coord);

      // Determine if we should break here
      const isIntersection = intersectionNodeIds.has(nodeId);
      const isEndOfRoad = i === road.nodeIds.length - 1;
      const isTooLong = segmentCoords.length >= MAX_NODES_PER_SEGMENT;
      
      // CRITICAL: Break at intersections OR end of road OR max length
      // BUT: Don't break on max length if we're at intersection (to preserve intersection node)
      const shouldBreak = isIntersection || isEndOfRoad || (isTooLong && !isIntersection);

      if (shouldBreak) {
        const fromNodeId = road.nodeIds[segmentStart];
        if (!fromNodeId) continue;

        // Push segment
        segments.push({
          roadId: road.roadId,
          fromNodeId,
          toNodeId: nodeId,
          coordinates: [...segmentCoords],
        });

        // Start new segment (CRITICAL: reuse current node as start of next segment)
        if (i < road.nodeIds.length - 1) {
        segmentStart = i;
          segmentCoords = [coord]; // Current node becomes start of next segment
        }
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
