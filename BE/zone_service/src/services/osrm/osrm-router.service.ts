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
  vehicle?: 'car' | 'motorbike';  // Vehicle type (determines which OSRM profile to use)
  // Routing mode determines WHICH motorbike OSRM instance to query

  mode?: 'strict_priority_with_delta' | 'flexible_priority_with_delta' | 'strict_priority_no_delta' | 'flexible_priority_no_delta' | 'base';
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



export class OSRMRouterService {
  private prisma: PrismaClient;
  private activeInstance: 1 | 2 = 1;
  private instance1Client: AxiosInstance;
  private instance2Client: AxiosInstance;
  private instance1Url: string;
  private instance2Url: string;
  
  // Motorbike routes use per-mode clients created on demand

  constructor() {
    this.prisma = prisma;

    // Get OSRM instance URLs from environment
    // Legacy (car dual-instance)
    this.instance1Url = process.env.OSRM_INSTANCE_1_URL || process.env.OSRM_FLEXIBLE_PRIORITY_WITH_DELTA_URL || 'http://localhost:5001';
    this.instance2Url = process.env.OSRM_INSTANCE_2_URL || process.env.OSRM_BASE_URL || 'http://localhost:5004';

    // Create axios clients for each instance
    this.instance1Client = axios.create({
      baseURL: this.instance1Url,
      timeout: 10000,
    });

    this.instance2Client = axios.create({
      baseURL: this.instance2Url,
      timeout: 10000,
    });
    
    // Motorbike clients are created per request based on mode

    logger.info(
      `OSRM Router initialized - ` +
      `Motorbike modes: strict_priority_with_delta=${process.env.OSRM_STRICT_PRIORITY_WITH_DELTA_URL || 'n/a'}, ` +
      `flexible_priority_with_delta=${process.env.OSRM_FLEXIBLE_PRIORITY_WITH_DELTA_URL || 'n/a'}, ` +
      `strict_priority_no_delta=${process.env.OSRM_STRICT_PRIORITY_NO_DELTA_URL || 'n/a'}, ` +
      `flexible_priority_no_delta=${process.env.OSRM_FLEXIBLE_PRIORITY_NO_DELTA_URL || 'n/a'}, ` +
      `base=${process.env.OSRM_BASE_URL || 'n/a'} | ` +
      `Car modes: strict_priority_with_delta=${process.env.OSRM_CAR_STRICT_PRIORITY_WITH_DELTA_URL || 'n/a'}, ` +
      `flexible_priority_with_delta=${process.env.OSRM_CAR_FLEXIBLE_PRIORITY_WITH_DELTA_URL || 'n/a'}, ` +
      `strict_priority_no_delta=${process.env.OSRM_CAR_STRICT_PRIORITY_NO_DELTA_URL || 'n/a'}, ` +
      `flexible_priority_no_delta=${process.env.OSRM_CAR_FLEXIBLE_PRIORITY_NO_DELTA_URL || 'n/a'}, ` +
      `base=${process.env.OSRM_CAR_BASE_URL || 'n/a'}`
    );

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
      // Request detailed annotations including OSM node IDs
      annotations: options.annotations === false ? false : 'nodes,distance,duration,weight,speed',
      continue_straight: options.continue_straight ?? true,
    };

    const queryString = new URLSearchParams(params).toString();
    
    // Determine vehicle profile
    const vehicle = options.vehicle || 'car';
    const profile = vehicle === 'motorbike' ? 'motorbike' : 'car';
    const path = `/route/v1/${profile}/${coordinates}?${queryString}`;

    try {
      // Both motorbike and car use per-mode instances
      if (vehicle === 'motorbike') {
        const client = this.getMotorbikeClientForMode(options.mode);
        const response = await client.get(path);
        return response.data;
      }
      
      // Car uses per-mode instances (same as motorbike)
      if (vehicle === 'car') {
        const client = this.getCarClientForMode(options.mode);
        const response = await client.get(path);
        return response.data;
      }
      
      // Fallback: use dual-instance with failover (legacy car routing)
      const response = await this.queryInstance(this.activeInstance, path);
      return response.data;
    } catch (error) {
      // Both motorbike and car instances have no failover
      if (vehicle === 'motorbike' || vehicle === 'car') {
        // Avoid logging circular structures
        const msg = error instanceof Error ? error.message : String(error);
        logger.error(`${vehicle} OSRM instance failed`, { message: msg, mode: options.mode });
        throw new Error(`${vehicle} routing service unavailable`);
      }
      
      logger.warn(`Active car instance ${this.activeInstance} failed, trying failover`);

      // Try other instance as failover (legacy car only)
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
   * Select motorbike OSRM axios client based on routing mode
   */
  private getMotorbikeClientForMode(
    mode: 'strict_priority_with_delta' | 'flexible_priority_with_delta' | 'strict_priority_no_delta' | 'flexible_priority_no_delta' | 'base' | undefined
  ): AxiosInstance {
    const map: Record<string, string | undefined> = {
      strict_priority_with_delta: process.env.OSRM_STRICT_PRIORITY_WITH_DELTA_URL,
      flexible_priority_with_delta: process.env.OSRM_FLEXIBLE_PRIORITY_WITH_DELTA_URL,
      strict_priority_no_delta: process.env.OSRM_STRICT_PRIORITY_NO_DELTA_URL,
      flexible_priority_no_delta: process.env.OSRM_FLEXIBLE_PRIORITY_NO_DELTA_URL,
      base: process.env.OSRM_BASE_URL,
    };
    const selected = (mode && map[mode]) || process.env.OSRM_FLEXIBLE_PRIORITY_WITH_DELTA_URL || this.instance2Url;
    return axios.create({ baseURL: selected, timeout: 10000 });
  }

  /**
   * Select car OSRM axios client based on routing mode
   */
  private getCarClientForMode(
    mode: 'strict_priority_with_delta' | 'flexible_priority_with_delta' | 'strict_priority_no_delta' | 'flexible_priority_no_delta' | 'base' | undefined
  ): AxiosInstance {
    const map: Record<string, string | undefined> = {
      strict_priority_with_delta: process.env.OSRM_CAR_STRICT_PRIORITY_WITH_DELTA_URL,
      flexible_priority_with_delta: process.env.OSRM_CAR_FLEXIBLE_PRIORITY_WITH_DELTA_URL,
      strict_priority_no_delta: process.env.OSRM_CAR_STRICT_PRIORITY_NO_DELTA_URL,
      flexible_priority_no_delta: process.env.OSRM_CAR_FLEXIBLE_PRIORITY_NO_DELTA_URL,
      base: process.env.OSRM_CAR_BASE_URL,
    };
    // Fallback to legacy car instances if new ones not configured
    const fallbackMap: Record<string, string | undefined> = {
      strict_priority_with_delta: process.env.OSRM_INSTANCE_1_URL,
      flexible_priority_with_delta: process.env.OSRM_INSTANCE_1_URL,
      strict_priority_no_delta: process.env.OSRM_INSTANCE_2_URL,
      flexible_priority_no_delta: process.env.OSRM_INSTANCE_2_URL,
      base: process.env.OSRM_INSTANCE_2_URL,
    };
    const selected = (mode && map[mode]) || (mode && fallbackMap[mode]) || process.env.OSRM_INSTANCE_1_URL || this.instance1Url;
    return axios.create({ baseURL: selected, timeout: 10000 });
  }

  /**
   * NOTE: Multi-stop and trip routing removed - use calculateDemoRoute instead
   * which handles priority-based waypoint ordering with proper OSRM mode selection
   */

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
