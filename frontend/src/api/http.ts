import axios, { type AxiosError } from 'axios'
import { clearSession } from './auth'

export type ApiResponse<T> = {
  code: number
  message: string
  data: T
}

export const AUTH_TOKEN_KEY = 'study-flow-token'

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

const LOGIN_PATH = '/login'

function isApiResponse(value: unknown): value is ApiResponse<unknown> {
  return (
    typeof value === 'object' &&
    value !== null &&
    'code' in value &&
    'message' in value &&
    'data' in value
  )
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

http.interceptors.response.use(
  (response) => {
    if (isApiResponse(response.data)) {
      if (response.data.code !== 0) {
        return Promise.reject(new Error(response.data.message || '请求失败'))
      }

      return response.data.data
    }

    return response.data
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    if (
      error.response &&
      (error.response.status === 401 || error.response.status === 403)
    ) {
      clearSession()
      if (window.location.pathname !== LOGIN_PATH) {
        window.location.href = `${LOGIN_PATH}?redirect=${encodeURIComponent(window.location.pathname)}`
      }
    }

    const message =
      error.response?.data?.message || error.message || '网络请求失败'
    return Promise.reject(new Error(message))
  },
)
