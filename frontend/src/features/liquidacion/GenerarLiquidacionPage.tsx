import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { ApiError, type ErrorCampo } from '../../shared/api/httpClient.ts'
import { calcularVistaPrevia, generarLiquidacion } from './liquidacionApi.ts'
import MatrizVentaFinal from './MatrizVentaFinal.tsx'
import type {
  LiquidacionPrevia,
  LoteParaLiquidar,
  PartidaPendiente,
  ProblemaLiquidacion,
} from './types.ts'

interface ErrorPantalla {
  mensaje: string
  errores: ErrorCampo[]
  partidasPendientes: PartidaPendiente[]
  idLiquidacionActiva?: number
}

const VISTA_PREVIA_DESACTUALIZADA = 'vista-previa-desactualizada'

function aErrorPantalla(e: unknown): ErrorPantalla {
  if (!(e instanceof ApiError)) {
    return { mensaje: 'Error inesperado. Intente de nuevo.', errores: [], partidasPendientes: [] }
  }
  const problema = e.problema as ProblemaLiquidacion
  return {
    mensaje: problema.detail ?? problema.title,
    errores: problema.errores ?? [],
    partidasPendientes: problema.partidasPendientes ?? [],
    idLiquidacionActiva: problema.idLiquidacionActiva,
  }
}

function ErrorCampoTexto({ error, campo }: { error: ErrorPantalla | null; campo: string }) {
  const mensaje = error?.errores.find((e) => e.campo === campo)?.mensaje
  return mensaje ? <p className="mt-1 text-sm text-error">{mensaje}</p> : null
}

/** P2 — Generar Liquidación en dos pasos: captura y vista previa (M3-CU03.FR-015). */
export default function GenerarLiquidacionPage() {
  const { idLote = '' } = useParams()
  const lote = useLocation().state as LoteParaLiquidar | null
  const navigate = useNavigate()
  const siniestroTotal = lote?.siniestroTotal ?? false

  const [paso, setPaso] = useState<'captura' | 'previa'>('captura')
  const [precio, setPrecio] = useState('')
  const [usuario, setUsuario] = useState('')
  const [previa, setPrevia] = useState<LiquidacionPrevia | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<ErrorPantalla | null>(null)
  const [datosCambiaron, setDatosCambiaron] = useState(false)

  const solicitud = () => ({
    idLote,
    precioKgCop: siniestroTotal || precio === '' ? null : Number(precio),
    usuario,
  })

  async function siguiente(evento: FormEvent) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      setPrevia(await calcularVistaPrevia(solicitud()))
      setDatosCambiaron(false)
      setPaso('previa')
    } catch (e) {
      setError(aErrorPantalla(e))
    } finally {
      setEnviando(false)
    }
  }

  async function confirmar() {
    if (!previa) return
    setEnviando(true)
    setError(null)
    try {
      const liquidacion = await generarLiquidacion({
        ...solicitud(),
        huellaDatos: previa.huellaDatos,
      })
      navigate(`/liquidaciones/${liquidacion.idLiquidacion}`, { replace: true })
    } catch (e) {
      if (e instanceof ApiError && e.problema.type.endsWith(VISTA_PREVIA_DESACTUALIZADA)) {
        // Caso borde de CU03: se recalcula y se avisa antes de permitir confirmar
        try {
          setPrevia(await calcularVistaPrevia(solicitud()))
          setDatosCambiaron(true)
        } catch (e2) {
          setError(aErrorPantalla(e2))
          setPaso('captura')
        }
      } else {
        setError(aErrorPantalla(e))
      }
    } finally {
      setEnviando(false)
    }
  }

  const titulo = lote ? `${lote.nombreGalpon} · Lote ${lote.nombreLote}` : `Lote ${idLote}`

  return (
    <section className="mx-auto max-w-4xl space-y-4">
      <Link to="/" className="text-sm text-primario underline">
        ← Volver a la lista de lotes
      </Link>
      <h1 className="text-2xl font-bold">Generar liquidación</h1>
      <p className="font-semibold">{titulo}</p>

      {error && (
        <div role="alert" className="rounded-md border border-error bg-red-50 p-3 text-error">
          <p>{error.mensaje}</p>
          {error.partidasPendientes.length > 0 && (
            <ul className="mt-2 list-disc pl-5 text-sm">
              {error.partidasPendientes.map((p) => (
                <li key={`${p.categoria}-${p.concepto}-${p.referenciaOrigen}`}>
                  {p.categoria === 'ALIMENTO' ? 'Alimento' : 'Medicina'}: {p.concepto}
                  {p.referenciaOrigen && ` (${p.referenciaOrigen})`}
                </li>
              ))}
            </ul>
          )}
          {error.idLiquidacionActiva !== undefined && (
            <Link
              to={`/liquidaciones/${error.idLiquidacionActiva}`}
              className="mt-2 inline-block underline"
            >
              Ver liquidación
            </Link>
          )}
        </div>
      )}

      {paso === 'captura' && (
        <form onSubmit={siguiente} className="space-y-4 rounded-lg bg-white p-5 shadow-sm">
          {siniestroTotal ? (
            <p className="rounded-md bg-red-100 p-3 text-sm text-error">
              Siniestro total: el lote no tuvo venta, así que no se ingresa precio por kg.
            </p>
          ) : (
            <div>
              <label htmlFor="precio" className="block text-sm font-medium">
                Precio por kg (COP)
              </label>
              <input
                id="precio"
                type="number"
                inputMode="decimal"
                step="0.01"
                value={precio}
                onChange={(e) => setPrecio(e.target.value)}
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2"
              />
              <p className="mt-1 text-xs text-gray-500">
                La venta bruta se calcula sobre el peso total, no sobre el peso promedio.
              </p>
              <ErrorCampoTexto error={error} campo="precioKgCop" />
            </div>
          )}
          <div>
            <label htmlFor="usuario" className="block text-sm font-medium">
              Usuario responsable
            </label>
            <input
              id="usuario"
              value={usuario}
              maxLength={100}
              onChange={(e) => setUsuario(e.target.value)}
              className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2"
            />
            <ErrorCampoTexto error={error} campo="usuario" />
          </div>
          <button
            type="submit"
            disabled={enviando}
            className="rounded-md bg-black px-4 py-2 text-white disabled:opacity-50"
          >
            Siguiente
          </button>
        </form>
      )}

      {paso === 'previa' && previa && (
        <div className="space-y-4">
          <span className="inline-block rounded-md bg-amber-100 px-2 py-1 text-sm text-amber-900">
            {previa.aviso}
          </span>
          {datosCambiaron && (
            <p role="status" className="rounded-md bg-amber-50 p-3 text-sm text-amber-900">
              Los datos sincronizados del lote cambiaron. Revise los valores actualizados antes de
              generar la liquidación.
            </p>
          )}
          <MatrizVentaFinal matriz={previa} />
          <div className="flex gap-3">
            <button
              type="button"
              disabled={enviando}
              onClick={() => {
                setError(null)
                setPaso('captura')
              }}
              className="rounded-md border border-gray-400 px-4 py-2"
            >
              Volver
            </button>
            <button
              type="button"
              disabled={enviando}
              onClick={confirmar}
              className="rounded-md bg-black px-4 py-2 text-white disabled:opacity-50"
            >
              Generar liquidación
            </button>
          </div>
        </div>
      )}
    </section>
  )
}
