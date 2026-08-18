export interface AuthResponse {
  token: string
  name:  string
  email: string
  role:  string
}

export interface Contact {
  id:        number
  name:      string
  email?:    string
  phone?:    string
  company?:  string
  stage:     ContactStage
  source?:   string
  notes?:    string
  createdAt: string
  updatedAt: string
}

export type ContactStage = 'LEAD' | 'PROSPECTO' | 'CALIFICADO' | 'CLIENTE' | 'PERDIDO'

export interface Company {
  id:        number
  name:      string
  industry?: string
  website?:  string
  size?:     string
  email?:    string
  phone?:    string
  notes?:    string
  createdAt: string
}

export interface Deal {
  id:           number
  title:        string
  value?:       number
  contactName?: string
  closeDate?:   string
  stage:        DealStage
  notes?:       string
  createdAt:    string
}

export type DealStage = 'NUEVO' | 'CONTACTADO' | 'PROPUESTA' | 'NEGOCIACION' | 'CERRADO' | 'PERDIDO'

export interface Task {
  id:           number
  title:        string
  contactName?: string
  dueDate?:     string
  priority:     TaskPriority
  type:         TaskType
  notes?:       string
  done:         boolean
  createdAt:    string
}

export type TaskPriority = 'NORMAL' | 'ALTA' | 'URGENTE'
export type TaskType     = 'LLAMADA' | 'EMAIL' | 'REUNION' | 'SEGUIMIENTO' | 'OTRO'

export interface ApiError {
  error:   string
  fields?: Record<string, string>
}
