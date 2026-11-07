// Backend simulado: reemplaza a fetch cuando VITE_USE_MOCKS=true y responde con los mismos contratos,
// cálculos y errores que los controladores reales. Las liquidaciones se guardan en localStorage.
import type {
  GenerarLiquidacionRequest,
  Liquidacion,
  LiquidacionPrevia,
  MatrizVentaFinal,
  PartidaPendiente,
  VistaPreviaRequest,
} from '../features/liquidacion/types.ts'
import type { EtapaLote, LotePage, LoteResumen } from '../features/lotes/types.ts'
import type { ErrorCampo } from '../shared/api/httpClient.ts'
import type { HistorialItem, HistorialPage } from '../features/reportes/types.ts'
import { LOTES, type LoteMock, type PartidaMock } from './datos.ts'
import { LIQUIDACIONES_HISTORICAS } from './historial.ts'

const LATENCIA_MS = 150
const CLAVE_ESTADO = 'avicontrol-mock'
const TIPO_BASE = 'https://avicontrol.edu.co/errors/'
const TAMANO_MAXIMO = 6
const KG_ADICIONALES_POR_CAMBIO = 120

const hace = (minutos: number) => new Date(Date.now() - minutos * 60_000).toISOString()
const SYNC_M1 = hace(20)
const SYNC_M2 = hace(15)

interface Estado {
  liquidaciones: Liquidacion[]
  /** Lotes con cambiaTrasVistaPrevia cuya actualización de M2 ya "llegó". */
  lotesActualizados: string[]
}

function leerEstado(): Estado {
  const guardado = localStorage.getItem(CLAVE_ESTADO)
  return guardado ? (JSON.parse(guardado) as Estado) : { liquidaciones: [], lotesActualizados: [] }
}

function guardarEstado(estado: Estado) {
  localStorage.setItem(CLAVE_ESTADO, JSON.stringify(estado))
}

export function reiniciarDatosSimulados() {
  localStorage.removeItem(CLAVE_ESTADO)
}

/** Permite cortar el flujo desde cualquier validación, como una excepción del backend. */
class RespuestaError extends Error {
  readonly respuesta: Response

  constructor(respuesta: Response) {
    super(`HTTP ${respuesta.status}`)
    this.respuesta = respuesta
  }
}

