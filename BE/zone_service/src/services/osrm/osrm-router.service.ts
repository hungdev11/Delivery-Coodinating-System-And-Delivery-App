/**
 * OSRM Router Service
 * Client for querying OSRM instances with failover support
 * Supports dual-instance setup for zero-downtime updates
 */

import { PrismaClient } from '@prisma/client';
import { prisma } from '../../common/database/prisma.client';
import { logger } from '../../common/logger/logger.service';
import axios, { AxiosInstance } from 'axios';

export interface Coordinate {
  lat: number;
  lon: number;
}

export interface RouteOptions {
  alternatives?: boolean;  // Return alternative routes
  steps?: boolean;         // Include step-by-step instructions
  overview?: 'full' | 'simplified' | 'false';  // Geometry detail level
  geometries?: 'geojson' | 'polyline' | 'polyline6';
  annotations?: boolean;   // Include additional metadata
  continue_straight?: boolean;
}

export interface OSRMRoute {
  distance: number;        // Total distance in meters
  duration: number;        // Total duration in seconds
  weight: number;          // Custom weight (with traffic)
  geometry: any;           // GeoJSON or encoded polyline
  legs: OSRMLeg[];
}

export interface OSRMLeg {
  distance: number;
  duration: number;
  weight: number;
  steps?: OSRMStep[];
  summary?: string;
}

export interface OSRMStep {
  distance: number;
  duration: number;
  weight: number;
  geometry: any;
  name: string;
  mode: string;
  maneuver: OSRMManeuver;
  intersections?: OSRMIntersection[];
}

export interface OSRMManeuver {
  type: string;            // turn, depart, arrive, etc.
  modifier?: string;       // left, right, straight, etc.
  location: [number, number];  // [lon, lat]
  bearing_before?: number;
  bearing_after?: number;
  instruction?: string;
}

export interface OSRMIntersection {
  location: [number, number];
  bearings: number[];
  entry: boolean[];
  in?: number;
  out?: number;
  lanes?: any[];
}

export interface OSRMRouteResponse {
  code: 'Ok' | 'NoRoute' | 'NoSegment' | 'Error';
  routes?: OSRMRoute[];
  waypoints?: Array<{
    location: [number, number];
    name: string;
    hint?: string;
  }>;
  message?: string;
}

export interface MultiStopRouteRequest {
  stops: Coordinate[];
  priorities?: number[];   // Priority for each stop (higher = visit first)
  optimize?: boolean;      // Optimize stop order
  options?: RouteOptions;
}

export class OSRMRouterService {
  private prisma: PrismaClient;
  private activeInstance: 1 | 2 = 1;
  private instance1Client: AxiosInstance;
  private instance2Client: AxiosInstance;
  private instance1Url: string;
  private instance2Url: string;

  constructor() {
    this.prisma = prisma;

    // Get OSRM instance URLs from environment
    this.instance1Url = process.env.OSRM_INSTANCE_1_URL || 'http://localhost:5000';
    this.instance2Url = process.env.OSRM_INSTANCE_2_URL || 'http://localhost:5001';

    // Create axios clients for each instance
    this.instance1Client = axios.create({
      baseURL: this.instance1Url,
      timeout: 10000,
    });

    this.instance2Client = axios.create({
      baseURL: this.instance2Url,
      timeout: 10000,
    });

    logger.info(`OSRM Router initialized with instances: ${this.instance1Url}, ${this.instance2Url}`);

    // Load active instance from database
    this.loadActiveInstance().catch(error => {
      logger.error('Failed to load active instance:', error);
    });
  }

