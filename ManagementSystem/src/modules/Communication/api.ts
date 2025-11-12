/**
 * Communication API Client
 *
 * API functions for communication (conversations, messages, proposals)
 */

import { AxiosHttpClient } from '@/common/utils/axios'
import type {
  GetConversationsResponse,
  GetConversationResponse,
  GetMessagesResponse,
  CreateProposalRequest,
  CreateProposalResponse,
  ProposalResponseRequest,
  RespondToProposalResponse,
  GetProposalConfigsResponse,
  ProposalConfigDTO,
  CreateProposalConfigResponse,
  UpdateProposalConfigResponse,
  DeleteProposalConfigResponse,
  ProposalTypeConfig,
} from './model.type'

const apiClient = new AxiosHttpClient(import.meta.env.VITE_API_URL)

/**
 * Get conversations for a user
 */
export const getConversations = async (userId: string): Promise<GetConversationsResponse | ConversationResponse[]> => {
  return apiClient.get<GetConversationsResponse | ConversationResponse[]>(`/v1/conversations/user/${userId}`)
}

/**
 * Find or create conversation between two users
 */
export const getConversationByUsers = async (
  userId1: string,
  userId2: string,
): Promise<GetConversationResponse> => {
  return apiClient.get<GetConversationResponse>(
    `/v1/conversations/find-by-users?user1=${userId1}&user2=${userId2}`,
  )
}

/**
 * Get messages for a conversation
 */
export const getMessages = async (
  conversationId: string,
  userId: string,
  page: number = 0,
  size: number = 30,
  sort: string = 'sentAt',
  direction: string = 'DESC',
): Promise<GetMessagesResponse> => {
  return apiClient.get<GetMessagesResponse>(
    `/v1/conversations/${conversationId}/messages?userId=${userId}&page=${page}&size=${size}&sort=${sort}&direction=${direction}`,
  )
}

/**
 * Create a new proposal
 */
export const createProposal = async (
  data: CreateProposalRequest,
): Promise<CreateProposalResponse> => {
  return apiClient.post<CreateProposalResponse, CreateProposalRequest>('/v1/proposals', data)
}

/**
 * Respond to a proposal
 */
export const respondToProposal = async (
  proposalId: string,
  userId: string,
  data: ProposalResponseRequest,
): Promise<RespondToProposalResponse> => {
  return apiClient.post<RespondToProposalResponse, ProposalResponseRequest>(
    `/v1/proposals/${proposalId}/respond?userId=${userId}`,
    data,
  )
}

/**
 * Get available proposal configs for roles
 * Backend returns direct array (not wrapped in IApiResponse)
 * AxiosHttpClient already unwraps response.data, so we get the array directly
 */
export const getAvailableConfigs = async (
  roles: string[],
): Promise<ProposalTypeConfig[]> => {
  const rolesParam = roles.join(',')
  // Backend returns ResponseEntity<List<ProposalTypeConfig>> which becomes direct array
  const response = await apiClient.get<ProposalTypeConfig[]>(
    `/v1/proposals/available-configs?roles=${rolesParam}`,
  )
  // AxiosHttpClient returns response.data, which is the direct array from backend
  return Array.isArray(response) ? response : []
}

/**
 * Get all proposal configs (Admin only)
 */
export const getProposalConfigs = async (): Promise<GetProposalConfigsResponse> => {
  return apiClient.get<GetProposalConfigsResponse>('/v1/admin/proposals/configs')
}

/**
 * Create a proposal config (Admin only)
 */
export const createProposalConfig = async (
  data: ProposalConfigDTO,
): Promise<CreateProposalConfigResponse> => {
  return apiClient.post<CreateProposalConfigResponse, ProposalConfigDTO>(
    '/v1/admin/proposals/configs',
    data,
  )
}

/**
 * Update a proposal config (Admin only)
 */
export const updateProposalConfig = async (
  configId: string,
  data: ProposalConfigDTO,
): Promise<UpdateProposalConfigResponse> => {
  return apiClient.put<UpdateProposalConfigResponse, ProposalConfigDTO>(
    `/v1/admin/proposals/configs/${configId}`,
    data,
  )
}

/**
 * Delete a proposal config (Admin only)
 */
export const deleteProposalConfig = async (
  configId: string,
): Promise<DeleteProposalConfigResponse> => {
  return apiClient.delete<DeleteProposalConfigResponse>(`/v1/admin/proposals/configs/${configId}`)
}
