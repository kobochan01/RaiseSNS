import { apiFetch } from './client'

export type RegisterRequest = {
  username: string
  email: string
  password: string
  displayName: string
}

export type RegisterResponse = {
  id: number
  username: string
  displayName: string
}

export type LoginRequest = {
  email: string
  password: string
}

export type LoginResponse = {
  id: number
  username: string
  displayName: string
  avatarUrl: string | null
}

export function register(request: RegisterRequest): Promise<RegisterResponse> {
  return apiFetch('/auth/register', { method: 'POST', body: JSON.stringify(request) })
}

export function login(request: LoginRequest): Promise<LoginResponse> {
  return apiFetch('/auth/login', { method: 'POST', body: JSON.stringify(request) })
}

export function logout(): Promise<void> {
  return apiFetch('/auth/logout', { method: 'POST' })
}

export function refresh(): Promise<LoginResponse> {
  return apiFetch('/auth/refresh', { method: 'POST' })
}
