import { axiosInstance } from '@/common/utils/axios'
import type { BaseResponse } from '@/common/types/baseResponse'

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
  const response = await axiosInstance.get<BaseResponse<ServiceHealth>>('/api/v1/health')
  return response.data
}

export const getAllServicesHealth = async (): Promise<BaseResponse<AllServicesHealth>> => {
  const response = await axiosInstance.get<BaseResponse<AllServicesHealth>>('/api/v1/health/all')
  return response.data
}

// OSRM Management APIs
export const getOSRMStatus = async (): Promise<BaseResponse<OSRMStatus>> => {
  const response = await axiosInstance.get<BaseResponse<OSRMStatus>>('/api/v1/osrm/status')
  return response.data
}

export const getOSRMInstanceStatus = async (instanceId: number): Promise<BaseResponse<OSRMInstance>> => {
  const response = await axiosInstance.get<BaseResponse<OSRMInstance>>(`/api/v1/osrm/status/${instanceId}`)
  return response.data
}

export const getOSRMHealth = async (): Promise<BaseResponse<OSRMHealth>> => {
  const response = await axiosInstance.get<BaseResponse<OSRMHealth>>('/api/v1/osrm/health')
  return response.data
}

export const buildOSRMInstance = async (instanceId: number): Promise<BaseResponse<any>> => {
  const response = await axiosInstance.post<BaseResponse<any>>(`/api/v1/osrm/build/${instanceId}`)
  return response.data
}

export const buildAllOSRMInstances = async (): Promise<BaseResponse<any>> => {
  const response = await axiosInstance.post<BaseResponse<any>>('/api/v1/osrm/build-all')
  return response.data
}

export const startOSRMInstance = async (instanceId: number): Promise<BaseResponse<any>> => {
  const response = await axiosInstance.post<BaseResponse<any>>(`/api/v1/osrm/start/${instanceId}`)
  return response.data
}

export const stopOSRMInstance = async (instanceId: number): Promise<BaseResponse<any>> => {
  const response = await axiosInstance.post<BaseResponse<any>>(`/api/v1/osrm/stop/${instanceId}`)
  return response.data
}

export const rollingRestartOSRM = async (): Promise<BaseResponse<any>> => {
  const response = await axiosInstance.post<BaseResponse<any>>('/api/v1/osrm/rolling-restart')
  return response.data
}

export const validateOSRMData = async (instanceId: number): Promise<BaseResponse<any>> => {
  const response = await axiosInstance.get<BaseResponse<any>>(`/api/v1/osrm/validate/${instanceId}`)
  return response.data
}

export const getOSRMBuildHistory = async (instanceId?: number): Promise<BaseResponse<OSRMBuildHistory[]>> => {
  const url = instanceId
    ? `/api/v1/osrm/history/${instanceId}`
    : '/api/v1/osrm/history'
  const response = await axiosInstance.get<BaseResponse<OSRMBuildHistory[]>>(url)
  return response.data
}

export const getOSRMDeploymentStatus = async (): Promise<BaseResponse<OSRMDeploymentStatus>> => {
  const response = await axiosInstance.get<BaseResponse<OSRMDeploymentStatus>>('/api/v1/osrm/deployment')
  return response.data
}
