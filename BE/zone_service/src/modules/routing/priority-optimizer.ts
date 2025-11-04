/**
 * Priority-based Route Optimizer
 * 
 * Implements intelligent waypoint ordering that considers:
 * 1. Priority levels (express > standard > economy)
 * 2. Traffic conditions
 * 3. Geographic optimization (TSP)
 * 4. Real-time weight calculations
 */

import { Coordinate } from '../../services/osrm/osrm-router.service.js';
import { logger } from '../../common/logger/logger.service.js';

export interface PriorityWaypoint extends Coordinate {
  index: number;
  priority: number;
  label?: string;
}

export interface RouteSegment {
  from: PriorityWaypoint;
  to: PriorityWaypoint;
  distance: number;
  duration: number;
  weight: number;  // Traffic-aware weight
}

export interface OptimizedRoute {
  waypoints: PriorityWaypoint[];
  totalDistance: number;
  totalDuration: number;
  totalWeight: number;
  segments: RouteSegment[];
}

/**
 * Priority-based route optimizer using greedy + local optimization
 * 
 * Strategy:
 * 1. Group waypoints by priority
 * 2. Within each priority group, solve mini-TSP
 * 3. Consider traffic weights for ordering
 */
export class PriorityRouteOptimizer {
  /**
   * Optimize waypoint order based on priority and traffic
   * 
   * @param start Starting point
   * @param waypoints Delivery waypoints with priorities
   * @param distanceMatrix Pre-computed distance/duration matrix
   * @returns Optimized waypoint order
   */
  static optimizeOrder(
    start: Coordinate,
    waypoints: PriorityWaypoint[],
    distanceMatrix?: Map<string, RouteSegment>
  ): PriorityWaypoint[] {
    if (waypoints.length === 0) return [];
    if (waypoints.length === 1) return waypoints;

    logger.info(`Optimizing route for ${waypoints.length} waypoints with priorities`);

    // Group by priority (descending)
    const priorityGroups = this.groupByPriority(waypoints);
    const optimizedWaypoints: PriorityWaypoint[] = [];

    let currentPosition = start;

    // Process each priority group in order (highest first)
    for (const [priority, group] of priorityGroups.entries()) {
      logger.info(`Processing priority ${priority} group with ${group.length} waypoints`);

      // For small groups (≤3), use exact solution
      // For larger groups, use nearest neighbor heuristic
      const optimizedGroup = group.length <= 3
        ? this.solveExactTSP(currentPosition, group, distanceMatrix)
        : this.solveNearestNeighbor(currentPosition, group, distanceMatrix);

      optimizedWaypoints.push(...optimizedGroup);

      // Update current position for next priority group
      if (optimizedGroup.length > 0) {
        currentPosition = optimizedGroup[optimizedGroup.length - 1]!;
      }
    }

    return optimizedWaypoints;
  }

  /**
   * Group waypoints by priority level
   */
  private static groupByPriority(waypoints: PriorityWaypoint[]): Map<number, PriorityWaypoint[]> {
    const groups = new Map<number, PriorityWaypoint[]>();

    for (const waypoint of waypoints) {
      const priority = waypoint.priority || 0;
      if (!groups.has(priority)) {
        groups.set(priority, []);
      }
      groups.get(priority)!.push(waypoint);
    }

    // Sort groups by priority (descending)
    return new Map([...groups.entries()].sort((a, b) => b[0] - a[0]));
  }

  /**
   * Solve TSP exactly for small groups (brute force)
   */
  private static solveExactTSP(
    start: Coordinate,
    waypoints: PriorityWaypoint[],
    distanceMatrix?: Map<string, RouteSegment>
  ): PriorityWaypoint[] {
    if (waypoints.length === 1) return waypoints;

    // Generate all permutations
    const permutations = this.generatePermutations(waypoints);
    let bestRoute: PriorityWaypoint[] = waypoints;
    let bestCost = Infinity;

    for (const perm of permutations) {
      const cost = this.calculateRouteCost(start, perm, distanceMatrix);
      if (cost < bestCost) {
        bestCost = cost;
        bestRoute = perm;
      }
    }

    logger.info(`Exact TSP: best cost = ${bestCost.toFixed(2)}`);
    return bestRoute;
  }

