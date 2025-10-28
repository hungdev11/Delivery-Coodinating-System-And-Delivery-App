/**
 * Routing Model
 * DTOs and data models for routing module
 */

import { IsArray, IsBoolean, IsNumber, IsOptional, ValidateNested } from 'class-validator';
import { Type } from 'class-transformer';

/**
 * Waypoint DTO
 */
export class WaypointDto {
  @IsNumber()
  lat!: number;

  @IsNumber()
  lon!: number;
}

/**
 * Route Request DTO
 */
export class RouteRequestDto {
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => WaypointDto)
  waypoints!: WaypointDto[];

  @IsOptional()
  @IsArray()
  @IsNumber({}, { each: true })
  priorities?: number[];

  @IsOptional()
  @IsBoolean()
  alternatives?: boolean;

  @IsOptional()
  @IsBoolean()
  steps?: boolean;

  @IsOptional()
  @IsBoolean()
  annotations?: boolean;
}

/**
 * Maneuver DTO
 */
export interface ManeuverDto {
  type: string;
  modifier?: string;
  location: [number, number];
}

/**
 * Route Step DTO
 */
export interface RouteStepDto {
  distance: number;
  duration: number;
  instruction: string;
  name: string;
  maneuver: ManeuverDto;
  addresses?: string[];
  trafficLevel?: string;
}

/**
 * Route Leg DTO
 */
export interface RouteLegDto {
  distance: number;
  duration: number;
  steps: RouteStepDto[];
}

/**
 * Traffic Summary DTO
 */
export interface TrafficSummaryDto {
  averageSpeed: number;
  congestionLevel: string;
  estimatedDelay: number;
}

/**
 * Route DTO
 */
export interface RouteDto {
  distance: number;
  duration: number;
  geometry: string;
  legs: RouteLegDto[];
  trafficSummary: TrafficSummaryDto;
}

/**
 * Route Response DTO
 */
export interface RouteResponseDto {
  code: string;
  routes: RouteDto[];
}

/**
 * Priority Group DTO for Demo Routing
 */
export class PriorityGroupDto {
  @IsNumber()
  priority!: number; // 1 = express, 2 = fast, 3 = normal, 4 = economy

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => WaypointDto)
  waypoints!: WaypointDto[];
}

/**
 * Demo Route Request DTO
 * Input: starting point + priority-grouped waypoints
 */
export class DemoRouteRequestDto {
  @ValidateNested()
  @Type(() => WaypointDto)
  startPoint!: WaypointDto;

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => PriorityGroupDto)
  priorityGroups!: PriorityGroupDto[];

  @IsOptional()
  @IsBoolean()
  steps?: boolean;

  @IsOptional()
  @IsBoolean()
  annotations?: boolean;
}

/**
 * Demo Route Response DTO
 * Output: optimized route with priority ordering
 */
export interface DemoRouteResponseDto {
  code: string;
  route: RouteDto;
  visitOrder: Array<{
    index: number;
    priority: number;
    priorityLabel: string;
    waypoint: WaypointDto;
  }>;
  summary: {
    totalDistance: number;
    totalDuration: number;
    totalWaypoints: number;
    priorityCounts: Record<string, number>;
  };
}
