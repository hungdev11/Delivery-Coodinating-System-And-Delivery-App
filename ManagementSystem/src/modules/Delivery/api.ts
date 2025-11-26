/**
 * Delivery API Client
 *
 * API functions for delivery operations (shippers, sessions, assignments)
 */

import { AxiosHttpClient } from '@/common/utils/axios'
import type { QueryPayload } from '@/common/types/filter'
import type { DemoRouteResponse } from '@/modules/Zones/routing.type'
import type {
  GetDeliveryManResponse,
  GetDeliveryManDetailResponse,
  CreateDeliveryManRequest,
  CreateDeliveryManResponse,
  UpdateDeliveryManRequest,
  UpdateDeliveryManResponse,
  GetDeliverySessionsResponse,
  GetDeliverySessionDetailResponse,
  UpdateAssignmentStatusRequestPayload,
  DeliveryAssignmentTaskResponse,
} from './model.type'

const apiClient = new AxiosHttpClient(import.meta.env.VITE_API_URL)

/**
 * =============================
 * Shipper Management Endpoints
 * =============================
 */

export const getDeliveryMen = async (params: QueryPayload): Promise<GetDeliveryManResponse> => {
  return apiClient.post<GetDeliveryManResponse, QueryPayload>('/v1/users/shippers', params)
}

export const getDeliveryMenV2 = async (params: QueryPayload): Promise<GetDeliveryManResponse> => {
  return apiClient.post<GetDeliveryManResponse, QueryPayload>('/v2/users/shippers', params)
}

export const getDeliveryManById = async (id: string): Promise<GetDeliveryManDetailResponse> => {
  return apiClient.get<GetDeliveryManDetailResponse>(`/v1/users/shippers/${id}`)
}

export const createDeliveryMan = async (
  data: CreateDeliveryManRequest,
): Promise<CreateDeliveryManResponse> => {
  return apiClient.post<CreateDeliveryManResponse, CreateDeliveryManRequest>(
    '/v1/users/shippers',
    data,
  )
}

export const updateDeliveryMan = async (
  id: string,
  data: UpdateDeliveryManRequest,
): Promise<UpdateDeliveryManResponse> => {
  return apiClient.put<UpdateDeliveryManResponse, UpdateDeliveryManRequest>(
    `/v1/users/shippers/${id}`,
    data,
  )
}

export const deleteDeliveryMan = async (id: string) => {
  return apiClient.delete(`/v1/users/shippers/${id}`)
}

/**
 * =============================
 * Session & Assignment Endpoints
 * =============================
 */

/**
 * Get delivery sessions with V2 enhanced filtering (Admin/Shipper)
 * Uses v2 endpoint for search functionality
 */
export const getDeliverySessions = async (
  params: QueryPayload,
): Promise<GetDeliverySessionsResponse> => {
  return apiClient.post<GetDeliverySessionsResponse, QueryPayload>(
    '/v2/delivery-sessions',
    params,
  )
}

/**
 * Get delivery sessions for client users
 * Uses v1 client-specific endpoint
 */
export const getDeliverySessionsForClient = async (
  params: QueryPayload,
): Promise<GetDeliverySessionsResponse> => {
  return apiClient.post<GetDeliverySessionsResponse, QueryPayload>(
    '/v1/client/delivery-sessions',
    params,
  )
}

export const getDeliverySessionDetail = async (
  sessionId: string,
): Promise<GetDeliverySessionDetailResponse> => {
  return apiClient.get<GetDeliverySessionDetailResponse>(
    `/v1/delivery-sessions/${sessionId}/with-assignments`,
  )
}

export const updateAssignmentStatus = async (
  sessionId: string,
  assignmentId: string,
  payload: UpdateAssignmentStatusRequestPayload,
) => {
  return apiClient.put(`/v1/delivery-sessions/${sessionId}/assignments/${assignmentId}/status`, payload)
}

export const getSessionDemoRoute = async (sessionId: string): Promise<DemoRouteResponse> => {
  return apiClient.get<DemoRouteResponse>(`/v1/delivery-sessions/${sessionId}/demo-route`)
}

/**
 * =============================
 * Assignment Task Endpoints
 * =============================
 */

export interface AssignmentQueryParams {
  status?: string[]
  page?: number
  size?: number
  createdAtStart?: string
  createdAtEnd?: string
  completedAtStart?: string
  completedAtEnd?: string
}

export const getActiveAssignmentsForDeliveryMan = async (
  deliveryManId: string,
  params: AssignmentQueryParams = {},
): Promise<DeliveryAssignmentTaskResponse> => {
  return apiClient.get<DeliveryAssignmentTaskResponse>(
    `/v1/assignments/session/delivery-man/${deliveryManId}/tasks/today`,
    {
      params: {
        ...params,
        status: params.status,
      },
    },
  )
}

export const getAssignmentHistoryForDeliveryMan = async (
  deliveryManId: string,
  params: AssignmentQueryParams = {},
): Promise<DeliveryAssignmentTaskResponse> => {
  return apiClient.get<DeliveryAssignmentTaskResponse>(
    `/v1/assignments/session/delivery-man/${deliveryManId}/tasks`,
    {
      params: {
        ...params,
        status: params.status,
        createdAtStart: params.createdAtStart,
        createdAtEnd: params.createdAtEnd,
        completedAtStart: params.completedAtStart,
        completedAtEnd: params.completedAtEnd,
      },
    },
  )
}

/**
 * Get active session for a deliveryman
 */
export const getActiveSessionForDeliveryMan = async (deliveryManId: string): Promise<GetDeliverySessionDetailResponse> => {
  return apiClient.get<GetDeliverySessionDetailResponse>(`/v1/sessions/drivers/${deliveryManId}/active`)
}

/**
 * Get all sessions for a deliveryman
 * @param deliveryManId ID of the delivery man
 * @param excludeParcelId (Optional) ParcelId to exclude - sessions containing this parcel will be excluded
 */
export const getAllSessionsForDeliveryMan = async (
  deliveryManId: string,
  excludeParcelId?: string,
): Promise<{ result: DeliverySessionDto[] }> => {
  const params: Record<string, string> = {}
  if (excludeParcelId) {
    params.excludeParcelId = excludeParcelId
  }
  return apiClient.get<{ result: DeliverySessionDto[] }>(
    `/v1/sessions/drivers/${deliveryManId}/sessions`,
    { params },
  )
}

/**
 * Get assignments by session ID
 */
export const getAssignmentsBySessionId = async (
  sessionId: string,
  params: { page?: number; size?: number } = {},
): Promise<DeliveryAssignmentTaskResponse> => {
  return apiClient.get<DeliveryAssignmentTaskResponse>(
    `/v1/assignments/session/${sessionId}/tasks`,
    {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 100,
      },
    },
  )
}
