import { useEffect, useState } from 'react'
import { ApiError } from '../../shared/api/httpClient.ts'
import { buscarLoteParaLiquidar } from './liquidacionApi.ts'
import type { LoteParaLiquidar } from './types.ts'

interface Consulta {
  idLote: string
  lote: LoteParaLiquidar | null
  error: string | null
}

/**
 * Datos del lote para la captura de CU03. Llegan de la lista al navegar; si se abre la URL
 * directamente se piden por UUID, porque sin ellos no se sabe si el lote es un siniestro total.
 */
export function useLoteParaLiquidar(idLote: string, desdeLista: LoteParaLiquidar | null) {
  const hayQueConsultar = desdeLista === null
  const [consulta, setConsulta] = useState<Consulta>()

  useEffect(() => {
    if (!hayQueConsultar) return
    let vigente = true
    buscarLoteParaLiquidar(idLote)
      .then((lote) => {
        if (vigente) setConsulta({ idLote, lote, error: null })
      })
      .catch((e: unknown) => {
        if (!vigente) return
        const error = e instanceof ApiError ? e.message : 'Error inesperado al consultar el lote.'
        setConsulta({ idLote, lote: null, error })
      })
    return () => {
      vigente = false
    }
  }, [idLote, hayQueConsultar])

  if (!hayQueConsultar) return { lote: desdeLista, cargando: false, error: null }
  if (consulta?.idLote !== idLote) return { lote: null, cargando: true, error: null }
  return { lote: consulta.lote, cargando: false, error: consulta.error }
}
