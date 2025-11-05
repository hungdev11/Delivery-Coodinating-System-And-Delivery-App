import { AxiosHttpClient } from '@/common/utils/axios'
import {
  LoginForm,
  type LoginRequest,
  type LoginResponse,
  type LogoutRequest,
  type RefreshTokenRequest,
  type TokenValidationResponse
} from './model.type'
import type { IApiResponse } from '@/common/types/http'

const axiosHttpClient = new AxiosHttpClient(import.meta.env.VITE_API_URL)

export const login = async (form: LoginForm) => {
  const response = await axiosHttpClient.post<IApiResponse<LoginResponse>, LoginRequest>(
    '/v1/auth/login',
    {
      username: form.username,
      password: form.password,
      type: 'FRONTEND' // Use FRONTEND type for client/shipper login
    },
  )
  return response
}

export const logout = async (refreshToken: string) => {
  const response = await axiosHttpClient.post<IApiResponse<boolean>, LogoutRequest>(
    '/v1/auth/logout',
    {
      refreshToken: refreshToken
    }
  )
  return response
}

export const validateToken = async (token: string) => {
  const response = await axiosHttpClient.post<IApiResponse<TokenValidationResponse>, Record<string, never>>(
    '/v1/auth/validate-token',
    {},
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  )
  return response
}

export const refreshToken = async (refreshToken: string) => {
  const response = await axiosHttpClient.post<IApiResponse<LoginResponse>, RefreshTokenRequest>(
    '/v1/auth/refresh-token',
    {
      refreshToken: refreshToken
    }
  )
  return response
}
