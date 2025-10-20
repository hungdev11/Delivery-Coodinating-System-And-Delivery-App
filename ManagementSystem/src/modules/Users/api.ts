/**
 * Users API Client
 *
 * API functions for user management
 */

import { AxiosHttpClient } from '@/common/utils/axios'
import type {
  GetUsersResponse,
  GetUserResponse,
  CreateUserRequest,
  CreateUserResponse,
  UpdateUserRequest,
  UpdateUserResponse,
  DeleteUserResponse,
} from './model.type'
import type { QueryPayload } from '@/common/types/filter'

const apiClient = new AxiosHttpClient(import.meta.env.VITE_API_URL)

/**
 * Get list of users (paginated) - Legacy endpoint
 */
export const getUsers = async (params: QueryPayload): Promise<GetUsersResponse> => {
  return apiClient.post<GetUsersResponse, QueryPayload>('/v1/users', params)
}

/**
 * Get current user
 */
export const getCurrentUser = async (): Promise<GetUserResponse> => {
  return apiClient.get<GetUserResponse>('/v1/users/me')
}

/**
 * Get user by ID
 */
export const getUserById = async (id: string): Promise<GetUserResponse> => {
  return apiClient.get<GetUserResponse>(`/v1/users/${id}`)
}

/**
 * Get user by username
 */
export const getUserByUsername = async (username: string): Promise<GetUserResponse> => {
  return apiClient.get<GetUserResponse>(`/v1/users/username/${username}`)
}

/**
 * Create new user
 */
export const createUser = async (data: CreateUserRequest): Promise<CreateUserResponse> => {
  return apiClient.post<CreateUserResponse, CreateUserRequest>('/v1/users', data)
}

/**
 * Update user
 */
export const updateUser = async (
  id: string,
  data: UpdateUserRequest,
): Promise<UpdateUserResponse> => {
  return apiClient.put<UpdateUserResponse, UpdateUserRequest>(`/v1/users/${id}`, data)
}

/**
 * Delete user
 */
export const deleteUser = async (id: string): Promise<DeleteUserResponse> => {
  return apiClient.delete<DeleteUserResponse>(`/v1/users/${id}`)
}
