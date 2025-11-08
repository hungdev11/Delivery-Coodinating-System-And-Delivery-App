/**
 * Zones API Client
 *
 * API functions for zone management
 */

import { AxiosHttpClient } from '@/common/utils/axios'
import type {
  GetZonesResponse,
  GetZoneResponse,
  CreateZoneRequest,
  CreateZoneResponse,
  UpdateZoneRequest,
  UpdateZoneResponse,
  DeleteZoneResponse,
  GetCentersResponse,
} from './model.type'
import type { QueryPayload } from '@/common/types/filter'

const apiClient = new AxiosHttpClient(import.meta.env.VITE_API_URL)

/**
 * Check zone service health
 */
export const checkHealth = async (): Promise<any> => {
  return apiClient.get('/v1/zone/health')
}

/**
 * Get list of zones (paginated) - Updated to use POST with PagingRequest
 */
export const getZones = async (params?: {
  page?: number
  size?: number
  search?: string
  centerId?: string
}): Promise<GetZonesResponse> => {
  // Build PagingRequest body according to new API spec
  const requestBody = {
    page: params?.page ?? 0,
    size: params?.size ?? 10,
    search: params?.search,
    filters: params?.centerId ? {
      logic: 'AND' as const,
      conditions: [
        {
          field: 'centerId',
          operator: 'eq' as const,
          value: params.centerId
        }
      ]
    } : undefined,
    sorts: [
      {
        field: 'name',
        direction: 'asc' as const
      }
    ],
    selected: []
  }

  return apiClient.post<GetZonesResponse, any>('/v1/zones', requestBody)
}

/**
 * Get list of zones (paginated) - V2 endpoint with enhanced filtering
 */
export const getZonesV2 = async (params: QueryPayload): Promise<GetZonesResponse> => {
  return apiClient.post<GetZonesResponse, QueryPayload>('/v2/zones', params)
}

/**
 * Get zone by ID
 */
export const getZoneById = async (id: string): Promise<GetZoneResponse> => {
  return apiClient.get<GetZoneResponse>(`/v1/zones/${id}`)
}

/**
 * Get zone by code
 */
export const getZoneByCode = async (code: string): Promise<GetZoneResponse> => {
  return apiClient.get<GetZoneResponse>(`/v1/zones/code/${code}`)
}

/**
 * Get zones by center ID
 */
export const getZonesByCenter = async (centerId: string): Promise<any> => {
  return apiClient.get(`/v1/zones/center/${centerId}`)
}

/**
 * Create new zone
 */
export const createZone = async (data: CreateZoneRequest): Promise<CreateZoneResponse> => {
  return apiClient.post<CreateZoneResponse, CreateZoneRequest>('/v1/zones/create', data)
}

/**
 * Update zone
 */
export const updateZone = async (
  id: string,
  data: UpdateZoneRequest,
): Promise<UpdateZoneResponse> => {
  return apiClient.put<UpdateZoneResponse, UpdateZoneRequest>(`/v1/zones/${id}`, data)
}

/**
 * Delete zone
 */
export const deleteZone = async (id: string): Promise<DeleteZoneResponse> => {
  return apiClient.delete<DeleteZoneResponse>(`/v1/zones/${id}`)
}

/**
 * Get list of centers (for dropdown)
 */
export const getCenters = async (): Promise<GetCentersResponse> => {
  return apiClient.get<GetCentersResponse>('/v1/centers')
}
