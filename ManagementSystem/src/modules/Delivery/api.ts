/**
 * Delivery API Client
 *
 * API functions for delivery operations (shippers, sessions, assignments)
 */

import { AxiosHttpClient } from '@/common/utils/axios'
import type { QueryPayload, FilterGroup } from '@/common/types/filter'
import type { FilterGroupItemV2 } from '@/common/types/filter-v2'
import { convertV1ToV2Filter } from '@/common/utils/filter-v2-converter'
import type { DemoRouteResponse } from '@/modules/Zones/routing.type'
import type {
  GetDeliveryManResponse,
  GetDeliveryManDetailResponse,
  CreateDeliveryManRequest,
  CreateDeliveryManResponse,
  UpdateDeliveryManRequest,
  UpdateDeliveryManResponse,
  GetDeliverySessionsResponse,
  UpdateAssignmentStatusRequestPayload,
  DeliveryAssignmentTaskResponse,
  DeliverySessionDto,
  ManualAssignmentRequest,
  ManualAssignmentResponse,
  AutoAssignmentRequest,
  AutoAssignmentResponse,
  CreateSessionRequest,
  SessionResponse,
} from './model.type'
import type { IApiResponse } from '@/common/types'

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
 * Automatically converts V1 filter format to V2 format if needed
 */
export const getDeliverySessions = async (
  params: QueryPayload,
): Promise<GetDeliverySessionsResponse> => {
  // Convert V1 filter format to V2 if needed (V2 endpoint requires V2 format)
  let v2Params: QueryPayload = { ...params }
  if (params.filters) {
    // Check if it's V1 format (has 'logic' and 'conditions' properties, but no 'type' property)
    const isV1Format = 'logic' in params.filters && 'conditions' in params.filters && !('type' in params.filters)
    if (isV1Format) {
      // Convert V1 FilterGroup to V2 FilterGroupItemV2
      v2Params = {
        ...params,
        filters: convertV1ToV2Filter(params.filters as FilterGroup),
      }
    }
    // If it's already V2 format (has 'type' property), use it as is
  }
  return apiClient.post<GetDeliverySessionsResponse, QueryPayload>('/v2/delivery-sessions', v2Params)
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
): Promise<IApiResponse<DeliverySessionDto>> => {
  return apiClient.get<IApiResponse<DeliverySessionDto>>(
    `/v1/delivery-sessions/${sessionId}/with-assignments`,
  )
}

/**
 * Get enriched session detail with shipper info, parcel details, and proofs
 */
export interface EnrichedSessionResponse {
  id: string
  deliveryManId: string
  status: string
  startTime?: string
  endTime?: string
  totalTasks: number
  completedTasks: number
  failedTasks: number
  deliveryMan?: {
    id?: string
    name?: string
    displayName?: string
    email?: string
    phone?: string
    vehicleType?: string
    capacityKg?: number
  }
  assignments?: Array<{
    id: string
    sessionId?: string
    parcelId: string
    status: string
    failReason?: string | null
    scanedAt?: string
    updatedAt?: string
    parcelInfo?: {
      id?: string
      code?: string
      targetDestination?: string
      value?: number
      deliveryType?: string
      receiverName?: string
      receiverPhoneNumber?: string | null
      weight?: number
      lat?: number
      lon?: number
    }
    proofs?: Array<{
      id: string
      type: string
      mediaUrl: string
      createdAt: string
    }>
  }>
}

export const getEnrichedSessionDetail = async (
  sessionId: string,
): Promise<{ result: EnrichedSessionResponse }> => {
  return apiClient.get<{ result: EnrichedSessionResponse }>(
    `/v1/sessions/${sessionId}/enriched`,
  )
}

export const updateAssignmentStatus = async (
  sessionId: string,
  assignmentId: string,
  payload: UpdateAssignmentStatusRequestPayload,
) => {
  return apiClient.put(
    `/v1/delivery-sessions/${sessionId}/assignments/${assignmentId}/status`,
    payload,
  )
}

export const getSessionDemoRoute = async (
  sessionId: string,
  options?: {
    vehicle?: 'bicycle' | 'car'
    routingType?: 'full' | 'rating-only' | 'blocking-only' | 'base'
  }
): Promise<DemoRouteResponse> => {
  const params: Record<string, string> = {}
  if (options?.vehicle) {
    params.vehicle = options.vehicle
  }
  if (options?.routingType) {
    params.routingType = options.routingType
  }
  return apiClient.get<DemoRouteResponse>(`/v1/delivery-sessions/${sessionId}/demo-route`, { params })
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
export const getActiveSessionForDeliveryMan = async (
  deliveryManId: string,
): Promise<IApiResponse<DeliverySessionDto>> => {
  return apiClient.get<IApiResponse<DeliverySessionDto>>(
    `/v1/sessions/drivers/${deliveryManId}/active`,
  )
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

/**
 * Get delivery proofs by parcel ID
 */
export interface DeliveryProofDto {
  id: string
  type: 'DELIVERED' | 'RETURNED'
  mediaUrl: string
  confirmedBy: string
  createdAt: string
}

export const getProofsByAssignment = async (
  assignmentId: string,
): Promise<{ result: DeliveryProofDto[] }> => {
  return apiClient.get<{ result: DeliveryProofDto[] }>(
    `/v1/delivery-proofs/assignments/${assignmentId}`,
  )
}

export const getProofsByParcel = async (
  parcelId: string,
): Promise<{ result: DeliveryProofDto[] }> => {
  return apiClient.get<{ result: DeliveryProofDto[] }>(
    `/v1/delivery-proofs/parcels/${parcelId}`,
  )
}

/**
 * =============================
 * Admin Assignment & Session Endpoints
 * =============================
 */

/**
 * Create session prepared (CREATED status) for a delivery man
 * Must be called before creating assignments
 */
export const createSessionPrepared = async (
  deliveryManId: string,
): Promise<IApiResponse<DeliverySessionDto>> => {
  return apiClient.post<IApiResponse<DeliverySessionDto>, undefined>(
    `/v1/admin/sessions/prepared/${deliveryManId}`,
    undefined,
  )
}

/**
 * Create a manual assignment for a shipper
 * IMPORTANT: Session must be created first (status CREATED) before calling this
 */
export const createManualAssignment = async (
  data: ManualAssignmentRequest,
): Promise<IApiResponse<ManualAssignmentResponse>> => {
  return apiClient.post<IApiResponse<ManualAssignmentResponse>, ManualAssignmentRequest>(
    '/v1/admin/assignments/manual',
    data,
  )
}

/**
 * Create auto assignment using VRP solver
 * IMPORTANT: Sessions must be created first (status CREATED) for each shipper before calling this
 */
export const createAutoAssignment = async (
  data: AutoAssignmentRequest,
): Promise<IApiResponse<AutoAssignmentResponse>> => {
  return apiClient.post<IApiResponse<AutoAssignmentResponse>, AutoAssignmentRequest>(
    '/v1/admin/assignments/auto',
    data,
  )
}

/**
 * Create session with assignments (admin workflow)
 */
export const createSessionWithAssignments = async (
  data: CreateSessionRequest,
): Promise<IApiResponse<DeliverySessionDto>> => {
  return apiClient.post<IApiResponse<DeliverySessionDto>, CreateSessionRequest>(
    '/v1/admin/sessions',
    data,
  )
}
