// Desglose simulado (CU05): usa las partidas del lote si la liquidación salió de ellas; las históricas
// solo guardan los subtotales, así que se reparten en partidas que cuadran al peso con esos subtotales.
import type { Liquidacion } from '../features/liquidacion/types.ts'
import type {
  CategoriaCosto,
  CategoriaDesglose,
  Desglose,
  PartidaDesglose,
} from '../features/reportes/types.ts'
import { LOTES, type PartidaMock } from './datos.ts'

const REPARTO_ALIMENTO = [
  { concepto: 'Pre-inicio', parte: 0.2, precio: 2600 },
  { concepto: 'Inicio', parte: 0.3, precio: 2500 },
  { concepto: 'Engorde', parte: 0.5, precio: 2400 },
]
const PRECIO_VACUNA = 125

const redondear3 = (valor: number) => Math.round(valor * 1000) / 1000

function valorizar(p: PartidaMock) {
  return Math.round(p.cantidad * (p.precioUnitarioCop ?? 0))
}

function partidaDeLote(p: PartidaMock, unidadMedida: string): PartidaDesglose {
  return {
    concepto: p.concepto,
    cantidad: p.cantidad,
    unidadMedida,
    precioUnitarioCop: p.precioUnitarioCop ?? 0,
    subtotalCop: valorizar(p),
    fuenteOrigen: 'MODULO_2',
    referenciaOrigen: p.referencia,
  }
}

function partidaRepartida(
  concepto: string,
  subtotalCop: number,
  precio: number,
  unidadMedida: string,
  referenciaOrigen: string,
): PartidaDesglose {
  return {
    concepto,
    cantidad: redondear3(subtotalCop / precio),
    unidadMedida,
    precioUnitarioCop: precio,
    subtotalCop,
    fuenteOrigen: 'MODULO_2',
    referenciaOrigen,
  }
}

function alimentoRepartido(l: Liquidacion) {
  const total = l.subtotalesCategoria.alimento
  let asignado = 0
  return REPARTO_ALIMENTO.map(({ concepto, parte, precio }, indice) => {
    const ultima = indice === REPARTO_ALIMENTO.length - 1
    const subtotal = ultima ? total - asignado : Math.round(total * parte)
    asignado += subtotal
    return partidaRepartida(concepto, subtotal, precio, 'KG', `PA-${l.idLiquidacion}-${indice + 1}`)
  })
}

function insumosRepartidos(l: Liquidacion) {
  const total = l.subtotalesCategoria.insumosMedicos
  if (total === 0) return []
  const referencia = `REC-${l.idLiquidacion}`
  return [partidaRepartida('Vacuna Newcastle', total, PRECIO_VACUNA, 'UNIDAD', referencia)]
}

function categoria(
  clave: CategoriaCosto,
  etiqueta: string,
  partidas: PartidaDesglose[],
): CategoriaDesglose {
  return {
    categoria: clave,
    etiqueta,
    subtotalCop: partidas.reduce((total, p) => total + p.subtotalCop, 0),
    partidas,
  }
}

export function armarDesglose(l: Liquidacion): Desglose {
  const lote = LOTES.find((candidato) => candidato.idLote === l.idLote)
  const { alimento, insumosMedicos, costoPoblacion } = l.subtotalesCategoria
  const sumar = (partidas: PartidaMock[]) => partidas.reduce((t, p) => t + valorizar(p), 0)
  const salioDelLote =
    lote !== undefined &&
    sumar(lote.alimento) === alimento &&
    sumar(lote.medicamentos) === insumosMedicos

  return {
    idLiquidacion: l.idLiquidacion,
    estado: l.estado,
    idLote: l.idLote,
    nombreLote: l.nombreLote,
    nombreGalpon: l.nombreGalpon,
    siniestroTotal: l.siniestroTotal,
    ingresos: {
      pollosVendidos: l.pollosVendidos,
      pesoTotalKg: l.pesoTotalKg,
      pesoPromedioKg: l.pesoPromedioKg,
      precioKgCop: l.precioKgCop,
      ventaBrutaCop: l.ventaBrutaCop,
    },
    categorias: [
      categoria(
        'ALIMENTO',
        'Alimento',
        salioDelLote ? lote.alimento.map((p) => partidaDeLote(p, 'KG')) : alimentoRepartido(l),
      ),
      categoria(
        'MEDICINA',
        'Insumos Médicos',
        salioDelLote
          ? lote.medicamentos.map((p) => partidaDeLote(p, 'UNIDAD'))
          : insumosRepartidos(l),
      ),
      categoria('POBLACION', 'Costo de Población', [
        {
          concepto: 'Costo de adquisición del lote',
          cantidad: 1,
          unidadMedida: 'LOTE',
          precioUnitarioCop: costoPoblacion,
          subtotalCop: costoPoblacion,
          fuenteOrigen: 'MODULO_1',
          referenciaOrigen: l.idLote,
        },
      ]),
    ],
    mortalidadAves: l.mortalidadAves,
    porcentajeMortalidad: l.porcentajeMortalidad,
    costosOperativosCop: l.costosOperativosCop,
    utilidadNetaCop: l.utilidadNetaCop,
    registroAnulacion: l.registroAnulacion,
    exportacionPermitida: l.estado === 'ACTIVA',
  }
}
