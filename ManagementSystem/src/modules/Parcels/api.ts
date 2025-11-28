/**
 * Parcels API Client
 *
 * API functions for parcel management
 */

import { AxiosHttpClient } from '@/common/utils/axios'
import type {
  GetParcelsResponse,
  GetParcelResponse,
  CreateParcelRequest,
  CreateParcelResponse,
  UpdateParcelRequest,
  UpdateParcelResponse,
  DeleteParcelResponse,
  ChangeStatusResponse,
} from './model.type'
import type { QueryPayload } from '@/common/types/filter'

const apiClient = new AxiosHttpClient(import.meta.env.VITE_API_URL)

/**
 * Get list of parcels (paginated) - V2 endpoint with enhanced filtering
 */
export const getParcelsV2 = async (params: QueryPayload): Promise<GetParcelsResponse> => {
  return apiClient.post<GetParcelsResponse, QueryPayload>('/v2/parcels', params)
}

/**
 * Get parcels assigned to the current client (receiver scope)
 */
export const getClientReceivedParcels = async (
  params: QueryPayload,
): Promise<GetParcelsResponse> => {
  return apiClient.post<GetParcelsResponse, QueryPayload>('/v1/client/parcels/received', params)
}

export interface ConfirmParcelRequest {
  confirmationSource: string
  note?: string
}

export const confirmParcelReceived = async (
  parcelId: string,
  data: ConfirmParcelRequest,
): Promise<GetParcelResponse> => {
  return apiClient.post<GetParcelResponse, ConfirmParcelRequest>(
    `/v1/client/parcels/${parcelId}/confirm`,
    data,
  )
}

/**
 * Get parcel by ID
 */
export const getParcelById = async (id: string): Promise<GetParcelResponse> => {
  return apiClient.get<GetParcelResponse>(`/v1/parcels/${id}`)
}

/**
 * Get parcel by code
 */
export const getParcelByCode = async (code: string): Promise<GetParcelResponse> => {
  return apiClient.get<GetParcelResponse>(`/v1/parcels/code/${code}`)
}

/**
 * Create new parcel
 */
export const createParcel = async (data: CreateParcelRequest): Promise<CreateParcelResponse> => {
  return apiClient.post<CreateParcelResponse, CreateParcelRequest>('/v1/parcels', data)
}

/**
 * Update parcel
 */
export const updateParcel = async (
  id: string,
  data: UpdateParcelRequest,
): Promise<UpdateParcelResponse> => {
  return apiClient.put<UpdateParcelResponse, UpdateParcelRequest>(`/v1/parcels/${id}`, data)
}

/**
 * Delete parcel
 */
export const deleteParcel = async (id: string): Promise<DeleteParcelResponse> => {
  return apiClient.delete<DeleteParcelResponse>(`/v1/parcels/${id}`)
}

/**
 * Change parcel status
 */
export const changeParcelStatus = async (
  id: string,
  event: string,
): Promise<ChangeStatusResponse> => {
  return apiClient.put<ChangeStatusResponse, null>(
    `/v1/parcels/change-status/${id}?event=${event}`,
    null,
  )
}

/**
 * Seed Parcels API
 */
export interface SeedParcelsRequest {
  count?: number
  shopId?: string
  clientId?: string
}

export interface SeedParcelsResponse {
  result: {
    successCount: number
    failCount: number
    total: number
    message: string
  }
  success: boolean
  message?: string
}

/**
 * Seed parcels randomly or with specific shop/client
 */
export const seedParcels = async (data?: SeedParcelsRequest): Promise<SeedParcelsResponse> => {
  return apiClient.post<SeedParcelsResponse, SeedParcelsRequest>('/v1/parcels/seed', data || {})
}
