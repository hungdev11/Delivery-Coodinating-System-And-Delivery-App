/**
 * User Addresses API Client
 *
 * API functions for user address management
 */

import type { IApiResponse } from '@/common/types/http'
import { AxiosHttpClient } from '@/common/utils/axios'

const apiClient = new AxiosHttpClient(import.meta.env.VITE_API_URL)

export interface UserAddressDto {
  id: string
  userId: string
  destinationId: string
  note?: string | null
  tag?: string | null
  isPrimary: boolean
  createdAt: string
  updatedAt: string
  destinationDetails?: {
    id: string
    name: string
    addressText?: string | null
    lat: number
    lon: number
  }
}

export interface CreateUserAddressRequest {
  destinationId: string
  note?: string
  tag?: string
  isPrimary?: boolean
}

export interface UpdateUserAddressRequest {
  destinationId?: string
  note?: string
  tag?: string
  isPrimary?: boolean
}

/**
 * Get all addresses for current user
 */
export const getMyAddresses = async (): Promise<IApiResponse<UserAddressDto[]>> => {
  return apiClient.get<IApiResponse<UserAddressDto[]>>('/v1/users/me/addresses')
}

/**
 * Get primary address for current user
 */
export const getMyPrimaryAddress = async (): Promise<IApiResponse<UserAddressDto>> => {
  return apiClient.get<IApiResponse<UserAddressDto>>('/v1/users/me/addresses/primary')
}

/**
 * Get address by ID for current user
 */
export const getMyAddress = async (addressId: string): Promise<IApiResponse<UserAddressDto>> => {
  return apiClient.get<IApiResponse<UserAddressDto>>(`/v1/users/me/addresses/${addressId}`)
}

/**
 * Create address for current user
 */
export const createMyAddress = async (
  data: CreateUserAddressRequest,
): Promise<IApiResponse<UserAddressDto>> => {
  return apiClient.post<IApiResponse<UserAddressDto>, CreateUserAddressRequest>(
    '/v1/users/me/addresses',
    data,
  )
}

/**
 * Update address for current user
 */
export const updateMyAddress = async (
  addressId: string,
  data: UpdateUserAddressRequest,
): Promise<IApiResponse<UserAddressDto>> => {
  return apiClient.put<IApiResponse<UserAddressDto>, UpdateUserAddressRequest>(
    `/v1/users/me/addresses/${addressId}`,
    data,
  )
}

/**
 * Delete address for current user
 */
export const deleteMyAddress = async (addressId: string): Promise<IApiResponse<null>> => {
  return apiClient.delete<IApiResponse<null>>(`/v1/users/me/addresses/${addressId}`)
}

/**
 * Set address as primary for current user
 */
export const setMyPrimaryAddress = async (
  addressId: string,
): Promise<IApiResponse<UserAddressDto>> => {
  return apiClient.put<IApiResponse<UserAddressDto>, null>(
    `/v1/users/me/addresses/${addressId}/set-primary`,
    null,
  )
}

// Admin endpoints
/**
 * Get all addresses for a user (Admin)
 */
export const getUserAddresses = async (userId: string): Promise<IApiResponse<UserAddressDto[]>> => {
  return apiClient.get<IApiResponse<UserAddressDto[]>>(`/v1/users/${userId}/addresses`)
}

/**
 * Create address for a user (Admin)
 */
export const createUserAddress = async (
  userId: string,
  data: CreateUserAddressRequest,
): Promise<IApiResponse<UserAddressDto>> => {
  return apiClient.post<IApiResponse<UserAddressDto>, CreateUserAddressRequest>(
    `/v1/users/${userId}/addresses`,
    data,
  )
}

/**
 * Update address for a user (Admin)
 */
export const updateUserAddress = async (
  userId: string,
  addressId: string,
  data: UpdateUserAddressRequest,
): Promise<IApiResponse<UserAddressDto>> => {
  return apiClient.put<IApiResponse<UserAddressDto>, UpdateUserAddressRequest>(
    `/v1/users/${userId}/addresses/${addressId}`,
    data,
  )
}

/**
 * Delete address for a user (Admin)
 */
export const deleteUserAddress = async (
  userId: string,
  addressId: string,
): Promise<IApiResponse<null>> => {
  return apiClient.delete<IApiResponse<null>>(`/v1/users/${userId}/addresses/${addressId}`)
}
