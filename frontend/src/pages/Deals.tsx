import { useState, useEffect, useCallback } from 'react'
import Modal from '../components/Modal'
import Toast from '../components/Toast'
import { getDeals, createDeal, updateDeal, deleteDeal } from '../api/deals'
import type { Deal } from '../types'

const STAGES: Deal['stage'][] = ['NUEVO', 'CONTACTADO', 'PROPUESTA', 'NEGOCIACION', 'CERRADO', 'PERDIDO']
const STAGE_LABEL: Record<Deal['stage'], string> = {
  NUEVO: 'Nuevo', CONTACTADO: 'Contactado', PROPUESTA: 'Propuesta',
  NEGOCIACION: 'Negociación', CERRADO: 'Cerrado', PERDIDO: 'Perdido',
}

const EMPTY = { title: '', value: '', contactName: '', stage: 'NUEVO' as Deal['stage'], closeDate: '', notes: '' }

function formatMXN(val: number) {
  return new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN', maximumFractionDigits: 0 }).format(val)
}

export default function Deals() {
  const [deals, setDeals] = useState<Deal[]>([])
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState<{ open: boolean; editing: Deal | null }>({ open: false, editing: null })
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)
  const [toast, setToast] = useState<{ msg: string; type: 'error' | 'success' } | null>(null)

  const load = useCallback(async () => {
    try { setDeals(await getDeals()) }
    catch { setToast({ msg: 'Error al cargar deals', type: 'error' }) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  function openNew() { setForm(EMPTY); setModal({ open: true, editing: null }) }
  function openEdit(d: Deal) {
    setForm({ title: d.title, value: d.value?.toString() ?? '', contactName: d.contactName ?? '', stage: d.stage, closeDate: d.closeDate ?? '', notes: d.notes ?? '' })
    setModal({ open: true, editing: d })
  }
  function closeModal() { setModal({ open: false, editing: null }) }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    try {
      const payload = { ...form, value: form.value ? parseFloat(form.value) : undefined }
      if (modal.editing) {
        await updateDeal(modal.editing.id, payload)
        setToast({ msg: 'Deal actualizado', type: 'success' })
      } else {
        await createDeal(payload)
        setToast({ msg: 'Deal creado', type: 'success' })
      }
      closeModal(); load()
    } catch (err) {
      setToast({ msg: err instanceof Error ? err.message : 'Error al guardar', type: 'error' })
    } finally { setSaving(false) }
  }

  async function handleDelete(d: Deal) {
    if (!confirm(`¿Eliminar "${d.title}"?`)) return
    try { await deleteDeal(d.id); setToast({ msg: 'Deal eliminado', type: 'success' }); load() }
    catch { setToast({ msg: 'Error al eliminar', type: 'error' }) }
  }

  async function moveStage(d: Deal, stage: Deal['stage']) {
    try { await updateDeal(d.id, { stage }); load() }
    catch { setToast({ msg: 'Error al mover deal', type: 'error' }) }
  }

  const columns = STAGES.map(stage => ({
    stage,
    deals: deals.filter(d => d.stage === stage),
    total: deals.filter(d => d.stage === stage).reduce((s, d) => s + (d.value ?? 0), 0),
  }))

  return (
    <div className="page page--wide">
      {toast && <Toast message={toast.msg} type={toast.type} onDone={() => setToast(null)} />}

      <div className="page-header">
        <h1>Pipeline</h1>
        <button className="btn-primary" onClick={openNew}>+ Nuevo deal</button>
      </div>

      {loading ? (
        <div className="loading">Cargando…</div>
      ) : (
        <div className="kanban">
          {columns.map(col => (
            <div key={col.stage} className="kanban-col">
              <div className="kanban-col__header">
                <span className={`badge stage-deal-${col.stage.toLowerCase()}`}>{STAGE_LABEL[col.stage]}</span>
                <span className="kanban-col__count">{col.deals.length}</span>
              </div>
              {col.total > 0 && <div className="kanban-col__total">{formatMXN(col.total)}</div>}
              <div className="kanban-cards">
                {col.deals.map(d => (
                  <div key={d.id} className="deal-card">
                    <div className="deal-card__title">{d.title}</div>
                    {d.contactName && <div className="deal-card__contact">{d.contactName}</div>}
                    {d.value != null && <div className="deal-card__value">{formatMXN(d.value)}</div>}
                    {d.closeDate && <div className="deal-card__date">Cierre: {d.closeDate}</div>}
                    <div className="deal-card__actions">
                      <button className="btn-icon" onClick={() => openEdit(d)}>✏️</button>
                      <button className="btn-icon btn-icon--danger" onClick={() => handleDelete(d)}>🗑️</button>
                    </div>
                    <div className="deal-card__move">
                      {STAGES.filter(s => s !== col.stage).map(s => (
                        <button key={s} className="btn-move" onClick={() => moveStage(d, s)}>→ {STAGE_LABEL[s]}</button>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {modal.open && (
        <Modal title={modal.editing ? 'Editar deal' : 'Nuevo deal'} onClose={closeModal}>
          <form onSubmit={handleSubmit} className="modal-form">
            <div className="form-row">
              <div className="form-group">
                <label>Título *</label>
                <input required value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} />
              </div>
              <div className="form-group">
                <label>Valor (MXN)</label>
                <input type="number" min="0" step="0.01" value={form.value} onChange={e => setForm(f => ({ ...f, value: e.target.value }))} />
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Contacto</label>
                <input value={form.contactName} onChange={e => setForm(f => ({ ...f, contactName: e.target.value }))} />
              </div>
              <div className="form-group">
                <label>Fecha de cierre</label>
                <input type="date" value={form.closeDate} onChange={e => setForm(f => ({ ...f, closeDate: e.target.value }))} />
              </div>
            </div>
            <div className="form-group">
              <label>Etapa</label>
              <select value={form.stage} onChange={e => setForm(f => ({ ...f, stage: e.target.value as Deal['stage'] }))}>
                {STAGES.map(s => <option key={s} value={s}>{STAGE_LABEL[s]}</option>)}
              </select>
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
