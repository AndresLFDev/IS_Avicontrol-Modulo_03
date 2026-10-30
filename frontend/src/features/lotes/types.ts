// Reflejo de LotePageDto / LoteResumenDto del backend (GET /api/v1/lotes).

/** Etapas que se pueden listar; LIQUIDADO no se lista (M3-CU01.FR-014). */
export type EtapaLote = 'PRODUCTIVO' | 'EN_COSECHA' | 'AISLAMIENTO' | 'POR_LIQUIDAR'

export interface LoteResumen {
  idLote: string
  nombreLote: string
  fechaIngreso: string
  idGalpon: string
  nombreGalpon: string
  etapa: EtapaLote
  siniestroTotal: boolean
  accion: 'GENERAR_LIQUIDACION'
  accionHabilitada: boolean
}

export interface EstadoFuente {
  ultimaSincronizacionExitosa: string | null
  desactualizada: boolean
}

export interface EstadoSincronizacion {
  modulo1: EstadoFuente
  modulo2: EstadoFuente
}

export interface LotePage {
  content: LoteResumen[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  mensaje: string
  sincronizacion: EstadoSincronizacion
}

export interface FiltroLotes {
  etapa?: EtapaLote
  search?: string
  page: number
}

/** Etiqueta que muestra la UI para cada código de etapa del API. */
export const ETIQUETA_ETAPA: Record<EtapaLote, string> = {
  POR_LIQUIDAR: 'Por liquidar',
  EN_COSECHA: 'En cosecha',
  PRODUCTIVO: 'Productivo',
  AISLAMIENTO: 'Aislamiento',
}
