import { AUTH_TOKEN_KEY, http } from './http'

export const AUTH_USER_KEY = 'study-flow-user'

export type UserResponse = {
  id: number
  username: string
  email: string
  role: string
  status: string
}

export type LoginRequest = {
  username: string
  password: string
}

export type RegisterRequest = {
  username: string
  email: string
  password: string
}

export type LoginResponse = {
  token: string
  user: UserResponse
}

export function login(request: LoginRequest) {
  return http.post<unknown, LoginResponse>('/auth/login', request)
}

export function register(request: RegisterRequest) {
  return http.post<unknown, UserResponse>('/auth/register', request)
}

export function getCurrentUser() {
  return http.get<unknown, UserResponse>('/users/me')
}

export function saveSession(response: LoginResponse) {
  localStorage.setItem(AUTH_TOKEN_KEY, response.token)
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(response.user))
}

export function clearSession() {
  localStorage.removeItem(AUTH_TOKEN_KEY)
  localStorage.removeItem(AUTH_USER_KEY)
}

export function getStoredUser(): UserResponse | null {
  const rawUser = localStorage.getItem(AUTH_USER_KEY)
  if (!rawUser) {
    return null
  }

  try {
    return JSON.parse(rawUser) as UserResponse
  } catch {
    clearSession()
    return null
  }
}
