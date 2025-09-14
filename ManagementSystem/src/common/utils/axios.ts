import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { IApiResponse } from '../types'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
const toast = useToast()

export interface IHttpConfig {
  headers?: Record<string, string>
  params?: Record<string, unknown>
  timeout?: number
  withCredentials?: boolean
}

export class AxiosHttpClient {
  private client: AxiosInstance

  constructor(baseURL: string, config?: AxiosRequestConfig) {
    this.client = axios.create({
      baseURL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
      ...config,
    })

    this.setupInterceptors()
  }

  private setupInterceptors(): void {
    // Request interceptor
    this.client.interceptors.request.use(
      (config) => {
        // Add auth token if available
        const token = localStorage.getItem('auth_token')
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
        return config
      },
      (error) => {
        return Promise.reject(error)
      },
    )

    // Response interceptor
    this.client.interceptors.response.use(
      (response: AxiosResponse) => {
        return response.data
      },
      (error) => {
        const apiError: IApiResponse<null> = {
          message: error.response?.data?.message || error.message,
        }
        toast.add({
          title: apiError.message,
          color: 'error',
        })
        return Promise.reject(apiError)
      },
    )
  }

  async get<TResponse>(url: string, config?: IHttpConfig): Promise<TResponse> {
    return this.client.get(url, this.mapConfig(config))
  }

  async post<TResponse, TRequest>(
    url: string,
    data?: TRequest,
    config?: IHttpConfig,
  ): Promise<TResponse> {
    return this.client.post(url, data, this.mapConfig(config))
  }

  async put<TResponse, TRequest>(
    url: string,
    data?: TRequest,
    config?: IHttpConfig,
  ): Promise<TResponse> {
    return this.client.put(url, data, this.mapConfig(config))
  }

  async delete<TResponse>(url: string, config?: IHttpConfig): Promise<TResponse> {
    return this.client.delete(url, this.mapConfig(config))
  }

  async patch<TResponse, TRequest>(
    url: string,
    data?: TRequest,
    config?: IHttpConfig,
  ): Promise<TResponse> {
    return this.client.patch(url, data, this.mapConfig(config))
  }

  private mapConfig(config?: IHttpConfig): AxiosRequestConfig {
    if (!config) return {}

    return {
      headers: config.headers,
      params: config.params,
      timeout: config.timeout,
      withCredentials: config.withCredentials,
    }
  }
}
