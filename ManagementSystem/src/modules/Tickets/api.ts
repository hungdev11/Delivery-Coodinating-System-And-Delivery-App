/**
 * Tickets API Client
 *
 * API functions for ticket management
 */

import { AxiosHttpClient } from '@/common/utils/axios'
import type {
  TicketDto,
  TicketResponse,
  TicketsResponse,
  CreateTicketRequest,
  UpdateTicketRequest,
  ReassignParcelRequest,
  TicketStatisticsResponse,
  TicketType,
  TicketStatus,
} from './model.type'

const apiClient = new AxiosHttpClient(import.meta.env.VITE_API_URL)

import type { QueryPayload } from '@/common/types/filter'
import type { PagedData } from '@/common/types/baseResponse'

/**
 * Get tickets with V2 filter system (POST endpoint) - Admin only
 */
export const getTicketsV2 = async (params: QueryPayload): Promise<{ result: PagedData<TicketDto> }> => {
  return apiClient.post<{ result: PagedData<TicketDto> }, QueryPayload>('/v2/tickets', params)
}

/**
 * Get all tickets (paginated) - Admin only
 * @deprecated Use getTicketsV2 instead
 */
export const getAllTickets = async (
  page: number = 0,
  size: number = 20,
): Promise<TicketsResponse> => {
  return apiClient.get<TicketsResponse>(`/v1/tickets?page=${page}&size=${size}`)
}

/**
 * Get tickets by status (paginated) - Admin only
 * @deprecated Use getTicketsV2 instead
 */
export const getTicketsByStatus = async (
  status: TicketStatus,
  page: number = 0,
  size: number = 20,
): Promise<TicketsResponse> => {
  return apiClient.get<TicketsResponse>(`/v1/tickets/status/${status}?page=${page}&size=${size}`)
}

/**
 * Get tickets by type (paginated) - Admin only
 * @deprecated Use getTicketsV2 instead
 */
export const getTicketsByType = async (
  type: TicketType,
  page: number = 0,
  size: number = 20,
): Promise<TicketsResponse> => {
  return apiClient.get<TicketsResponse>(`/v1/tickets/type/${type}?page=${page}&size=${size}`)
}

/**
 * Get tickets by parcel ID
 */
export const getTicketsByParcel = async (parcelId: string): Promise<{ result: TicketDto[] }> => {
  return apiClient.get<{ result: TicketDto[] }>(`/v1/tickets/parcel/${parcelId}`)
}

/**
 * Get tickets by delivery assignment ID
 */
export const getTicketsByAssignment = async (
  assignmentId: string,
): Promise<{ result: TicketDto[] }> => {
  return apiClient.get<{ result: TicketDto[] }>(`/v1/tickets/assignment/${assignmentId}`)
}

/**
 * Get ticket by ID
 */
export const getTicketById = async (id: string): Promise<TicketResponse> => {
  return apiClient.get<TicketResponse>(`/v1/tickets/${id}`)
}

/**
 * Create a new ticket
 */
export const createTicket = async (data: CreateTicketRequest): Promise<TicketResponse> => {
  return apiClient.post<TicketResponse, CreateTicketRequest>('/v1/tickets', data)
}

/**
 * Update ticket status
 */
export const updateTicket = async (
  id: string,
  data: UpdateTicketRequest,
): Promise<TicketResponse> => {
  return apiClient.put<TicketResponse, UpdateTicketRequest>(`/v1/tickets/${id}`, data)
}

/**
 * Resolve ticket (admin action)
 */
export const resolveTicket = async (
  id: string,
  resolutionNotes?: string,
): Promise<TicketResponse> => {
  return apiClient.put<TicketResponse, string | null>(
    `/v1/tickets/${id}/resolve`,
    resolutionNotes || null,
  )
}

/**
 * Cancel ticket (admin action)
 */
export const cancelTicket = async (id: string): Promise<TicketResponse> => {
  return apiClient.put<TicketResponse, null>(`/v1/tickets/${id}/cancel`, null)
}

/**
 * Reassign parcel (admin action)
 */
export const reassignParcel = async (
  id: string,
  data: ReassignParcelRequest,
): Promise<TicketResponse> => {
  return apiClient.put<TicketResponse, ReassignParcelRequest>(`/v1/tickets/${id}/reassign`, data)
}

/**
 * Get ticket statistics - Admin only
 */
export const getTicketStatistics = async (): Promise<TicketStatisticsResponse> => {
  return apiClient.get<TicketStatisticsResponse>('/v1/tickets/statistics')
}
