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

export interface FiltroHistorial {
  search: string
  /** Fechas ISO `yyyy-MM-dd`; cadena vacía significa sin límite. */
  desde: string
  hasta: string
  page: number
}
