/**
 * Routing Service
 * Static class for routing business logic
 */

import { prisma } from '../../common/database/prisma.client';
import { logger } from '../../common/logger/logger.service';
import { createError } from '../../common/middleware/error.middleware';
import { OSRMRouterService } from '../../services/osrm/osrm-router.service';
import {
  RouteRequestDto,
  RouteResponseDto,
  RouteStepDto,
  RouteLegDto,
  RouteDto,
  DemoRouteRequestDto,
  DemoRouteResponseDto,
  WaypointDto,
} from './routing.model';

export class RoutingService {
  private static osrmRouter = new OSRMRouterService();

  /**
   * Calculate route between waypoints
   * Handles moveToEnd flag: waypoints with moveToEnd=true are moved to end of route
   */
  public static async calculateRoute(request: RouteRequestDto): Promise<RouteResponseDto> {
    try {
      logger.info(`Calculating route with ${request.waypoints.length} waypoints (vehicle: ${request.vehicle || 'car'})`);

      // Separate waypoints with moveToEnd flag
      const normalWaypoints: WaypointDto[] = [];
      const moveToEndWaypoints: WaypointDto[] = [];
      
      for (const waypoint of request.waypoints) {
        if (waypoint.moveToEnd === true) {
          moveToEndWaypoints.push(waypoint);
          logger.debug(`Waypoint ${waypoint.parcelId || 'unknown'} has moveToEnd flag, will be moved to end`);
        } else {
          normalWaypoints.push(waypoint);
        }
      }
      
      // Reorder: normal waypoints first, then moveToEnd waypoints
      const reorderedWaypoints = [...normalWaypoints, ...moveToEndWaypoints];
      
      if (reorderedWaypoints.length !== request.waypoints.length) {
        logger.warn(`Waypoint count mismatch: original=${request.waypoints.length}, reordered=${reorderedWaypoints.length}`);
      }
      
      if (moveToEndWaypoints.length > 0) {
        logger.info(`Reordered ${moveToEndWaypoints.length} waypoints to end of route due to moveToEnd flag`);
      }

      // Query OSRM using dual-instance router with reordered waypoints
      const osrmResponse = await this.osrmRouter.getRoute(
        reorderedWaypoints,
        {
          steps: request.steps !== false,
          annotations: true,  // Always enable for traffic data
          alternatives: request.alternatives || false,
          overview: 'full',
          geometries: 'geojson',
          vehicle: request.vehicle || 'motorbike',
          mode: request.mode || 'v2-full',
        }
      );

      if (osrmResponse.code !== 'Ok') {
        throw createError(`OSRM error: ${osrmResponse.message || osrmResponse.code}`, 503);
      }

      // Enrich with our custom data (use original request to preserve waypoint order info)
      const enrichedResponse = await this.enrichRouteData(osrmResponse, {
        ...request,
        waypoints: reorderedWaypoints, // Use reordered waypoints
      });

      return enrichedResponse;
    } catch (error) {
      logger.error('Route calculation failed', { error });
      throw createError('Failed to calculate route', 500);
    }
  }

  /**
   * Calculate priority-based multi-stop route with intelligent optimization
   */
  public static async calculatePriorityRoutes(request: RouteRequestDto): Promise<RouteResponseDto> {
    try {
      if (!request.priorities || request.priorities.length !== request.waypoints.length - 1) {
        throw createError('Priorities must be provided for each waypoint (excluding origin)', 400);
      }

      logger.info(`Calculating INTELLIGENT priority-based route (vehicle: ${request.vehicle || 'car'})`);

      // Import priority optimizer
      const { PriorityRouteOptimizer } = await import('./priority-optimizer.js');
      type PriorityWaypoint = import('./priority-optimizer.js').PriorityWaypoint;

      // Prepare waypoints with priorities
      const origin = request.waypoints[0];
      if (!origin) {
        throw createError('Origin waypoint is required', 400);
      }
      const destinations: PriorityWaypoint[] = request.waypoints.slice(1).map((wp, idx) => {
        const priority = request.priorities![idx];
        if (priority === undefined) {
          throw createError(`Priority missing for waypoint at index ${idx + 1}`, 400);
        }
        return {
          lat: wp.lat,
          lon: wp.lon,
          index: idx,
          priority,
        };
      });

      // Optimize waypoint order based on priority and traffic
      logger.info(`Original waypoints: ${destinations.map(d => `[${d.index}:P${d.priority}]`).join(' → ')}`);
      const optimizedDestinations = PriorityRouteOptimizer.optimizeOrder(origin, destinations);
      logger.info(`Optimized waypoints: ${optimizedDestinations.map(d => `[${d.index}:P${d.priority}]`).join(' → ')}`);

      // Build final waypoint list (origin + optimized destinations)
      const optimizedWaypoints = [origin, ...optimizedDestinations].filter(Boolean);

      // Call OSRM with optimized order
      const osrmResponse = await this.osrmRouter.getRoute(optimizedWaypoints as any[], {
        steps: request.steps !== false,
        annotations: true,  // Enable traffic/speed annotations
        vehicle: request.vehicle || 'motorbike',
          mode: request.mode || 'v2-full',
        geometries: 'geojson',
        overview: 'full',
      });

      if (osrmResponse.code !== 'Ok') {
        throw createError(`OSRM error: ${osrmResponse.message || osrmResponse.code}`, 503);
      }

      // Enrich with our custom data
      const enrichedResponse = await this.enrichRouteData(osrmResponse, request);

      // Add visit order information
      enrichedResponse.visitOrder = optimizedDestinations.map(dest => ({
        index: dest.index,
        priority: dest.priority,
        priorityLabel: PriorityRouteOptimizer.getPriorityLabel(dest.priority),
        waypoint: { lat: dest.lat, lon: dest.lon },
      }));

      logger.info(`Priority route calculated: ${enrichedResponse.route.distance}m, ${enrichedResponse.route.duration}s`);

      return enrichedResponse;
    } catch (error) {
      logger.error('Priority route calculation failed', { error });
      if (error instanceof Error && error.message.includes('Priorities must')) {
        throw error;
      }
      throw createError('Failed to calculate priority route', 500);
    }
  }

  /**
   * Get OSRM router status
   */
  public static async getOSRMStatus(): Promise<any> {
    return this.osrmRouter.getStatus();
  }

  /**
   * Switch OSRM instance
   */
  public static async switchOSRMInstance(targetInstance: 1 | 2): Promise<void> {
    await this.osrmRouter.switchInstance(targetInstance);
  }

