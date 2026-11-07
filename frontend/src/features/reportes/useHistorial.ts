import { useEffect, useState } from 'react'
import { ApiError } from '../../shared/api/httpClient.ts'
import { consultarHistorial } from './reportesApi.ts'
import type { FiltroHistorial, HistorialPage } from './types.ts'

interface Respuesta {
  clave: string
  datos: HistorialPage | null
  error: string | null
}

/**
 * Consulta el historial cada vez que cambia el filtro y descarta la respuesta de un filtro
 * anterior. Con `habilitado` en false no consulta (rango de fechas invertido, CU06.FR-005).
 */
export function useHistorial({ search, desde, hasta, page }: FiltroHistorial, habilitado: boolean) {
  const clave = JSON.stringify([search, desde, hasta, page])
  const [respuesta, setRespuesta] = useState<Respuesta>()

  useEffect(() => {
    if (!habilitado) return
    let vigente = true
    consultarHistorial({ search, desde, hasta, page })
      .then((datos) => {
        if (vigente) setRespuesta({ clave, datos, error: null })
      })
      .catch((e: unknown) => {
        if (!vigente) return
        const error =
          e instanceof ApiError ? e.message : 'Error inesperado al consultar el historial.'
        setRespuesta({ clave, datos: null, error })
      })
    return () => {
      vigente = false
    }
  }, [habilitado, clave, search, desde, hasta, page])

  // Mientras llega la respuesta del filtro nuevo se sigue mostrando la anterior
  return {
    datos: respuesta?.datos ?? null,
    error: habilitado ? (respuesta?.error ?? null) : null,
    cargando: habilitado && respuesta?.clave !== clave,
  }
}
