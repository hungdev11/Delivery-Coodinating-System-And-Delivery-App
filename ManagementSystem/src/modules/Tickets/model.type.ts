/**
 * Ticket Types
 */

export interface TicketDto {
  id: string
  parcelId: string
  deliveryAssignmentId?: string | null
  userId: string
  type: TicketType
  status: TicketStatus
  description?: string | null
  createdAt: string
  updatedAt: string
  resolvedAt?: string | null
  resolvedBy?: string | null
  resolutionNotes?: string | null
}

export type TicketType = 'DELIVERY_FAILED' | 'NOT_RECEIVED'
export type TicketStatus = 'PENDING' | 'RESOLVED' | 'CANCELLED'

export interface CreateTicketRequest {
  parcelId: string
  deliveryAssignmentId?: string | null
  userId: string
  type: TicketType
  description?: string | null
}

export interface UpdateTicketRequest {
  status?: TicketStatus
  resolutionNotes?: string | null
}

export interface ReassignParcelRequest {
  deliveryAssignmentId: string
  notes?: string | null
}

export interface TicketResponse {
  result: TicketDto
  message?: string
}

export interface TicketsResponse {
  result: {
    content: TicketDto[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }
  message?: string
}

export interface TicketStatistics {
  pendingCount: number
  resolvedCount: number
  cancelledCount: number
  totalCount: number
}

export interface TicketStatisticsResponse {
  result: TicketStatistics
  message?: string
}
