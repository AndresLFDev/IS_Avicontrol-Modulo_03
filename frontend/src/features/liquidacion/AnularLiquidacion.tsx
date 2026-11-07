import { useState, type FormEvent } from 'react'
import { ApiError, type ErrorCampo } from '../../shared/api/httpClient.ts'
import Modal from '../../shared/ui/Modal.tsx'
import { anularLiquidacion } from './liquidacionApi.ts'
import type { Liquidacion } from './types.ts'

const MOTIVO_MIN = 10
const MOTIVO_MAX = 500
const USUARIO_MIN = 3
const LIQUIDACION_YA_ANULADA = 'liquidacion-ya-anulada'

interface Props {
  liquidacion: Liquidacion
  onCancelar: () => void
  /** La anulación terminó; `aviso` se muestra si otro usuario se adelantó. */
  onTerminar: (aviso?: string) => void
}

/** Ventana de anulación en dos pasos: captura y confirmación (M3-CU04.FR-003 y FR-004). */
export default function AnularLiquidacion({ liquidacion, onCancelar, onTerminar }: Props) {
  const [paso, setPaso] = useState<'captura' | 'confirmacion'>('captura')
  const [motivo, setMotivo] = useState('')
  const [usuario, setUsuario] = useState('')
  const [intentoContinuar, setIntentoContinuar] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [errores, setErrores] = useState<ErrorCampo[]>([])

  const largoMotivo = motivo.trim().length
  const motivoValido = largoMotivo >= MOTIVO_MIN
  const usuarioValido = usuario.trim().length >= USUARIO_MIN

  const errorMotivo =
    (motivo !== '' || intentoContinuar) && !motivoValido
      ? `Escribe al menos ${MOTIVO_MIN} caracteres (faltan ${MOTIVO_MIN - largoMotivo}).`
      : errores.find((e) => e.campo === 'motivo')?.mensaje
  const errorUsuario =
    intentoContinuar && !usuarioValido
      ? `Escribe al menos ${USUARIO_MIN} caracteres.`
      : errores.find((e) => e.campo === 'usuario')?.mensaje

  function continuar(evento: FormEvent) {
    evento.preventDefault()
    setIntentoContinuar(true)
    if (!motivoValido || !usuarioValido) return
    setError(null)
    setErrores([])
    setPaso('confirmacion')
  }

  async function confirmar() {
    setEnviando(true)
    setError(null)
    try {
      await anularLiquidacion(liquidacion.idLiquidacion, { motivo, usuario })
      onTerminar()
    } catch (e) {
      if (!(e instanceof ApiError)) {
        setError('Error inesperado. Intente de nuevo.')
      } else if (e.problema.type.endsWith(LIQUIDACION_YA_ANULADA)) {
        onTerminar(e.message)
      } else {
        setError(e.message)
        setErrores(e.problema.errores ?? [])
        if (e.problema.errores?.length) setPaso('captura')
      }
    } finally {
      setEnviando(false)
    }
  }

  const alerta = error && (
    <p role="alert" className="rounded-md border border-error bg-red-50 p-3 text-error">
      {error}
    </p>
  )

  if (paso === 'captura') {
    return (
      <Modal titulo="Anular liquidación" onCerrar={onCancelar}>
        <form onSubmit={continuar} className="space-y-4">
          <p className="text-sm text-gray-600">
            Escribe por qué se anula. Quedará registrado con tu usuario, la fecha y la hora.
          </p>
          {alerta}
          <div>
            <label htmlFor="motivo" className="block text-sm font-medium">
              Motivo de anulación
            </label>
            <textarea
              id="motivo"
              rows={4}
              value={motivo}
              maxLength={MOTIVO_MAX}
              onChange={(e) => setMotivo(e.target.value)}
              className={`mt-1 w-full rounded-md border px-3 py-2 ${
                errorMotivo ? 'border-error' : 'border-gray-300'
              }`}
            />
            <div className="flex justify-between gap-3 text-xs">
              <span className="text-error">{errorMotivo}</span>
              <span className="text-gray-500">
                {largoMotivo} / {MOTIVO_MAX} · mínimo {MOTIVO_MIN}
              </span>
            </div>
          </div>
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
            {errorUsuario && <p className="mt-1 text-sm text-error">{errorUsuario}</p>}
          </div>
          <div className="flex gap-3">
            <button type="submit" className="rounded-md bg-red-400 px-4 py-2 text-white">
              Continuar
            </button>
            <button
              type="button"
              onClick={onCancelar}
              className="rounded-md bg-black px-4 py-2 text-white"
            >
              Cancelar
            </button>
          </div>
        </form>
      </Modal>
    )
  }

  return (
    <Modal titulo="¿Anular la liquidación de este lote?" bloqueado={enviando} onCerrar={onCancelar}>
      <p className="font-medium">
        Vas a anular la liquidación del Lote {liquidacion.nombreLote} del {liquidacion.nombreGalpon}
        . La liquidación quedará en estado Anulada y el lote podrá liquidarse de nuevo.
      </p>
      {alerta}
      <div>
        <p className="text-sm font-medium text-gray-600">Motivo de anulación</p>
        <p className="mt-1 rounded-md bg-gray-100 px-3 py-2 text-sm break-words">{motivo.trim()}</p>
      </div>
      <div className="flex gap-3">
        <button
          type="button"
          disabled={enviando}
          onClick={confirmar}
          className="rounded-md bg-red-400 px-4 py-2 text-white disabled:opacity-50"
        >
          Confirmar anulación
        </button>
        <button
          type="button"
          disabled={enviando}
          onClick={onCancelar}
          className="rounded-md bg-black px-4 py-2 text-white disabled:opacity-50"
        >
          Cancelar
        </button>
      </div>
    </Modal>
  )
}