  /**
   * Calculate distance between two coordinates (Haversine formula) in meters
   */
  private static calculateDistance(
    lat1: number,
    lon1: number,
    lat2: number,
    lon2: number
  ): number {
    const R = 6371000; // Earth radius in meters
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = 
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  /**
   * Check if a point is collinear with two other points (within tolerance)
   * Calculates perpendicular distance from point to line segment p1-p2
   * 
   * @param point Point to check [lon, lat]
   * @param p1 First point of the line segment [lon, lat]
   * @param p2 Second point of the line segment [lon, lat]
   * @param tolerance Distance tolerance in meters (default: 10m)
   * @returns true if point is collinear with p1-p2 segment (within tolerance)
   */
  private static isCollinear(
    point: [number, number], // [lon, lat]
    p1: [number, number],    // [lon, lat]
    p2: [number, number],    // [lon, lat]
    tolerance: number = 10   // meters
  ): boolean {
    const pointLat = point[1];
    const pointLon = point[0];
    const p1Lat = p1[1];
    const p1Lon = p1[0];
    const p2Lat = p2[1];
    const p2Lon = p2[0];

    // Calculate perpendicular distance from point to line p1-p2
    // Using cross product method: distance = |(point - p1) × (p2 - p1)| / |p2 - p1|
    
    // Vector from p1 to p2
    const dx = p2Lon - p1Lon;
    const dy = p2Lat - p1Lat;
    
    // Vector from p1 to point
    const dxPoint = pointLon - p1Lon;
    const dyPoint = pointLat - p1Lat;
    
    // Cross product magnitude (area of parallelogram)
    const crossProduct = Math.abs(dx * dyPoint - dy * dxPoint);
    
    // Length of segment p1-p2
    const segmentLength = Math.sqrt(dx * dx + dy * dy);
    
    if (segmentLength === 0) {
      // p1 and p2 are the same point, check if point is close to them
      return this.calculateDistance(pointLat, pointLon, p1Lat, p1Lon) <= tolerance;
    }
    
    // Perpendicular distance = cross product / segment length
    // Convert to meters: 1 degree ≈ 111km
    const latScale = 111000; // meters per degree latitude
    const lonScale = 111000 * Math.cos((p1Lat + p2Lat) / 2 * Math.PI / 180); // meters per degree longitude (average)
    
    // Approximate perpendicular distance in meters
    const perpendicularDistance = (crossProduct / segmentLength) * Math.sqrt(lonScale * lonScale + latScale * latScale);

    return perpendicularDistance <= tolerance;
  }

  /**
   * Check if direct crossing is possible between two nodes
   * Uses multiple strategies:
   * 1. Try to find nodes by OSM IDs in DB and check for direct segment
   * 2. Fallback: If nodes not found, use coordinate-based distance check
   */
  private static async canCrossDirectly(
    nodeA: number,
    nodeB: number,
    coordA?: [number, number], // [lon, lat] for fallback
    coordB?: [number, number]  // [lon, lat] for fallback
  ): Promise<boolean> {
    try {
      const nodeAStr = nodeA.toString();
      const nodeBStr = nodeB.toString();
      
      logger.info(`🔍 Checking direct crossing: OSM nodes ${nodeA} ↔ ${nodeB}`);
      
      // Strategy 1: Try to find nodes by OSM IDs in DB
      const dbNodes = await prisma.road_nodes.findMany({
        where: {
          osm_id: { in: [nodeAStr, nodeBStr] }
        },
        select: {
          node_id: true,
          osm_id: true,
          lat: true,
          lon: true
        }
      });

      if (dbNodes.length === 2) {
        const dbNodeA = dbNodes.find(n => n.osm_id === nodeAStr);
        const dbNodeB = dbNodes.find(n => n.osm_id === nodeBStr);

        if (dbNodeA && dbNodeB) {
          logger.info(`✅ Found DB nodes: OSM ${nodeA}→DB ${dbNodeA.node_id}, OSM ${nodeB}→DB ${dbNodeB.node_id}`);

          // Check if there's a direct segment A->B (bidirectional check)
          const segment = await prisma.road_segments.findFirst({
            where: {
              OR: [
                { from_node_id: dbNodeA.node_id, to_node_id: dbNodeB.node_id },
                { from_node_id: dbNodeB.node_id, to_node_id: dbNodeA.node_id }
              ]
            },
            select: { segment_id: true }
          });

          if (segment) {
            logger.info(`✅ Direct segment found in DB: ${dbNodeA.node_id} ↔ ${dbNodeB.node_id} (segment_id: ${segment.segment_id})`);
            return true;
          } else {
            logger.info(`❌ No direct segment in DB between ${dbNodeA.node_id} ↔ ${dbNodeB.node_id}`);
          }
        }
      } else {
        logger.info(`⚠️ Found ${dbNodes.length}/2 nodes in DB by OSM IDs (${nodeA}, ${nodeB}), trying fallback method...`);
      }

      // Strategy 2: Fallback - Use coordinate-based distance check
      // If nodes are very close (< 150m), it's likely we can merge (U-turn optimization)
      if (coordA && coordB && coordA.length >= 2 && coordB.length >= 2) {
        const distance = this.calculateDistance(
          coordA[1], // lat
          coordA[0], // lon
          coordB[1], // lat
          coordB[0]  // lon
        );

        // Threshold: 150 meters - if points are this close, U-turn merge is likely safe
        const DISTANCE_THRESHOLD = 150;
        
        if (distance < DISTANCE_THRESHOLD) {
          logger.info(`✅ Fallback: Nodes are very close (${distance.toFixed(1)}m < ${DISTANCE_THRESHOLD}m), allowing merge`);
          return true;
        } else {
          logger.info(`❌ Fallback: Nodes are too far apart (${distance.toFixed(1)}m >= ${DISTANCE_THRESHOLD}m), skipping merge`);
        }
      } else {
        logger.info(`❌ Fallback not available: Missing coordinates for distance check`);
      }

      return false;
    } catch (error) {
      logger.warn(`❌ Failed to check direct crossing: ${error instanceof Error ? error.message : 'Unknown error'}`);
      return false;
    }
  }

  /**
   * Optimize steps by merging U-turn patterns (A->C->B into A->B)
   */
  private static async optimizeUTurnPattern(
    steps: RouteStepDto[],
    osmNodeIds: number[]
  ): Promise<RouteStepDto[]> {
    if (steps.length < 2 || osmNodeIds.length < 3) {
      logger.info(`⚠️ U-turn optimization skipped: steps=${steps.length}, nodes=${osmNodeIds.length} (need steps≥2, nodes≥3)`);
      return steps;
    }

    logger.info(`🔍 Checking ${steps.length} steps for U-turn patterns (${osmNodeIds.length} OSM nodes available)`);
    
    const optimizedSteps: RouteStepDto[] = [];
    let i = 0;

    while (i < steps.length) {
      const currentStep = steps[i];
      if (!currentStep) {
        i++;
        continue;
      }

      // Check if we have at least 2 steps remaining
      if (i < steps.length - 1) {
        const stepA = steps[i];      // A->C
        const stepB = steps[i + 1];  // C->B

        if (!stepA || !stepB) {
          optimizedSteps.push(currentStep);
          i++;
          continue;
        }

        // Pattern detection: A->C->B where we can go A->B directly
        // Node mapping: step i goes from osmNodeIds[i] to osmNodeIds[i+1]
        //               step i+1 goes from osmNodeIds[i+1] to osmNodeIds[i+2]
        // For U-turn optimization, we need nodeB to exist (step i+1 must not be the last step)
        // OR we need to check geometry coordinates if nodeB is missing
        
        // Check if stepB has U-turn
        const stepBHasUTurn = 
          (stepB.maneuver?.type === 'turn' || stepB.maneuver?.type === 'continue' || stepB.maneuver?.type === 'end of road') &&
          (stepB.maneuver?.modifier === 'uturn' || 
           stepB.maneuver?.modifier === 'sharp left' ||
           stepB.maneuver?.modifier === 'sharp right');

        if (stepBHasUTurn) {
          // Get nodes: A (start of stepA), C (junction), B (end of stepB)
          const nodeA = osmNodeIds[i];     // Start of stepA
          const nodeC = osmNodeIds[i + 1]; // Junction (U-turn point)
          
          // NodeB: end of stepB
          // If stepB is not the last step, nodeB = osmNodeIds[i+2]
          // If stepB is the last step, we need to get it from stepB's geometry end point
          let nodeB: number | undefined = osmNodeIds[i + 2];
          
          // If nodeB is missing from annotations, try to get from stepB's end coordinate
          // by finding nearest OSM node in DB (fallback approach)
          if (!nodeB && stepB.geometry?.coordinates && stepB.geometry.coordinates.length > 0) {
            const stepBEndCoord = stepB.geometry.coordinates[stepB.geometry.coordinates.length - 1];
            if (stepBEndCoord && Array.isArray(stepBEndCoord) && stepBEndCoord.length >= 2) {
              // For now, log that we're missing nodeB - we'll need to query nearest node from DB
              logger.info(`⚠️ nodeB missing from annotations for step ${i+1}, end coord: [${stepBEndCoord[0]}, ${stepBEndCoord[1]}]`);
              // TODO: Query nearest OSM node from DB using coordinates
              // For now, skip optimization if nodeB is missing
            }
          }

          if (nodeA && nodeC && nodeB) {
            logger.info(`🔄 U-turn detected at step ${i+1}: stepA(${i}): ${stepA.maneuver?.type}/${stepA.maneuver?.modifier}, stepB(${i+1}): ${stepB.maneuver?.type}/${stepB.maneuver?.modifier}, nodes=[A:${nodeA}, C:${nodeC}, B:${nodeB}]`);
            
            // Get coordinates from step geometry for fallback check
            const stepACoords = stepA.geometry?.coordinates || [];
            const stepBCoords = stepB.geometry?.coordinates || [];
            const coordA = stepACoords.length > 0 && Array.isArray(stepACoords[0]) 
              ? stepACoords[0] as [number, number] 
              : undefined;
            const coordB = stepBCoords.length > 0 && Array.isArray(stepBCoords[stepBCoords.length - 1])
              ? stepBCoords[stepBCoords.length - 1] as [number, number]
              : undefined;
            
            // Check if we can cross directly from A to B (skip the U-turn at C)
            logger.info(`🔍 Checking if we can cross directly: nodeA=${nodeA} (start of step ${i}) → nodeB=${nodeB} (end of step ${i+1})`);
            const canCross = await this.canCrossDirectly(nodeA, nodeB, coordA, coordB);

            if (canCross) {
              logger.info(`✅ U-turn optimization: Merging steps ${i}→${i+1} (nodes ${nodeA}→${nodeC}→${nodeB} ⇒ ${nodeA}→${nodeB})`);

              // Merge stepA and stepB into a single step A->B
              // IMPORTANT: Don't concat coordinates because stepB contains U-turn geometry
              // Instead, create a simplified geometry from start of stepA to end of stepB
              const stepACoords = stepA.geometry?.coordinates || [];
              const stepBCoords = stepB.geometry?.coordinates || [];
              
              // Get start coordinate from stepA and end coordinate from stepB
              const startCoord = stepACoords.length > 0 && Array.isArray(stepACoords[0]) 
                ? stepACoords[0] as [number, number]
                : undefined;
              const endCoord = stepBCoords.length > 0 && Array.isArray(stepBCoords[stepBCoords.length - 1])
                ? stepBCoords[stepBCoords.length - 1] as [number, number]
                : undefined;

              if (!startCoord || !endCoord) {
                logger.warn(`⚠️ Cannot create merged geometry: missing start or end coordinates`);
                // Fallback: keep steps as-is if we can't create proper geometry
                optimizedSteps.push(currentStep);
                i++;
                continue;
              }

              // Create optimized geometry by finding where end of stepB is collinear with stepA
              // Strategy:
              // 1. Start = start of stepA (step n-2)
              // 2. End = end of stepB (step n-1)
              // 3. Find the adjacent pair of points in stepA where end of stepB is collinear
              // 4. Keep all coordinates from stepA up to the insertion point, insert end, remove rest
              
              const optimizedCoords: [number, number][] = [];
              
              // Step 1: Find where end of stepB is collinear with adjacent points in stepA
              // According to user: "end của step n-1 chắc chắn là 1 điểm thẳng hàng với 1 cặp điểm liền kề nào đó của step n-2"
              let insertIndex = -1;
              let bestCollinearDistance = Infinity;
              const COLLINEAR_TOLERANCE = 30; // 30 meters tolerance (increased for better detection)
              
              // First pass: Find the best collinear segment (lowest distance from end to line)
              for (let j = 0; j < stepACoords.length - 1; j++) {
                const p1 = stepACoords[j];
                const p2 = stepACoords[j + 1];
                
                if (!p1 || !p2 || !Array.isArray(p1) || !Array.isArray(p2) || 
                    p1.length < 2 || p2.length < 2) {
                  continue;
                }
                
                const p1Coord: [number, number] = [p1[0], p1[1]];
                const p2Coord: [number, number] = [p2[0], p2[1]];
                
                // Check if end of stepB is collinear with this segment
                const isCollinear = this.isCollinear(endCoord, p1Coord, p2Coord, COLLINEAR_TOLERANCE);
                
                if (isCollinear) {
                  // Calculate perpendicular distance from end to the line segment
                  const p1Lat = p1Coord[1];
                  const p1Lon = p1Coord[0];
                  const p2Lat = p2Coord[1];
                  const p2Lon = p2Coord[0];
                  const endLat = endCoord[1];
                  const endLon = endCoord[0];
                  
                  // Vector from p1 to p2
                  const dx = p2Lon - p1Lon;
                  const dy = p2Lat - p1Lat;
                  
                  // Vector from p1 to end
                  const dxEnd = endLon - p1Lon;
                  const dyEnd = endLat - p1Lat;
                  
                  // Calculate perpendicular distance
                  const crossProduct = Math.abs(dx * dyEnd - dy * dxEnd);
                  const segmentLength = Math.sqrt(dx * dx + dy * dy);
                  const perpendicularDist = segmentLength > 0 ? crossProduct / segmentLength : Infinity;
                  
                  // Check if end is in the same direction as the segment (not behind p1)
                  const dotProduct = dx * dxEnd + dy * dyEnd;
                  
                  if (dotProduct >= 0 && perpendicularDist < bestCollinearDistance) {
                    bestCollinearDistance = perpendicularDist;
                    insertIndex = j + 1; // Insert after p1, before p2
                  }
                }
              }
              
              if (insertIndex >= 0) {
                logger.info(`✅ Found collinear segment at index ${insertIndex - 1}: end of stepB is collinear with stepA[${insertIndex - 1}] and stepA[${insertIndex}] (distance=${bestCollinearDistance.toFixed(2)}m)`);
              }
              
              // Step 2: Build optimized coordinates
              if (insertIndex >= 0) {
                // Found collinear segment: keep all coordinates from stepA up to insertIndex
                // Then replace the rest with end coordinate (removing U-turn section)
                
                // IMPORTANT: If insertIndex is too early (< 3), we might be cutting too much
                // In this case, use a more conservative approach: keep more points from stepA
                const MIN_INSERT_INDEX = Math.min(3, Math.floor(stepACoords.length * 0.5)); // At least 3 points or 50% of stepA
                
                if (insertIndex < MIN_INSERT_INDEX) {
                  logger.warn(`⚠️ Insert index ${insertIndex} is too early (min=${MIN_INSERT_INDEX}), using conservative approach`);
                  // Keep more points: use insertIndex but ensure we keep at least MIN_INSERT_INDEX points
                  const effectiveInsertIndex = Math.max(insertIndex, MIN_INSERT_INDEX);
                  
                  for (let j = 0; j < effectiveInsertIndex; j++) {
                    const coord = stepACoords[j];
                    if (coord && Array.isArray(coord) && coord.length >= 2) {
                      optimizedCoords.push([coord[0], coord[1]]);
                    }
                  }
                } else {
                  // Normal case: use the found insertIndex
                  for (let j = 0; j < insertIndex; j++) {
                    const coord = stepACoords[j];
                    if (coord && Array.isArray(coord) && coord.length >= 2) {
                      optimizedCoords.push([coord[0], coord[1]]);
                    }
                  }
                }
                
                // Check if end is very close to the last point we kept
                // If so, we can skip it and use end directly
                const lastKeptPoint = optimizedCoords[optimizedCoords.length - 1];
                if (lastKeptPoint) {
                  const distanceToLastPoint = this.calculateDistance(
                    endCoord[1], endCoord[0],
                    lastKeptPoint[1], lastKeptPoint[0]
                  );
                  
                  // If end is very close to last point (< 5m), replace last point with end
                  if (distanceToLastPoint < 5) {
                    optimizedCoords.pop(); // Remove last point
                  }
                }
                
                // Add end coordinate (destination after removing U-turn)
                optimizedCoords.push(endCoord);
                
                logger.info(`🔀 Merged geometry: ${optimizedCoords.length} points (from stepA[0..${insertIndex}] + end, removed U-turn section)`);
              } else {
                // Fallback: If no collinear segment found, try to find where end should be inserted
                // Strategy: Find the segment in stepA where end would naturally fit
                // by checking which segment's extension would pass closest to end
                logger.warn(`⚠️ No collinear segment found, trying to find best insertion point`);
                
                let bestInsertIndex = -1;
                let bestScore = Infinity;
                
                // Check each segment in stepA to see which one end is closest to
                for (let j = 0; j < stepACoords.length - 1; j++) {
                  const p1 = stepACoords[j];
                  const p2 = stepACoords[j + 1];
                  
                  if (!p1 || !p2 || !Array.isArray(p1) || !Array.isArray(p2) || 
                      p1.length < 2 || p2.length < 2) {
                    continue;
                  }
                  
                  const p1Coord: [number, number] = [p1[0], p1[1]];
                  const p2Coord: [number, number] = [p2[0], p2[1]];
                  
                  // Calculate distance from end to this segment (perpendicular distance)
                  const p1Lat = p1Coord[1];
                  const p1Lon = p1Coord[0];
                  const p2Lat = p2Coord[1];
                  const p2Lon = p2Coord[0];
                  const endLat = endCoord[1];
                  const endLon = endCoord[0];
                  
                  // Vector from p1 to p2
                  const dx = p2Lon - p1Lon;
                  const dy = p2Lat - p1Lat;
                  const segmentLength = Math.sqrt(dx * dx + dy * dy);
                  
                  if (segmentLength === 0) continue;
                  
                  // Vector from p1 to end
                  const dxEnd = endLon - p1Lon;
                  const dyEnd = endLat - p1Lat;
                  
                  // Calculate perpendicular distance (in degrees, need to convert to meters)
                  const crossProduct = Math.abs(dx * dyEnd - dy * dxEnd);
                  const perpendicularDistDegrees = segmentLength > 0 ? crossProduct / segmentLength : Infinity;
                  
                  // Convert to meters: 1 degree lat ≈ 111km, 1 degree lon ≈ 111km * cos(lat)
                  const latScale = 111000; // meters per degree latitude
                  const avgLat = (p1Lat + p2Lat) / 2;
                  const lonScale = 111000 * Math.cos(avgLat * Math.PI / 180);
                  const perpendicularDistMeters = perpendicularDistDegrees * Math.sqrt(lonScale * lonScale + latScale * latScale);
                  
                  // Check if end is in the forward direction of the segment
                  const dotProduct = dx * dxEnd + dy * dyEnd;
                  const t = dotProduct / (segmentLength * segmentLength); // Parameter along segment (0-1 or beyond)
                  
                  // Prefer segments where:
                  // 1. End is in forward direction (t >= 0)
                  // 2. Perpendicular distance is small (in meters)
                  // 3. End is not too far beyond the segment (t <= 2, allowing some extension)
                  if (t >= 0 && t <= 2) {
                    const score = perpendicularDistMeters * (1 + Math.max(0, t - 1)); // Penalize if beyond segment
                    if (score < bestScore) {
                      bestScore = score;
                      bestInsertIndex = j + 1;
                    }
                  }
                }
                
                if (bestInsertIndex >= 0 && bestScore < 100) {
                  // Found a good insertion point: keep coordinates up to insertion point, then add end
                  for (let j = 0; j < bestInsertIndex; j++) {
                    const coord = stepACoords[j];
                    if (coord && Array.isArray(coord) && coord.length >= 2) {
                      optimizedCoords.push([coord[0], coord[1]]);
                    }
                  }
                  
                  // Optionally keep the point at bestInsertIndex if end is not too close
                  const pointAtInsert = stepACoords[bestInsertIndex];
                  if (pointAtInsert && Array.isArray(pointAtInsert) && pointAtInsert.length >= 2) {
                    const distToPoint = this.calculateDistance(
                      endCoord[1], endCoord[0],
                      pointAtInsert[1], pointAtInsert[0]
                    );
                    // If end is more than 10m away from the point at insert index, keep that point too
                    if (distToPoint > 10) {
                      optimizedCoords.push([pointAtInsert[0], pointAtInsert[1]]);
                    }
                  }
                  
                  optimizedCoords.push(endCoord);
                  logger.info(`🔀 Merged geometry (best fit): ${optimizedCoords.length} points (inserted at index ${bestInsertIndex}, score=${bestScore.toFixed(2)}m)`);
                } else {
                  // Last resort: keep all stepA coordinates (except last point) and add end
                  // This ensures we have a complete geometry path that follows stepA's route
                  if (stepACoords.length > 0) {
                    // Keep all coordinates except the last one (which is the junction where U-turn happens)
                    for (let j = 0; j < stepACoords.length - 1; j++) {
                      const coord = stepACoords[j];
                      if (coord && Array.isArray(coord) && coord.length >= 2) {
                        optimizedCoords.push([coord[0], coord[1]]);
                      }
                    }
                  }
                  optimizedCoords.push(endCoord);
                  logger.info(`🔀 Merged geometry (fallback - all stepA except last): ${optimizedCoords.length} points (kept ${stepACoords.length - 1} from stepA + end)`);
                }
              }
              
              // Ensure we have at least 2 points (start and end)
              if (optimizedCoords.length < 2) {
                optimizedCoords.length = 0;
                optimizedCoords.push(startCoord, endCoord);
                logger.warn(`⚠️ Geometry too short, using only start and end points`);
              }

              const mergedStep: RouteStepDto = {
                distance: stepA.distance + stepB.distance,
                duration: stepA.duration + stepB.duration,
                instruction: `Continue on ${stepA.name || stepB.name} (U-turn optimized)`,
                name: stepA.name || stepB.name,
                maneuver: {
                  type: 'turn',
                  modifier: 'straight', // Direct crossing
                  location: stepA.maneuver?.location || [0, 0],
                },
                geometry: {
                  type: 'LineString',
                  coordinates: optimizedCoords
                },
                addresses: [...(stepA.addresses || []), ...(stepB.addresses || [])],
                trafficLevel: stepA.trafficLevel || stepB.trafficLevel || 'NORMAL',
              };

              logger.info(`🔀 Merged geometry: ${optimizedCoords.length} points (from ${stepACoords.length + stepBCoords.length} original points)`);

              optimizedSteps.push(mergedStep);
              i += 2; // Skip both steps
              continue;
            } else {
              logger.info(`❌ Cannot optimize: No direct segment found between nodes ${nodeA}↔${nodeB} in database (U-turn at step ${i+1} will remain)`);
            }
          }
        }
      }

      // No optimization possible, keep step as-is
      optimizedSteps.push(currentStep);
      i++;
    }

    const savedSteps = steps.length - optimizedSteps.length;
    if (savedSteps > 0) {
      logger.info(`✅ Optimized ${savedSteps} U-turn step(s)`);
    }

    return optimizedSteps;
  }

  /**
   * Get road names from database for OSM node sequence (lightweight query)
   */
  private static async getRoadNamesFromDB(
    osmNodeIds: number[]
  ): Promise<Map<string, string>> {
    if (osmNodeIds.length < 2) return new Map();

    try {
      const nodeIdStrings = osmNodeIds.map(id => id.toString());
      
      // Load nodes to map OSM IDs to DB node_ids
      const dbNodes = await prisma.road_nodes.findMany({
        where: {
          osm_id: { in: nodeIdStrings }
        },
        select: {
          node_id: true,
          osm_id: true
        }
      });

      const osmIdToDbId = new Map<string, string>();
      for (const node of dbNodes) {
        if (node.osm_id) {
          osmIdToDbId.set(node.osm_id, node.node_id);
        }
      }

      if (osmIdToDbId.size === 0) return new Map();

      // Get DB node IDs
      const dbNodeIds = Array.from(osmIdToDbId.values());
      
      // Query segments with only name field (lightweight)
      const segments = await prisma.road_segments.findMany({
        where: {
          from_node_id: { in: dbNodeIds },
          to_node_id: { in: dbNodeIds }
        },
        select: {
          name: true,
          from_node: {
            select: { osm_id: true }
          },
          to_node: {
            select: { osm_id: true }
          }
        }
      });

      // Build name map
      const nameMap = new Map<string, string>();
      for (const segment of segments) {
        const fromOsmId = segment.from_node.osm_id;
        const toOsmId = segment.to_node.osm_id;
        
        if (fromOsmId && toOsmId && segment.name) {
          const key = `${fromOsmId}-${toOsmId}`;
          nameMap.set(key, segment.name);
        }
      }

      logger.info(`Loaded names for ${nameMap.size} road segments from DB`);
      return nameMap;
    } catch (error) {
      logger.error('Failed to load road names from database', { error });
      return new Map();
    }
  }

  /**
   * Get detailed geometry and traffic data from database for OSM node sequence
   * @deprecated Currently unused - OSRM provides detailed geometry directly
   * Reserved for future DB enrichment enhancements
   */
  public static async _getSegmentGeometriesFromDB(
    osmNodeIds: number[]
  ): Promise<Map<string, any>> {
    if (osmNodeIds.length < 2) return new Map();

    try {
      const nodeIdStrings = osmNodeIds.map(id => id.toString());
      
      // Load all nodes first to get their node_ids from DB
      const dbNodes = await prisma.road_nodes.findMany({
        where: {
          osm_id: { in: nodeIdStrings }
        },
        select: {
          node_id: true,
          osm_id: true
        }
      });

      // Create mapping: osm_id (number) → node_id (uuid)
      const osmIdToDbId = new Map<string, string>();
      for (const node of dbNodes) {
        if (node.osm_id) {
          osmIdToDbId.set(node.osm_id, node.node_id);
        }
      }

      logger.info(`Matched ${osmIdToDbId.size}/${osmNodeIds.length} OSM nodes to DB nodes`);

      if (osmIdToDbId.size === 0) {
        logger.warn('No OSM nodes matched to DB nodes - routing may use simplified geometry');
        return new Map();
      }

      // Build list of DB node IDs to query segments
      const dbNodeIds = Array.from(osmIdToDbId.values());
      
      // Query ALL segments connecting these nodes (efficient single query)
      const allSegments = await prisma.road_segments.findMany({
        where: {
          from_node_id: { in: dbNodeIds },
          to_node_id: { in: dbNodeIds }
        },
        include: {
          from_node: {
            select: { osm_id: true }
          },
          to_node: {
            select: { osm_id: true }
          },
          traffic_conditions: {
            where: {
              expires_at: { gte: new Date() }
            },
            orderBy: {
              source_timestamp: 'desc'
            },
            take: 1
          }
        }
      });

      logger.info(`Loaded ${allSegments.length} potential segments from DB`);

      // Create reverse mapping: DB node_id → OSM ID
      const dbIdToOsmId = new Map<string, string>();
      for (const node of dbNodes) {
        if (node.osm_id) {
          dbIdToOsmId.set(node.node_id, node.osm_id);
        }
      }

      // Build map of consecutive node pairs from OSRM route
      const routePairMap = new Map<string, string>(); // "fromOsmId" → "toOsmId"
      for (let i = 0; i < osmNodeIds.length - 1; i++) {
        const fromOsmId = osmNodeIds[i];
        const toOsmId = osmNodeIds[i + 1];
        if (fromOsmId !== undefined && toOsmId !== undefined) {
          routePairMap.set(fromOsmId.toString(), toOsmId.toString());
        }
      }

      // Filter segments to only those on the route (consecutive pairs)
      const segmentDataMap = new Map<string, any>();
      
      for (const segment of allSegments) {
        const fromOsmId = segment.from_node.osm_id;
        const toOsmId = segment.to_node.osm_id;
        
        if (!fromOsmId || !toOsmId) continue;

        // Check if this segment is on the route (consecutive node pair)
        const expectedToOsmId = routePairMap.get(fromOsmId);
        if (expectedToOsmId !== toOsmId) {
          continue; // Not on the route
        }

        const key = `${fromOsmId}-${toOsmId}`;
        const trafficCondition = segment.traffic_conditions[0];
        
        segmentDataMap.set(key, {
          geometry: segment.geometry,
          segmentId: segment.segment_id,
          name: segment.name,
          roadType: segment.road_type,
          maxSpeed: segment.max_speed,
          avgSpeed: segment.avg_speed,
          baseWeight: segment.base_weight,
          currentWeight: segment.current_weight,
          deltaWeight: segment.delta_weight,
          traffic: trafficCondition ? {
            level: trafficCondition.traffic_level,
            currentSpeed: trafficCondition.current_speed,
            congestionScore: trafficCondition.congestion_score,
            weightMultiplier: trafficCondition.weight_multiplier,
            recordedAt: trafficCondition.source_timestamp,
          } : null
        });
      }

      logger.info(`Matched ${segmentDataMap.size}/${osmNodeIds.length - 1} consecutive route segments from DB`);
      return segmentDataMap;
    } catch (error) {
      logger.error('Failed to load segment data from database', { error });
      return new Map();
    }
  }

  /**
   * Enrich route data with traffic, addresses, etc.
   */
  private static async enrichRouteData(
    osrmResponse: any,
    _request: RouteRequestDto
  ): Promise<RouteResponseDto> {
    logger.info('Enriching route data');

    const enrichedRoutes: RouteDto[] = [];

    for (const route of osrmResponse.routes || []) {
      const enrichedLegs: RouteLegDto[] = [];

            for (const leg of route.legs || []) {
        const enrichedSteps: RouteStepDto[] = [];

        // Get OSM node IDs from leg annotation to enrich with road names       
        const osmNodeIds = leg.annotation?.nodes || [];

        // Query road names from DB based on node sequence
        const roadNamesMap = await this.getRoadNamesFromDB(osmNodeIds);

        for (let stepIdx = 0; stepIdx < leg.steps.length; stepIdx++) {
          const step = leg.steps[stepIdx];

          // Get road name from DB using node pair
          let stepName = 'Unknown road';
          if (stepIdx < osmNodeIds.length - 1) {
            const fromNodeId = osmNodeIds[stepIdx];
            const toNodeId = osmNodeIds[stepIdx + 1];
            const key = `${fromNodeId}-${toNodeId}`;
            const roadName = roadNamesMap.get(key);
            if (roadName) {
              stepName = roadName;
            }
          }
          
          enrichedSteps.push({
            distance: step.distance,
            duration: step.duration,
            instruction: this.generateInstruction(step),
            name: stepName,
            maneuver: {
              type: step.maneuver.type,
              modifier: step.maneuver.modifier,
              location: step.maneuver.location,
            },
            // Use OSRM geometry directly (detailed from our export)
            geometry: step.geometry,
            addresses: [],
            trafficLevel: 'NORMAL', // TODO: Add traffic enrichment later
          });
        }

        // Post-process: Optimize U-turn patterns (A->C->B into A->B)
        logger.debug(`📍 Leg has ${enrichedSteps.length} steps, ${osmNodeIds.length} OSM nodes`);
        const optimizedSteps = await this.optimizeUTurnPattern(enrichedSteps, osmNodeIds);

        // Recalculate leg distance and duration after optimization
        const optimizedDistance = optimizedSteps.reduce((sum, s) => sum + s.distance, 0);
        const optimizedDuration = optimizedSteps.reduce((sum, s) => sum + s.duration, 0);

        enrichedLegs.push({
          distance: optimizedDistance,
          duration: optimizedDuration,
          steps: optimizedSteps,
          // parcelId will be mapped after enrichment in calculateDemoRoute
        });
      }

      // Calculate traffic summary
      const trafficSummary = this.calculateTrafficSummary(route);

      // Build a dense geometry for the whole route by concatenating step geometries when available
      const stepCoordinates: Array<[number, number]> = [];
      for (const l of enrichedLegs) {
        // CRITICAL: Use enriched steps with DB geometry, not OSRM's simplified steps!
        for (const s of l.steps) {
          if (s.geometry?.coordinates && Array.isArray(s.geometry.coordinates)) {
            stepCoordinates.push(...(s.geometry.coordinates as Array<[number, number]>));
          }
        }
      }

      const routeCoordinates: Array<[number, number]> = stepCoordinates.length > 0
        ? stepCoordinates
        : (route.geometry?.coordinates || []);

      const mergedGeometry = {
        type: 'LineString',
        coordinates: routeCoordinates,
      } as const;

      // Recalculate total route distance and duration (in case legs were optimized)
      const totalDistance = enrichedLegs.reduce((sum, leg) => sum + leg.distance, 0);
      const totalDuration = enrichedLegs.reduce((sum, leg) => sum + leg.duration, 0);

      enrichedRoutes.push({
        distance: totalDistance,
        duration: totalDuration,
        geometry: JSON.stringify(mergedGeometry),
        legs: enrichedLegs,
        trafficSummary,
      });
    }

    return {
      code: osrmResponse.code,
      route: enrichedRoutes[0],
    } as any;
  }

  /**
   * Find road segments along a path
   * @deprecated Currently unused
   * Reserved for future spatial query enhancements
   */
  public static async _findSegmentsAlongPath(_coordinates: Array<[number, number]>): Promise<string[]> {
    // Simplified version
    // TODO: Use PostGIS spatial queries for production
    return [];
  }

  /**
   * Get traffic info for segments
   * @deprecated Currently unused - will be re-enabled when DB enrichment is ready
   */
  public static async _getTrafficInfo(segmentIds: string[]): Promise<{ level: string; avgSpeed: number }> {
    if (segmentIds.length === 0) {
      return { level: 'NORMAL', avgSpeed: 30 };
    }

    try {
      const trafficConditions = await prisma.traffic_conditions.findMany({
        where: {
          segment_id: { in: segmentIds },
          expires_at: { gte: new Date() },
        },
      });

      if (trafficConditions.length === 0) {
        return { level: 'NORMAL', avgSpeed: 30 };
      }

      const avgSpeed = trafficConditions.reduce((sum, tc) => sum + (tc.current_speed || 30), 0) / trafficConditions.length;

      const levels = ['FREE_FLOW', 'NORMAL', 'SLOW', 'CONGESTED', 'BLOCKED'];
      const worstLevel = trafficConditions
        .map(tc => levels.indexOf(tc.traffic_level))
        .reduce((max, level) => Math.max(max, level), 0);

      return {
        level: levels[worstLevel] || 'NORMAL',
        avgSpeed,
      };
    } catch (error) {
      logger.error('Failed to get traffic info', { error });
      return { level: 'NORMAL', avgSpeed: 30 };
    }
  }

  /**
   * Get nearby addresses along a path
   * @deprecated Currently unused - reserved for future address geocoding
   */
  public static async _getNearbyAddresses(_coordinates: Array<[number, number]>): Promise<any[]> {
    // TODO: Implement spatial queries
    return [];
  }

  /**
   * Calculate traffic summary for route
   */
  private static calculateTrafficSummary(route: any): any {
    const totalDistance = route.distance;
    const baseDuration = route.duration;

    const normalDuration = totalDistance / (30 / 3.6);
    const estimatedDelay = Math.max(0, baseDuration - normalDuration);
    const avgSpeed = totalDistance / baseDuration * 3.6;

    let congestionLevel = 'NORMAL';
    if (avgSpeed > 40) congestionLevel = 'FREE_FLOW';
    else if (avgSpeed < 30) congestionLevel = 'SLOW';
    else if (avgSpeed < 20) congestionLevel = 'CONGESTED';
    else if (avgSpeed < 10) congestionLevel = 'BLOCKED';

    return {
      averageSpeed: avgSpeed,
      congestionLevel,
      estimatedDelay: Math.round(estimatedDelay),
    };
  }

  /**
   * Generate instruction from maneuver
   */
  private static generateInstruction(step: any): string {
    const { type, modifier } = step.maneuver || {};
    const name = step.name || 'the road';

    const instructions: Record<string, string> = {
      'turn-right': `Turn right onto ${name}`,
      'turn-left': `Turn left onto ${name}`,
      'turn-slight-right': `Turn slight right onto ${name}`,
      'turn-slight-left': `Turn slight left onto ${name}`,
      'depart': `Head ${modifier || 'straight'} on ${name}`,
      'arrive': `Arrive at your destination`,
      'continue': `Continue on ${name}`,
    };

    const key = modifier ? `${type}-${modifier}` : type;
    return instructions[key || 'continue'] || `Continue on ${name}`;
  }

    /**
   * Get OSRM table (distance/duration matrix) using appropriate vehicle/mode
   */
  private static async getOSRMTableForDemoRoute(
    points: Array<{ lat: number; lon: number }>,
    vehicle: 'car' | 'motorbike' = 'motorbike',
    mode: 'v2-full' | 'v2-rating-only' | 'v2-blocking-only' | 'v2-base' = 'v2-full'
  ): Promise<{ durations: number[][]; distances: number[][] }> {
    const vehicleType = vehicle === 'motorbike' ? 'motorbike' : 'driving';
    const coordinates = points.map(p => `${p.lon},${p.lat}`).join(';');
    
    // V2 Models (Simplified Architecture) - works for both motorbike and car
    const modeUrls: Record<string, string> = {
      'v2-full': process.env.OSRM_V2_FULL_URL || 'http://localhost:25920',
      'v2-rating-only': process.env.OSRM_V2_RATING_URL || 'http://localhost:25921',
      'v2-blocking-only': process.env.OSRM_V2_BLOCKING_URL || 'http://localhost:25922',
      'v2-base': process.env.OSRM_V2_BASE_URL || 'http://localhost:25923',
    };
    const baseUrl = modeUrls[mode] || modeUrls['v2-full'] || 'http://localhost:25920';

    const url = `${baseUrl}/table/v1/${vehicleType}/${coordinates}?annotations=duration,distance`;
    
    try {
      const axios = await import('axios');
      const response = await axios.default.get(url);

      if (response.data.code !== 'Ok') {
        throw new Error(`OSRM table API error: ${response.data.code}`);
      }

      return {
        durations: response.data.durations || [],
        distances: response.data.distances || [],
      };
    } catch (error: any) {
      logger.error(`Failed to fetch OSRM table from ${baseUrl}:`, error.message);
      throw new Error(`Failed to fetch OSRM table: ${error.message}`);
    }
  }

  /**
   * Calculate demo route with priority-based waypoint ordering
   * Input: startPoint + priorityGroups (express, fast, normal, economy)        
   * Output: optimized route visiting higher priority points first
   * 
   * Uses OSRM table API for accurate distance/duration calculations
   * Supports 5 routing modes with intelligent waypoint ordering
   */
  public static async calculateDemoRoute(request: DemoRouteRequestDto): Promise<DemoRouteResponseDto> {                                                         
    try {
      logger.info(`Calculating demo route with priority-based ordering (mode: ${request.mode || 'v2-full'})`);       

      // Priority labels mapping (1-10 scale: higher = more urgent)
      // 10: URGENT (khẩn cấp - giao ngay)
      // 7-9: EXPRESS (nhanh - ưu tiên cao)
      // 4-6: NORMAL (bình thường)
      // 2-3: ECONOMY (tiết kiệm)
      // 1: LOW (thấp nhất)
      const priorityLabels: Record<number, string> = {
        10: 'urgent',
        9: 'express-high',
        8: 'express',
        7: 'express-standard',
        6: 'normal-high',
        5: 'normal',
        4: 'normal-standard',
        3: 'economy-high',
        2: 'economy',
        1: 'low',
      };

      // Legacy priority mapping (0-4) → new scale (1-10) for backward compatibility
      const legacyToNewPriority: Record<number, number> = {
        0: 10,  // urgent → 10
        1: 8,   // express → 8
        2: 6,   // fast → 6
        3: 4,   // normal → 4
        4: 2,   // economy → 2
      };

      // Flatten waypoints with priority (convert legacy 0-4 scale to new 1-10 scale if needed)
      const allWaypoints: Array<{ 
        waypoint: WaypointDto; 
        priority: number; 
        originalPriority: number;
        index: number 
      }> = [];                                                               
      let waypointIndex = 0;

      for (const group of request.priorityGroups) {
        for (const waypoint of group.waypoints) {
          const originalPriority = group.priority;
          // Convert legacy priority (0-4) to new scale (1-10) if in old range
          const normalizedPriority = originalPriority <= 4 
            ? (legacyToNewPriority[originalPriority] || originalPriority)
            : originalPriority;
          
          allWaypoints.push({
            waypoint,
            priority: normalizedPriority,
            originalPriority: originalPriority,
            index: waypointIndex++,
          });
        }
      }

      if (allWaypoints.length === 0) {
        throw createError('No waypoints provided', 400);
      }

      // Get OSRM distance/duration matrix for intelligent ordering
      const vehicle = request.vehicle || 'motorbike';
      // V2 Models only: 'v2-full' | 'v2-rating-only' | 'v2-blocking-only' | 'v2-base'
      const mode = request.mode || 'v2-full';
      
      const allPoints = [
        { lat: request.startPoint.lat, lon: request.startPoint.lon }, // Start point (index 0)
        ...allWaypoints.map(w => ({ lat: w.waypoint.lat, lon: w.waypoint.lon })),
      ];

      logger.info(`Fetching OSRM table for distance/duration matrix (mode: ${mode})...`);
      const matrix = await this.getOSRMTableForDemoRoute(
        allPoints, 
        vehicle, 
        mode as 'v2-full' | 'v2-rating-only' | 'v2-blocking-only' | 'v2-base'
      );

      // Helper function to calculate priority weight
      const calculatePriorityWeight = (priority: number): number => {
        // Exponential scaling: e^((priority-5)/3)
        // Priority 10: 4.48x boost, Priority 1: 0.26x penalty
        return Math.exp((priority - 5) / 3);
      };

      // Helper function to calculate effective cost
      const calculateEffectiveCost = (actualDuration: number, priority: number): number => {
        const weight = calculatePriorityWeight(priority);
        return actualDuration / weight;
      };

      // Create a map of waypoint to matrix index for fast lookup
      const waypointToMatrixIdx = new Map<typeof allWaypoints[0], number>();
      allWaypoints.forEach((w, idx) => {
        waypointToMatrixIdx.set(w, idx + 1); // +1 because index 0 is start point
      });

      // Handle strategy: strict_urgent vs flexible
      const strategy = request.strategy || 'strict_urgent';
      let orderedWaypoints: Array<{ waypoint: WaypointDto; priority: number; originalPriority: number; index: number }> = [];

      if (strategy === 'strict_urgent') {
        // Step 1: Separate URGENT (P≥9) from others
        const urgentWaypoints = allWaypoints.filter(w => w.priority >= 9);     
        const otherWaypoints = allWaypoints.filter(w => w.priority < 9);
        
        logger.info(`Strategy: STRICT_URGENT - ${urgentWaypoints.length} urgent waypoints (P≥9) will be visited first`);

        // Step 2: Order URGENT using Nearest Neighbor (by priority first, then distance)
        const orderedUrgent: typeof allWaypoints = [];
        const remainingUrgent = [...urgentWaypoints];
        let currentPos = 0; // Start from index 0 (start point)

        while (remainingUrgent.length > 0) {
          // Find nearest urgent waypoint (by priority, then by actual distance)
          let bestIdx = 0;
          let bestScore = Infinity;

          for (let i = 0; i < remainingUrgent.length; i++) {
            const waypoint = remainingUrgent[i];
            if (!waypoint) continue;
            
            const waypointIdx = waypointToMatrixIdx.get(waypoint) ?? 0;
            const duration = matrix.durations[currentPos]?.[waypointIdx] ?? Infinity;
            
            // Score = (-priority * 1000) + duration (prioritize higher priority, then closer)
            const score = (-waypoint.priority * 1000) + duration;
            
            if (score < bestScore) {
              bestScore = score;
              bestIdx = i;
            }
          }

          const selected = remainingUrgent.splice(bestIdx, 1)[0];
          if (selected) {
            orderedUrgent.push(selected);
            currentPos = waypointToMatrixIdx.get(selected) ?? 0; // Update current position
          }
        }

        // Step 3: Order OTHER waypoints using Nearest Neighbor with priority weighting
        const orderedOthers: typeof allWaypoints = [];
        const remainingOthers = [...otherWaypoints];
        // currentPos is now at last urgent waypoint (or start if no urgent)

        while (remainingOthers.length > 0) {
          let bestIdx = 0;
          let bestEffectiveCost = Infinity;

          for (let i = 0; i < remainingOthers.length; i++) {
            const waypoint = remainingOthers[i];
            if (!waypoint) continue;
            
            const waypointIdx = waypointToMatrixIdx.get(waypoint) ?? 0;
            const duration = matrix.durations[currentPos]?.[waypointIdx] ?? Infinity;
            const effectiveCost = calculateEffectiveCost(duration, waypoint.priority);
            
            if (effectiveCost < bestEffectiveCost) {
              bestEffectiveCost = effectiveCost;
              bestIdx = i;
            }
          }

          const selected = remainingOthers.splice(bestIdx, 1)[0];
          if (selected) {
            orderedOthers.push(selected);
            currentPos = waypointToMatrixIdx.get(selected) ?? 0;
          }
        }

        orderedWaypoints = [...orderedUrgent, ...orderedOthers];
        
      } else {
        // Flexible: Nearest Neighbor with priority weighting for ALL waypoints
        logger.info(`Strategy: FLEXIBLE - using Nearest Neighbor with priority weighting`);
        
        const remaining = [...allWaypoints];
        let currentPos = 0; // Start point

        while (remaining.length > 0) {
          let bestIdx = 0;
          let bestEffectiveCost = Infinity;

          for (let i = 0; i < remaining.length; i++) {
            const waypoint = remaining[i];
            if (!waypoint) continue;
            
            const waypointIdx = waypointToMatrixIdx.get(waypoint) ?? 0;
            const duration = matrix.durations[currentPos]?.[waypointIdx] ?? Infinity;
            const effectiveCost = calculateEffectiveCost(duration, waypoint.priority);
            
            if (effectiveCost < bestEffectiveCost) {
              bestEffectiveCost = effectiveCost;
              bestIdx = i;
            }
          }

          const selected = remaining.splice(bestIdx, 1)[0];
          if (selected) {
            orderedWaypoints.push(selected);
            currentPos = waypointToMatrixIdx.get(selected) ?? 0;
          }
        }
      }

      // Log the selection process for debugging
      logger.info(`Nearest Neighbor ordering complete: ${orderedWaypoints.map(w => 
        `P${w.priority}(${w.waypoint.parcelId || 'N/A'})`
      ).join(' → ')}`);


            // Note: Routing algorithm (priority vs speed) is handled by OSRM Lua profiles
      // Application layer only sorts waypoints by priority; OSRM chooses optimal paths
      
      // Build ordered route: start point + sorted waypoints
      const orderedWaypointDtos = [
        request.startPoint,
        ...orderedWaypoints.map((w) => w.waypoint),
      ];

      // Log the final ordered sequence for debugging
      logger.info(`Final waypoint order for OSRM (will NOT be reordered):`);
      logger.info(`  Start: [${request.startPoint.lat.toFixed(6)}, ${request.startPoint.lon.toFixed(6)}]`);
      orderedWaypoints.forEach((w, idx) => {
        logger.info(`  ${idx + 1}. P${w.priority} [${w.waypoint.lat.toFixed(6)}, ${w.waypoint.lon.toFixed(6)}] ${w.waypoint.parcelId || ''}`);
      });

      // Calculate route using OSRM
      // IMPORTANT: OSRM /route API respects waypoint order (does NOT reorder)
      // Only /trip API would reorder waypoints for TSP optimization
      const osrmResponse = await this.osrmRouter.getRoute(orderedWaypointDtos, {
        steps: request.steps !== false,
        annotations: request.annotations !== false,
        overview: 'full',
        geometries: 'geojson',
        vehicle: request.vehicle || 'motorbike',
          mode: request.mode || 'v2-full',
        continue_straight: true, // Explicitly set to ensure no waypoint skipping
      });

      if (osrmResponse.code !== 'Ok' || !osrmResponse.routes || osrmResponse.routes.length === 0) {
        throw createError(`OSRM error: ${osrmResponse.message || osrmResponse.code}`, 503);
      }

      // Verify OSRM respected our waypoint order
      const osrmRoute = osrmResponse.routes[0];
      const expectedLegs = orderedWaypointDtos.length - 1; // n waypoints = n-1 legs
      const actualLegs = osrmRoute?.legs?.length || 0;
      
      if (actualLegs !== expectedLegs) {
        logger.warn(`⚠️ OSRM leg count mismatch! Expected ${expectedLegs} legs, got ${actualLegs}`);
        logger.warn(`This might indicate OSRM reordered waypoints or skipped some.`);
      } else {
        logger.info(`✅ OSRM route has ${actualLegs} legs as expected (waypoints were NOT reordered)`);
      }

            // Enrich the first route with parcelId mapping
      const enrichedResponse = await this.enrichRouteData(osrmResponse, {       
        waypoints: orderedWaypointDtos,
        steps: request.steps,
        annotations: request.annotations,
      } as RouteRequestDto);

            // Map parcelId to route legs (legs correspond to ordered waypoints)
      // OSRM returns legs where: leg[0] = start->waypoint[0], leg[1] = waypoint[0]->waypoint[1], etc.
      // Each leg should have the parcelId of its destination waypoint
      if (enrichedResponse.route && enrichedResponse.route.legs && orderedWaypoints.length > 0) {
        logger.info(`Mapping parcelId to ${enrichedResponse.route.legs.length} legs from ${orderedWaypoints.length} waypoints`);
        for (let i = 0; i < orderedWaypoints.length && i < enrichedResponse.route.legs.length; i++) {
          const waypoint = orderedWaypoints[i];
          const leg = enrichedResponse.route.legs[i];
          if (leg && waypoint && waypoint.waypoint.parcelId) {
            leg.parcelId = waypoint.waypoint.parcelId;
            logger.debug(`Mapped parcelId ${waypoint.waypoint.parcelId} to leg[${i}]`);
          }
        }
      }

      const route = (enrichedResponse as any).route || (enrichedResponse as any).routes?.[0];
      if (!route) {
        throw createError('No route found', 500);
      }

      // Validate that the route has valid distance and duration
      if (route.distance === 0 || route.duration === 0) {
        throw createError('OSRM returned an invalid route with zero distance/duration. The OSRM instance may not have road data for this area.', 503);
      }

      // Ensure trafficSummary exists (it should be added by enrichRouteData)
      if (!route.trafficSummary) {
        // Fallback if trafficSummary is missing
        route.trafficSummary = {
          lowTraffic: 0,
          mediumTraffic: 0,
          highTraffic: 0,
          totalSegments: 0,
          averageSpeed: 0,
        };
      }

      // Build visit order information with cumulative distances
      const visitOrder = orderedWaypoints.map((w) => {
        // Calculate distance from start point for reference
        const matrixIdx = waypointToMatrixIdx.get(w) ?? 0;
        const durationFromStart = matrix.durations[0]?.[matrixIdx] ?? 0;
        const distanceFromStart = matrix.distances[0]?.[matrixIdx] ?? 0;
        const priorityWeight = calculatePriorityWeight(w.priority);
        const effectiveCost = calculateEffectiveCost(durationFromStart, w.priority);
        
        return {
          index: w.index,
          priority: w.priority,
          originalPriority: w.originalPriority,
          priorityLabel: priorityLabels[w.priority] || `priority-${w.priority}`,
          waypoint: w.waypoint, // Preserve parcelId from original waypoint
          durationFromStart: durationFromStart, // Direct distance from start (for reference)
          distanceFromStart: distanceFromStart,
          effectiveCost: effectiveCost, // Priority-weighted cost
          priorityWeight: priorityWeight,
        };
      });

      // Calculate priority counts
      const priorityCounts: Record<string, number> = {};
      for (const group of request.priorityGroups) {
        const label = priorityLabels[group.priority] || 'unknown';
        priorityCounts[label] = group.waypoints.length;
      }

      // Build summary
      const summary = {
        totalDistance: route.distance,
        totalDuration: route.duration,
        totalWaypoints: orderedWaypoints.length, // Only count visited waypoints
        priorityCounts,
      };

      return {
        code: 'Ok',
        route,
        visitOrder,
        summary,
      };
    } catch (error) {
      // Avoid logging circular structures (e.g., Axios errors contain sockets)
      const safeMessage = error instanceof Error ? error.message : 'Unknown error';
      const safeStack = error instanceof Error ? error.stack : undefined;
      logger.error('Demo route calculation failed', { message: safeMessage, stack: safeStack });
      if (error instanceof Error && error.message.includes('OSRM error')) {
        throw error;
      }
      throw createError(safeMessage || 'Failed to calculate demo route', 500);
    }
  }
}