  /**
   * Get route between waypoints
   */
  async getRoute(
    waypoints: Coordinate[],
    options: RouteOptions = {}
  ): Promise<OSRMRouteResponse> {
    if (waypoints.length < 2) {
      return {
        code: 'Error',
        message: 'At least 2 waypoints required',
      };
    }

    // Convert waypoints to OSRM format: lon,lat;lon,lat;...
    const coordinates = waypoints
      .map(wp => `${wp.lon},${wp.lat}`)
      .join(';');

    // Build query parameters
    const params: Record<string, any> = {
      overview: options.overview || 'full',
      geometries: options.geometries || 'geojson',
      steps: options.steps ?? true,
      alternatives: options.alternatives ?? false,
      annotations: options.annotations ?? true,
      continue_straight: options.continue_straight ?? true,
    };

    const queryString = new URLSearchParams(params).toString();
    const path = `/route/v1/driving/${coordinates}?${queryString}`;

    try {
      // Try active instance first
      const response = await this.queryInstance(this.activeInstance, path);
      return response.data;
    } catch (error) {
      logger.warn(`Active instance ${this.activeInstance} failed, trying failover`, error);

      // Try other instance as failover
      const failoverInstance = this.activeInstance === 1 ? 2 : 1;

      try {
        const response = await this.queryInstance(failoverInstance, path);
        logger.info(`Failover to instance ${failoverInstance} successful`);
        return response.data;
      } catch (failoverError) {
        logger.error('Both OSRM instances failed:', failoverError);
        throw new Error('OSRM service unavailable');
      }
    }
  }

  /**
   * Get optimized route for multiple stops (TSP - Traveling Salesman Problem)
   */
  async getMultiStopRoute(
    request: MultiStopRouteRequest
  ): Promise<OSRMRouteResponse> {
    const { stops, priorities, optimize = true, options = {} } = request;

    if (stops.length < 2) {
      return {
        code: 'Error',
        message: 'At least 2 stops required',
      };
    }

    // If optimization requested and priorities provided, reorder stops
    let orderedStops = stops;

    if (optimize && priorities && priorities.length === stops.length) {
      // Sort by priority (descending)
      const indexed = stops.map((stop, i) => ({ stop, priority: priorities[i] }));
      indexed.sort((a, b) => (b.priority || 0) - (a.priority || 0));
      orderedStops = indexed.map(item => item.stop);

      logger.info(`Optimized stop order by priority: ${priorities.join(', ')}`);
    }

    // For simple multi-stop routing, use the trip endpoint
    if (optimize && !priorities) {
      return this.getTripRoute(orderedStops, options);
    }

    // Otherwise, get point-to-point routes
    return this.getRoute(orderedStops, options);
  }

  /**
   * Get optimized trip route (visits all waypoints in optimal order)
   */
  async getTripRoute(
    waypoints: Coordinate[],
    options: RouteOptions = {}
  ): Promise<OSRMRouteResponse> {
    if (waypoints.length < 2) {
      return {
        code: 'Error',
        message: 'At least 2 waypoints required',
      };
    }

    const coordinates = waypoints
      .map(wp => `${wp.lon},${wp.lat}`)
      .join(';');

    const params: Record<string, any> = {
      overview: options.overview || 'full',
      geometries: options.geometries || 'geojson',
      steps: options.steps ?? true,
      annotations: options.annotations ?? true,
      roundtrip: false,  // Don't return to start
      source: 'first',   // Start at first waypoint
      destination: 'last', // End at last waypoint
    };

    const queryString = new URLSearchParams(params).toString();
    const path = `/trip/v1/driving/${coordinates}?${queryString}`;

    try {
      const response = await this.queryInstance(this.activeInstance, path);
      return response.data;
    } catch (error) {
      logger.warn(`Trip query failed on instance ${this.activeInstance}, trying failover`);

      const failoverInstance = this.activeInstance === 1 ? 2 : 1;

      try {
        const response = await this.queryInstance(failoverInstance, path);
        return response.data;
      } catch (failoverError) {
        logger.error('Both OSRM instances failed for trip query:', failoverError);
        throw new Error('OSRM service unavailable');
      }
    }
  }

