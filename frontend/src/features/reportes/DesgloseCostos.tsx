import { formatCantidad, formatCop, formatPrecio } from '../../shared/format/formatCop.ts'
import type { CategoriaCosto, CategoriaDesglose, PartidaDesglose } from './types.ts'

const ORIGEN: Record<CategoriaCosto, string> = {
  ALIMENTO: 'Módulo 2 / Partida de alimento',
  MEDICINA: 'Módulo 2 / Consumo de medicamento',
  POBLACION: 'Módulo 1 / Costo total del lote',
}

function FilaPartida({
  categoria,
  partida,
}: {
  categoria: CategoriaCosto
  partida: PartidaDesglose
}) {
  return (
    <tr className="border-b border-gray-300 align-top">
      <td className="px-3 py-2">
        <div className="font-medium">{partida.concepto}</div>
        <div className="text-xs break-all text-gray-500">
          {ORIGEN[categoria]} · {partida.referenciaOrigen}
        </div>
      </td>
      <td className="px-3 py-2 text-right">{formatCantidad(partida.cantidad)}</td>
      <td className="px-3 py-2">{partida.unidadMedida}</td>
      <td className="px-3 py-2 text-right">{formatPrecio(partida.precioUnitarioCop)}</td>
      <td className="px-3 py-2 text-right">{formatCop(partida.subtotalCop)}</td>
    </tr>
  )
}

function Categoria({ categoria }: { categoria: CategoriaDesglose }) {
  return (
    <tbody>
      <tr className="bg-gray-100">
        <th colSpan={5} scope="colgroup" className="px-3 py-2 text-left font-semibold">
          {categoria.etiqueta}
        </th>
      </tr>
      {categoria.partidas.map((partida, indice) => (
        <FilaPartida key={indice} categoria={categoria.categoria} partida={partida} />
      ))}
      {categoria.partidas.length === 0 && (
        <tr>
          <td colSpan={5} className="px-3 py-2 text-gray-500">
            Sin partidas registradas
          </td>
        </tr>
      )}
      <tr className="font-semibold">
        <td colSpan={4} className="px-3 py-2">
          Subtotal {categoria.etiqueta}
        </td>
        <td className="px-3 py-2 text-right">{formatCop(categoria.subtotalCop)}</td>
      </tr>
    </tbody>
  )
}

/** Tabla de costos por categoría; cada fila indica su fuente sincronizada (CU05.SC-003). */
export default function DesgloseCostos({
  categorias,
  costosOperativosCop,
}: {
  categorias: CategoriaDesglose[]
  costosOperativosCop: number
}) {
  return (
    <section aria-label="Costos operativos" className="rounded-lg bg-white p-5 shadow-sm">
      <h2 className="mb-3 font-bold">Costos operativos</h2>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[640px] text-sm">
          <thead>
            <tr className="text-gray-500">
              <th scope="col" className="px-3 py-2 text-left font-semibold">
                Concepto
              </th>
              <th scope="col" className="px-3 py-2 text-right font-semibold">
                Cantidad
              </th>
              <th scope="col" className="px-3 py-2 text-left font-semibold">
                Unidad
              </th>
              <th scope="col" className="px-3 py-2 text-right font-semibold">
                Precio unitario (COP)
              </th>
              <th scope="col" className="px-3 py-2 text-right font-semibold">
                Subtotal (COP)
              </th>
            </tr>
          </thead>
          {categorias.map((categoria) => (
            <Categoria key={categoria.categoria} categoria={categoria} />
          ))}
          <tfoot>
            <tr className="border-t-2 border-gray-400 text-base font-bold">
              <td colSpan={4} className="px-3 py-2">
                Total costos operativos
              </td>
              <td className="px-3 py-2 text-right">{formatCop(costosOperativosCop)}</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </section>
  )
}
