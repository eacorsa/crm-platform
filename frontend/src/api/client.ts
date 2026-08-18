import axios from 'axios'
import type { ApiError } from '../types'

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'

const client = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

// Adjuntar token JWT en cada request
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('crm_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Redirigir al login si el token expiró
client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('crm_token')
      localStorage.removeItem('crm_user')
      window.location.href = '/login'
    }
    const data: ApiError = err.response?.data ?? { error: 'Error de conexión' }
    return Promise.reject(new Error(data.error))
  }
)

export default client
