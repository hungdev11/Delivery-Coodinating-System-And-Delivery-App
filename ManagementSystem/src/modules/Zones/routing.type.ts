/**
 * Routing Types
 *
 * Type definitions for routing and navigation
 */

import type { IApiResponse } from '@/common/types/http'

export interface Waypoint {
  lat: number
  lon: number
  parcelId?: string
}

export interface PriorityGroup {
  priority: PriorityLevelType // 1 = express, 2 = fast, 3 = normal, 4 = economy
  waypoints: Waypoint[]
}

export interface DemoRouteRequest {
  startPoint: Waypoint
  priorityGroups: PriorityGroup[]
  steps?: boolean
  annotations?: boolean
  mode?: 'strict_priority_with_delta' | 'flexible_priority_with_delta' | 'strict_priority_no_delta' | 'flexible_priority_no_delta' | 'base'
  strategy?: 'strict_urgent' | 'flexible'  // 🚨 Cách xử lý URGENT
}

export interface RouteRequest {
  waypoints: Waypoint[]
  priorities?: number[]
  alternatives?: boolean
  steps?: boolean
  annotations?: boolean
  mode?: 'strict_priority_with_delta' | 'flexible_priority_with_delta' | 'strict_priority_no_delta' | 'flexible_priority_no_delta' | 'base'
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
  // Detailed geometry for the step when available
  geometry?: {
    type: 'LineString'
    coordinates: [number, number][]
  }
  addresses?: string[]
  trafficLevel?: string
}

export interface RouteLeg {
  distance: number
  duration: number
  steps: RouteStep[]
  parcelId?: string // Optional parcel ID for tracking delivery parcels
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

export interface RouteResponseData {
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

export interface DemoRouteResponseData {
  code: string
  route: Route
  visitOrder: VisitOrder[]
  summary: RouteSummary
}

// API Response types wrapped in IApiResponse
export type RouteResponse = IApiResponse<RouteResponseData>
export type DemoRouteResponse = IApiResponse<DemoRouteResponseData>

export const PriorityLevel = {
  URGENT: 0,     // 🚨 Gấp tuyệt đối - phải giao đầu tiên
  EXPRESS: 1,    // 🔥 Đơn hàng gấp
  FAST: 2,       // ⚡ Giao nhanh
  NORMAL: 3,     // 📦 Đơn bình thường
  ECONOMY: 4,    // 💰 Ưu tiên giá (có thể giao sau)
} as const

export const PriorityLabel = {
  [PriorityLevel.URGENT]: '🚨 Urgent (Gấp tuyệt đối)',
  [PriorityLevel.EXPRESS]: '🔥 Express (Đơn hàng gấp)',
  [PriorityLevel.FAST]: '⚡ Fast (Giao nhanh)',
  [PriorityLevel.NORMAL]: '📦 Normal (Bình thường)',
  [PriorityLevel.ECONOMY]: '💰 Economy (Ưu tiên giá)',
} as const

export type PriorityLevelType = (typeof PriorityLevel)[keyof typeof PriorityLevel]
