// Reflejo de HistorialPageDto / HistorialItemDto del backend (GET /api/v1/liquidaciones, plan 004).

export interface HistorialItem {
  idLiquidacion: number
  fechaHoraGeneracion: string
  idGalpon: string
  nombreGalpon: string
  idLote: string
  nombreLote: string
  ventaBrutaCop: number
  porcentajeMortalidad: number
  costosOperativosCop: number
  utilidadNetaCop: number
  estado: 'ACTIVA' | 'ANULADA'
}

export interface HistorialPage {
  content: HistorialItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  /** Null con resultados; texto de CU06.FR-007 cuando content está vacío. */
  mensaje: string | null
}

// Reflejo de DesgloseDto del backend (GET /api/v1/liquidaciones/{id}/desglose, plan 004).

export type CategoriaCosto = 'ALIMENTO' | 'MEDICINA' | 'POBLACION'

export interface PartidaDesglose {
  concepto: string
  cantidad: number
  unidadMedida: string
  precioUnitarioCop: number
  subtotalCop: number
  fuenteOrigen: 'MODULO_1' | 'MODULO_2'
  referenciaOrigen: string
}

export interface CategoriaDesglose {
  categoria: CategoriaCosto
  etiqueta: string
  subtotalCop: number
  partidas: PartidaDesglose[]
}

export interface Ingresos {
  pollosVendidos: number
  pesoTotalKg: number
  /** Null en siniestro total. */
  pesoPromedioKg: number | null
  /** Null en siniestro total. */
  precioKgCop: number | null
  ventaBrutaCop: number
}

export interface RegistroAnulacion {
  idAnulacion: number
  motivo: string
  fechaHoraAnulacion: string
  usuarioResponsable: string
}

export interface Desglose {
  idLiquidacion: number
  estado: 'ACTIVA' | 'ANULADA'
  idLote: string
  nombreLote: string
  nombreGalpon: string
  siniestroTotal: boolean
  ingresos: Ingresos
  categorias: CategoriaDesglose[]
  mortalidadAves: number
  porcentajeMortalidad: number
  costosOperativosCop: number
  utilidadNetaCop: number
  registroAnulacion: RegistroAnulacion | null
  exportacionPermitida: boolean
}

export interface FiltroHistorial {
  search: string
  /** Fechas ISO `yyyy-MM-dd`; cadena vacía significa sin límite. */
  desde: string
  hasta: string
  page: number
}
