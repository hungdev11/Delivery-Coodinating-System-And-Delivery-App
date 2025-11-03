/**
 * Sequencing Service
 * 
 * Two routing modes for waypoint prioritization:
 * 1. Priority-first: Always visit high-priority points, accept larger detours
 * 2. Speed-leaning: Prefer fast routes, skip low-priority points if detour is large
 * 
 * Uses OSRM /table API for distance/duration matrix.
 */

import axios from 'axios';

export interface Waypoint {
  id: string;
  latitude: number;
  longitude: number;
  priority: number; // 1 = highest, 5 = lowest
  name?: string;
}

export interface SequencingOptions {
  start: { latitude: number; longitude: number };
  end?: { latitude: number; longitude: number }; // Optional: if null, return to start
  waypoints: Waypoint[];
  mode: 'priority_first' | 'speed_leaning';
  lambda?: number; // Trade-off factor (auto-set based on mode if not provided)
  osrm_url?: string; // Default: http://localhost:5001
  vehicle_type?: 'car' | 'motorbike'; // Default: motorbike
}

export interface SequencingResult {
  ordered_waypoints: Waypoint[];
  skipped_waypoints: Waypoint[];
  total_duration: number; // seconds
  total_distance: number; // meters
  route_coordinates?: Array<[number, number]>; // Full route geometry
}

export class SequencingService {
  private osrmBaseUrl: string;
  private vehicleType: string;

  constructor(osrmBaseUrl: string = 'http://localhost:5001', vehicleType: string = 'motorbike') {
    this.osrmBaseUrl = osrmBaseUrl;
    this.vehicleType = vehicleType;
  }

  /**
   * Main sequencing entry point
   */
  async sequence(options: SequencingOptions): Promise<SequencingResult> {
    const { mode, lambda } = options;

    // Auto-set lambda based on mode if not provided
    const effectiveLambda = lambda || (mode === 'priority_first' ? 1.0 : 0.2);

    if (mode === 'priority_first') {
      return this.sequencePriorityFirst(options, effectiveLambda);
    } else {
      return this.sequenceSpeedLeaning(options, effectiveLambda);
    }
  }

  /**
   * Priority-first mode: Always visit high-priority points
   * Uses precedence constraints (implicit via greedy selection)
   */
  private async sequencePriorityFirst(
    options: SequencingOptions,
    lambda: number
  ): Promise<SequencingResult> {
    const { start, end, waypoints } = options;

    // Build candidate points: start + waypoints + end (if different from start)
    const allPoints = [
      { ...start, id: 'start', priority: 0 },
      ...waypoints,
      ...(end ? [{ ...end, id: 'end', priority: 0 }] : []),
    ];

    // Get distance/duration matrix from OSRM
    const matrix = await this.getOSRMTable(allPoints);

    // Greedy sequencing with priority-first logic
    const ordered: Waypoint[] = [];
    const remaining = [...waypoints].sort((a, b) => a.priority - b.priority); // Sort by priority (1=highest first)
    let currentIdx = 0; // Start point index

    while (remaining.length > 0) {
      // For priority-first, we strictly follow priority order
      // Pick the highest priority waypoint not yet visited
      const nextWaypoint = remaining.shift()!;
      const nextIdx = allPoints.findIndex(p => p.id === nextWaypoint.id);

      ordered.push(nextWaypoint);
      currentIdx = nextIdx;
    }

    // Calculate total duration and distance
    let totalDuration = 0;
    let totalDistance = 0;
    let prevIdx = 0; // Start

    for (const wp of ordered) {
      const wpIdx = allPoints.findIndex(p => p.id === wp.id);
      totalDuration += matrix.durations[prevIdx][wpIdx];
      totalDistance += matrix.distances[prevIdx][wpIdx];
      prevIdx = wpIdx;
    }

    // Add return to end/start
    const endIdx = end ? allPoints.findIndex(p => p.id === 'end') : 0;
    totalDuration += matrix.durations[prevIdx][endIdx];
    totalDistance += matrix.distances[prevIdx][endIdx];

    return {
      ordered_waypoints: ordered,
      skipped_waypoints: [],
      total_duration: totalDuration,
      total_distance: totalDistance,
    };
  }

