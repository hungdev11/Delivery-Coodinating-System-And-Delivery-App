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
 * Get list of users (paginated) - V1 endpoint
 */
export const getUsers = async (params: QueryPayload): Promise<GetUsersResponse> => {
  return apiClient.post<GetUsersResponse, QueryPayload>('/v1/users', params)
}

/**
 * Get list of users (paginated) - V2 endpoint
 */
export const getUsersV2 = async (params: QueryPayload): Promise<GetUsersResponse> => {
  return apiClient.post<GetUsersResponse, QueryPayload>('/v2/users', params)
}

/**
 * Get current user - V1 endpoint
 */
export const getCurrentUser = async (): Promise<GetUserResponse> => {
  return apiClient.get<GetUserResponse>('/v1/users/me')
}

/**
 * Get current user - V2 endpoint
 */
export const getCurrentUserV2 = async (): Promise<GetUserResponse> => {
  return apiClient.get<GetUserResponse>('/v2/users/me')
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

/**
 * User Address API
 */

export interface UserAddressDto {
  id: string
  userId: string
  destinationId: string
  note?: string | null
  tag?: string | null
  isPrimary: boolean
  createdAt?: string
  updatedAt?: string
}

export interface GetUserAddressesResponse {
  result: UserAddressDto[]
  success: boolean
  message?: string
}

export interface GetUserPrimaryAddressResponse {
  result: UserAddressDto
  success: boolean
  message?: string
}

/**
 * Get primary address for a user
 */
export const getUserPrimaryAddress = async (userId: string): Promise<GetUserPrimaryAddressResponse> => {
  return apiClient.get<GetUserPrimaryAddressResponse>(`/v1/users/${userId}/addresses/primary`)
}

/**
 * Get all addresses for a user
 */
export const getUserAddresses = async (userId: string): Promise<GetUserAddressesResponse> => {
  return apiClient.get<GetUserAddressesResponse>(`/v1/users/${userId}/addresses`)
}

export interface CreateUserAddressRequest {
  destinationId: string
  note?: string
  tag?: string
  isPrimary?: boolean
}

export interface CreateUserAddressResponse {
  result: UserAddressDto
  success: boolean
  message?: string
}

/**
 * Create address for a user (admin endpoint)
 */
export const createUserAddress = async (
  userId: string,
  data: CreateUserAddressRequest,
): Promise<CreateUserAddressResponse> => {
  return apiClient.post<CreateUserAddressResponse, CreateUserAddressRequest>(
    `/v1/users/${userId}/addresses`,
    data,
  )
}
