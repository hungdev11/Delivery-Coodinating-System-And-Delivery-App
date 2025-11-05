/**
 * Routing API Client
 *
 * API functions for routing and navigation
 */

import { AxiosHttpClient } from '@/common/utils/axios'
import type {
  DemoRouteRequest,
  DemoRouteResponse,
  RouteRequest,
  RouteResponse,
} from './routing.type'

const apiClient = new AxiosHttpClient(import.meta.env.VITE_API_URL)

/**
 * Calculate demo route with priority-based ordering
 */
export const calculateDemoRoute = async (
  request: DemoRouteRequest
): Promise<DemoRouteResponse> => {
  return apiClient.post<DemoRouteResponse, DemoRouteRequest>(
    '/v1/routing/demo-route',
    request
  )
}

/**
 * Calculate simple route between waypoints
 */
export const calculateRoute = async (request: RouteRequest): Promise<RouteResponse> => {
  return apiClient.post<RouteResponse, RouteRequest>('/v1/routing/route', request)
}

/**
 * Get OSRM status
 */
export const getOSRMStatus = async (): Promise<any> => {
  return apiClient.get('/v1/routing/status')
}
