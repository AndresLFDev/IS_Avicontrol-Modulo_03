import { httpClient } from '../../shared/api/httpClient.ts'
import type { FiltroHistorial, HistorialPage } from './types.ts'

export const TAMANO_PAGINA = 10

export function consultarHistorial({ search, desde, hasta, page }: FiltroHistorial) {
  const params = new URLSearchParams({ page: String(page), size: String(TAMANO_PAGINA) })
  if (search.trim()) params.set('search', search.trim())
  if (desde) params.set('desde', desde)
  if (hasta) params.set('hasta', hasta)
  return httpClient.get<HistorialPage>(`/api/v1/liquidaciones?${params}`)
}
