// Liquidaciones ya generadas para probar el historial (CU06): 13 filas para ver la paginación, un
// siniestro total, una pérdida, un lote liquidado dos veces (L-2025-08) y una fila cuya fecha UTC
// cae un día después de la fecha en Colombia (L-2025-09). Los totales se calculan como en el backend.
import type { Liquidacion, RegistroAnulacion } from '../features/liquidacion/types.ts'
import { LOTES } from './datos.ts'

const GALPONES = {
  norte: { idGalpon: LOTES[0].idGalpon, nombreGalpon: LOTES[0].nombreGalpon },
  sur: { idGalpon: LOTES[1].idGalpon, nombreGalpon: LOTES[1].nombreGalpon },
  este: { idGalpon: LOTES[2].idGalpon, nombreGalpon: LOTES[2].nombreGalpon },
  oeste: { idGalpon: LOTES[3].idGalpon, nombreGalpon: LOTES[3].nombreGalpon },
  central: { idGalpon: LOTES[4].idGalpon, nombreGalpon: LOTES[4].nombreGalpon },
  alto: { idGalpon: LOTES[5].idGalpon, nombreGalpon: LOTES[5].nombreGalpon },
}

interface Historica {
  generada: string
  galpon: keyof typeof GALPONES
  lote: string
  /** UUID del lote; por defecto se deriva del número de ciclo de L-2025-NN. */
  idLote?: string
  precio: number | null
  pollos: number
  pesoKg: number
  poblacion: [inicial: number, final: number]
  costos: [alimento: number, medicina: number, poblacion: number]
  usuario: string
  anulacion?: Omit<RegistroAnulacion, 'idAnulacion'>
}

const redondear2 = (valor: number) => Math.round(valor * 100) / 100

function aLiquidacion(h: Historica, indice: number): Liquidacion {
  const [inicial, final] = h.poblacion
  const [alimento, insumosMedicos, costoPoblacion] = h.costos
  const ventaBrutaCop = Math.round(h.pesoKg * (h.precio ?? 0))
  const costosOperativosCop = alimento + insumosMedicos + costoPoblacion
  const idLiquidacion = 1001 + indice
  return {
    ...GALPONES[h.galpon],
    idLiquidacion,
    estado: h.anulacion ? 'ANULADA' : 'ACTIVA',
    idLote: h.idLote ?? `c0000000-0000-0000-0000-0000000000${h.lote.slice(-2)}`,
    nombreLote: h.lote,
    siniestroTotal: final === 0,
    precioKgCop: h.precio,
    pollosVendidos: h.pollos,
    pesoTotalKg: h.pesoKg,
    pesoPromedioKg: h.pollos === 0 ? null : redondear2(h.pesoKg / h.pollos),
    ventaBrutaCop,
    mortalidadAves: inicial - final,
    porcentajeMortalidad: redondear2(((inicial - final) / inicial) * 100),
    subtotalesCategoria: { alimento, insumosMedicos, costoPoblacion },
    costosOperativosCop,
    utilidadNetaCop: ventaBrutaCop - costosOperativosCop,
    fechaHoraGeneracion: h.generada,
    usuarioResponsable: h.usuario,
    fechaSincronizacionM1: h.generada,
    fechaSincronizacionM2: h.generada,
    registroAnulacion: h.anulacion ? { idAnulacion: idLiquidacion, ...h.anulacion } : null,
    accionesDisponibles: h.anulacion ? ['VER_DESGLOSE'] : ['VER_DESGLOSE', 'ANULAR'],
  }
}