  /**
   * Solve TSP using nearest neighbor heuristic (greedy)
   */
  private static solveNearestNeighbor(
    start: Coordinate,
    waypoints: PriorityWaypoint[],
    distanceMatrix?: Map<string, RouteSegment>
  ): PriorityWaypoint[] {
    const remaining = new Set(waypoints);
    const route: PriorityWaypoint[] = [];
    let current = start;

    while (remaining.size > 0) {
      let nearest: PriorityWaypoint | null = null;
      let nearestCost = Infinity;

      for (const waypoint of remaining) {
        const cost = this.getDistance(current, waypoint, distanceMatrix);
        if (cost < nearestCost) {
          nearestCost = cost;
          nearest = waypoint;
        }
      }

      if (nearest) {
        route.push(nearest);
        remaining.delete(nearest);
        current = nearest;
      }
    }

    logger.info(`Nearest neighbor: total cost = ${this.calculateRouteCost(start, route, distanceMatrix).toFixed(2)}`);
    return route;
  }

  /**
   * Calculate total route cost (distance + traffic weight)
   */
  private static calculateRouteCost(
    start: Coordinate,
    route: PriorityWaypoint[],
    distanceMatrix?: Map<string, RouteSegment>
  ): number {
    let totalCost = 0;
    let current = start;

    for (const waypoint of route) {
      const segment = this.getSegment(current, waypoint, distanceMatrix);
      // Cost = distance + traffic weight penalty
      totalCost += segment ? (segment.distance + segment.weight * 100) : this.getDistance(current, waypoint);
      current = waypoint;
    }

    return totalCost;
  }

  /**
   * Get distance between two points (Haversine formula)
   */
  private static getDistance(
    from: Coordinate,
    to: Coordinate,
    distanceMatrix?: Map<string, RouteSegment>
  ): number {
    const segment = this.getSegment(from, to, distanceMatrix);
    if (segment) return segment.distance;

    // Fallback: Haversine distance
    const R = 6371000; // Earth radius in meters
    const φ1 = (from.lat * Math.PI) / 180;
    const φ2 = (to.lat * Math.PI) / 180;
    const Δφ = ((to.lat - from.lat) * Math.PI) / 180;
    const Δλ = ((to.lon - from.lon) * Math.PI) / 180;

    const a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
      Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return R * c;
  }

  /**
   * Get route segment from distance matrix
   */
  private static getSegment(
    from: Coordinate,
    to: Coordinate,
    distanceMatrix?: Map<string, RouteSegment>
  ): RouteSegment | undefined {
    if (!distanceMatrix) return undefined;
    const key = `${from.lat},${from.lon}-${to.lat},${to.lon}`;
    return distanceMatrix.get(key);
  }

  /**
   * Generate all permutations of waypoints
   */
  private static generatePermutations<T>(arr: T[]): T[][] {
    if (arr.length <= 1) return [arr];
    if (arr.length === 2) return [arr, [arr[1]!, arr[0]!]];

    const result: T[][] = [];
    for (let i = 0; i < arr.length; i++) {
      const rest = [...arr.slice(0, i), ...arr.slice(i + 1)];
      const perms = this.generatePermutations(rest);
      for (const perm of perms) {
        result.push([arr[i]!, ...perm]);
      }
    }
    return result;
  }

  /**
   * Calculate priority label from numeric priority
   */
  static getPriorityLabel(priority: number): string {
    if (priority >= 3) return 'express';
    if (priority >= 2) return 'standard';
    return 'economy';
  }
}
