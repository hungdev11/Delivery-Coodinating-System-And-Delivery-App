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

export interface OSRMInstance {
  id: number
  name: string
  status: string
  port?: number
  dataPath?: string
  [key: string]: any
}

export interface OSRMStatus {
  instances: OSRMInstance[]
  activeInstance?: number
  totalInstances: number
}

export interface OSRMHealth {
  overallHealthy: boolean
  activeInstance?: number
  instances: Array<{
    instanceId: number
    healthy: boolean
    [key: string]: any
  }>
  healthyCount: number
  totalInstances: number
}

export interface OSRMBuildHistory {
  instanceId: number
  buildDate: string
  status: string
  duration?: number
  [key: string]: any
}

export interface OSRMDeploymentStatus {
  instances: Array<{
    instanceId: number
    status: string
    lastBuild?: string
    [key: string]: any
  }>
  [key: string]: any
}

// Health Check APIs
export const getApiGatewayHealth = async (): Promise<BaseResponse<ServiceHealth>> => {
  return apiClient.get<BaseResponse<ServiceHealth>>('/api/v1/health')
}

export const getAllServicesHealth = async (): Promise<BaseResponse<AllServicesHealth>> => {
  return apiClient.get<BaseResponse<AllServicesHealth>>('/api/v1/health/all')
}

// OSRM Management APIs
export const getOSRMStatus = async (): Promise<BaseResponse<OSRMStatus>> => {
  return apiClient.get<BaseResponse<OSRMStatus>>('/api/v1/osrm/status')
}

export const getOSRMInstanceStatus = async (instanceId: number): Promise<BaseResponse<OSRMInstance>> => {
  return apiClient.get<BaseResponse<OSRMInstance>>(`/api/v1/osrm/status/${instanceId}`)
}

export const getOSRMHealth = async (): Promise<BaseResponse<OSRMHealth>> => {
  return apiClient.get<BaseResponse<OSRMHealth>>('/api/v1/osrm/health')
}

export const buildOSRMInstance = async (instanceId: number): Promise<BaseResponse<any>> => {
  return apiClient.post<BaseResponse<any>>(`/api/v1/osrm/build/${instanceId}`)
}

export const buildAllOSRMInstances = async (): Promise<BaseResponse<any>> => {
  return apiClient.post<BaseResponse<any>>('/api/v1/osrm/build-all')
}

export const startOSRMInstance = async (instanceId: number): Promise<BaseResponse<any>> => {
  return apiClient.post<BaseResponse<any>>(`/api/v1/osrm/start/${instanceId}`)
}

export const stopOSRMInstance = async (instanceId: number): Promise<BaseResponse<any>> => {
  return apiClient.post<BaseResponse<any>>(`/api/v1/osrm/stop/${instanceId}`)
}

export const rollingRestartOSRM = async (): Promise<BaseResponse<any>> => {
  return apiClient.post<BaseResponse<any>>('/api/v1/osrm/rolling-restart')
}

export const validateOSRMData = async (instanceId: number): Promise<BaseResponse<any>> => {
  return apiClient.get<BaseResponse<any>>(`/api/v1/osrm/validate/${instanceId}`)
}

export const getOSRMBuildHistory = async (instanceId?: number): Promise<BaseResponse<OSRMBuildHistory[]>> => {
  const url = instanceId 
    ? `/api/v1/osrm/history/${instanceId}`
    : '/api/v1/osrm/history'
  return apiClient.get<BaseResponse<OSRMBuildHistory[]>>(url)
}

export const getOSRMDeploymentStatus = async (): Promise<BaseResponse<OSRMDeploymentStatus>> => {
  return apiClient.get<BaseResponse<OSRMDeploymentStatus>>('/api/v1/osrm/deployment')
}