export const LIQUIDACIONES_HISTORICAS: Liquidacion[] = (
  [
    {
      generada: '2025-02-14T15:00:00Z',
      galpon: 'norte',
      lote: 'L-2025-01',
      precio: 4300,
      pollos: 8900,
      pesoKg: 24_500,
      poblacion: [9200, 8900],
      costos: [62_000_000, 1_900_000, 18_400_000],
      usuario: 'Laura Pérez',
    },
    {
      generada: '2025-04-22T14:30:00Z',
      galpon: 'sur',
      lote: 'L-2025-02',
      precio: 4400,
      pollos: 9000,
      pesoKg: 25_200,
      poblacion: [9300, 9000],
      costos: [64_000_000, 2_100_000, 18_600_000],
      usuario: 'Laura Pérez',
    },
    {
      generada: '2025-06-18T16:10:00Z',
      galpon: 'este',
      lote: 'L-2025-03',
      precio: 4250,
      pollos: 8300,
      pesoKg: 22_800,
      poblacion: [9000, 8300],
      costos: [61_000_000, 3_400_000, 18_000_000],
      usuario: 'Jorge Díaz',
    },
    {
      generada: '2025-08-05T13:45:00Z',
      galpon: 'oeste',
      lote: 'L-2025-04',
      precio: null,
      pollos: 0,
      pesoKg: 0,
      poblacion: [8000, 0],
      costos: [19_000_000, 1_500_000, 16_000_000],
      usuario: 'Jorge Díaz',
    },
    {
      generada: '2025-09-30T15:20:00Z',
      galpon: 'central',
      lote: 'L-2025-05',
      precio: 3600,
      pollos: 7500,
      pesoKg: 20_200,
      poblacion: [7800, 7500],
      costos: [60_000_000, 2_600_000, 15_600_000],
      usuario: 'Laura Pérez',
    },
    {
      generada: '2025-11-12T14:00:00Z',
      galpon: 'alto',
      lote: 'L-2025-06',
      precio: 4500,
      pollos: 8400,
      pesoKg: 23_700,
      poblacion: [8800, 8400],
      costos: [63_000_000, 2_200_000, 17_600_000],
      usuario: 'Jorge Díaz',
    },
    {
      generada: '2026-01-20T15:40:00Z',
      galpon: 'norte',
      lote: 'L-2025-07',
      precio: 4600,
      pollos: 9000,
      pesoKg: 25_500,
      poblacion: [9400, 9000],
      costos: [65_000_000, 2_000_000, 18_800_000],
      usuario: 'Laura Pérez',
    },
    {
      generada: '2026-02-10T14:15:00Z',
      galpon: 'sur',
      lote: 'L-2025-08',
      precio: 3800,
      pollos: 9100,
      pesoKg: 25_000,
      poblacion: [9500, 9100],
      costos: [66_000_000, 2_300_000, 19_000_000],
      usuario: 'Jorge Díaz',
      anulacion: {
        motivo: 'El precio por kg se digitó mal: el acordado era $4.700.',
        fechaHoraAnulacion: '2026-02-11T13:05:00Z',
        usuarioResponsable: 'Laura Pérez',
      },
    },
    {
      generada: '2026-02-12T15:00:00Z',
      galpon: 'sur',
      lote: 'L-2025-08',
      precio: 4700,
      pollos: 9100,
      pesoKg: 25_000,
      poblacion: [9500, 9100],
      costos: [66_000_000, 2_300_000, 19_000_000],
      usuario: 'Laura Pérez',
    },
    {
      // 28 de febrero a las 9:00 p. m. en Colombia: en UTC ya es 1 de marzo
      generada: '2026-03-01T02:00:00Z',
      galpon: 'este',
      lote: 'L-2025-09',
      precio: 4550,
      pollos: 8600,
      pesoKg: 24_100,
      poblacion: [9000, 8600],
      costos: [63_500_000, 2_400_000, 18_000_000],
      usuario: 'Jorge Díaz',
    },
    {
      generada: '2026-05-19T16:30:00Z',
      galpon: 'central',
      lote: 'L-2025-10',
      precio: 4650,
      pollos: 7500,
      pesoKg: 21_000,
      poblacion: [7800, 7500],
      costos: [54_000_000, 1_800_000, 15_600_000],
      usuario: 'Laura Pérez',
    },
    {
      generada: '2026-08-27T14:50:00Z',
      galpon: 'alto',
      lote: 'L-2025-11',
      precio: 4800,
      pollos: 8500,
      pesoKg: 24_000,
      poblacion: [8800, 8500],
      costos: [62_000_000, 2_100_000, 17_600_000],
      usuario: 'Jorge Díaz',
    },
    {
      // Vuelve a la lista de lotes como POR_LIQUIDAR porque quedó anulada
      generada: '2026-10-21T15:30:00Z',
      galpon: 'este',
      lote: 'L-2026-03',
      idLote: LOTES[2].idLote,
      precio: 4200,
      pollos: 8500,
      pesoKg: 23_800,
      poblacion: [9000, 8500],
      costos: [64_800_000, 2_200_000, 18_000_000],
      usuario: 'Jorge Díaz',
      anulacion: {
        motivo: 'Se liquidó con un precio anterior al acordado con el comprador.',
        fechaHoraAnulacion: '2026-10-22T13:10:00Z',
        usuarioResponsable: 'Laura Pérez',
      },
    },
  ] satisfies Historica[]
).map(aLiquidacion)
