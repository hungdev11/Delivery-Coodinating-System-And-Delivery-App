import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { IApiResponse } from '../types'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { useCookies } from '@vueuse/integrations/useCookies'
import { isTokenExpired } from './jwtDecode/jwtDecode.util'
import { removeToken, shouldRefreshToken } from '../guards/roleGuard.guard'

const toast = useToast()
const cookie = useCookies(['token'])

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
      // Remove timeout limit for web (allow long-running requests)
      // timeout: 10000, // Commented out to allow unlimited timeout
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
        // Add auth token if available and not expired
        const token = cookie.get('jwt_token')
        if (token) {
          // Check if token is expired before sending request
          if (isTokenExpired(token)) {
            console.warn('Token is expired, logging out user')
            removeToken()
            return Promise.reject(new Error('Token expired'))
          }

          // Check if token needs refresh (optional warning)
          if (shouldRefreshToken()) {
            console.warn('Token will expire soon, consider refreshing')
            // You could trigger a token refresh here if needed
          }

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
        // Handle 401 Unauthorized responses (token expired on server)
        if (error.response?.status === 401) {
          console.warn('Received 401 Unauthorized, token may be expired')
          removeToken()
          return Promise.reject(new Error('Authentication failed'))
        }

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
