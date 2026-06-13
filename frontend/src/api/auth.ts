import { http } from './http'
import type { AuthUser, LoginPayload, TokenResponse } from '../types'

export async function loginSession(payload: LoginPayload): Promise<AuthUser> {
  const { data } = await http.post<AuthUser>('/auth/login', payload)
  return data
}

export async function logoutSession(): Promise<void> {
  await http.post('/auth/logout')
}

export async function meSession(): Promise<AuthUser> {
  const { data } = await http.get<AuthUser>('/auth/me')
  return data
}

export async function loginToken(payload: LoginPayload): Promise<TokenResponse> {
  const { data } = await http.post<TokenResponse>('/token/login', payload)
  return data
}

export async function refreshToken(refreshToken: string): Promise<TokenResponse> {
  const { data } = await http.post<TokenResponse>('/token/refresh', { refreshToken })
  return data
}