  /**
   * Speed-leaning mode: Skip low-priority points if detour is large
   * Uses disjunction penalties (greedy approximation)
   */
  private async sequenceSpeedLeaning(
    options: SequencingOptions,
    lambda: number
  ): Promise<SequencingResult> {
    const { start, end, waypoints } = options;

    // Build candidate points
    const allPoints = [
      { ...start, id: 'start', priority: 0 },
      ...waypoints,
      ...(end ? [{ ...end, id: 'end', priority: 0 }] : []),
    ];

    // Get distance/duration matrix from OSRM
    const matrix = await this.getOSRMTable(allPoints);

    // Greedy sequencing with speed-leaning logic
    const ordered: Waypoint[] = [];
    const skipped: Waypoint[] = [];
    const remaining = [...waypoints];
    let currentIdx = 0; // Start point index
    const endIdx = end ? allPoints.findIndex(p => p.id === 'end') : 0;

    while (remaining.length > 0) {
      // For each remaining waypoint, calculate: priority_benefit - lambda * detour_cost
      let bestScore = -Infinity;
      let bestWaypoint: Waypoint | null = null;
      let bestWaypointIdx = -1;

      for (const wp of remaining) {
        const wpIdx = allPoints.findIndex(p => p.id === wp.id);

        // Calculate detour: (current -> wp -> end) - (current -> end)
        const directDuration = matrix.durations[currentIdx][endIdx];
        const detourDuration = matrix.durations[currentIdx][wpIdx] + matrix.durations[wpIdx][endIdx];
        const detour = detourDuration - directDuration;

        // Priority benefit: higher priority = higher benefit
        // Map priority 1->5 to benefit 5->1 (inverse)
        const priorityBenefit = (6 - wp.priority) * 100; // Scale to seconds for comparison

        // Score: benefit - lambda * detour
        const score = priorityBenefit - lambda * detour;

        if (score > bestScore) {
          bestScore = score;
          bestWaypoint = wp;
          bestWaypointIdx = wpIdx;
        }
      }

      // Decision: visit or skip
      if (bestScore > 0 && bestWaypoint) {
        // Visit: detour is worth it
        ordered.push(bestWaypoint);
        currentIdx = bestWaypointIdx;
        remaining.splice(remaining.indexOf(bestWaypoint), 1);
      } else {
        // Skip: detour is too large relative to priority
        if (bestWaypoint) {
          skipped.push(bestWaypoint);
          remaining.splice(remaining.indexOf(bestWaypoint), 1);
        } else {
          break;
        }
      }
    }

    // Calculate total duration and distance
    let totalDuration = 0;
    let totalDistance = 0;
    let prevIdx = 0; // Start

    for (const wp of ordered) {
      const wpIdx = allPoints.findIndex(p => p.id === wp.id);
      totalDuration += matrix.durations[prevIdx][wpIdx];
      totalDistance += matrix.distances[prevIdx][wpIdx];
      prevIdx = wpIdx;
    }

    // Add return to end/start
    totalDuration += matrix.durations[prevIdx][endIdx];
    totalDistance += matrix.distances[prevIdx][endIdx];

    return {
      ordered_waypoints: ordered,
      skipped_waypoints: skipped,
      total_duration: totalDuration,
      total_distance: totalDistance,
    };
  }

  /**
   * Get distance/duration matrix from OSRM /table API
   */
  private async getOSRMTable(points: Array<{ latitude: number; longitude: number; id: string }>): Promise<{
    durations: number[][];
    distances: number[][];
  }> {
    // Build coordinates string: lon,lat;lon,lat;...
    const coordinates = points.map(p => `${p.longitude},${p.latitude}`).join(';');

    const url = `${this.osrmBaseUrl}/table/v1/${this.vehicleType}/${coordinates}?annotations=duration,distance`;

    try {
      const response = await axios.get(url);
      
      if (response.data.code !== 'Ok') {
        throw new Error(`OSRM table API error: ${response.data.code}`);
      }

      return {
        durations: response.data.durations,
        distances: response.data.distances,
      };
    } catch (error: any) {
      throw new Error(`Failed to fetch OSRM table: ${error.message}`);
    }
  }

  /**
   * Get full route geometry from OSRM /route API
   */
  async getRouteGeometry(
    orderedPoints: Array<{ latitude: number; longitude: number }>
  ): Promise<Array<[number, number]>> {
    const coordinates = orderedPoints.map(p => `${p.longitude},${p.latitude}`).join(';');
    const url = `${this.osrmBaseUrl}/route/v1/${this.vehicleType}/${coordinates}?overview=full&geometries=geojson`;

    try {
      const response = await axios.get(url);
      
      if (response.data.code !== 'Ok') {
        throw new Error(`OSRM route API error: ${response.data.code}`);
      }

      // Extract coordinates from GeoJSON
      const geometry = response.data.routes[0].geometry;
      return geometry.coordinates;
    } catch (error: any) {
      throw new Error(`Failed to fetch OSRM route: ${error.message}`);
    }
  }
}

export const sequencingService = new SequencingService();
