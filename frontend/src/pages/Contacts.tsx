import { useState, useEffect, useCallback } from 'react'
import Modal from '../components/Modal'
import Toast from '../components/Toast'
import { getContacts, createContact, updateContact, deleteContact } from '../api/contacts'
import { optionalText } from '../api/forms'
import type { Contact } from '../types'

const STAGES: Contact['stage'][] = ['LEAD', 'PROSPECTO', 'CALIFICADO', 'CLIENTE', 'PERDIDO']
const STAGE_LABEL: Record<Contact['stage'], string> = {
  LEAD: 'Lead', PROSPECTO: 'Prospecto', CALIFICADO: 'Calificado', CLIENTE: 'Cliente', PERDIDO: 'Perdido',
}

const EMPTY = { name: '', email: '', phone: '', company: '', stage: 'LEAD' as Contact['stage'], source: '', notes: '' }

export default function Contacts() {
  const [contacts, setContacts] = useState<Contact[]>([])
  const [filtered, setFiltered] = useState<Contact[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState<{ open: boolean; editing: Contact | null }>({ open: false, editing: null })
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)
  const [toast, setToast] = useState<{ msg: string; type: 'error' | 'success' } | null>(null)

  const load = useCallback(async () => {
    try {
      const data = await getContacts()
      setContacts(data)
      setFiltered(data)
    } catch {
      setToast({ msg: 'Error al cargar contactos', type: 'error' })
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  useEffect(() => {
    const q = search.toLowerCase()
    setFiltered(contacts.filter(c =>
      c.name.toLowerCase().includes(q) ||
      c.email?.toLowerCase().includes(q) ||
      c.company?.toLowerCase().includes(q)
    ))
  }, [search, contacts])

  function openNew() {
    setForm(EMPTY)
    setModal({ open: true, editing: null })
  }

  function openEdit(c: Contact) {
    setForm({ name: c.name, email: c.email ?? '', phone: c.phone ?? '', company: c.company ?? '', stage: c.stage, source: c.source ?? '', notes: c.notes ?? '' })
    setModal({ open: true, editing: c })
  }

  function closeModal() { setModal({ open: false, editing: null }) }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    try {
      const payload = { ...form, name: form.name.trim(), email: optionalText(form.email), phone: optionalText(form.phone), company: optionalText(form.company), source: optionalText(form.source), notes: optionalText(form.notes) }
      if (modal.editing) {
        await updateContact(modal.editing.id, payload)
        setToast({ msg: 'Contacto actualizado', type: 'success' })
      } else {
        await createContact(payload)
        setToast({ msg: 'Contacto creado', type: 'success' })
      }
      closeModal()
      load()
    } catch (err) {
      setToast({ msg: err instanceof Error ? err.message : 'Error al guardar', type: 'error' })
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(c: Contact) {
    if (!confirm(`¿Eliminar a ${c.name}?`)) return
    try {
      await deleteContact(c.id)
      setToast({ msg: 'Contacto eliminado', type: 'success' })
      load()
    } catch {
      setToast({ msg: 'Error al eliminar', type: 'error' })
    }
  }

  return (
    <div className="page">
      {toast && <Toast message={toast.msg} type={toast.type} onDone={() => setToast(null)} />}

      <div className="page-header">
        <h1>Contactos</h1>
        <div className="page-header__actions">
          <input className="search-input" placeholder="Buscar…" value={search} onChange={e => setSearch(e.target.value)} />
          <button className="btn-primary" onClick={openNew}>+ Nuevo contacto</button>
        </div>
      </div>

      {loading ? (
        <div className="loading">Cargando…</div>
      ) : (
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Nombre</th><th>Correo</th><th>Teléfono</th><th>Empresa</th><th>Etapa</th><th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr><td colSpan={6} className="empty-row">Sin contactos</td></tr>
              ) : filtered.map(c => (
                <tr key={c.id}>
                  <td><strong>{c.name}</strong></td>
                  <td>{c.email ?? '—'}</td>
                  <td>{c.phone ?? '—'}</td>
                  <td>{c.company ?? '—'}</td>
                  <td><span className={`badge stage-${c.stage.toLowerCase()}`}>{STAGE_LABEL[c.stage]}</span></td>
                  <td className="actions-cell">
                    <button className="btn-icon" aria-label="Editar" onClick={() => openEdit(c)}>✏️</button>
                    <button className="btn-icon btn-icon--danger" aria-label="Eliminar" onClick={() => handleDelete(c)}>🗑️</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {modal.open && (
        <Modal title={modal.editing ? 'Editar contacto' : 'Nuevo contacto'} onClose={closeModal}>
          <form onSubmit={handleSubmit} className="modal-form">
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="contacts-field-1">Nombre *</label>
                <input id="contacts-field-1" required maxLength={150} value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
              </div>
              <div className="form-group">
                <label htmlFor="contacts-field-2">Correo</label>
                <input id="contacts-field-2" type="email" maxLength={180} value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} />
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="contacts-field-3">Teléfono</label>
                <input id="contacts-field-3" maxLength={30} value={form.phone} onChange={e => setForm(f => ({ ...f, phone: e.target.value }))} />
              </div>
              <div className="form-group">
                <label htmlFor="contacts-field-4">Empresa</label>
                <input id="contacts-field-4" maxLength={150} value={form.company} onChange={e => setForm(f => ({ ...f, company: e.target.value }))} />
              </div>
            </div>
            <div className="form-group">
              <label htmlFor="contacts-field-5">Etapa</label>
              <select id="contacts-field-5" value={form.stage} onChange={e => setForm(f => ({ ...f, stage: e.target.value as Contact['stage'] }))}>
                {STAGES.map(s => <option key={s} value={s}>{STAGE_LABEL[s]}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label htmlFor="contacts-field-6">Origen</label>
              <input id="contacts-field-6" maxLength={60} value={form.source} onChange={e => setForm(f => ({ ...f, source: e.target.value }))} />
            </div>
            <div className="form-group">
              <label htmlFor="contacts-field-7">Notas</label>
              <textarea id="contacts-field-7" rows={3} maxLength={2000} value={form.notes} onChange={e => setForm(f => ({ ...f, notes: e.target.value }))} />
            </div>
            <div className="modal-footer">
              <button type="button" className="btn-secondary" onClick={closeModal}>Cancelar</button>
              <button type="submit" className="btn-primary" disabled={saving}>{saving ? 'Guardando…' : 'Guardar'}</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
