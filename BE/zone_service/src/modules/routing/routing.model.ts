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

  /**
   * Flag to indicate this parcel should be moved to end of route (nullable)
   * Used when postpone is within session time - parcel stays IN_PROGRESS but moves to end
   */
  @IsOptional()
  @IsBoolean()
  moveToEnd?: boolean | null;
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
  @IsIn(['v2-full', 'v2-rating-only', 'v2-blocking-only', 'v2-base'])
  mode?: 'v2-full' | 'v2-rating-only' | 'v2-blocking-only' | 'v2-base';
}

/**
 * Table Matrix Request DTO
 */
export class TableMatrixRequestDto {
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => CoordinateDto)
  coordinates!: CoordinateDto[];

  @IsOptional()
  @IsString()
  @IsIn(['car', 'motorbike'])
  vehicle?: 'car' | 'motorbike' = 'motorbike';

  @IsOptional()
  @IsString()
  @IsIn(['v2-full', 'v2-rating-only', 'v2-blocking-only', 'v2-base', 'v2-car-full', 'v2-car-rating-only', 'v2-car-blocking-only', 'v2-car-base'])
  mode?: 'v2-full' | 'v2-rating-only' | 'v2-blocking-only' | 'v2-base' | 'v2-car-full' | 'v2-car-rating-only' | 'v2-car-blocking-only' | 'v2-car-base' = 'v2-full';
}

/**
 * Coordinate DTO
 */
export class CoordinateDto {
  @IsNumber()
  lat!: number;

  @IsNumber()
  lon!: number;
}

/**
 * Table Matrix Response DTO
 */
export interface TableMatrixResponseDto {
  code: string;
  durations: number[][];
  distances: number[][];
  sources?: Array<{ hint: string; distance: number; name: string; location: [number, number] }>;
  destinations?: Array<{ hint: string; distance: number; name: string; location: [number, number] }>;
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
  @IsIn(['v2-full', 'v2-rating-only', 'v2-blocking-only', 'v2-base'])
  mode?: 'v2-full' | 'v2-rating-only' | 'v2-blocking-only' | 'v2-base';

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

/**
 * VRP Order (Parcel) DTO
 */
export class VRPOrderDto {
  @IsString()
  orderId!: string; // Parcel ID

  @IsNumber()
  lat!: number;

  @IsNumber()
  lon!: number;

  @IsNumber()
  serviceTime!: number; // Service time in seconds

  @IsNumber()
  priority!: number; // 0 = urgent (P0), higher = less urgent

  @IsOptional()
  @IsString()
  zoneId?: string; // Zone ID for zone-based filtering

  @IsOptional()
  @IsString()
  deliveryAddressId?: string; // All parcels with same deliveryAddressId should be in same assignment
}

/**
 * VRP Shipper (DeliveryMan) DTO
 */
export class VRPShipperDto {
  @IsString()
  shipperId!: string; // DeliveryMan ID

  @IsNumber()
  lat!: number; // Start location latitude

  @IsNumber()
  lon!: number; // Start location longitude

  @IsString()
  shiftStart!: string; // Shift start time (ISO format or HH:mm:ss)

  @IsNumber()
  maxSessionTime!: number; // Maximum session time in hours (3.5 for morning, 4.5 for afternoon)

  @IsNumber()
  capacity!: number; // Vehicle capacity (number of parcels)

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  zoneIds?: string[]; // Working zone IDs (ordered by priority)

  @IsOptional()
  @IsString()
  vehicle?: 'car' | 'motorbike'; // Vehicle type
}

/**
 * VRP Assignment Request DTO
 */
export class VRPAssignmentRequestDto {
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => VRPShipperDto)
  shippers!: VRPShipperDto[];

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => VRPOrderDto)
  orders!: VRPOrderDto[];

  @IsOptional()
  @IsString()
  @IsIn(['car', 'motorbike'])
  vehicle?: 'car' | 'motorbike' = 'motorbike';

  @IsOptional()
  @IsString()
  @IsIn(['v2-full', 'v2-rating-only', 'v2-blocking-only', 'v2-base', 'v2-car-full', 'v2-car-rating-only', 'v2-car-blocking-only', 'v2-car-base'])
  mode?: 'v2-full' | 'v2-rating-only' | 'v2-blocking-only' | 'v2-base' | 'v2-car-full' | 'v2-car-rating-only' | 'v2-car-blocking-only' | 'v2-car-base' = 'v2-full';
}

/**
 * VRP Task DTO (Assigned order in route)
 */
export interface VRPTaskDto {
  orderId: string;
  sequenceIndex: number; // 0 = first stop, 1 = second stop, etc.
  estimatedArrivalTime: string; // ISO datetime format
  travelTimeFromPreviousStop?: number; // in seconds
}

/**
 * VRP Assignment Response DTO
 * Map of shipperId -> List of Tasks
 */
export interface VRPAssignmentResponseDto {
  assignments: Record<string, VRPTaskDto[]>; // Map<shipperId, List<Task>>
  unassignedOrders: string[]; // Order IDs that could not be assigned
  statistics?: {
    totalShippers: number;
    totalOrders: number;
    assignedOrders: number;
    averageOrdersPerShipper: number;
    workloadVariance?: number; // Workload balancing metric
  };
}
