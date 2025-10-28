/**
 * Routing Types
 *
 * Type definitions for routing and navigation
 */

export interface Waypoint {
  lat: number
  lon: number
}

export interface PriorityGroup {
  priority: number // 1 = express, 2 = fast, 3 = normal, 4 = economy
  waypoints: Waypoint[]
}

export interface DemoRouteRequest {
  startPoint: Waypoint
  priorityGroups: PriorityGroup[]
  steps?: boolean
  annotations?: boolean
}

export interface RouteRequest {
  waypoints: Waypoint[]
  priorities?: number[]
  alternatives?: boolean
  steps?: boolean
  annotations?: boolean
}

export interface Maneuver {
  type: string
  modifier?: string
  location: [number, number]
}

export interface RouteStep {
  distance: number
  duration: number
  instruction: string
  name: string
  maneuver: Maneuver
  addresses?: string[]
  trafficLevel?: string
}

export interface RouteLeg {
  distance: number
  duration: number
  steps: RouteStep[]
}

export interface TrafficSummary {
  averageSpeed: number
  congestionLevel: string
  estimatedDelay: number
}

export interface Route {
  distance: number
  duration: number
  geometry: string
  legs: RouteLeg[]
  trafficSummary: TrafficSummary
}

export interface RouteResponse {
  code: string
  routes: Route[]
}

export interface VisitOrder {
  index: number
  priority: number
  priorityLabel: string
  waypoint: Waypoint
}

export interface RouteSummary {
  totalDistance: number
  totalDuration: number
  totalWaypoints: number
  priorityCounts: Record<string, number>
}

export interface DemoRouteResponse {
  code: string
  route: Route
  visitOrder: VisitOrder[]
  summary: RouteSummary
}

export const PriorityLevel = {
  EXPRESS: 1,
  FAST: 2,
  NORMAL: 3,
  ECONOMY: 4,
} as const

export const PriorityLabel = {
  [PriorityLevel.EXPRESS]: 'Express',
  [PriorityLevel.FAST]: 'Fast',
  [PriorityLevel.NORMAL]: 'Normal',
  [PriorityLevel.ECONOMY]: 'Economy',
} as const

export type PriorityLevelType = (typeof PriorityLevel)[keyof typeof PriorityLevel]
