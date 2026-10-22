import { useEffect, useState } from 'react'
import { getMetrics, type MetricsPeriod } from '../api/metrics'
import { GrowthChart, StageChart, TaskChart } from '../components/MetricCharts'
import { formatCount, formatMoney, percentage } from '../metrics/charts'
import type { MetricsResponse } from '../types'

export default function Metrics() {
  const [months, setMonths] = useState<MetricsPeriod>(6)
  const [refresh, setRefresh] = useState(0)
  const [data, setData] = useState<MetricsResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    let active = true
    setLoading(true); setError(''); setData(null)
    getMetrics(months, controller.signal)
      .then(result => { if (active) setData(result) })
      .catch(err => { if (active) setError(err instanceof Error ? err.message : 'No se pudieron cargar las métricas.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false; controller.abort() }
  }, [months, refresh])

  const summary = data?.summary
  const cards = summary ? [
    { label: 'Contactos', value: formatCount(summary.contacts), note: 'Total de tus registros' },
    { label: 'Empresas', value: formatCount(summary.companies), note: 'Total de tus registros' },
    { label: 'Negocios', value: formatCount(summary.deals), note: 'Todas las etapas' },
    { label: 'Oportunidades abiertas', value: formatMoney(summary.openValue), note: 'Excluye cerrados y perdidos' },
    { label: 'Importe cerrado', value: formatMoney(summary.closedValue), note: 'Valor registrado en etapa Cerrado' },
    { label: 'Tareas completadas', value: percentage(summary.completedTasks, summary.completedTasks + summary.pendingTasks).toFixed(0) + '%',
      note: formatCount(summary.completedTasks) + ' de ' + formatCount(summary.completedTasks + summary.pendingTasks) + ' tareas' },
  ] : []

  return (
    <div className="page metrics-page">
      <div className="page-header metrics-header">
        <div><h1>Métricas</h1><p className="metrics-intro">Tu actividad comercial, en cifras.</p></div>
        <button type="button" className="btn-secondary" disabled={loading} onClick={() => setRefresh(value => value + 1)}>Actualizar</button>
      </div>
      <p className="metrics-scope">Los totales, negocios y tareas muestran todos tus registros actuales. El periodo solo cambia las altas mensuales. Importes en MXN.</p>
      {loading && <div className="loading" role="status">Cargando métricas…</div>}
      {error && <div className="metrics-error" role="alert"><p>{error}</p>
        <button type="button" className="btn-secondary" onClick={() => setRefresh(value => value + 1)}>Reintentar</button></div>}
      {data && <>
        <section className="metrics-cards" aria-label="Resumen de tus registros">
          {cards.map(card => <article key={card.label} className="metrics-kpi"><h2>{card.label}</h2>
            <p className="metrics-kpi-value">{card.value}</p><p className="metrics-kpi-note">{card.note}</p></article>)}
        </section>
        <div className="metrics-grid">
          <section className="metrics-chart"><h2>Negocios por etapa</h2><p>Distribución actual de tus oportunidades.</p><StageChart stages={data.dealStages} /></section>
          <section className="metrics-chart"><h2>Importe por etapa</h2><p>Valor registrado de los negocios, en MXN.</p><StageChart stages={data.dealStages} amount /></section>
          <section className="metrics-chart"><h2>Estado de las tareas</h2><p>Avance sobre todas tus actividades registradas.</p><TaskChart statuses={data.taskStatuses} /></section>
          <section className="metrics-chart metrics-chart--growth"><div className="metrics-growth-heading"><div><h2>Altas mensuales</h2><p>Nuevos contactos y empresas; incluye el mes actual.</p></div>
            <label className="metrics-period">Periodo<select value={months} onChange={event => setMonths(Number(event.target.value) as MetricsPeriod)}>
              <option value={6}>Últimos 6 meses</option><option value={12}>Últimos 12 meses</option>
            </select></label></div><GrowthChart growth={data.growth} /></section>
        </div>
        <p className="metrics-footnote">Fecha de consulta: {data.asOf}. Calendario de Ciudad de México. Las gráficas reflejan el estado actual; no representan un historial de cambios de etapa.</p>
      </>}
    </div>
  )
}
