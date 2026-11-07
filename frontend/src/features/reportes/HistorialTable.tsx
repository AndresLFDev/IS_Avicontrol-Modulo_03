import { Link } from 'react-router'
import { formatCop, formatNumero } from '../../shared/format/formatCop.ts'
import { formatFechaHora } from '../../shared/format/formatFecha.ts'
import type { HistorialItem } from './types.ts'

const COLOR_ESTADO: Record<HistorialItem['estado'], string> = {
  ACTIVA: 'bg-green-100 text-green-900',
  ANULADA: 'bg-red-100 text-error',
}

interface Props {
  liquidaciones: HistorialItem[]
  /** Ruta del historial con sus filtros, para que la vista de la Liquidación vuelva a ella. */
  volverA: string
}

export default function HistorialTable({ liquidaciones, volverA }: Props) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full border-separate border-spacing-y-2 text-left">
        <thead>
          <tr className="text-sm text-gray-500">
            <th className="px-4 py-2 font-medium">Fecha y hora</th>
            <th className="px-4 py-2 font-medium">Galpón / Lote</th>
            <th className="px-4 py-2 text-right font-medium">Venta bruta</th>
            <th className="px-4 py-2 text-right font-medium">Mortalidad</th>
            <th className="px-4 py-2 text-right font-medium">Costos operativos</th>
            <th className="px-4 py-2 text-right font-medium">Utilidad neta</th>
            <th className="px-4 py-2 font-medium">Estado</th>
            <th className="px-4 py-2 font-medium">
              <span className="sr-only">Acciones</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {liquidaciones.map((l) => {
            const anulada = l.estado === 'ANULADA'
            return (
              <tr
                key={l.idLiquidacion}
                className={`shadow-sm ${anulada ? 'bg-gray-100 text-gray-500' : 'bg-white'}`}
              >
                <td className="rounded-l-lg px-4 py-3 text-sm whitespace-nowrap">
                  {formatFechaHora(l.fechaHoraGeneracion)}
                </td>
                <td className="px-4 py-3 whitespace-nowrap">
                  <div className="font-semibold">{l.nombreGalpon}</div>
                  <div className="text-xs">Lote {l.nombreLote}</div>
                </td>
                <td className="px-4 py-3 text-right whitespace-nowrap">
                  {formatCop(l.ventaBrutaCop)}
                </td>
                <td className="px-4 py-3 text-right whitespace-nowrap">
                  {formatNumero(l.porcentajeMortalidad)} %
                </td>
                <td className="px-4 py-3 text-right whitespace-nowrap">
                  {formatCop(l.costosOperativosCop)}
                </td>
                <td
                  className={`px-4 py-3 text-right font-semibold whitespace-nowrap ${
                    l.utilidadNetaCop < 0 ? 'text-error' : ''
                  }`}
                >
                  {formatCop(l.utilidadNetaCop)}
                </td>
                <td className="px-4 py-3">
                  <span
                    className={`rounded-md px-2 py-1 text-sm font-semibold ${COLOR_ESTADO[l.estado]}`}
                  >
                    {l.estado}
                  </span>
                </td>
                <td className="rounded-r-lg px-4 py-3 whitespace-nowrap">
                  <Link
                    to={`/liquidaciones/${l.idLiquidacion}`}
                    state={{ volverA }}
                    aria-label={`Ver liquidación del lote ${l.nombreLote}, ${l.estado}`}
                    className="text-sm text-primario underline"
                  >
                    Ver
                  </Link>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
