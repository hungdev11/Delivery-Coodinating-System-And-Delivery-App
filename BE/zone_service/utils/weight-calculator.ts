/**
 * Weight calculation utilities for road segments
 */

import { RoadType } from '@prisma/client';

/**
 * Calculate base weight for a road segment
 * Weight = time (in minutes) with additional factors
 */
export function calculateBaseWeight(
  lengthMeters: number,
  avgSpeedKmh: number,
  roadType: string,
  lanes?: number
): number {
  // Base time calculation: length / speed
  const timeHours = lengthMeters / 1000 / avgSpeedKmh;
  const timeMinutes = timeHours * 60;

  // Apply road type multiplier
  const roadTypeMultiplier = getRoadTypeMultiplier(roadType as RoadType);

  // Apply lane penalty (fewer lanes = higher weight)
  const laneMultiplier = getLaneMultiplier(lanes);

  // Final weight
  const baseWeight = timeMinutes * roadTypeMultiplier * laneMultiplier;

  return Math.max(baseWeight, 0.01); // Minimum weight of 0.01
}

/**
 * Get road type multiplier
 * Better roads (motorway) have lower multipliers
 */
function getRoadTypeMultiplier(roadType: RoadType): number {
  const multipliers: Record<RoadType, number> = {
    MOTORWAY: 1.0,      // Best roads
    TRUNK: 1.1,
    PRIMARY: 1.2,
    SECONDARY: 1.3,
    TERTIARY: 1.4,
    RESIDENTIAL: 1.5,
    SERVICE: 1.7,
    UNCLASSIFIED: 1.6,
    LIVING_STREET: 1.8,
    PEDESTRIAN: 2.0,
    TRACK: 2.5,
    PATH: 3.0,          // Worst for vehicles
  };

  return multipliers[roadType] || 1.5;
}

/**
 * Get lane multiplier
 * More lanes = better flow = lower multiplier
 */
function getLaneMultiplier(lanes?: number): number {
  if (!lanes) return 1.0;

  if (lanes >= 4) return 0.9;
  if (lanes === 3) return 0.95;
  if (lanes === 2) return 1.0;
  if (lanes === 1) return 1.2;

  return 1.0;
}

/**
 * Calculate delta weight based on traffic conditions
 */
export function calculateDeltaWeight(
  baseWeight: number,
  trafficMultiplier: number,
  userFeedbackAdj: number = 0
): number {
  // Traffic impact: multiplier > 1 means slower traffic
  const trafficImpact = baseWeight * (trafficMultiplier - 1);

  // Total delta
  const delta = trafficImpact + userFeedbackAdj;

  return delta;
}

/**
 * Calculate traffic multiplier from traffic level
 */
export function getTrafficMultiplier(trafficLevel: string, congestionScore: number): number {
  // Base multipliers by traffic level
  const baseMultipliers: Record<string, number> = {
    FREE_FLOW: 0.9,      // 10% faster than normal
    NORMAL: 1.0,         // No change
    SLOW: 1.3,           // 30% slower
    CONGESTED: 1.8,      // 80% slower
    BLOCKED: 3.0,        // 3x slower
  };

  const baseMultiplier = baseMultipliers[trafficLevel] || 1.0;

  // Fine-tune with congestion score (0-100)
  // Add up to 50% more impact based on score
  const scoreImpact = (congestionScore / 100) * 0.5;

  return baseMultiplier + scoreImpact;
}

/**
 * Calculate user feedback adjustment
 * Based on severity and feedback type
 */
export function calculateFeedbackAdjustment(
  baseWeight: number,
  feedbackType: string,
  severity: string
): number {
  // Severity multipliers
  const severityMultipliers: Record<string, number> = {
    MINOR: 0.2,
    MODERATE: 0.5,
    MAJOR: 1.0,
    CRITICAL: 2.0,
  };

  // Feedback type impacts (as percentage of base weight)
  const feedbackImpacts: Record<string, number> = {
    ROAD_CLOSED: 10.0,           // Effectively infinite weight
    CONSTRUCTION: 2.0,            // Double the time
    ACCIDENT: 1.5,                // 1.5x time
    POOR_CONDITION: 0.5,          // 50% more time
    TRAFFIC_ALWAYS_BAD: 0.8,      // 80% more time
    BETTER_ROUTE: -0.3,           // Suggest this route more
    INCORRECT_INFO: 0,
    OTHER: 0.3,
  };

  const severityMult = severityMultipliers[severity] || 0.5;
  const feedbackImpact = feedbackImpacts[feedbackType] || 0;

  return baseWeight * feedbackImpact * severityMult;
}

/**
 * Recalculate current weight for a segment
 */
export function recalculateCurrentWeight(
  baseWeight: number,
  deltaWeight: number
): number {
  const currentWeight = baseWeight + deltaWeight;
  return Math.max(currentWeight, 0.01); // Minimum weight
}
