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
} from './routing.model';

export class RoutingService {
  private static osrmRouter = new OSRMRouterService();

  /**
   * Calculate route between waypoints
   */
  public static async calculateRoute(request: RouteRequestDto): Promise<RouteResponseDto> {
    try {
      logger.info(`Calculating route with ${request.waypoints.length} waypoints`);

      // Query OSRM using dual-instance router
      const osrmResponse = await this.osrmRouter.getRoute(
        request.waypoints,
        {
          steps: request.steps !== false,
          annotations: request.annotations !== false,
          alternatives: request.alternatives || false,
          overview: 'full',
          geometries: 'geojson',
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
   * Calculate priority-based multi-stop route
   */
  public static async calculatePriorityRoutes(request: RouteRequestDto): Promise<RouteResponseDto> {
    try {
      if (!request.priorities || request.priorities.length !== request.waypoints.length - 1) {
        throw createError('Priorities must be provided for each segment', 400);
      }

      logger.info('Calculating priority-based multi-route');

      // Use OSRMRouterService multi-stop routing
      const osrmResponse = await this.osrmRouter.getMultiStopRoute({
        stops: request.waypoints,
        priorities: request.priorities,
        optimize: true,
        options: {
          steps: request.steps !== false,
          annotations: request.annotations !== false,
        },
      });

      if (osrmResponse.code !== 'Ok') {
        throw createError(`OSRM error: ${osrmResponse.message || osrmResponse.code}`, 503);
      }

      // Enrich with our custom data
      const enrichedResponse = await this.enrichRouteData(osrmResponse, request);

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

        for (const step of leg.steps || []) {
          // Find segments along this step
          const segmentIds = await this.findSegmentsAlongPath(step.geometry?.coordinates || []);

          // Get traffic info
          const trafficInfo = await this.getTrafficInfo(segmentIds);

          // Get nearby addresses
          const addresses = await this.getNearbyAddresses(step.geometry?.coordinates || []);

          enrichedSteps.push({
            distance: step.distance,
            duration: step.duration,
            instruction: this.generateInstruction(step),
            name: step.name || 'Unknown road',
            maneuver: {
              type: step.maneuver.type,
              modifier: step.maneuver.modifier,
              location: step.maneuver.location,
            },
            addresses: addresses.map(a => a.name).slice(0, 3),
            trafficLevel: trafficInfo.level,
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

      enrichedRoutes.push({
        distance: route.distance,
        duration: route.duration,
        geometry: JSON.stringify(route.geometry),
        legs: enrichedLegs,
        trafficSummary,
      });
    }

    return {
      code: osrmResponse.code,
      routes: enrichedRoutes,
    };
  }

  /**
   * Find road segments along a path
   */
  private static async findSegmentsAlongPath(_coordinates: Array<[number, number]>): Promise<string[]> {
    // Simplified version
    // TODO: Use PostGIS spatial queries for production
    return [];
  }

  /**
   * Get traffic info for segments
   */
  private static async getTrafficInfo(segmentIds: string[]): Promise<{ level: string; avgSpeed: number }> {
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
   */
  private static async getNearbyAddresses(_coordinates: Array<[number, number]>): Promise<any[]> {
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
}
