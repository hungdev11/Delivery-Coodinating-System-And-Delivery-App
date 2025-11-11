/**
 * Communication Module Types
 *
 * Type definitions for Communication module
 */

import type { IApiResponse } from '@/common/types'

/**
 * Content Type
 */
export type ContentType = 'TEXT' | 'PROPOSAL'

/**
 * Proposal Status
 */
export type ProposalStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED'

/**
 * Proposal Type
 */
export type ProposalType = 'CONFIRM_REFUSAL' | 'RESCHEDULE' | 'CANCEL_ORDER' | 'OTHER'

/**
 * Proposal Action Type
 */
export type ProposalActionType = 'ACCEPT_DECLINE' | 'DATE_PICKER' | 'TEXT_INPUT' | 'CHOICE'

/**
 * Conversation Response
 */
export interface ConversationResponse {
  conversationId: string
  partnerId: string
  partnerName: string
  partnerAvatar?: string | null
  partnerUsername?: string | null // Add username for display
  isOnline?: boolean | null // Online status (null if unavailable)
  lastMessageTime?: string | null // ISO date string of last message time
}

/**
 * Message Response
 */
export interface MessageResponse {
  id: string
  conversationId?: string // Optional for backward compatibility
  senderId: string
  content: string
  sentAt: string
  type: ContentType
  status?: 'SENT' | 'DELIVERED' | 'READ' // Message status
  deliveredAt?: string // When message was delivered
  readAt?: string // When message was read
  proposal?: InteractiveProposalResponseDTO
}

/**
 * Interactive Proposal Response DTO
 */
export interface InteractiveProposalResponseDTO {
  id: string
  type: string
  status: string
  proposerId: string
  recipientId: string
  data: string // JSON string
  actionType: string
  resultData?: string | null
}

/**
 * Create Proposal Request
 */
export interface CreateProposalRequest {
  type: ProposalType
  recipientId: string
  data: string // JSON string
}

/**
 * Proposal Response Request
 */
export interface ProposalResponseRequest {
  resultData: string
}

/**
 * Proposal Type Config
 */
export interface ProposalTypeConfig {
  id: string
  type: ProposalType
  requiredRole: string
  actionType: ProposalActionType
  template: string // JSON template
  description?: string
}

/**
 * Proposal Config DTO (for create/update)
 */
export interface ProposalConfigDTO {
  type: ProposalType
  requiredRole: string
  actionType: ProposalActionType
  template: string
  description?: string
}

/**
 * Page Response
 */
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

/**
 * Chat Message Payload (for WebSocket)
 */
export interface ChatMessagePayload {
  content: string
  recipientId: string
  conversationId?: string // Optional conversation ID for tracking
}

/**
 * API Responses
 */
export type GetConversationsResponse = IApiResponse<ConversationResponse[]>
export type GetConversationResponse = IApiResponse<ConversationResponse>
export type GetMessagesResponse = IApiResponse<PageResponse<MessageResponse>>
export type CreateProposalResponse = IApiResponse<InteractiveProposalResponseDTO>
export type RespondToProposalResponse = IApiResponse<InteractiveProposalResponseDTO>
export type GetAvailableConfigsResponse = IApiResponse<ProposalTypeConfig[]>
export type GetProposalConfigsResponse = IApiResponse<ProposalTypeConfig[]>
export type CreateProposalConfigResponse = IApiResponse<ProposalTypeConfig>
export type UpdateProposalConfigResponse = IApiResponse<ProposalTypeConfig>
export type DeleteProposalConfigResponse = IApiResponse<null>
