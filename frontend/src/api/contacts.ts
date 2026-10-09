import client from './client'
import type { Contact, ContactRequest } from '../types'

export const getContacts  = ()                              => client.get<Contact[]>('/contacts').then(r => r.data)
export const createContact = (data: ContactRequest)       => client.post<Contact>('/contacts', data).then(r => r.data)
export const updateContact = (id: number, data: ContactRequest) => client.put<Contact>(`/contacts/${id}`, data).then(r => r.data)
export const deleteContact = (id: number)                   => client.delete(`/contacts/${id}`)
