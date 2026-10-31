import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { ApiError } from '../../shared/api/httpClient.ts'
import { formatFechaHora } from '../../shared/format/formatFecha.ts'
import { consultarLiquidacion } from './liquidacionApi.ts'
import MatrizVentaFinal from './MatrizVentaFinal.tsx'
import type { Liquidacion } from './types.ts'

interface Respuesta {
  id: string
  liquidacion: Liquidacion | null
  error: string | null
}

/** Vista de una Liquidación existente, ACTIVA o ANULADA (M3-CU03.FR-014). */
export default function VerLiquidacionPage() {
  const { idLiquidacion = '' } = useParams()
  const [respuesta, setRespuesta] = useState<Respuesta>()

  useEffect(() => {
    let vigente = true
    consultarLiquidacion(idLiquidacion)
      .then((liquidacion) => {
        if (vigente) setRespuesta({ id: idLiquidacion, liquidacion, error: null })
      })
      .catch((e: unknown) => {
        if (!vigente) return
        const error = e instanceof ApiError ? e.message : 'Error inesperado.'
        setRespuesta({ id: idLiquidacion, liquidacion: null, error })
      })
    return () => {
      vigente = false
    }
  }, [idLiquidacion])

  if (respuesta?.id !== idLiquidacion) {
    return <p className="text-gray-600">Cargando liquidación…</p>
  }
  const { liquidacion, error } = respuesta
  if (!liquidacion) {
    return (
      <p role="alert" className="text-error">
        {error}
      </p>
    )
  }

  const activa = liquidacion.estado === 'ACTIVA'
  const anulacion = liquidacion.registroAnulacion

  return (
    <section className="mx-auto max-w-4xl space-y-4">
      <Link to="/" className="text-sm text-primario underline">
        ← Volver a la lista de lotes
      </Link>
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-bold">
          {liquidacion.nombreGalpon} · Lote {liquidacion.nombreLote}
        </h1>
        <span
          className={`rounded-full px-3 py-1 text-sm font-semibold ${
            activa ? 'bg-green-100 text-green-900' : 'bg-gray-200 text-gray-700'
          }`}
        >
          {liquidacion.estado}
        </span>
        {liquidacion.siniestroTotal && (
          <span className="rounded-md bg-red-100 px-2 py-1 text-sm text-error">
            Siniestro total
          </span>
        )}
      </div>

      <MatrizVentaFinal matriz={liquidacion} />

      <p className="text-sm text-gray-600">
        Generada por <span className="font-semibold">{liquidacion.usuarioResponsable}</span> ·{' '}
        {formatFechaHora(liquidacion.fechaHoraGeneracion)}
      </p>

      {anulacion && (
        <div className="rounded-md bg-gray-100 p-3 text-sm">
          <p className="font-semibold">Anulada</p>
          <p>Motivo: {anulacion.motivo}</p>
          <p>
            Por {anulacion.usuarioResponsable} · {formatFechaHora(anulacion.fechaHoraAnulacion)}
          </p>
        </div>
      )}

      {/* El desglose (CU05) y la anulación (CU04) llegan en sus propios planes */}
      <div className="flex gap-3">
        {liquidacion.accionesDisponibles.includes('VER_DESGLOSE') && (
          <button
            type="button"
            disabled
            title="Disponible con M3-CU05"
            className="rounded-md bg-black px-4 py-2 text-white disabled:opacity-40"
          >
            Ver desglose
          </button>
        )}
        {liquidacion.accionesDisponibles.includes('ANULAR') && (
          <button
            type="button"
            disabled
            title="Disponible con M3-CU04"
            className="rounded-md bg-red-400 px-4 py-2 text-white disabled:opacity-40"
          >
            Anular liquidación
          </button>
        )}
      </div>
    </section>
  )
}
