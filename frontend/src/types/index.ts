export interface AuthResponse {
  token: string
  name: string
  email: string
  role: 'ADMIN' | 'AGENT'
}

interface Resource {
  id: number
  createdAt: string
  updatedAt: string
}

export type ContactStage = 'LEAD' | 'PROSPECTO' | 'CALIFICADO' | 'CLIENTE' | 'PERDIDO'
export interface ContactRequest {
  name: string
  email?: string | null
  phone?: string | null
  company?: string | null
  stage: ContactStage
  source?: string | null
  notes?: string | null
}
export interface Contact extends Resource, ContactRequest {}

export interface CompanyRequest {
  name: string
  industry?: string | null
  website?: string | null
  phone?: string | null
  notes?: string | null
}
export interface Company extends Resource, CompanyRequest {}

export type DealStage = 'NUEVO' | 'CONTACTADO' | 'PROPUESTA' | 'NEGOCIACION' | 'CERRADO' | 'PERDIDO'
export interface DealRequest {
  title: string
  value?: number | null
  contactName?: string | null
  closeDate?: string | null
  stage: DealStage
  notes?: string | null
}
export interface Deal extends Resource, DealRequest {}

export type TaskPriority = 'NORMAL' | 'ALTA' | 'URGENTE'
export type TaskType = 'LLAMADA' | 'EMAIL' | 'REUNION' | 'SEGUIMIENTO' | 'OTRO'
export interface TaskRequest {
  title: string
  contactName?: string | null
  dueDate?: string | null
  priority: TaskPriority
  type: TaskType
  notes?: string | null
  done: boolean
}
export interface Task extends Resource, TaskRequest {}

export interface ApiError {
  error: string
  fields?: Record<string, string>
}
