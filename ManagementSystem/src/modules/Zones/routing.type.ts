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
  mode?: 'v2-full' | 'v2-rating-only' | 'v2-blocking-only' | 'v2-base'
  strategy?: 'strict_urgent' | 'flexible' // 🚨 Cách xử lý URGENT
  vehicle?: 'car' | 'motorbike' // Vehicle type (default: motorbike)
}

export interface RouteRequest {
  waypoints: Waypoint[]
  priorities?: number[]
  alternatives?: boolean
  steps?: boolean
  annotations?: boolean
  mode?: 'v2-full' | 'v2-rating-only' | 'v2-blocking-only' | 'v2-base'
  vehicle?: 'car' | 'motorbike' // Vehicle type (default: motorbike)
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

// ============================================
// PRIORITY SYSTEM (1-10 Scale)
// ============================================
// New system: 1-10 scale where higher = more urgent
// 10: URGENT (khẩn cấp - giao ngay)
// 7-9: EXPRESS (nhanh - ưu tiên cao)
// 4-6: NORMAL (bình thường)
// 2-3: ECONOMY (tiết kiệm)
// 1: LOW (thấp nhất)
//
// Legacy support: 0-4 scale is auto-converted to 1-10
// ============================================

export const PriorityLevel = {
  // Legacy (0-4) - backward compatible
  URGENT: 0, // Legacy: auto-converts to 10
  EXPRESS: 1, // Legacy: auto-converts to 8
  FAST: 2, // Legacy: auto-converts to 6
  NORMAL: 3, // Legacy: auto-converts to 4
  ECONOMY: 4, // Legacy: auto-converts to 2

  // New scale (1-10) - recommended
  URGENT_10: 10, // 🚨 Khẩn cấp tuyệt đối - giao ngay
  EXPRESS_HIGH_9: 9, // 🔥 Express cao nhất
  EXPRESS_8: 8, // 🔥 Express tiêu chuẩn
  EXPRESS_STANDARD_7: 7, // 🔥 Express cơ bản
  NORMAL_HIGH_6: 6, // 📦 Normal cao
  NORMAL_5: 5, // 📦 Normal trung bình
  NORMAL_STANDARD_4: 4, // 📦 Normal tiêu chuẩn
  ECONOMY_HIGH_3: 3, // 💰 Economy cao
  ECONOMY_2: 2, // 💰 Economy tiêu chuẩn
  LOW_1: 1, // 🐢 Thấp nhất
} as const

export const PriorityLabel = {
  // Legacy labels (0-4)
  [PriorityLevel.URGENT]: '🚨 Urgent (Gấp tuyệt đối)',
  [PriorityLevel.EXPRESS]: '🔥 Express (Đơn hàng gấp)',
  [PriorityLevel.FAST]: '⚡ Fast (Giao nhanh)',
  [PriorityLevel.NORMAL]: '📦 Normal (Bình thường)',
  [PriorityLevel.ECONOMY]: '💰 Economy (Ưu tiên giá)',

  // New scale labels (1-10)
  10: '🚨 P10: URGENT (Khẩn cấp tuyệt đối)',
  9: '🔥 P9: EXPRESS HIGH (Express cao nhất)',
  8: '🔥 P8: EXPRESS (Express tiêu chuẩn)',
  7: '🔥 P7: EXPRESS STANDARD (Express cơ bản)',
  6: '📦 P6: NORMAL HIGH (Normal cao)',
  5: '📦 P5: NORMAL (Normal trung bình)',
  4: '📦 P4: NORMAL STANDARD (Normal tiêu chuẩn)',
  3: '💰 P3: ECONOMY HIGH (Economy cao)',
  2: '💰 P2: ECONOMY (Economy tiêu chuẩn)',
  1: '🐢 P1: LOW (Thấp nhất)',
} as const

export type PriorityLevelType = (typeof PriorityLevel)[keyof typeof PriorityLevel]
