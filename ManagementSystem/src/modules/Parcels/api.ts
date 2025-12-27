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
 * Report parcel not received (create dispute)
 */
export const reportParcelNotReceived = async (parcelId: string): Promise<GetParcelResponse> => {
  return apiClient.put<GetParcelResponse, null>(`/v1/parcels/dispute/${parcelId}`, null)
}

/**
 * Retract dispute (customer confirms received after dispute)
 */
export const retractDispute = async (parcelId: string): Promise<GetParcelResponse> => {
  return apiClient.put<GetParcelResponse, null>(`/v1/parcels/dispute/${parcelId}/retract`, null)
}

/**
 * Admin resolve dispute as misunderstanding
 */
export const resolveDisputeAsMisunderstanding = async (
  parcelId: string,
): Promise<GetParcelResponse> => {
  return apiClient.put<GetParcelResponse, null>(
    `/v1/parcels/resolve-dispute/misunderstanding/${parcelId}`,
    null,
  )
}

/**
 * Admin resolve dispute as shipper fault
 */
export const resolveDisputeAsFault = async (parcelId: string): Promise<GetParcelResponse> => {
  return apiClient.put<GetParcelResponse, null>(
    `/v1/parcels/resolve-dispute/fault/${parcelId}`,
    null,
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
 * Get latest assignment info for a parcel (sessionId, assignmentId, status, deliveryManId)
 * Proxied by API Gateway to session-service.
 */
export interface LatestAssignmentResponse {
  assignmentId: string | null
  sessionId: string | null
  status: string | null
  deliveryManId: string | null
}

export const getLatestAssignmentForParcel = async (
  parcelId: string,
): Promise<LatestAssignmentResponse | null> => {
  const response = await apiClient.get<{
    result: LatestAssignmentResponse | null
    message?: string
  }>(`/v1/assignments/parcels/${parcelId}/latest-assignment`)

  return response.result ?? null
}

/**
 * Auto Seed Parcels API
 */
export interface AutoSeedParcelsResponse {
  result: {
    failedOldParcelsCount: number
    seededParcelsCount: number
    skippedAddressesCount: number
    errorMessage?: string
  }
  success: boolean
  message?: string
}

/**
 * Auto seed parcels:
 * - Fail parcels older than 48 hours
 * - Seed parcels for addresses without parcels in DELAYED/IN_WAREHOUSE/ON_ROUTE status
 * 
 * @param sessionKey Optional session key for progress tracking. If provided, process runs async and returns immediately.
 */
export const autoSeedParcels = async (
  sessionKey?: string,
): Promise<AutoSeedParcelsResponse> => {
  const url = sessionKey
    ? `/v1/parcels/auto-seed?sessionKey=${encodeURIComponent(sessionKey)}`
    : '/v1/parcels/auto-seed'
  return apiClient.post<AutoSeedParcelsResponse, null>(url, null)
}

/**
 * Auto seed parcels response with session key (when sessionKey is provided)
 */
export interface AutoSeedParcelsWithSessionResponse {
  result: {
    sessionKey: string
    status: string
    message: string
  }
  success: boolean
  message?: string
}
