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

export const getDeliverySessions = async (
  params: QueryPayload,
): Promise<GetDeliverySessionsResponse> => {
  return apiClient.post<GetDeliverySessionsResponse, QueryPayload>(
    '/v1/delivery-sessions/search',
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
