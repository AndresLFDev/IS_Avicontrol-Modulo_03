import { useEffect, useState } from 'react'
import { ApiError } from '../../shared/api/httpClient.ts'
import { listarLotes } from './lotesApi.ts'
import type { FiltroLotes, LotePage } from './types.ts'

interface Respuesta {
  clave: string
  datos: LotePage | null
  error: string | null
}

/** Consulta la lista cada vez que cambia el filtro; descarta la respuesta de un filtro anterior. */
export function useLotes({ etapa, search, page }: FiltroLotes) {
  const clave = JSON.stringify([etapa, search, page])
  const [respuesta, setRespuesta] = useState<Respuesta>()

  useEffect(() => {
    let vigente = true
    listarLotes({ etapa, search, page })
      .then((datos) => {
        if (vigente) setRespuesta({ clave, datos, error: null })
      })
      .catch((e: unknown) => {
        if (!vigente) return
        const error = e instanceof ApiError ? e.message : 'Error inesperado al consultar los lotes.'
        setRespuesta({ clave, datos: null, error })
      })
    return () => {
      vigente = false
    }
  }, [clave, etapa, search, page])

  // Mientras llega la respuesta del filtro nuevo se sigue mostrando la anterior
  return {
    datos: respuesta?.datos ?? null,
    error: respuesta?.error ?? null,
    cargando: respuesta?.clave !== clave,
  }
}
