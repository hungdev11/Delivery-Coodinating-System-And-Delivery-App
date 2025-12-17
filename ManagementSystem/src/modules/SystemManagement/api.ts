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

export interface OSRMBuildStatus {
  model: string
  currentBuild: {
    buildId: string
    status: string
    startedAt: string | null
  } | null
  latestReady: {
    buildId: string
    completedAt: string
    outputPath: string
  } | null
  latestDeployed: {
    buildId: string
    deployedAt: string
  } | null
}

export interface OSRMStatus {
  models: OSRMModel[]
  allExist: boolean
  existingCount: number
  totalModels: number
  ready: boolean
  buildStatus?: OSRMBuildStatus[]
}

// Health Check APIs
export const getApiGatewayHealth = async (): Promise<BaseResponse<ServiceHealth>> => {
  return apiClient.get<BaseResponse<ServiceHealth>>('/v1/health')
}

export const getAllServicesHealth = async (): Promise<BaseResponse<AllServicesHealth>> => {
  return apiClient.get<BaseResponse<AllServicesHealth>>('/v1/health/all')
}

// OSRM Management APIs (V2 - simplified, delegates to osrm-management-system)
export const getOSRMStatus = async (): Promise<BaseResponse<OSRMStatus>> => {
  return apiClient.get<BaseResponse<OSRMStatus>>('/v1/osrm/status')
}

export const generateV2OSRM = async (): Promise<BaseResponse<any>> => {
  return apiClient.post<BaseResponse<any>>('/v1/osrm/generate-v2')
}

// Extract operations (delegates to osrm-management-system)
export interface ExtractCompleteRequest {
  polyFile?: string
}

export interface ExtractCompleteResponse {
  success: boolean
  outputPath?: string
  error?: string
}

export const extractCompleteOSM = async (request?: ExtractCompleteRequest): Promise<BaseResponse<ExtractCompleteResponse>> => {
  // Always send an object, even if empty, to ensure proper JSON serialization
  return apiClient.post<BaseResponse<ExtractCompleteResponse>>('/v1/osrm/extract/complete', request || {})
}

// Container management (delegates to osrm-management-system)
export interface ContainerStatus {
  model: string
  status: 'stopped' | 'running' | 'starting' | 'stopping' | 'error'
  containerName?: string
  port?: number
  health?: {
    healthy: boolean
    responseTime?: number
    message?: string
  }
}

export interface ContainerActionResult {
  success: boolean
  message?: string
  error?: string
}

export const getOSRMContainerStatus = async (): Promise<BaseResponse<ContainerStatus[]>> => {
  return apiClient.get<BaseResponse<ContainerStatus[]>>('/v1/osrm/containers/status')
}

export const startOSRMContainer = async (model: string): Promise<BaseResponse<ContainerActionResult>> => {
  return apiClient.post<BaseResponse<ContainerActionResult>>(`/v1/osrm/containers/${model}/start`)
}

export const stopOSRMContainer = async (model: string): Promise<BaseResponse<ContainerActionResult>> => {
  return apiClient.post<BaseResponse<ContainerActionResult>>(`/v1/osrm/containers/${model}/stop`)
}

export const restartOSRMContainer = async (model: string): Promise<BaseResponse<ContainerActionResult>> => {
  return apiClient.post<BaseResponse<ContainerActionResult>>(`/v1/osrm/containers/${model}/restart`)
}

export const healthCheckOSRMContainer = async (model: string): Promise<BaseResponse<{ healthy: boolean; message?: string; responseTime?: number }>> => {
  return apiClient.get<BaseResponse<{ healthy: boolean; message?: string; responseTime?: number }>>(`/v1/osrm/containers/${model}/health`)
}
