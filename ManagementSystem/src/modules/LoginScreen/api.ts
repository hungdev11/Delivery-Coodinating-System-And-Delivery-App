import { AxiosHttpClient } from '@/common/utils/axios'
import { LoginForm, type TokenPayload } from './model.type'
import type { IApiResponse } from '@/common/types'

const axiosHttpClient = new AxiosHttpClient(import.meta.env.VITE_API_URL)

export const login = async (form: LoginForm) => {
  const response = await axiosHttpClient.post<IApiResponse<TokenPayload>, LoginForm>(
    '/v1/auth/login',
    form,
  )
  return response
}

export const logout = async () => {
  const response = await axiosHttpClient.post('/v1/auth/logout')
  return response
}
