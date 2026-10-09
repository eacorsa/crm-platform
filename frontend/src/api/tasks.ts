import client from './client'
import type { Task, TaskRequest } from '../types'

export const getTasks   = ()                            => client.get<Task[]>('/tasks').then(r => r.data)
export const createTask = (data: TaskRequest)         => client.post<Task>('/tasks', data).then(r => r.data)
export const updateTask = (id: number, data: TaskRequest) => client.put<Task>(`/tasks/${id}`, data).then(r => r.data)
export const deleteTask = (id: number)                  => client.delete(`/tasks/${id}`)

export const updateTaskDone = (id: number, done: boolean) =>
  client.patch<Task>(`/tasks/${id}/done`, { done }).then(r => r.data)
