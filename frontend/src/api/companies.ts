import client from './client'
import type { Company } from '../types'

export const getCompanies   = ()                               => client.get<Company[]>('/companies').then(r => r.data)
export const createCompany  = (data: Partial<Company>)         => client.post<Company>('/companies', data).then(r => r.data)
export const updateCompany  = (id: number, data: Partial<Company>) => client.put<Company>(`/companies/${id}`, data).then(r => r.data)
export const deleteCompany  = (id: number)                     => client.delete(`/companies/${id}`)
