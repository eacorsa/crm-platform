import client from './client'
import type { MetricsResponse } from '../types'

export type MetricsPeriod = 6 | 12

export const getMetrics = (months: MetricsPeriod, signal?: AbortSignal) =>
  client.get<MetricsResponse>('/metrics', { params: { months }, signal }).then(response => response.data)
