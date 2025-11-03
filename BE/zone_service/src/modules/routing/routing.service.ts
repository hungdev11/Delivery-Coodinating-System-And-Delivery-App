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
   */
  public static async calculateRoute(request: RouteRequestDto): Promise<RouteResponseDto> {
    try {
      logger.info(`Calculating route with ${request.waypoints.length} waypoints (vehicle: ${request.vehicle || 'car'})`);

      // Query OSRM using dual-instance router
      const osrmResponse = await this.osrmRouter.getRoute(
        request.waypoints,
        {
          steps: request.steps !== false,
          annotations: true,  // Always enable for traffic data
          alternatives: request.alternatives || false,
          overview: 'full',
          geometries: 'geojson',
          vehicle: request.vehicle || 'car',
        }
      );

      if (osrmResponse.code !== 'Ok') {
        throw createError(`OSRM error: ${osrmResponse.message || osrmResponse.code}`, 503);
      }

      // Enrich with our custom data
      const enrichedResponse = await this.enrichRouteData(osrmResponse, request);

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
        mode: request.mode || 'balanced',
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

        enrichedLegs.push({
          distance: leg.distance,
          duration: leg.duration,
          steps: enrichedSteps,
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

      enrichedRoutes.push({
        distance: route.distance,
        duration: route.duration,
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
   * Calculate demo route with priority-based waypoint ordering
   * Input: startPoint + priorityGroups (express, fast, normal, economy)
   * Output: optimized route visiting higher priority points first
   */
  public static async calculateDemoRoute(request: DemoRouteRequestDto): Promise<DemoRouteResponseDto> {
    try {
      logger.info('Calculating demo route with priority-based ordering');

      // Priority labels mapping (0=urgent, 1=express, 2=fast, 3=normal, 4=economy)
      const priorityLabels: Record<number, string> = {
        0: 'urgent',
        1: 'express',
        2: 'fast',
        3: 'normal',
        4: 'economy',
      };

      // Flatten and sort waypoints by priority
      const allWaypoints: Array<{ waypoint: WaypointDto; priority: number; index: number }> = [];
      let waypointIndex = 0;

      for (const group of request.priorityGroups) {
        for (const waypoint of group.waypoints) {
          allWaypoints.push({
            waypoint,
            priority: group.priority,
            index: waypointIndex++,
          });
        }
      }

      // Handle strategy: strict_urgent vs flexible
      const strategy = request.strategy || 'strict_urgent';
      
      if (strategy === 'strict_urgent') {
        // URGENT (priority 0) MUST be visited first, regardless of location
        const urgentWaypoints = allWaypoints.filter(w => w.priority === 0);
        const otherWaypoints = allWaypoints.filter(w => w.priority > 0);
        
        // Sort each group by priority
        urgentWaypoints.sort((a, b) => a.priority - b.priority);
        otherWaypoints.sort((a, b) => a.priority - b.priority);
        
        // Combine: URGENT first, then others
        allWaypoints.length = 0;
        allWaypoints.push(...urgentWaypoints, ...otherWaypoints);
        
        logger.info(`Strategy: STRICT_URGENT - ${urgentWaypoints.length} urgent waypoints will be visited first`);
      } else {
        // Flexible: treat URGENT as very high priority but allow optimization
        // Sort by priority (lower number = higher priority)
        allWaypoints.sort((a, b) => a.priority - b.priority);
        
        logger.info(`Strategy: FLEXIBLE - all waypoints sorted by priority`);
      }

      // Build ordered route: start point + sorted waypoints
      const orderedWaypoints = [
        request.startPoint,
        ...allWaypoints.map((w) => w.waypoint),
      ];

      // Calculate route using OSRM
      const osrmResponse = await this.osrmRouter.getRoute(orderedWaypoints, {
        steps: request.steps !== false,
        annotations: request.annotations !== false,
        overview: 'full',
        geometries: 'geojson',
        vehicle: request.vehicle || 'car',
      });

      if (osrmResponse.code !== 'Ok' || !osrmResponse.routes || osrmResponse.routes.length === 0) {
        throw createError(`OSRM error: ${osrmResponse.message || osrmResponse.code}`, 503);
      }

      // Enrich the first route
      const enrichedResponse = await this.enrichRouteData(osrmResponse, {
        waypoints: orderedWaypoints,
        steps: request.steps,
        annotations: request.annotations,
      } as RouteRequestDto);

      const route = (enrichedResponse as any).route || (enrichedResponse as any).routes?.[0];
      if (!route) {
        throw createError('No route found', 500);
      }

      // Validate that the route has valid distance and duration
      if (route.distance === 0 || route.duration === 0) {
        throw createError('OSRM returned an invalid route with zero distance/duration. The OSRM instance may not have road data for this area.', 503);
      }

      // Build visit order information
      const visitOrder = allWaypoints.map((w) => ({
        index: w.index,
        priority: w.priority,
        priorityLabel: priorityLabels[w.priority] || 'unknown',
        waypoint: w.waypoint,
      }));

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
        totalWaypoints: allWaypoints.length,
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
