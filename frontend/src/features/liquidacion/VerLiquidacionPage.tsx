import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { ApiError } from '../../shared/api/httpClient.ts'
import { formatFechaHora } from '../../shared/format/formatFecha.ts'
import AnularLiquidacion from './AnularLiquidacion.tsx'
import { consultarLiquidacion } from './liquidacionApi.ts'
import MatrizVentaFinal from './MatrizVentaFinal.tsx'
import type { Liquidacion, LoteParaLiquidar } from './types.ts'

interface Respuesta {
  id: string
  liquidacion: Liquidacion | null
  error: string | null
}

/** Vista de una Liquidación existente, ACTIVA o ANULADA (M3-CU03.FR-014). */
export default function VerLiquidacionPage() {
  const { idLiquidacion = '' } = useParams()
  // El historial envía su ruta con filtros para que el enlace de regreso los conserve
  const volverA = (useLocation().state as { volverA?: string } | null)?.volverA
  const [respuesta, setRespuesta] = useState<Respuesta>()
  const [recargas, setRecargas] = useState(0)
  const [anulando, setAnulando] = useState(false)
  const [aviso, setAviso] = useState<string>()

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
  }, [idLiquidacion, recargas])

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
  const loteParaLiquidar: LoteParaLiquidar = {
    nombreLote: liquidacion.nombreLote,
    nombreGalpon: liquidacion.nombreGalpon,
    siniestroTotal: liquidacion.siniestroTotal,
  }

  return (
    <section className="mx-auto max-w-4xl space-y-4">
      <Link to={volverA ?? '/'} className="text-sm text-primario underline">
        ← Volver {volverA ? 'al historial' : 'a la lista de lotes'}
      </Link>
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-bold">
          {liquidacion.nombreGalpon} · Lote {liquidacion.nombreLote}
        </h1>
        <span
          className={`rounded-full px-3 py-1 text-sm font-semibold ${
            activa ? 'bg-green-100 text-green-900' : 'bg-red-200/70 text-red-800'
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

      {aviso && (
        <p role="status" className="rounded-md bg-amber-50 p-3 text-sm text-amber-900">
          {aviso}
        </p>
      )}

      <MatrizVentaFinal matriz={liquidacion} />

      <p className="text-sm text-gray-600">
        Generada por <span className="font-semibold">{liquidacion.usuarioResponsable}</span> ·{' '}
        {formatFechaHora(liquidacion.fechaHoraGeneracion)}
      </p>

      {anulacion && (
        <div className="rounded-md bg-red-50/60 p-3 text-sm text-red-900/60">
          <p className="font-semibold text-red-800/70">Anulada</p>
          <p>Motivo: {anulacion.motivo}</p>
          <p>
            Por {anulacion.usuarioResponsable} · {formatFechaHora(anulacion.fechaHoraAnulacion)}
          </p>
        </div>
      )}

      <div className="flex gap-3">
        {liquidacion.accionesDisponibles.includes('VER_DESGLOSE') && (
          <Link
            to={`/liquidaciones/${liquidacion.idLiquidacion}/desglose`}
            state={{ volverA }}
            className="rounded-md bg-black px-4 py-2 text-white"
          >
            Ver desglose
          </Link>
        )}
        {liquidacion.accionesDisponibles.includes('ANULAR') && (
          <button
            type="button"
            onClick={() => setAnulando(true)}
            className="rounded-md bg-red-400 px-4 py-2 text-white"
          >
            Anular liquidación
          </button>
        )}
        {!activa && (
          <Link
            to={`/lotes/${liquidacion.idLote}/liquidacion`}
            state={loteParaLiquidar}
            className="rounded-md bg-gray-600 px-4 py-2 text-white"
          >
            Generar nueva liquidación
          </Link>
        )}
      </div>

      {anulando && (
        <AnularLiquidacion
          liquidacion={liquidacion}
          onCancelar={() => setAnulando(false)}
          onTerminar={(mensaje) => {
            setAviso(mensaje)
            setAnulando(false)
            setRecargas((n) => n + 1)
          }}
        />
      )}
    </section>
  )
}
