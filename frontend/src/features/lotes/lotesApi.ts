import { httpClient } from '../../shared/api/httpClient.ts'
import type { FiltroLotes, LotePage } from './types.ts'

export const TAMANO_PAGINA = 6

export function listarLotes({ etapa, search, page }: FiltroLotes): Promise<LotePage> {
  const params = new URLSearchParams({ page: String(page), size: String(TAMANO_PAGINA) })
  if (etapa) params.set('etapa', etapa)
  if (search?.trim()) params.set('search', search.trim())
  return httpClient.get<LotePage>(`/api/v1/lotes?${params}`)
}
