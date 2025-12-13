import { AxiosHttpClient } from '@/common/utils/axios'
import type { BaseResponse } from '@/common/types/baseResponse'

const apiClient = new AxiosHttpClient(import.meta.env.VITE_API_URL)

export interface ServiceHealth {
  status: string
  timestamp?: string
  error?: string
  [key: string]: any
}

export interface AllServicesHealth {
  overallStatus: string
  services: Record<string, ServiceHealth>
  healthyCount: number
  totalCount: number
  timestamp: string
}

export interface OSRMModel {
  name: string
  exists: boolean
  path: string
}

export interface OSRMStatus {
  models: OSRMModel[]
  allExist: boolean
  existingCount: number
  totalModels: number
  ready: boolean
}

// Health Check APIs
export const getApiGatewayHealth = async (): Promise<BaseResponse<ServiceHealth>> => {
  return apiClient.get<BaseResponse<ServiceHealth>>('/v1/health')
}

export const getAllServicesHealth = async (): Promise<BaseResponse<AllServicesHealth>> => {
  return apiClient.get<BaseResponse<AllServicesHealth>>('/v1/health/all')
}

// OSRM Management APIs (V2 - simplified)
export const getOSRMStatus = async (): Promise<BaseResponse<OSRMStatus>> => {
  return apiClient.get<BaseResponse<OSRMStatus>>('/v1/osrm/status')
}

export const generateV2OSRM = async (): Promise<BaseResponse<any>> => {
  return apiClient.post<BaseResponse<any>>('/v1/osrm/generate-v2')
}
