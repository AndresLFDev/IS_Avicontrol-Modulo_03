import { httpClient } from '../../shared/api/httpClient.ts'
import type { Desglose, FiltroHistorial, HistorialPage } from './types.ts'

export const TAMANO_PAGINA = 10

export function consultarHistorial({ search, desde, hasta, page }: FiltroHistorial) {
  const params = new URLSearchParams({ page: String(page), size: String(TAMANO_PAGINA) })
  if (search.trim()) params.set('search', search.trim())
  if (desde) params.set('desde', desde)
  if (hasta) params.set('hasta', hasta)
  return httpClient.get<HistorialPage>(`/api/v1/liquidaciones?${params}`)
}

function rutaDesglose(idLiquidacion: string) {
  return `/api/v1/liquidaciones/${encodeURIComponent(idLiquidacion)}/desglose`
}

export function consultarDesglose(idLiquidacion: string) {
  return httpClient.get<Desglose>(rutaDesglose(idLiquidacion))
}

/** Descarga el .xlsx; el nombre se fija aquí porque CORS no expone Content-Disposition. */
export async function exportarDesgloseExcel(idLiquidacion: string) {
  const archivo = await httpClient.descargar(`${rutaDesglose(idLiquidacion)}/excel`)
  const url = URL.createObjectURL(archivo)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = `Liquidacion_Lote_${idLiquidacion}.xlsx`
  enlace.click()
  URL.revokeObjectURL(url)
}
