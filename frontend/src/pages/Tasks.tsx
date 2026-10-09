import { useState, useEffect, useCallback } from 'react'
import Modal from '../components/Modal'
import Toast from '../components/Toast'
import { getTasks, createTask, updateTask, deleteTask, updateTaskDone } from '../api/tasks'
import { optionalDate, optionalText } from '../api/forms'
import type { Task } from '../types'

const PRIORITIES: Task['priority'][] = ['NORMAL', 'ALTA', 'URGENTE']
const TYPES: Task['type'][] = ['LLAMADA', 'EMAIL', 'REUNION', 'SEGUIMIENTO', 'OTRO']
const TYPE_ICON: Record<Task['type'], string> = {
  LLAMADA: '📞', EMAIL: '📧', REUNION: '🤝', SEGUIMIENTO: '🔁', OTRO: '📌',
}
const PRIORITY_LABEL: Record<Task['priority'], string> = { NORMAL: 'Normal', ALTA: 'Alta', URGENTE: 'Urgente' }

const EMPTY = { title: '', type: 'LLAMADA' as Task['type'], priority: 'NORMAL' as Task['priority'], dueDate: '', contactName: '', notes: '', done: false }

type Filter = 'all' | 'pending' | 'done'

export default function Tasks() {
  const [tasks, setTasks] = useState<Task[]>([])
  const [filter, setFilter] = useState<Filter>('all')
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState<{ open: boolean; editing: Task | null }>({ open: false, editing: null })
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)
  const [toast, setToast] = useState<{ msg: string; type: 'error' | 'success' } | null>(null)

  const load = useCallback(async () => {
    try { setTasks(await getTasks()) }
    catch { setToast({ msg: 'Error al cargar tareas', type: 'error' }) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  const filtered = tasks.filter(t =>
    filter === 'all' ? true : filter === 'done' ? t.done : !t.done
  )

  function openNew() { setForm(EMPTY); setModal({ open: true, editing: null }) }
  function openEdit(t: Task) {
    setForm({ title: t.title, type: t.type, priority: t.priority, dueDate: t.dueDate ?? '', contactName: t.contactName ?? '', notes: t.notes ?? '', done: t.done })
    setModal({ open: true, editing: t })
  }
  function closeModal() { setModal({ open: false, editing: null }) }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    try {
      const payload = { ...form, title: form.title.trim(), dueDate: optionalDate(form.dueDate),
        contactName: optionalText(form.contactName), notes: optionalText(form.notes) }
      if (modal.editing) {
        await updateTask(modal.editing.id, payload)
        setToast({ msg: 'Tarea actualizada', type: 'success' })
      } else {
        await createTask(payload)
        setToast({ msg: 'Tarea creada', type: 'success' })
      }
      closeModal(); load()
    } catch (err) {
      setToast({ msg: err instanceof Error ? err.message : 'Error al guardar', type: 'error' })
    } finally { setSaving(false) }
  }

  async function toggleDone(t: Task) {
    try { await updateTaskDone(t.id, !t.done); load() }
    catch { setToast({ msg: 'Error al actualizar', type: 'error' }) }
  }

  async function handleDelete(t: Task) {
    if (!confirm(`¿Eliminar "${t.title}"?`)) return
    try { await deleteTask(t.id); setToast({ msg: 'Tarea eliminada', type: 'success' }); load() }
    catch { setToast({ msg: 'Error al eliminar', type: 'error' }) }
  }

  return (
    <div className="page">
      {toast && <Toast message={toast.msg} type={toast.type} onDone={() => setToast(null)} />}

      <div className="page-header">
        <h1>Tareas</h1>
        <button className="btn-primary" onClick={openNew}>+ Nueva tarea</button>
      </div>

      <div className="filter-chips">
        {(['all', 'pending', 'done'] as Filter[]).map(f => (
          <button key={f} className={`chip${filter === f ? ' active' : ''}`} onClick={() => setFilter(f)}>
            {f === 'all' ? 'Todas' : f === 'pending' ? 'Pendientes' : 'Completadas'}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="loading">Cargando…</div>
      ) : filtered.length === 0 ? (
        <div className="empty-state">Sin tareas</div>
      ) : (
        <div className="task-list">
          {filtered.map(t => (
            <div key={t.id} className={`task-item${t.done ? ' task-item--done' : ''}`}>
              <input type="checkbox" aria-label={`Completar ${t.title}`} checked={t.done} onChange={() => toggleDone(t)} className="task-check" />
              <div className="task-info">
                <div className="task-title">
                  {TYPE_ICON[t.type]} {t.title}
                </div>
                <div className="task-meta">
                  {t.contactName && <span>{t.contactName}</span>}
                  {t.dueDate && <span>📅 {t.dueDate}</span>}
                  <span className={`badge priority-${t.priority.toLowerCase()}`}>{PRIORITY_LABEL[t.priority]}</span>
                </div>
              </div>
              <div className="task-actions">
                <button className="btn-icon" aria-label="Editar" onClick={() => openEdit(t)}>✏️</button>
                <button className="btn-icon btn-icon--danger" aria-label="Eliminar" onClick={() => handleDelete(t)}>🗑️</button>
              </div>
            </div>
          ))}
        </div>
      )}

      {modal.open && (
        <Modal title={modal.editing ? 'Editar tarea' : 'Nueva tarea'} onClose={closeModal}>
          <form onSubmit={handleSubmit} className="modal-form">
            <div className="form-group">
              <label htmlFor="tasks-field-1">Título *</label>
              <input id="tasks-field-1" required maxLength={255} value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} />
            </div>
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="tasks-field-2">Tipo</label>
                <select id="tasks-field-2" value={form.type} onChange={e => setForm(f => ({ ...f, type: e.target.value as Task['type'] }))}>
                  {TYPES.map(t => <option key={t} value={t}>{TYPE_ICON[t]} {t}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label htmlFor="tasks-field-3">Prioridad</label>
                <select id="tasks-field-3" value={form.priority} onChange={e => setForm(f => ({ ...f, priority: e.target.value as Task['priority'] }))}>
                  {PRIORITIES.map(p => <option key={p} value={p}>{PRIORITY_LABEL[p]}</option>)}
                </select>
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="tasks-field-4">Fecha límite</label>
                <input id="tasks-field-4" type="date" value={form.dueDate} onChange={e => setForm(f => ({ ...f, dueDate: e.target.value }))} />
              </div>
              <div className="form-group">
                <label htmlFor="tasks-field-5">Contacto</label>
                <input id="tasks-field-5" maxLength={100} value={form.contactName} onChange={e => setForm(f => ({ ...f, contactName: e.target.value }))} />
              </div>
            </div>
            <div className="form-group">
              <label htmlFor="tasks-field-6">Notas</label>
              <textarea id="tasks-field-6" rows={3} maxLength={2000} value={form.notes} onChange={e => setForm(f => ({ ...f, notes: e.target.value }))} />
            </div>
            {modal.editing && (
              <div className="form-group form-group--inline">
                <input type="checkbox" id="done" checked={form.done} onChange={e => setForm(f => ({ ...f, done: e.target.checked }))} />
                <label htmlFor="done">Completada</label>
              </div>
            )}
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
