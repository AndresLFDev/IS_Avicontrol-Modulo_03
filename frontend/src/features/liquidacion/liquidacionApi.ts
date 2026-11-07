import { httpClient } from '../../shared/api/httpClient.ts'
import type {
  Anulacion,
  AnulacionRequest,
  GenerarLiquidacionRequest,
  Liquidacion,
  LiquidacionPrevia,
  LoteParaLiquidar,
  VistaPreviaRequest,
} from './types.ts'

/** Busca por UUID exacto en la lista de lotes; null si no se lista (ya liquidado o inexistente). */
export async function buscarLoteParaLiquidar(idLote: string): Promise<LoteParaLiquidar | null> {
  const { content } = await httpClient.get<{
    content: (LoteParaLiquidar & { idLote: string })[]
  }>(`/api/v1/lotes?${new URLSearchParams({ search: idLote })}`)
  return content.find((lote) => lote.idLote === idLote) ?? null
}

/** Paso 2 de CU03: calcula la vista previa sin persistir nada. */
export function calcularVistaPrevia(request: VistaPreviaRequest) {
  return httpClient.post<LiquidacionPrevia>('/api/v1/liquidaciones/previa', request)
}

/** Confirmación: persiste la Liquidación con la huella de la vista previa mostrada. */
export function generarLiquidacion(request: GenerarLiquidacionRequest) {
  return httpClient.post<Liquidacion>('/api/v1/liquidaciones', request)
}

export function consultarLiquidacion(idLiquidacion: string) {
  return httpClient.get<Liquidacion>(`/api/v1/liquidaciones/${encodeURIComponent(idLiquidacion)}`)
}

export function anularLiquidacion(idLiquidacion: number, request: AnulacionRequest) {
  return httpClient.put<Anulacion>(`/api/v1/liquidaciones/${idLiquidacion}/anulacion`, request)
}
