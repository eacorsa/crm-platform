import { useState, useEffect, useCallback } from 'react'
import Modal from '../components/Modal'
import Toast from '../components/Toast'
import { getCompanies, createCompany, updateCompany, deleteCompany } from '../api/companies'
import type { Company } from '../types'

const EMPTY = { name: '', industry: '', website: '', phone: '', notes: '' }

export default function Companies() {
  const [companies, setCompanies] = useState<Company[]>([])
  const [filtered, setFiltered] = useState<Company[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState<{ open: boolean; editing: Company | null }>({ open: false, editing: null })
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)
  const [toast, setToast] = useState<{ msg: string; type: 'error' | 'success' } | null>(null)

  const load = useCallback(async () => {
    try {
      const data = await getCompanies()
      setCompanies(data)
      setFiltered(data)
    } catch {
      setToast({ msg: 'Error al cargar empresas', type: 'error' })
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  useEffect(() => {
    const q = search.toLowerCase()
    setFiltered(companies.filter(c =>
      c.name.toLowerCase().includes(q) ||
      c.industry?.toLowerCase().includes(q)
    ))
  }, [search, companies])

  function openNew() { setForm(EMPTY); setModal({ open: true, editing: null }) }
  function openEdit(c: Company) {
    setForm({ name: c.name, industry: c.industry ?? '', website: c.website ?? '', phone: c.phone ?? '', notes: c.notes ?? '' })
    setModal({ open: true, editing: c })
  }
  function closeModal() { setModal({ open: false, editing: null }) }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    try {
      if (modal.editing) {
        await updateCompany(modal.editing.id, form)
        setToast({ msg: 'Empresa actualizada', type: 'success' })
      } else {
        await createCompany(form)
        setToast({ msg: 'Empresa creada', type: 'success' })
      }
      closeModal(); load()
    } catch (err) {
      setToast({ msg: err instanceof Error ? err.message : 'Error al guardar', type: 'error' })
    } finally { setSaving(false) }
  }

  async function handleDelete(c: Company) {
    if (!confirm(`¿Eliminar ${c.name}?`)) return
    try {
      await deleteCompany(c.id)
      setToast({ msg: 'Empresa eliminada', type: 'success' })
      load()
    } catch { setToast({ msg: 'Error al eliminar', type: 'error' }) }
  }

  return (
    <div className="page">
      {toast && <Toast message={toast.msg} type={toast.type} onDone={() => setToast(null)} />}

      <div className="page-header">
        <h1>Empresas</h1>
        <div className="page-header__actions">
          <input className="search-input" placeholder="Buscar…" value={search} onChange={e => setSearch(e.target.value)} />
          <button className="btn-primary" onClick={openNew}>+ Nueva empresa</button>
        </div>
      </div>

      {loading ? (
        <div className="loading">Cargando…</div>
      ) : (
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr><th>Empresa</th><th>Industria</th><th>Sitio web</th><th>Teléfono</th><th>Acciones</th></tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr><td colSpan={5} className="empty-row">Sin empresas</td></tr>
              ) : filtered.map(c => (
                <tr key={c.id}>
                  <td><strong>{c.name}</strong></td>
                  <td>{c.industry ?? '—'}</td>
                  <td>{c.website ? <a href={c.website} target="_blank" rel="noreferrer">{c.website}</a> : '—'}</td>
                  <td>{c.phone ?? '—'}</td>
                  <td className="actions-cell">
                    <button className="btn-icon" onClick={() => openEdit(c)}>✏️</button>
                    <button className="btn-icon btn-icon--danger" onClick={() => handleDelete(c)}>🗑️</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {modal.open && (
        <Modal title={modal.editing ? 'Editar empresa' : 'Nueva empresa'} onClose={closeModal}>
          <form onSubmit={handleSubmit} className="modal-form">
            <div className="form-row">
              <div className="form-group">
                <label>Nombre *</label>
                <input required value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
              </div>
              <div className="form-group">
                <label>Industria</label>
                <input value={form.industry} onChange={e => setForm(f => ({ ...f, industry: e.target.value }))} />
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Sitio web</label>
                <input type="url" value={form.website} onChange={e => setForm(f => ({ ...f, website: e.target.value }))} />
              </div>
              <div className="form-group">
                <label>Teléfono</label>
                <input value={form.phone} onChange={e => setForm(f => ({ ...f, phone: e.target.value }))} />
              </div>
            </div>
            <div className="form-group">
              <label>Notas</label>
              <textarea rows={3} value={form.notes} onChange={e => setForm(f => ({ ...f, notes: e.target.value }))} />
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
