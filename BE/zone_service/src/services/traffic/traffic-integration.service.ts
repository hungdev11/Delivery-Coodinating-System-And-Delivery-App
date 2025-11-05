/**
 * Traffic Data Integration Service
 * Fetches traffic data from tracking-asia and updates road segment weights
 */

import { PrismaClient, TrafficLevel } from '@prisma/client';
import { prisma } from '../../common/database/prisma.client';
import { logger } from '../../common/logger/logger.service';
import axios from 'axios';

export interface TrafficDataPoint {
  lat: number;
  lon: number;
  speed: number;
  congestionLevel: string;
  timestamp: Date;
}

export interface TrackingAsiaResponse {
  // Define based on actual tracking-asia API response
  traffic: Array<{
    coordinates: [number, number];
    speed: number;
    level: string;
  }>;
}

export class TrafficIntegrationService {
  private prisma: PrismaClient;
  private updateInterval: number;
  private intervalId: NodeJS.Timeout | null = null;

  constructor(updateIntervalMinutes: number = 30) {
    this.prisma = prisma;
    this.updateInterval = updateIntervalMinutes * 60 * 1000; // Convert to ms
  }

  /**
   * Start periodic traffic updates
   */
  start(): void {
    logger.info(`Starting traffic integration service (update every ${this.updateInterval / 60000} minutes)`);

    // Initial update
    this.updateTrafficData().catch(error => {
      logger.error('Initial traffic update failed:', error);
    });

    // Schedule periodic updates
    this.intervalId = setInterval(() => {
      this.updateTrafficData().catch(error => {
        logger.error('Periodic traffic update failed:', error);
      });
    }, this.updateInterval);
  }

