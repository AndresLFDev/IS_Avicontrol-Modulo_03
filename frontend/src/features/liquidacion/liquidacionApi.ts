import { httpClient } from '../../shared/api/httpClient.ts'
import type {
  GenerarLiquidacionRequest,
  Liquidacion,
  LiquidacionPrevia,
  VistaPreviaRequest,
} from './types.ts'

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
