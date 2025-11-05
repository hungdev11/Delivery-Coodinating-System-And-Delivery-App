/**
 * Addresses API Client
 *
 * API functions for address search, by-point lookup, and creation via API Gateway.
 */

import type { IApiResponse } from '@/common/types/http'
import { AxiosHttpClient } from '@/common/utils/axios'

const apiClient = new AxiosHttpClient(import.meta.env.VITE_API_URL)

export interface CreateAddressRequest {
  name: string
  addressText?: string
  lat: number
  lon: number
}

export interface AddressDto {
  id: string
  name: string
  lat: number
  lon: number
  addressText?: string | null
}

export interface SearchExternalSuggestion {
  placeId: string
  name: string
  formattedAddress?: string
  lat: number
  lon: number
  types?: string[]
}

export interface SearchAddressesResult {
  local: AddressDto[]
  external: SearchExternalSuggestion[]
}

export const searchAddresses = async (params: {
  q: string
  limit?: number
}): Promise<IApiResponse<SearchAddressesResult>> => {
  const query: Record<string, string | number> = {
    q: params.q,
  }
  if (params.limit !== undefined) query.limit = params.limit

  const queryString = new URLSearchParams(
    Object.fromEntries(Object.entries(query).map(([k, v]) => [k, String(v)]))
  ).toString()
  return apiClient.get<IApiResponse<SearchAddressesResult>>(`/v1/addresses/search?${queryString}`)
}

export interface ByPointExternalItem {
  placeId: string
  name: string
  lat: number
  lon: number
  formattedAddress?: string
  types?: string[]
}

export interface ByPointResult {
  source: 'local' | 'track-asia'
  local?: AddressDto[]
  external?: ByPointExternalItem[]
}

export const findByPoint = async (params: {
  lat: number
  lon: number
  radius?: number
  limit?: number
}): Promise<IApiResponse<ByPointResult>> => {
  const query: Record<string, string | number> = {
    lat: params.lat,
    lon: params.lon,
  }
  if (params.radius !== undefined) query.radius = params.radius
  if (params.limit !== undefined) query.limit = params.limit

  const queryString = new URLSearchParams(
    Object.fromEntries(Object.entries(query).map(([k, v]) => [k, String(v)]))
  ).toString()
  return apiClient.get<IApiResponse<ByPointResult>>(`/v1/addresses/by-point?${queryString}`)
}

export const createAddress = async (
  data: CreateAddressRequest,
): Promise<{ id: string } & AddressDto> => {
  return apiClient.post<{ id: string } & AddressDto, CreateAddressRequest>('/v1/addresses', data)
}

export interface UpdateAddressRequest {
  name?: string
  addressText?: string | null
  lat?: number
  lon?: number
}

export const updateAddress = async (
  id: string,
  data: UpdateAddressRequest,
): Promise<{ id: string } & AddressDto> => {
  return apiClient.put<{ id: string } & AddressDto, UpdateAddressRequest>(`/v1/addresses/${id}`, data)
}

export const getAddressById = async (id: string): Promise<IApiResponse<AddressDto>> => {
  return apiClient.get<IApiResponse<AddressDto>>(`/v1/addresses/${id}`)
}

export const deleteAddress = async (id: string): Promise<IApiResponse<null>> => {
  return apiClient.delete<IApiResponse<null>>(`/v1/addresses/${id}`)
}