  /**
   * Get distance matrix between multiple points
   */
  async getMatrix(
    sources: Coordinate[],
    destinations?: Coordinate[]
  ): Promise<any> {
    const dests = destinations || sources;

    const sourceCoords = sources.map(s => `${s.lon},${s.lat}`).join(';');
    const destCoords = dests.map(d => `${d.lon},${d.lat}`).join(';');

    const coordinates = sourceCoords === destCoords
      ? sourceCoords
      : `${sourceCoords};${destCoords}`;

    const sourceIndices = sources.map((_, i) => i).join(';');
    const destIndices = destinations
      ? destinations.map((_, i) => i + sources.length).join(';')
      : sourceIndices;

    const path = `/table/v1/driving/${coordinates}?sources=${sourceIndices}&destinations=${destIndices}`;

    try {
      const response = await this.queryInstance(this.activeInstance, path);
      return response.data;
    } catch (error) {
      const failoverInstance = this.activeInstance === 1 ? 2 : 1;

      try {
        const response = await this.queryInstance(failoverInstance, path);
        return response.data;
      } catch (failoverError) {
        logger.error('Both OSRM instances failed for matrix query:', failoverError);
        throw new Error('OSRM service unavailable');
      }
    }
  }

  /**
   * Get nearest road segment for a coordinate (map matching)
   */
  async getNearest(
    coordinate: Coordinate,
    number: number = 1
  ): Promise<any> {
    const coords = `${coordinate.lon},${coordinate.lat}`;
    const path = `/nearest/v1/driving/${coords}?number=${number}`;

    try {
      const response = await this.queryInstance(this.activeInstance, path);
      return response.data;
    } catch (error) {
      const failoverInstance = this.activeInstance === 1 ? 2 : 1;

      try {
        const response = await this.queryInstance(failoverInstance, path);
        return response.data;
      } catch (failoverError) {
        logger.error('Both OSRM instances failed for nearest query:', failoverError);
        throw new Error('OSRM service unavailable');
      }
    }
  }

  /**
   * Switch active instance
   */
  async switchInstance(targetInstance: 1 | 2): Promise<void> {
    logger.info(`Switching active OSRM instance from ${this.activeInstance} to ${targetInstance}`);

    // Verify target instance is healthy
    const isHealthy = await this.checkInstanceHealth(targetInstance);

    if (!isHealthy) {
      throw new Error(`Instance ${targetInstance} is not healthy`);
    }

    this.activeInstance = targetInstance;

    // Update in database
    await this.prisma.osrm_builds.updateMany({
      where: {
        instance_name: `osrm-instance-${targetInstance}`,
        status: 'READY',
      },
      data: {
        status: 'DEPLOYED',
        deployed_at: new Date(),
      },
    });

    logger.info(`Switched to OSRM instance ${targetInstance}`);
  }

  /**
   * Get active instance number
   */
  getActiveInstance(): 1 | 2 {
    return this.activeInstance;
  }

  /**
   * Get instance status
   */
  async getStatus(): Promise<{
    activeInstance: 1 | 2;
    instance1Healthy: boolean;
    instance2Healthy: boolean;
    instance1Url: string;
    instance2Url: string;
  }> {
    const [instance1Healthy, instance2Healthy] = await Promise.all([
      this.checkInstanceHealth(1),
      this.checkInstanceHealth(2),
    ]);

    return {
      activeInstance: this.activeInstance,
      instance1Healthy,
      instance2Healthy,
      instance1Url: this.instance1Url,
      instance2Url: this.instance2Url,
    };
  }

  /**
   * Check if an instance is healthy
   */
  private async checkInstanceHealth(instance: 1 | 2): Promise<boolean> {
    try {
      const client = instance === 1 ? this.instance1Client : this.instance2Client;
      await client.get('/health', { timeout: 3000 });
      return true;
    } catch (error) {
      return false;
    }
  }

  /**
   * Query specific OSRM instance
   */
  private async queryInstance(instance: 1 | 2, path: string): Promise<any> {
    const client = instance === 1 ? this.instance1Client : this.instance2Client;
    return client.get(path);
  }

  /**
   * Load active instance from database
   */
  private async loadActiveInstance(): Promise<void> {
    try {
      const deployedBuilds = await this.prisma.osrm_builds.findMany({
        where: { status: 'DEPLOYED' },
        orderBy: { deployed_at: 'desc' },
        take: 1,
      });

      if (deployedBuilds.length > 0) {
        const instanceName = deployedBuilds[0]?.instance_name;
        this.activeInstance = instanceName === 'osrm-instance-1' ? 1 : 2;
        logger.info(`Loaded active instance from database: ${this.activeInstance}`);
      }
    } catch (error) {
      logger.error('Failed to load active instance from database:', error);
    }
  }
}
