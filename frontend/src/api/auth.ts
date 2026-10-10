import client from './client'
import type { AuthResponse } from '../types'

export const login = (email: string, password: string) =>
  client.post<AuthResponse>('/auth/login', { email, password }).then(r => r.data)

interface AuthMessage { message: string }

export const forgotPassword = (email: string) =>
  client.post<AuthMessage>('/auth/forgot-password', { email: email.trim() }).then(r => r.data)

export const resetPassword = (token: string, password: string) =>
  client.post<AuthMessage>('/auth/reset-password', { token, password }).then(r => r.data)
