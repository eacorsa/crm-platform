import client from './client'
import type { Contact } from '../types'

export const getContacts  = ()                              => client.get<Contact[]>('/contacts').then(r => r.data)
export const createContact = (data: Partial<Contact>)       => client.post<Contact>('/contacts', data).then(r => r.data)
export const updateContact = (id: number, data: Partial<Contact>) => client.put<Contact>(`/contacts/${id}`, data).then(r => r.data)
export const deleteContact = (id: number)                   => client.delete(`/contacts/${id}`)
