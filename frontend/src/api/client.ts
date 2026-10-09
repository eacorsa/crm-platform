import axios from 'axios'
import type { ApiError } from '../types'
import { TOKEN_KEY, SESSION_EXPIRED_EVENT, clearSession } from '../auth/session'

export class ApiRequestError extends Error {
  constructor(message: string, public readonly status?: number,
              public readonly fields?: Record<string, string>, public readonly retryAfter?: string) {
    super(message)
    this.name = 'ApiRequestError'
  }
}

const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  headers: { 'Content-Type': 'application/json' },
})

const isLogin = (url?: string) => url?.split('?')[0].replace(/\/$/, '').endsWith('/auth/login') ?? false

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token && !isLogin(config.url)) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use(
  res => res,
  (err: unknown) => {
    if (!axios.isAxiosError<ApiError>(err)) return Promise.reject(err)
    const status = err.response?.status
    // An older request must not clear a newer session after signing in again.
    const token = localStorage.getItem(TOKEN_KEY)
    if (status === 401 && !isLogin(err.config?.url) && token &&
        err.config?.headers?.Authorization === `Bearer ${token}`) {
      clearSession()
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
    }
    const data = err.response?.data
    return Promise.reject(new ApiRequestError(
      typeof data?.error === 'string' ? data.error : 'Error de conexión o respuesta inválida',
      status, data?.fields, err.response?.headers['retry-after'],
    ))
  },
)

export default client