function json(status: number, cuerpo: unknown) {
  return new Response(JSON.stringify(cuerpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function problema(
  status: number,
  tipo: string,
  title: string,
  detail: string,
  extra: Record<string, unknown> = {},
) {
  const cuerpo = {
    type: TIPO_BASE + tipo,
    title,
    status,
    detail,
    timestamp: new Date().toISOString(),
    ...extra,
  }
  return new RespuestaError(
    new Response(JSON.stringify(cuerpo), {
      status,
      headers: { 'Content-Type': 'application/problem+json' },
    }),
  )
}

function validacion(errores: ErrorCampo[]) {
  return problema(
    400,
    'validacion',
    'Error de validación',
    'La solicitud contiene parámetros inválidos.',
    { errores },
  )
}

function noLiquidable(causa: string, detail: string, extra: Record<string, unknown> = {}) {
  return problema(422, 'lote-no-liquidable', 'Lote no apto para liquidar', detail, {
    causa,
    ...extra,
  })
}

function listarLotes(parametros: URLSearchParams): LotePage {
  const { liquidaciones } = leerEstado()
  const etapa = parametros.get('etapa') as EtapaLote | null
  const termino = parametros.get('search')?.trim().toLowerCase()
  const page = Number(parametros.get('page') ?? 0)
  const size = Math.min(Number(parametros.get('size') ?? TAMANO_MAXIMO), TAMANO_MAXIMO)

  const liquidados = new Set(
    liquidaciones.filter((l) => l.estado === 'ACTIVA').map((l) => l.idLote),
  )
  const listables = LOTES.filter((l) => !liquidados.has(l.idLote))
  const filtrados = listables
    .filter((l) => !etapa || l.etapa === etapa)
    .filter(
      (l) =>
        !termino ||
        l.nombreLote.toLowerCase().includes(termino) ||
        l.nombreGalpon.toLowerCase().includes(termino) ||
        l.idLote === termino ||
        l.idGalpon === termino,
    )
    .toSorted(
      (a, b) =>
        a.fechaIngreso.localeCompare(b.fechaIngreso) || a.nombreLote.localeCompare(b.nombreLote),
    )

  const content = filtrados.slice(page * size, (page + 1) * size).map(aFila)
  let mensaje = 'No se encontraron resultados'
  if (content.length > 0) mensaje = `${content.length} de ${filtrados.length} lotes`
  else if (listables.length === 0) mensaje = 'No hay lotes disponibles para consultar'

  return {
    content,
    page,
    size,
    totalElements: filtrados.length,
    totalPages: Math.ceil(filtrados.length / size),
    mensaje,
    sincronizacion: {
      modulo1: { ultimaSincronizacionExitosa: SYNC_M1, desactualizada: false },
      modulo2: { ultimaSincronizacionExitosa: SYNC_M2, desactualizada: false },
    },
  }
}

function aFila(lote: LoteMock): LoteResumen {
  const siniestroTotal = lote.poblacionActual === 0
  return {
    idLote: lote.idLote,
    nombreLote: lote.nombreLote,
    fechaIngreso: lote.fechaIngreso,
    idGalpon: lote.idGalpon,
    nombreGalpon: lote.nombreGalpon,
    etapa: lote.etapa,
    siniestroTotal,
    accion: 'GENERAR_LIQUIDACION',
    accionHabilitada: lote.etapa === 'POR_LIQUIDAR' && (lote.resultado !== null || siniestroTotal),
  }
}

/** Las mismas precondiciones y en el mismo orden que GenerarLiquidacionService. */
function cargarYValidar(solicitud: VistaPreviaRequest, estado: Estado): LoteMock {
  const usuario = solicitud.usuario?.trim() ?? ''
  const errores: ErrorCampo[] = []
  if (usuario.length < 3 || usuario.length > 100) {
    errores.push({
      campo: 'usuario',
      mensaje: usuario
        ? 'El usuario responsable debe tener entre 3 y 100 caracteres.'
        : 'El usuario responsable es obligatorio.',
    })
  }
  if (solicitud.precioKgCop !== null && solicitud.precioKgCop <= 0) {
    errores.push({ campo: 'precioKgCop', mensaje: 'El precio por kg debe ser mayor que 0' })
  }
  if (errores.length > 0) throw validacion(errores)

  const lote = LOTES.find((l) => l.idLote === solicitud.idLote)
  if (!lote) {
    throw problema(
      404,
      'lote-no-encontrado',
      'Recurso no encontrado',
      `No se encontró el lote ${solicitud.idLote} en la copia local sincronizada.`,
    )
  }

  const siniestroTotal = lote.poblacionActual === 0
  if (siniestroTotal && solicitud.precioKgCop !== null) {
    throw validacion([
      {
        campo: 'precioKgCop',
        mensaje: 'El lote es un siniestro total: no se ingresa precio por kg.',
      },
    ])
  }
  if (!siniestroTotal && solicitud.precioKgCop === null) {
    throw validacion([{ campo: 'precioKgCop', mensaje: 'El precio por kg es obligatorio.' }])
  }

  const activa = estado.liquidaciones.find((l) => l.idLote === lote.idLote && l.estado === 'ACTIVA')
  if (activa) {
    throw problema(
      409,
      'liquidacion-ya-existente',
      'Conflicto de Liquidación',
      `El lote ${lote.idLote} ya tiene una Liquidación ACTIVA (ID: ${activa.idLiquidacion}). Anúlela antes de generar una nueva.`,
      { idLiquidacionActiva: activa.idLiquidacion },
    )
  }

  if (lote.etapa !== 'POR_LIQUIDAR') {
    throw noLiquidable(
      'SIN_ALERTA_VACIADO',
      'La Liquidación se habilita cuando Módulo 1 registre la alerta de vaciado sanitario tras retirar todas las aves del galpón.',
    )
  }
  if (!lote.resultado && !siniestroTotal) {
    throw noLiquidable(
      'SIN_RESULTADO_SACRIFICIO',
      'El lote no cuenta con resultado final de sacrificio sincronizado desde Módulo 2. No es posible liquidar',
    )
  }
  if (lote.alimento.length === 0) {
    throw noLiquidable(
      'SIN_PARTIDAS_ALIMENTO',
      'No hay partidas de alimento sincronizadas para este ciclo',
    )
  }

  const sinPrecio = (categoria: PartidaPendiente['categoria'], partidas: PartidaMock[]) =>
    partidas
      .filter((p) => p.precioUnitarioCop === null)
      .map((p) => ({ categoria, concepto: p.concepto, referenciaOrigen: p.referencia }))
  const partidasPendientes = [
    ...sinPrecio('ALIMENTO', lote.alimento),
    ...sinPrecio('MEDICINA', lote.medicamentos),
  ]
  if (partidasPendientes.length > 0) {
    throw noLiquidable(
      'PARTIDAS_SIN_PRECIO',
      'Hay partidas de costo sin precio aplicable. No es posible liquidar.',
      { partidasPendientes },
    )
  }
  return lote
}

function pesoActual(lote: LoteMock, estado: Estado) {
  const peso = lote.resultado?.pesoTotalKg ?? 0
  return estado.lotesActualizados.includes(lote.idLote) ? peso + KG_ADICIONALES_POR_CAMBIO : peso
}

function subtotal(partidas: PartidaMock[]) {
  return partidas.reduce(
    (total, p) => total + Math.round(p.cantidad * (p.precioUnitarioCop ?? 0)),
    0,
  )
}

/** Igual que CalculoLiquidacion: la venta usa el peso total y la mortalidad las poblaciones de M1. */
function calcular(lote: LoteMock, precioKgCop: number | null, estado: Estado): MatrizVentaFinal {
  const siniestroTotal = lote.poblacionActual === 0
  const pollosVendidos = siniestroTotal ? 0 : (lote.resultado?.pollos ?? 0)
  const pesoTotalKg = siniestroTotal ? 0 : pesoActual(lote, estado)
  const ventaBrutaCop = siniestroTotal ? 0 : Math.round(pesoTotalKg * (precioKgCop ?? 0))
  const mortalidadAves = lote.poblacionInicial - lote.poblacionActual
  const subtotalesCategoria = {
    alimento: subtotal(lote.alimento),
    insumosMedicos: subtotal(lote.medicamentos),
    costoPoblacion: lote.costoPoblacionCop,
  }
  const costosOperativosCop =
    subtotalesCategoria.alimento +
    subtotalesCategoria.insumosMedicos +
    subtotalesCategoria.costoPoblacion

  return {
    idLote: lote.idLote,
    idGalpon: lote.idGalpon,
    nombreLote: lote.nombreLote,
    nombreGalpon: lote.nombreGalpon,
    siniestroTotal,
    precioKgCop: siniestroTotal ? null : precioKgCop,
    pollosVendidos,
    pesoTotalKg,
    pesoPromedioKg: pollosVendidos === 0 ? null : redondear2(pesoTotalKg / pollosVendidos),
    ventaBrutaCop,
    mortalidadAves,
    porcentajeMortalidad: redondear2((mortalidadAves / lote.poblacionInicial) * 100),
    subtotalesCategoria,
    costosOperativosCop,
    utilidadNetaCop: ventaBrutaCop - costosOperativosCop,
    fechaSincronizacionM1: SYNC_M1,
    fechaSincronizacionM2: SYNC_M2,
  }
}

function redondear2(valor: number) {
  return Math.round(valor * 100) / 100
}

function huella(lote: LoteMock, precioKgCop: number | null, estado: Estado) {
  return JSON.stringify([lote.idLote, precioKgCop, pesoActual(lote, estado)])
}

function vistaPrevia(solicitud: VistaPreviaRequest): LiquidacionPrevia {
  const estado = leerEstado()
  const lote = cargarYValidar(solicitud, estado)
  const previa: LiquidacionPrevia = {
    ...calcular(lote, solicitud.precioKgCop, estado),
    esVistaPrevia: true,
    aviso: 'Vista previa · sin generar',
    huellaDatos: huella(lote, solicitud.precioKgCop, estado),
  }
  if (lote.cambiaTrasVistaPrevia && !estado.lotesActualizados.includes(lote.idLote)) {
    estado.lotesActualizados.push(lote.idLote)
    guardarEstado(estado)
  }
  return previa
}

function generar(solicitud: GenerarLiquidacionRequest): Liquidacion {
  const estado = leerEstado()
  const lote = cargarYValidar(solicitud, estado)
  if (huella(lote, solicitud.precioKgCop, estado) !== solicitud.huellaDatos) {
    throw problema(
      409,
      'vista-previa-desactualizada',
      'Los datos cambiaron',
      'Los datos sincronizados del lote cambiaron después de la vista previa. Revise la nueva vista previa antes de confirmar.',
    )
  }
  const liquidacion: Liquidacion = {
    ...calcular(lote, solicitud.precioKgCop, estado),
    idLiquidacion: estado.liquidaciones.length + 1,
    estado: 'ACTIVA',
    fechaHoraGeneracion: new Date().toISOString(),
    usuarioResponsable: solicitud.usuario.trim(),
    registroAnulacion: null,
    accionesDisponibles: ['VER_DESGLOSE', 'ANULAR'],
  }
  estado.liquidaciones.push(liquidacion)
  guardarEstado(estado)
  return liquidacion
}

function consultar(id: string): Liquidacion {
  if (!/^\d+$/.test(id)) {
    throw validacion([{ campo: 'id', mensaje: `El valor '${id}' no es válido.` }])
  }
  const liquidacion = todasLasLiquidaciones().find((l) => l.idLiquidacion === Number(id))
  if (!liquidacion) {
    throw problema(
      404,
      'liquidacion-no-encontrada',
      'Recurso no encontrado',
      `No se encontró la liquidación con ID ${id}.`,
    )
  }
  return liquidacion
}

function todasLasLiquidaciones() {
  return [...LIQUIDACIONES_HISTORICAS, ...leerEstado().liquidaciones]
}

/** Día calendario de un instante en Colombia, como compara el backend (plan 004). */
const FECHA_COLOMBIA = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Bogota' })

function aFilaHistorial(l: Liquidacion): HistorialItem {
  return {
    idLiquidacion: l.idLiquidacion,
    fechaHoraGeneracion: l.fechaHoraGeneracion,
    idGalpon: l.idGalpon,
    nombreGalpon: l.nombreGalpon,
    idLote: l.idLote,
    nombreLote: l.nombreLote,
    ventaBrutaCop: l.ventaBrutaCop,
    porcentajeMortalidad: l.porcentajeMortalidad,
    costosOperativosCop: l.costosOperativosCop,
    utilidadNetaCop: l.utilidadNetaCop,
    estado: l.estado,
  }
}

function consultarHistorial(parametros: URLSearchParams): HistorialPage {
  const termino = parametros.get('search')?.trim().toLowerCase() ?? ''
  const desde = parametros.get('desde') ?? ''
  const hasta = parametros.get('hasta') ?? ''
  const page = Number(parametros.get('page') ?? 0)
  const size = Number(parametros.get('size') ?? 10)

  const errores: ErrorCampo[] = []
  if (termino.length > 100) {
    errores.push({ campo: 'search', mensaje: 'La búsqueda admite máximo 100 caracteres.' })
  }
  if (!Number.isInteger(page) || page < 0) {
    errores.push({ campo: 'page', mensaje: 'La página debe ser mayor o igual a 0.' })
  }
  if (!Number.isInteger(size) || size < 1 || size > 50) {
    errores.push({ campo: 'size', mensaje: 'El tamaño de página debe estar entre 1 y 50.' })
  }
  if (errores.length > 0) throw validacion(errores)
  if (desde && hasta && desde > hasta) {
    throw problema(
      400,
      'rango-fechas-invalido',
      'Rango de fechas inválido',
      `La fecha inicial (${desde}) debe ser menor o igual a la fecha final (${hasta}).`,
    )
  }

  const todas = todasLasLiquidaciones()
  const filtradas = todas
    .filter(
      (l) =>
        !termino ||
        l.nombreLote.toLowerCase().includes(termino) ||
        l.nombreGalpon.toLowerCase().includes(termino) ||
        l.idLote === termino ||
        l.idGalpon === termino,
    )
    .filter((l) => {
      const fecha = FECHA_COLOMBIA.format(new Date(l.fechaHoraGeneracion))
      return (!desde || fecha >= desde) && (!hasta || fecha <= hasta)
    })
    .toSorted((a, b) => b.fechaHoraGeneracion.localeCompare(a.fechaHoraGeneracion))

  const content = filtradas.slice(page * size, (page + 1) * size).map(aFilaHistorial)
  let mensaje: string | null = null
  if (content.length === 0) {
    mensaje =
      todas.length === 0 && !termino && !desde && !hasta
        ? 'No hay liquidaciones registradas aún'
        : 'No se encontraron liquidaciones para los criterios de búsqueda aplicados'
  }

  return {
    content,
    page,
    size,
    totalElements: filtradas.length,
    totalPages: Math.ceil(filtradas.length / size),
    mensaje,
  }
}

export async function mockFetch(url: string, init: RequestInit = {}): Promise<Response> {
  await new Promise((resolver) => setTimeout(resolver, LATENCIA_MS))
  const { pathname, searchParams } = new URL(url)
  const metodo = init.method ?? 'GET'
  const cuerpo: unknown = init.body ? JSON.parse(String(init.body)) : undefined
  const idLiquidacion = /^\/api\/v1\/liquidaciones\/([^/]+)$/.exec(pathname)?.[1]

  try {
    if (metodo === 'GET' && pathname === '/api/v1/lotes') {
      return json(200, listarLotes(searchParams))
    }
    if (metodo === 'GET' && pathname === '/api/v1/sincronizacion/estado') {
      return json(200, listarLotes(searchParams).sincronizacion)
    }
    if (metodo === 'GET' && pathname === '/api/v1/liquidaciones') {
      return json(200, consultarHistorial(searchParams))
    }
    if (metodo === 'POST' && pathname === '/api/v1/liquidaciones/previa') {
      return json(200, vistaPrevia(cuerpo as VistaPreviaRequest))
    }
    if (metodo === 'POST' && pathname === '/api/v1/liquidaciones') {
      return json(201, generar(cuerpo as GenerarLiquidacionRequest))
    }
    if (metodo === 'GET' && idLiquidacion) {
      return json(200, consultar(decodeURIComponent(idLiquidacion)))
    }
    throw problema(
      404,
      'recurso-no-encontrado',
      'Recurso no encontrado',
      `${metodo} ${pathname} no está simulado.`,
    )
  } catch (e) {
    if (e instanceof RespuestaError) return e.respuesta
    throw e
  }
}
