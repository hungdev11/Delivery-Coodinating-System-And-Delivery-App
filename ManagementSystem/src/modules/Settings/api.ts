/**
 * Settings API Client
 */

import { AxiosHttpClient } from '@/common/utils/axios'
import type {
  GetSettingsResponse,
  GetSettingResponse,
  UpsertSettingRequest,
  UpsertSettingResponse,
  DeleteSettingResponse,
} from './model.type'

const apiClient = new AxiosHttpClient(import.meta.env.VITE_API_URL)

/**
 * Get all settings by group
 */
export const getSettingsByGroup = async (group: string): Promise<GetSettingsResponse> => {
  return apiClient.get<GetSettingsResponse>(`/v1/settings/${group}`)
}

/**
 * Get setting by group and key
 */
export const getSettingByKey = async (
  group: string,
  key: string,
): Promise<GetSettingResponse> => {
  return apiClient.get<GetSettingResponse>(`/v1/settings/${group}/${key}`)
}

/**
 * Upsert (create or update) a setting
 */
export const upsertSetting = async (
  group: string,
  key: string,
  data: UpsertSettingRequest,
  userId?: string,
): Promise<UpsertSettingResponse> => {
  const config = userId
    ? {
        headers: {
          'X-User-Id': userId,
        },
      }
    : undefined

  return apiClient.put<UpsertSettingResponse, UpsertSettingRequest>(
    `/v1/settings/${group}/${key}`,
    data,
    config,
  )
}

/**
 * Delete a setting
 */
export const deleteSetting = async (group: string, key: string): Promise<DeleteSettingResponse> => {
  return apiClient.delete<DeleteSettingResponse>(`/v1/settings/${group}/${key}`)
}
