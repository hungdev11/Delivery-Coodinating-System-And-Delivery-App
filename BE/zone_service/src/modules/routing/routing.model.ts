/**
 * Routing Model
 * DTOs and data models for routing module
 */

import { IsArray, IsBoolean, IsNumber, IsOptional, IsString, IsIn, ValidateNested } from 'class-validator';
import { Type } from 'class-transformer';

/**
 * Waypoint DTO
 */
export class WaypointDto {
  @IsNumber()
  lat!: number;

  @IsNumber()
  lon!: number;

  @IsOptional()
  @IsString()
  parcelId?: string;
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
  
  @IsOptional()
  @IsString()
  @IsIn(['car', 'motorbike'])
  vehicle?: 'car' | 'motorbike';

  @IsOptional()
  @IsString()
  @IsIn(['strict_priority_with_delta', 'flexible_priority_with_delta', 'strict_priority_no_delta', 'flexible_priority_no_delta', 'base'])
  mode?: 'strict_priority_with_delta' | 'flexible_priority_with_delta' | 'strict_priority_no_delta' | 'flexible_priority_no_delta' | 'base';
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
  /**
   * Detailed geometry for this step (GeoJSON LineString)
   * Present when OSRM is queried with geometries=geojson & steps=true
   */
  geometry?: {
    type: 'LineString';
    coordinates: Array<[number, number]>;
  };
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
  parcelId?: string;
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
  route: RouteDto;
  visitOrder?: Array<{
    index: number;
    priority: number;
    priorityLabel: string;
    waypoint: WaypointDto;
  }>;
  summary?: {
    totalDistance: number;
    totalDuration: number;
    totalWaypoints: number;
    priorityCounts?: Record<string, number>;
  };
}

/**
 * Priority Group DTO for Demo Routing
 */
export class PriorityGroupDto {
  @IsNumber()
  priority!: number; // 0 = urgent, 1 = express, 2 = fast, 3 = normal, 4 = economy

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
  startPoint!: WaypointDto; // Điểm của shipper

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => PriorityGroupDto)
  priorityGroups!: PriorityGroupDto[]; // Các điểm của khách hàng

  @IsOptional()
  @IsBoolean()
  steps: boolean = true; // default: true

  @IsOptional()
  @IsBoolean()
  annotations: boolean = true; // default: true
  
  @IsOptional()
  @IsString()
  @IsIn(['car', 'motorbike'])
  vehicle?: 'car' | 'motorbike';

  @IsOptional()
  @IsString()
  @IsIn(['strict_priority_with_delta', 'flexible_priority_with_delta', 'strict_priority_no_delta', 'flexible_priority_no_delta', 'base'])
  mode?: 'strict_priority_with_delta' | 'flexible_priority_with_delta' | 'strict_priority_no_delta' | 'flexible_priority_no_delta' | 'base';

  @IsOptional()
  @IsString()
  @IsIn(['strict_urgent', 'flexible'])
  strategy?: 'strict_urgent' | 'flexible';
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
