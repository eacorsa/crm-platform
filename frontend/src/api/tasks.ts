import client from './client'
import type { Task } from '../types'

export const getTasks   = ()                            => client.get<Task[]>('/tasks').then(r => r.data)
export const createTask = (data: Partial<Task>)         => client.post<Task>('/tasks', data).then(r => r.data)
export const updateTask = (id: number, data: Partial<Task>) => client.put<Task>(`/tasks/${id}`, data).then(r => r.data)
export const deleteTask = (id: number)                  => client.delete(`/tasks/${id}`)