  /**
   * Stop periodic updates
   */
  stop(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
      logger.info('Traffic integration service stopped');
    }
  }

  /**
   * Fetch traffic data from tracking-asia
   */
  private async fetchTrafficData(): Promise<TrafficDataPoint[]> {
    // TODO: Replace with actual tracking-asia API endpoint
    const TRACKING_ASIA_API = process.env.TRACKING_ASIA_API || 'https://api.tracking-asia.com/traffic';

    try {
      logger.info('Fetching traffic data from tracking-asia...');

      const response = await axios.get<TrackingAsiaResponse>(TRACKING_ASIA_API, {
        params: {
          region: 'hochiminh',
          // Add authentication if required
        },
        timeout: 30000, // 30 second timeout
      });

      // Transform to our format
      const trafficData: TrafficDataPoint[] = response.data.traffic.map(item => ({
        lat: item.coordinates[1],
        lon: item.coordinates[0],
        speed: item.speed,
        congestionLevel: item.level,
        timestamp: new Date(),
      }));

      logger.info(`Fetched ${trafficData.length} traffic data points`);
      return trafficData;
    } catch (error) {
      logger.error('Failed to fetch traffic data:', error);
      return [];
    }
  }

  /**
   * Update traffic data and recalculate weights
   */
  private async updateTrafficData(): Promise<void> {
    const startTime = Date.now();
    logger.info('Starting traffic data update...');

    try {
      // Step 1: Fetch latest traffic data
      const trafficData = await this.fetchTrafficData();

      if (trafficData.length === 0) {
        logger.warn('No traffic data received, skipping update');
        return;
      }

      // Step 2: Match traffic data to road segments
      const updates = await this.matchTrafficToSegments(trafficData);

      // Step 3: Update traffic conditions in database
      await this.updateTrafficConditions(updates);

      // Step 4: Recalculate weights
      await this.recalculateWeights();

      const duration = Date.now() - startTime;
      logger.info(`Traffic update completed in ${duration}ms`);
    } catch (error) {
      logger.error('Traffic update failed:', error);
      throw error;
    }
  }

  /**
   * Match traffic data points to road segments
   * Uses spatial proximity matching
   */
  private async matchTrafficToSegments(
    trafficData: TrafficDataPoint[]
  ): Promise<Array<{ segmentId: string; traffic: TrafficDataPoint }>> {
    logger.info('Matching traffic data to road segments...');

    const matches: Array<{ segmentId: string; traffic: TrafficDataPoint }> = [];

    // Get all segments (in production, use spatial indexing)
    const segments = await this.prisma.road_segments.findMany({
      select: {
        segment_id: true,
        geometry: true,
        from_node: {
          select: { lat: true, lon: true },
        },
        to_node: {
          select: { lat: true, lon: true },
        },
      },
    });

    // For each traffic data point, find nearest segment
    for (const traffic of trafficData) {
      let nearestSegment: string | null = null;
      let minDistance = Infinity;

      for (const segment of segments) {
        // Calculate distance to segment midpoint (simplified)
        const midLat = (segment.from_node.lat + segment.to_node.lat) / 2;
        const midLon = (segment.from_node.lon + segment.to_node.lon) / 2;

        const distance = TrafficIntegrationService.calculateDistance(
          traffic.lat,
          traffic.lon,
          midLat,
          midLon
        );

        if (distance < minDistance && distance < 500) { // Within 500m
          minDistance = distance;
          nearestSegment = segment.segment_id;
        }
      }

      if (nearestSegment) {
        matches.push({ segmentId: nearestSegment, traffic });
      }
    }

    logger.info(`Matched ${matches.length} traffic points to segments`);
    return matches;
  }

  /**
   * Update traffic conditions in database
   */
  private async updateTrafficConditions(
    updates: Array<{ segmentId: string; traffic: TrafficDataPoint }>
  ): Promise<void> {
    logger.info('Updating traffic conditions...');

    const expireTime = new Date(Date.now() + this.updateInterval * 2); // Expire after 2 update cycles

    for (const update of updates) {
      const trafficLevel = this.mapCongestionLevel(update.traffic.congestionLevel);
      const congestionScore = this.calculateCongestionScore(update.traffic.speed, trafficLevel);

      // Get segment to calculate weight multiplier
      const segment = await this.prisma.road_segments.findUnique({
        where: { segment_id: update.segmentId },
      });

      if (!segment) continue;

      const speedRatio = segment.avg_speed
        ? update.traffic.speed / segment.avg_speed
        : 1.0;

      const weightMultiplier = this.calculateWeightMultiplier(trafficLevel, congestionScore);

      // Delete existing traffic condition for this segment (if any)
      await this.prisma.traffic_conditions.deleteMany({
        where: { segment_id: update.segmentId },
      });

      // Create new traffic condition
      await this.prisma.traffic_conditions.create({
        data: {
          segment_id: update.segmentId,
          traffic_level: trafficLevel,
          congestion_score: congestionScore,
          current_speed: update.traffic.speed,
          speed_ratio: speedRatio,
          weight_multiplier: weightMultiplier,
          source: 'tracking-asia',
          source_timestamp: update.traffic.timestamp,
          expires_at: expireTime,
        },
      });
    }

    logger.info('Traffic conditions updated');
  }

  /**
   * Recalculate weights for all affected segments
   */
  private async recalculateWeights(): Promise<void> {
    logger.info('Recalculating segment weights...');

    // Get all active traffic conditions
    const activeConditions = await this.prisma.traffic_conditions.findMany({
      where: {
        expires_at: { gte: new Date() },
      },
      include: {
        road_segment: true,
      },
    });

    let updatedCount = 0;

    for (const condition of activeConditions) {
      const segment = condition.road_segment;

      // Calculate delta weight
      const trafficImpact = segment.base_weight * (condition.weight_multiplier - 1);

      // Get user feedback adjustments
      const feedbackAdj = await this.getUserFeedbackAdjustment(segment.segment_id);

      const deltaWeight = trafficImpact + feedbackAdj;
      const currentWeight = Math.max(segment.base_weight + deltaWeight, 0.01);

      // Update segment
      await this.prisma.road_segments.update({
        where: { segment_id: segment.segment_id },
        data: {
          delta_weight: deltaWeight,
          current_weight: currentWeight,
          weight_updated_at: new Date(),
        },
      });

      // Log to history
      await this.prisma.weight_history.create({
        data: {
          segment_id: segment.segment_id,
          base_weight: segment.base_weight,
          delta_weight: deltaWeight,
          current_weight: currentWeight,
          traffic_multiplier: condition.weight_multiplier,
          user_feedback_adj: feedbackAdj,
          calculation_trigger: 'traffic_update',
        },
      });

      updatedCount++;
    }

    logger.info(`Updated weights for ${updatedCount} segments`);
  }

  /**
   * Get aggregated user feedback adjustment for a segment
   */
  private async getUserFeedbackAdjustment(segmentId: string): Promise<number> {
    const feedback = await this.prisma.user_feedback.findMany({
      where: {
        segment_id: segmentId,
        applied: true,
        status: 'APPROVED',
      },
    });

    return feedback.reduce((sum, fb) => sum + (fb.weight_adjustment || 0), 0);
  }

  /**
   * Map congestion level string to enum
   */
  private mapCongestionLevel(level: string): TrafficLevel {
    const mapping: Record<string, TrafficLevel> = {
      'free': 'FREE_FLOW',
      'normal': 'NORMAL',
      'slow': 'SLOW',
      'congested': 'CONGESTED',
      'blocked': 'BLOCKED',
    };

    return mapping[level.toLowerCase()] || 'NORMAL';
  }

  /**
   * Calculate congestion score from speed and level
   */
  private calculateCongestionScore(_speed: number, level: TrafficLevel): number {
    const baseScores: Record<TrafficLevel, number> = {
      FREE_FLOW: 0,
      NORMAL: 25,
      SLOW: 50,
      CONGESTED: 75,
      BLOCKED: 100,
    };

    return baseScores[level];
  }

  /**
   * Calculate weight multiplier from traffic data
   */
  private calculateWeightMultiplier(level: TrafficLevel, score: number): number {
    const baseMultipliers: Record<TrafficLevel, number> = {
      FREE_FLOW: 0.9,
      NORMAL: 1.0,
      SLOW: 1.3,
      CONGESTED: 1.8,
      BLOCKED: 3.0,
    };

    const baseMultiplier = baseMultipliers[level];
    const scoreImpact = (score / 100) * 0.5;

    return baseMultiplier + scoreImpact;
  }

  /**
   * Calculate distance between two coordinates (Haversine)
   */
  private static calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
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
   * Cleanup expired traffic conditions
   */
  async cleanupExpiredConditions(): Promise<void> {
    const result = await this.prisma.traffic_conditions.deleteMany({
      where: {
        expires_at: { lt: new Date() },
      },
    });

    logger.info(`Cleaned up ${result.count} expired traffic conditions`);
  }
}
