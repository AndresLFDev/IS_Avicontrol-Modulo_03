import { formatCop, formatNumero, formatPrecio } from '../../shared/format/formatCop.ts'
import { formatFechaHora } from '../../shared/format/formatFecha.ts'
import type { MatrizVentaFinal as Matriz } from './types.ts'

function Indicador({
  titulo,
  valor,
  detalle,
  className = '',
}: {
  titulo: string
  valor: string
  detalle?: string
  className?: string
}) {
  return (
    <div className="rounded-lg bg-white p-4 shadow-sm">
      <div className="text-sm text-gray-500">{titulo}</div>
      <div className={`text-3xl font-bold ${className}`}>{valor}</div>
      {detalle && <div className="text-xs text-gray-500">{detalle}</div>}
    </div>
  )
}

function Dato({ titulo, valor }: { titulo: string; valor: string }) {
  return (
    <div className="rounded-lg bg-white px-3 py-2 shadow-sm">
      <div className="text-xs text-gray-500">{titulo}</div>
      <div className="font-semibold">{valor}</div>
    </div>
  )
}

function fechaSync(fecha: string | null) {
  return fecha ? formatFechaHora(fecha) : 'sin datos'
}

/** Presentación de la Liquidación (CU03.FR-013): cuatro indicadores, datos de venta y costos. */
export default function MatrizVentaFinal({ matriz }: { matriz: Matriz }) {
  const subtotales = matriz.subtotalesCategoria
  return (
    <section aria-label="Matriz de Venta Final" className="space-y-3">
      <h2 className="font-semibold">Matriz de Venta Final</h2>
      <div className="grid gap-3 sm:grid-cols-2">
        <Indicador titulo="Venta bruta" valor={formatCop(matriz.ventaBrutaCop)} />
        <Indicador
          titulo="Mortalidad del lote"
          valor={formatNumero(matriz.mortalidadAves)}
          detalle={`${formatNumero(matriz.porcentajeMortalidad)} % de la población inicial`}
        />
        <Indicador titulo="Costos operativos" valor={formatCop(matriz.costosOperativosCop)} />
        <Indicador
          titulo="Utilidad neta"
          valor={formatCop(matriz.utilidadNetaCop)}
          className={matriz.utilidadNetaCop < 0 ? 'text-error' : 'text-green-700'}
        />
      </div>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <Dato titulo="Pollos vendidos" valor={formatNumero(matriz.pollosVendidos)} />
        <Dato titulo="Peso total" valor={`${formatNumero(matriz.pesoTotalKg)} kg`} />
        <Dato
          titulo="Peso promedio"
          valor={matriz.pesoPromedioKg === null ? '—' : `${formatNumero(matriz.pesoPromedioKg)} kg`}
        />
        <Dato
          titulo="Precio por kg"
          valor={matriz.precioKgCop === null ? '—' : formatPrecio(matriz.precioKgCop)}
        />
      </div>

      <dl className="space-y-1 text-sm">
        <div className="flex justify-between">
          <dt>Alimento</dt>
          <dd>{formatCop(subtotales.alimento)}</dd>
        </div>
        <div className="flex justify-between">
          <dt>Insumos médicos</dt>
          <dd>{formatCop(subtotales.insumosMedicos)}</dd>
        </div>
        <div className="flex justify-between">
          <dt>Costo de población</dt>
          <dd>{formatCop(subtotales.costoPoblacion)}</dd>
        </div>
        <div className="flex justify-between border-t border-gray-400 pt-1 font-semibold">
          <dt>Total costos operativos</dt>
          <dd>{formatCop(matriz.costosOperativosCop)}</dd>
        </div>
      </dl>

      <p className="text-sm text-gray-600">
        Datos sincronizados: Módulo 1 · {fechaSync(matriz.fechaSincronizacionM1)} · Módulo 2 ·{' '}
        {fechaSync(matriz.fechaSincronizacionM2)}
      </p>
    </section>
  )
}
