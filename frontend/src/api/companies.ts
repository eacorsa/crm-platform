import client from './client'
import type { Company, CompanyRequest } from '../types'

export const getCompanies   = ()                               => client.get<Company[]>('/companies').then(r => r.data)
export const createCompany  = (data: CompanyRequest)         => client.post<Company>('/companies', data).then(r => r.data)
export const updateCompany  = (id: number, data: CompanyRequest) => client.put<Company>(`/companies/${id}`, data).then(r => r.data)
export const deleteCompany  = (id: number)                     => client.delete(`/companies/${id}`)
