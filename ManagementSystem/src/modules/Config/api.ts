/**
 * Config API Client
 *
 * API functions for fetching public configuration from API Gateway.
 */

import type { IApiResponse } from '@/common/types/http'
import { AxiosHttpClient } from '@/common/utils/axios'

const apiClient = new AxiosHttpClient(import.meta.env.VITE_API_URL)

export interface PublicConfig {
  secrets: Record<string, string>
  version: string
}

export const getPublicConfig = async (): Promise<IApiResponse<PublicConfig>> => {
  return apiClient.get<IApiResponse<PublicConfig>>('/v1/config/public')
}
