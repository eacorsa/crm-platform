import client from './client'
import type { Deal } from '../types'

export const getDeals   = ()                            => client.get<Deal[]>('/deals').then(r => r.data)
export const createDeal = (data: Partial<Deal>)         => client.post<Deal>('/deals', data).then(r => r.data)
export const updateDeal = (id: number, data: Partial<Deal>) => client.put<Deal>(`/deals/${id}`, data).then(r => r.data)
export const deleteDeal = (id: number)                  => client.delete(`/deals/${id}`)
