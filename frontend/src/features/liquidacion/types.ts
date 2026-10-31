// Reflejo de LiquidacionPreviaDto y LiquidacionDto del backend (plan 003).
import type { ProblemDetail } from '../../shared/api/httpClient.ts'

export interface SubtotalesCategoria {
  alimento: number
  insumosMedicos: number
  costoPoblacion: number
}

export interface RegistroAnulacion {
  idAnulacion: number
  motivo: string
  fechaHoraAnulacion: string
  usuarioResponsable: string
}

/** Campos de la Matriz de Venta Final comunes a la vista previa y a la Liquidación generada. */
export interface MatrizVentaFinal {
  idLote: string
  idGalpon: string
  nombreLote: string
  nombreGalpon: string
  siniestroTotal: boolean
  precioKgCop: number | null
  pollosVendidos: number
  pesoTotalKg: number
  pesoPromedioKg: number | null
  ventaBrutaCop: number
  mortalidadAves: number
  porcentajeMortalidad: number
  subtotalesCategoria: SubtotalesCategoria
  costosOperativosCop: number
  utilidadNetaCop: number
  fechaSincronizacionM1: string | null
  fechaSincronizacionM2: string | null
}

export interface LiquidacionPrevia extends MatrizVentaFinal {
  esVistaPrevia: true
  aviso: string
  huellaDatos: string
}

export type AccionLiquidacion = 'VER_DESGLOSE' | 'ANULAR'

export interface Liquidacion extends MatrizVentaFinal {
  idLiquidacion: number
  estado: 'ACTIVA' | 'ANULADA'
  fechaHoraGeneracion: string
  usuarioResponsable: string
  registroAnulacion: RegistroAnulacion | null
  accionesDisponibles: AccionLiquidacion[]
}

export interface VistaPreviaRequest {
  idLote: string
  precioKgCop: number | null
  usuario: string
}

export interface GenerarLiquidacionRequest extends VistaPreviaRequest {
  huellaDatos: string
}

/** Datos del lote que la lista envía al navegar, para no pedirlos de nuevo en la captura. */
export interface LoteParaLiquidar {
  nombreLote: string
  nombreGalpon: string
  siniestroTotal: boolean
}

export interface PartidaPendiente {
  categoria: 'ALIMENTO' | 'MEDICINA'
  concepto: string
  referenciaOrigen: string | null
}

/** Campos extra de los errores de CU03 (catálogo de errores de la arquitectura general). */
export interface ProblemaLiquidacion extends ProblemDetail {
  causa?: string
  partidasPendientes?: PartidaPendiente[]
  idLiquidacionActiva?: number
}
