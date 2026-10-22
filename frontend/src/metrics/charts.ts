import type { DealStage } from '../types'

export const STAGE_LABELS: Record<DealStage, string> = {
  NUEVO: 'Nuevo', CONTACTADO: 'Contactado', PROPUESTA: 'Propuesta',
  NEGOCIACION: 'Negociación', CERRADO: 'Cerrado', PERDIDO: 'Perdido',
}
export const STAGE_COLORS: Record<DealStage, string> = {
  NUEVO: '#2563eb', CONTACTADO: '#7c3aed', PROPUESTA: '#b45309',
  NEGOCIACION: '#c2410c', CERRADO: '#15803d', PERDIDO: '#b91c1c',
}
export const formatCount = (value: number) => new Intl.NumberFormat('es-MX').format(value)
export const formatMoney = (value: number) =>
  new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN', maximumFractionDigits: 2 }).format(value)
export const monthLabel = (month: string) => new Intl.DateTimeFormat('es-MX', {
  month: 'short', year: '2-digit', timeZone: 'UTC',
}).format(new Date(month + '-01T12:00:00Z'))

export function percentage(part: number, total: number): number {
  return total > 0 ? Math.max(0, Math.min(100, part / total * 100)) : 0
}

/** Integer count ticks, including empty and small series, without division by zero. */
export function countAxis(values: number[]): number {
  return Math.max(4, Math.ceil(Math.max(0, ...values) / 4) * 4)
}

export function pointX(index: number, count: number): number {
  return count <= 1 ? 340 : 52 + index * 572 / (count - 1)
}
