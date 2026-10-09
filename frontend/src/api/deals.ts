import client from './client'
import type { Deal, DealRequest } from '../types'

export const getDeals   = ()                            => client.get<Deal[]>('/deals').then(r => r.data)
export const createDeal = (data: DealRequest)         => client.post<Deal>('/deals', data).then(r => r.data)
export const updateDeal = (id: number, data: DealRequest) => client.put<Deal>(`/deals/${id}`, data).then(r => r.data)
export const deleteDeal = (id: number)                  => client.delete(`/deals/${id}`)

export const updateDealStage = (id: number, stage: Deal['stage']) =>
  client.patch<Deal>(`/deals/${id}/stage`, { stage }).then(r => r.data)
