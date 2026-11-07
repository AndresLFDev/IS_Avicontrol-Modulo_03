// Datos simulados: L-2026-01 a 06 replican el seed del backend (V1000__datos_demo_galpones.sql);
// L-2026-07 y 08 se agregan para probar la paginación y dos casos que el seed no cubre.
import type { EtapaLote } from '../features/lotes/types.ts'

export interface PartidaMock {
  concepto: string
  cantidad: number
  precioUnitarioCop: number | null
  referencia: string
}

export interface LoteMock {
  idLote: string
  nombreLote: string
  fechaIngreso: string
  idGalpon: string
  nombreGalpon: string
  etapa: EtapaLote
  poblacionInicial: number
  poblacionActual: number
  costoPoblacionCop: number
  resultado: { pollos: number; pesoTotalKg: number } | null
  alimento: PartidaMock[]
  medicamentos: PartidaMock[]
  /** Simula que llega un evento de M2 entre la vista previa y la confirmación (409). */
  cambiaTrasVistaPrevia?: boolean
}

export const LOTES: LoteMock[] = [
  {
    idLote: 'b0000000-0000-0000-0000-000000000001',
    nombreLote: 'L-2026-01',
    fechaIngreso: '2026-09-20',
    idGalpon: 'a0000000-0000-0000-0000-000000000001',
    nombreGalpon: 'Galpón Norte',
    etapa: 'PRODUCTIVO',
    poblacionInicial: 9500,
    poblacionActual: 9420,
    costoPoblacionCop: 19_000_000,
    resultado: null,
    alimento: [],
    medicamentos: [],
  },
  {
    idLote: 'b0000000-0000-0000-0000-000000000002',
    nombreLote: 'L-2026-02',
    fechaIngreso: '2026-08-25',
    idGalpon: 'a0000000-0000-0000-0000-000000000002',
    nombreGalpon: 'Galpón Sur',
    etapa: 'EN_COSECHA',
    poblacionInicial: 9800,
    poblacionActual: 9550,
    costoPoblacionCop: 19_600_000,
    resultado: null,
    alimento: [],
    medicamentos: [],
  },
  {
    // Caso normal (CU03 esc. 1): a $4.500/kg da venta de $107.100.000 y utilidad de $22.100.000
    idLote: 'b0000000-0000-0000-0000-000000000003',
    nombreLote: 'L-2026-03',
    fechaIngreso: '2026-08-01',
    idGalpon: 'a0000000-0000-0000-0000-000000000003',
    nombreGalpon: 'Galpón Este',
    etapa: 'POR_LIQUIDAR',
    poblacionInicial: 9000,
    poblacionActual: 8500,
    costoPoblacionCop: 18_000_000,
    resultado: { pollos: 8500, pesoTotalKg: 23_800 },
    alimento: [
      {
        concepto: 'Pre-inicio',
        cantidad: 6000,
        precioUnitarioCop: 2600,
        referencia: 'REC-2026-101',
      },
      { concepto: 'Inicio', cantidad: 12_000, precioUnitarioCop: 2500, referencia: 'REC-2026-108' },
      { concepto: 'Engorde', cantidad: 8000, precioUnitarioCop: 2400, referencia: 'REC-2026-118' },
    ],
    medicamentos: [
      {
        concepto: 'Amoxicilina',
        cantidad: 30,
        precioUnitarioCop: 20_000,
        referencia: 'REC-2026-090',
      },
      {
        concepto: 'Amoxicilina',
        cantidad: 20,
        precioUnitarioCop: 21_000,
        referencia: 'REC-2026-101',
      },
      {
        concepto: 'Vacuna Newcastle',
        cantidad: 9440,
        precioUnitarioCop: 125,
        referencia: 'REC-2026-085',
      },
    ],
  },
  {
    // Siniestro total: sin venta, sin precio y costos de $32.800.000
    idLote: 'b0000000-0000-0000-0000-000000000004',
    nombreLote: 'L-2026-04',
    fechaIngreso: '2026-08-05',
    idGalpon: 'a0000000-0000-0000-0000-000000000004',
    nombreGalpon: 'Galpón Oeste',
    etapa: 'POR_LIQUIDAR',
    poblacionInicial: 8000,
    poblacionActual: 0,
    costoPoblacionCop: 10_000_000,
    resultado: null,
    alimento: [
      { concepto: 'Inicio', cantidad: 7000, precioUnitarioCop: 3000, referencia: 'REC-2026-109' },
    ],
    medicamentos: [
      {
        concepto: 'Enrofloxacina',
        cantidad: 90,
        precioUnitarioCop: 20_000,
        referencia: 'REC-2026-095',
      },
    ],
  },
  {
    idLote: 'b0000000-0000-0000-0000-000000000005',
    nombreLote: 'L-2026-05',
    fechaIngreso: '2026-09-10',
    idGalpon: 'a0000000-0000-0000-0000-000000000005',
    nombreGalpon: 'Galpón Central',
    etapa: 'AISLAMIENTO',
    poblacionInicial: 7800,
    poblacionActual: 7610,
    costoPoblacionCop: 15_600_000,
    resultado: null,
    alimento: [],
    medicamentos: [],
  },
  {
    // Una partida sin precio: 422 PARTIDAS_SIN_PRECIO
    idLote: 'b0000000-0000-0000-0000-000000000006',
    nombreLote: 'L-2026-06',
    fechaIngreso: '2026-08-03',
    idGalpon: 'a0000000-0000-0000-0000-000000000006',
    nombreGalpon: 'Galpón Alto',
    etapa: 'POR_LIQUIDAR',
    poblacionInicial: 8800,
    poblacionActual: 8650,
    costoPoblacionCop: 17_600_000,
    resultado: { pollos: 8600, pesoTotalKg: 24_510 },
    alimento: [
      { concepto: 'Inicio', cantidad: 11_000, precioUnitarioCop: 2500, referencia: 'REC-2026-110' },
      { concepto: 'Engorde', cantidad: 9000, precioUnitarioCop: null, referencia: 'REC-2026-121' },
    ],
    medicamentos: [],
  },
  {
    // Solo mock: los datos cambian después de la primera vista previa (409 y nueva vista previa)
    idLote: 'b0000000-0000-0000-0000-000000000007',
    nombreLote: 'L-2026-07',
    fechaIngreso: '2026-08-10',
    idGalpon: 'a0000000-0000-0000-0000-000000000008',
    nombreGalpon: 'Galpón Lago',
    etapa: 'POR_LIQUIDAR',
    poblacionInicial: 9000,
    poblacionActual: 8700,
    costoPoblacionCop: 18_000_000,
    resultado: { pollos: 8650, pesoTotalKg: 24_000 },
    alimento: [
      { concepto: 'Inicio', cantidad: 20_000, precioUnitarioCop: 2500, referencia: 'REC-2026-130' },
    ],
    medicamentos: [
      {
        concepto: 'Amoxicilina',
        cantidad: 40,
        precioUnitarioCop: 20_000,
        referencia: 'REC-2026-131',
      },
    ],
    cambiaTrasVistaPrevia: true,
  },
  {
    // Solo mock: por liquidar pero sin resultado de M2, así que la acción queda deshabilitada
    idLote: 'b0000000-0000-0000-0000-000000000008',
    nombreLote: 'L-2026-08',
    fechaIngreso: '2026-08-12',
    idGalpon: 'a0000000-0000-0000-0000-000000000009',
    nombreGalpon: 'Galpón Loma',
    etapa: 'POR_LIQUIDAR',
    poblacionInicial: 8500,
    poblacionActual: 8300,
    costoPoblacionCop: 17_000_000,
    resultado: null,
    alimento: [
      { concepto: 'Inicio', cantidad: 9000, precioUnitarioCop: 2500, referencia: 'REC-2026-140' },
    ],
    medicamentos: [],
  },
]
