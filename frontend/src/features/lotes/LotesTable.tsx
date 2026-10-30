import { formatFecha } from '../../shared/format/formatFecha.ts'
import { ETIQUETA_ETAPA, type EtapaLote, type LoteResumen } from './types.ts'

const COLOR_ETAPA: Record<EtapaLote, string> = {
  POR_LIQUIDAR: 'bg-violet-100 text-violet-900',
  EN_COSECHA: 'bg-amber-100 text-amber-900',
  PRODUCTIVO: 'bg-green-100 text-green-900',
  AISLAMIENTO: 'bg-sky-100 text-sky-900',
}

/** Primeros y últimos 4 caracteres del UUID; el completo queda en el title. */
function uuidCorto(uuid: string) {
  return `${uuid.slice(0, 4)}…${uuid.slice(-4)}`
}

interface Props {
  lotes: LoteResumen[]
  onGenerar: (lote: LoteResumen) => void
}

export default function LotesTable({ lotes, onGenerar }: Props) {
  return (
    <table className="w-full border-separate border-spacing-y-2 text-left">
      <thead>
        <tr className="text-sm text-gray-500">
          <th className="px-4 py-2 font-medium">Lote</th>
          <th className="px-4 py-2 font-medium">Galpón</th>
          <th className="px-4 py-2 font-medium">Etapa</th>
          <th className="px-4 py-2 font-medium">Acciones</th>
        </tr>
      </thead>
      <tbody>
        {lotes.map((lote) => (
          <tr key={lote.idLote} className="bg-white shadow-sm">
            <td className="rounded-l-lg px-4 py-3">
              <div className="font-semibold">{lote.nombreLote}</div>
              <div className="text-xs text-gray-500" title={lote.idLote}>
                {uuidCorto(lote.idLote)}
              </div>
              <div className="text-xs text-gray-500">Ingresó: {formatFecha(lote.fechaIngreso)}</div>
            </td>
            <td className="px-4 py-3">
              <div className="font-semibold">{lote.nombreGalpon}</div>
              <div className="text-xs text-gray-500" title={lote.idGalpon}>
                {uuidCorto(lote.idGalpon)}
              </div>
            </td>
            <td className="px-4 py-3">
              <span className={`rounded-md px-2 py-1 text-sm ${COLOR_ETAPA[lote.etapa]}`}>
                {ETIQUETA_ETAPA[lote.etapa]}
              </span>
              {lote.siniestroTotal && (
                <span className="ml-2 rounded-md bg-red-100 px-2 py-1 text-sm text-error">
                  Siniestro total
                </span>
              )}
            </td>
            <td className="rounded-r-lg px-4 py-3">
              <button
                type="button"
                disabled={!lote.accionHabilitada}
                onClick={() => onGenerar(lote)}
                className="rounded-md bg-black px-3 py-1 text-sm text-white disabled:cursor-not-allowed disabled:border disabled:border-gray-400 disabled:bg-transparent disabled:text-gray-500"
              >
                Generar liquidación
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
