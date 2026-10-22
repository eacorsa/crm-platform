import { useId } from 'react'
import type { MetricsResponse } from '../types'
import { countAxis, formatCount, formatMoney, monthLabel, percentage, pointX, STAGE_COLORS, STAGE_LABELS } from '../metrics/charts'

export function StageChart({ stages, amount = false }: { stages: MetricsResponse['dealStages']; amount?: boolean }) {
  const values = stages.map(stage => amount ? stage.value : stage.count)
  const maximum = Math.max(0, ...values)
  const format = amount ? formatMoney : formatCount
  return (
    <>
      {maximum === 0 && <p className="metrics-empty">{amount ? 'No hay importes registrados.' : 'No hay negocios registrados.'}</p>}
      <ul className="metrics-bars" aria-label={amount ? 'Importe de negocios por etapa en pesos mexicanos' : 'Cantidad de negocios por etapa'}>
        {stages.map((stage, index) => (
          <li key={stage.stage}>
            <div className="metrics-bar-label"><span>{STAGE_LABELS[stage.stage]}</span><strong>{format(values[index])}</strong></div>
            <div className="metrics-bar-track" aria-hidden="true"><div style={{ width: percentage(values[index], maximum) + '%', background: STAGE_COLORS[stage.stage] }} /></div>
          </li>
        ))}
      </ul>
    </>
  )
}

export function TaskChart({ statuses }: { statuses: MetricsResponse['taskStatuses'] }) {
  const done = statuses.find(status => status.done)?.count ?? 0
  const pending = statuses.find(status => !status.done)?.count ?? 0
  const total = done + pending
  const progress = percentage(done, total)
  const titleId = useId()
  return (
    <div className="metrics-tasks">
      <svg viewBox="0 0 200 200" role="img" aria-labelledby={titleId} className="metrics-donut">
        <title id={titleId}>{formatCount(done) + " tareas completadas y " + formatCount(pending) + " pendientes"}</title>
        <circle cx="100" cy="100" r="74" fill="none" stroke="#e4e4ea" strokeWidth="22" />
        {total > 0 && <>
          <circle cx="100" cy="100" r="74" fill="none" stroke="#c2410c" strokeWidth="22" />
          <circle cx="100" cy="100" r="74" fill="none" stroke="#15803d" strokeWidth="22"
            pathLength="100" strokeDasharray={progress + ' ' + (100 - progress)} transform="rotate(-90 100 100)" />
        </>}
        <text x="100" y="99" textAnchor="middle" className="metrics-donut-value">{progress.toFixed(0)}%</text>
        <text x="100" y="122" textAnchor="middle" className="metrics-donut-label">completadas</text>
      </svg>
      <ul className="metrics-legend">
        <li><span className="metrics-dot" style={{ background: '#15803d' }} />Completadas <strong>{formatCount(done)}</strong></li>
        <li><span className="metrics-dot" style={{ background: '#c2410c' }} />Pendientes <strong>{formatCount(pending)}</strong></li>
      </ul>
      {total === 0 && <p className="metrics-empty">No hay tareas registradas.</p>}
    </div>
  )
}

export function GrowthChart({ growth }: { growth: MetricsResponse['growth'] }) {
  const titleId = useId()
  const maximum = countAxis(growth.flatMap(month => [month.contacts, month.companies]))
  const y = (count: number) => 206 - count / maximum * 176
  const series = [{ key: 'contacts' as const, label: 'Contactos', color: '#c2410c' },
    { key: 'companies' as const, label: 'Empresas', color: '#2563eb' }]
  const hasData = growth.some(month => month.contacts > 0 || month.companies > 0)
  return (
    <>
      {!hasData && <p className="metrics-empty">No hay altas en el periodo seleccionado.</p>}
      <div className="metrics-growth-scroll">
        <svg viewBox="0 0 660 258" role="img" aria-labelledby={titleId} className="metrics-growth">
          <title id={titleId}>Altas mensuales de contactos y empresas. Los valores están disponibles en la tabla de datos.</title>
          {[0, 1, 2, 3, 4].map(tick => {
            const value = maximum * tick / 4
            return <g key={tick}>
              <line x1="52" x2="624" y1={y(value)} y2={y(value)} stroke="#e4e4ea" />
              <text x="42" y={y(value) + 4} textAnchor="end">{formatCount(value)}</text>
            </g>
          })}
          {growth.map((month, index) => <text key={month.month} x={pointX(index, growth.length)} y="236" textAnchor="middle">{monthLabel(month.month)}</text>)}
          {series.map(({ key, label, color }) => <g key={key}>
            <polyline points={growth.map((month, index) => pointX(index, growth.length) + ',' + y(month[key])).join(' ')}
              fill="none" stroke={color} strokeWidth="3" strokeDasharray={key === 'companies' ? '6 4' : undefined} />
            {growth.map((month, index) => <circle key={month.month} cx={pointX(index, growth.length)} cy={y(month[key])}
              r={key === 'contacts' ? 5 : 3} fill={color} stroke="white" strokeWidth="1">
              <title>{monthLabel(month.month) + ": " + formatCount(month[key]) + " " + label.toLowerCase()}</title>
            </circle>)}
          </g>)}
        </svg>
      </div>
      <ul className="metrics-legend metrics-legend--inline">
        {series.map(item => <li key={item.key}><span className="metrics-dot" style={{ background: item.color }} />{item.label}{item.key === 'companies' && ' (línea discontinua)'}</li>)}
      </ul>
      <details className="metrics-data"><summary>Ver tabla de datos</summary>
        <div className="metrics-table-scroll"><table>
          <caption>Altas mensuales en el periodo seleccionado</caption>
          <thead><tr><th scope="col">Mes</th><th scope="col">Contactos</th><th scope="col">Empresas</th></tr></thead>
          <tbody>{growth.map(month => <tr key={month.month}><th scope="row">{monthLabel(month.month)}</th>
            <td>{formatCount(month.contacts)}</td><td>{formatCount(month.companies)}</td></tr>)}</tbody>
        </table></div>
      </details>
    </>
  )
}
