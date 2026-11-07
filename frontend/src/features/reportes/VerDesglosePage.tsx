import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { ApiError } from '../../shared/api/httpClient.ts'
import { formatCop, formatNumero, formatPrecio } from '../../shared/format/formatCop.ts'
import { formatFechaHora } from '../../shared/format/formatFecha.ts'
import DesgloseCostos from './DesgloseCostos.tsx'
import { consultarDesglose, exportarDesgloseExcel } from './reportesApi.ts'
import type { Desglose, Ingresos } from './types.ts'

const EXPORTACION_NO_PERMITIDA = 'exportacion-no-permitida'

interface Respuesta {
  id: string
  desglose: Desglose | null
  error: string | null
}

function Dato({ titulo, valor }: { titulo: string; valor: string }) {
  return (
    <div>
      <div className="text-sm text-gray-500">{titulo}</div>
      <div className="text-2xl font-bold">{valor}</div>
    </div>
  )
}

function SeccionIngresos({ ingresos, sinVenta }: { ingresos: Ingresos; sinVenta: boolean }) {
  return (
    <section aria-label="Ingresos" className="space-y-3 rounded-lg bg-white p-5 shadow-sm">
      <h2 className="font-bold">Ingresos</h2>
      {sinVenta && <p className="text-sm text-gray-600">Este lote no registró venta.</p>}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <Dato titulo="Pollos vendidos" valor={formatNumero(ingresos.pollosVendidos)} />
        <Dato titulo="Peso total" valor={`${formatNumero(ingresos.pesoTotalKg)} kg`} />
        <Dato
          titulo="Peso promedio"
          valor={
            ingresos.pesoPromedioKg === null ? '—' : `${formatNumero(ingresos.pesoPromedioKg)} kg`
          }
        />
        <Dato
          titulo="Precio por kg"
          valor={ingresos.precioKgCop === null ? '—' : formatPrecio(ingresos.precioKgCop)}
        />
      </div>
      <div className="flex justify-between rounded-md bg-gray-100 px-3 py-2 font-semibold">
        <span>Venta bruta</span>
        <span>{formatCop(ingresos.ventaBrutaCop)}</span>
      </div>
    </section>
  )
}

/** Desglose de una Liquidación con su exportación a Excel (M3-CU05). */
export default function VerDesglosePage() {
  const { idLiquidacion = '' } = useParams()
  const volverA = (useLocation().state as { volverA?: string } | null)?.volverA
  const [respuesta, setRespuesta] = useState<Respuesta>()
  const [recargas, setRecargas] = useState(0)
  const [exportando, setExportando] = useState(false)
  const [errorExcel, setErrorExcel] = useState<string | null>(null)

  useEffect(() => {
    let vigente = true
    consultarDesglose(idLiquidacion)
      .then((desglose) => {
        if (vigente) setRespuesta({ id: idLiquidacion, desglose, error: null })
      })
      .catch((e: unknown) => {
        if (!vigente) return
        const error = e instanceof ApiError ? e.message : 'Error inesperado.'
        setRespuesta({ id: idLiquidacion, desglose: null, error })
      })
    return () => {
      vigente = false
    }
  }, [idLiquidacion, recargas])

  /** Si falla, la vista sigue intacta y se ofrece reintentar (CU05.FR-005). */
  async function exportar() {
    setExportando(true)
    setErrorExcel(null)
    try {
      await exportarDesgloseExcel(idLiquidacion)
    } catch (e) {
      if (e instanceof ApiError && e.problema.type.endsWith(EXPORTACION_NO_PERMITIDA)) {
        // Otro usuario la anuló con la vista abierta: se muestra su estado actual
        setRecargas((n) => n + 1)
      } else {
        setErrorExcel(e instanceof ApiError ? e.message : 'Error inesperado.')
      }
    } finally {
      setExportando(false)
    }
  }

  if (respuesta?.id !== idLiquidacion) {
    return <p className="text-gray-600">Cargando desglose…</p>
  }
  const { desglose, error } = respuesta
  if (!desglose) {
    return (
      <p role="alert" className="text-error">
        {error}
      </p>
    )
  }

  const activa = desglose.estado === 'ACTIVA'
  const anulacion = desglose.registroAnulacion

  return (
    <section className="mx-auto max-w-4xl space-y-4">
      <Link
        to={`/liquidaciones/${desglose.idLiquidacion}`}
        state={{ volverA }}
        className="text-sm text-primario underline"
      >
        ← Volver a la liquidación
      </Link>
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-bold">
          {desglose.nombreGalpon} · Lote {desglose.nombreLote}
        </h1>
        <span
          className={`rounded-full px-3 py-1 text-sm font-semibold ${
            activa ? 'bg-green-100 text-green-900' : 'bg-gray-200 text-gray-700'
          }`}
        >
          {desglose.estado}
        </span>
        {desglose.siniestroTotal && (
          <span className="rounded-md bg-red-100 px-2 py-1 text-sm text-error">
            Siniestro total
          </span>
        )}
      </div>

      {anulacion && (
        <div className="rounded-md border-2 border-error bg-red-50 p-4 text-sm">
          <p className="text-lg font-bold text-error">LIQUIDACIÓN ANULADA</p>
          <p>Motivo: {anulacion.motivo}</p>
          <p>
            Por {anulacion.usuarioResponsable} · {formatFechaHora(anulacion.fechaHoraAnulacion)}
          </p>
        </div>
      )}

      <SeccionIngresos ingresos={desglose.ingresos} sinVenta={desglose.siniestroTotal} />
      <DesgloseCostos
        categorias={desglose.categorias}
        costosOperativosCop={desglose.costosOperativosCop}
      />
      <div className="flex justify-between rounded-lg bg-white p-5 font-bold shadow-sm">
        <span>Utilidad neta</span>
        <span className={desglose.utilidadNetaCop < 0 ? 'text-error' : 'text-green-700'}>
          {formatCop(desglose.utilidadNetaCop)}
        </span>
      </div>

      {errorExcel && (
        <div role="alert" className="rounded-md border border-error bg-red-50 p-3 text-error">
          <p>No se pudo generar el archivo Excel. {errorExcel}</p>
          <button type="button" onClick={exportar} className="mt-2 font-semibold underline">
            Reintentar
          </button>
        </div>
      )}

      <button
        type="button"
        disabled={!desglose.exportacionPermitida || exportando}
        title={
          desglose.exportacionPermitida ? undefined : 'Una liquidación anulada no se puede exportar'
        }
        onClick={exportar}
        className="rounded-md bg-black px-4 py-2 text-white disabled:opacity-40"
      >
        {exportando ? 'Generando archivo…' : 'Exportar a Excel'}
      </button>
    </section>
  )
}
