import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { formatCop } from '../../shared/format/formatCop.ts'
import type { Desglose } from './types.ts'
import VerDesglosePage from './VerDesglosePage.tsx'

const ACTIVA: Desglose = {
  idLiquidacion: 42,
  estado: 'ACTIVA',
  idLote: 'b0000000-0000-0000-0000-000000000003',
  nombreLote: 'L-2026-03',
  nombreGalpon: 'Galpón Este',
  siniestroTotal: false,
  ingresos: {
    pollosVendidos: 8500,
    pesoTotalKg: 23_800,
    pesoPromedioKg: 2.8,
    precioKgCop: 4500,
    ventaBrutaCop: 107_100_000,
  },
  categorias: [
    {
      categoria: 'ALIMENTO',
      etiqueta: 'Alimento',
      subtotalCop: 64_800_000,
      partidas: [
        {
          concepto: 'Pre-inicio',
          cantidad: 6000,
          unidadMedida: 'KG',
          precioUnitarioCop: 2600,
          subtotalCop: 15_600_000,
          fuenteOrigen: 'MODULO_2',
          referenciaOrigen: 'PA-L3-1',
        },
        {
          concepto: 'Engorde',
          cantidad: 20_000.125,
          unidadMedida: 'KG',
          precioUnitarioCop: 2460,
          subtotalCop: 49_200_000,
          fuenteOrigen: 'MODULO_2',
          referenciaOrigen: 'PA-L3-2',
        },
      ],
    },
    { categoria: 'MEDICINA', etiqueta: 'Insumos Médicos', subtotalCop: 0, partidas: [] },
    {
      categoria: 'POBLACION',
      etiqueta: 'Costo de Población',
      subtotalCop: 18_000_000,
      partidas: [
        {
          concepto: 'Costo de adquisición del lote (9000 aves)',
          cantidad: 1,
          unidadMedida: 'LOTE',
          precioUnitarioCop: 18_000_000,
          subtotalCop: 18_000_000,
          fuenteOrigen: 'MODULO_1',
          referenciaOrigen: 'b0000000-0000-0000-0000-000000000003',
        },
      ],
    },
  ],
  mortalidadAves: 500,
  porcentajeMortalidad: 5.56,
  costosOperativosCop: 82_800_000,
  utilidadNetaCop: 24_300_000,
  registroAnulacion: null,
  exportacionPermitida: true,
}

const ANULADA: Desglose = {
  ...ACTIVA,
  estado: 'ANULADA',
  exportacionPermitida: false,
  registroAnulacion: {
    idAnulacion: 7,
    motivo: 'Error en el precio por kg',
    fechaHoraAnulacion: '2026-10-30T12:00:00Z',
    usuarioResponsable: 'Ana',
  },
}

const SINIESTRO: Desglose = {
  ...ACTIVA,
  siniestroTotal: true,
  ingresos: {
    pollosVendidos: 0,
    pesoTotalKg: 0,
    pesoPromedioKg: null,
    precioKgCop: null,
    ventaBrutaCop: 0,
  },
  utilidadNetaCop: -82_800_000,
}

function json(status: number, cuerpo: unknown) {
  return new Response(JSON.stringify(cuerpo), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}

function problema(status: number, tipo: string, detail: string) {
  return json(status, {
    type: `https://avicontrol.edu.co/errors/${tipo}`,
    title: 'Error',
    status,
    detail,
  })
}

// La pantalla usa espacio duro y Testing Library compara con el texto ya normalizado
const pesos = (valor: number) => formatCop(valor).replace(/\s/g, ' ')

const fetchMock = vi.fn<typeof fetch>()
const crearUrl = vi.fn<() => string>(() => 'blob:excel')
const descargas: string[] = []

function renderPagina() {
  const router = createMemoryRouter(
    [{ path: '/liquidaciones/:idLiquidacion/desglose', element: <VerDesglosePage /> }],
    { initialEntries: ['/liquidaciones/42/desglose'] },
  )
  render(<RouterProvider router={router} />)
}

describe('VerDesglosePage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    URL.createObjectURL = crearUrl
    URL.revokeObjectURL = vi.fn<() => void>()
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      descargas.push(this.download)
    })
  })

  afterEach(() => {
    fetchMock.mockReset()
    crearUrl.mockClear()
    descargas.length = 0
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  it('presenta ingresos y costos por categoría con su origen', async () => {
    fetchMock.mockResolvedValueOnce(json(200, ACTIVA))
    renderPagina()

    expect(await screen.findByRole('heading', { name: /Lote L-2026-03/ })).toBeInTheDocument()
    expect(fetchMock.mock.calls[0][0]).toBe(
      'http://localhost:8080/api/v1/liquidaciones/42/desglose',
    )
    const ingresos = within(screen.getByRole('region', { name: 'Ingresos' }))
    expect(ingresos.getByText('8.500')).toBeInTheDocument()
    expect(ingresos.getByText(pesos(107_100_000))).toBeInTheDocument()

    const costos = within(screen.getByRole('region', { name: 'Costos operativos' }))
    expect(costos.getByText('Módulo 2 / Partida de alimento · PA-L3-1')).toBeInTheDocument()
    expect(costos.getByText('20.000,125')).toBeInTheDocument()
    expect(costos.getByRole('row', { name: /Subtotal Alimento/ })).toHaveTextContent(
      pesos(64_800_000),
    )
    expect(costos.getByText('Sin partidas registradas')).toBeInTheDocument()
    expect(costos.getByRole('row', { name: /Total costos operativos/ })).toHaveTextContent(
      pesos(82_800_000),
    )
  })

  it('exporta el archivo con su nombre', async () => {
    fetchMock.mockResolvedValueOnce(json(200, ACTIVA)).mockResolvedValueOnce(new Response('xlsx'))
    renderPagina()

    await userEvent.click(await screen.findByRole('button', { name: 'Exportar a Excel' }))

    expect(fetchMock.mock.calls[1][0]).toBe(
      'http://localhost:8080/api/v1/liquidaciones/42/desglose/excel',
    )
    expect(descargas).toEqual(['Liquidacion_Lote_42.xlsx'])
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('si la exportación falla conserva la vista y permite reintentar', async () => {
    fetchMock
      .mockResolvedValueOnce(json(200, ACTIVA))
      .mockResolvedValueOnce(problema(500, 'error-interno', 'Ocurrió un error inesperado.'))
      .mockResolvedValueOnce(new Response('xlsx'))
    renderPagina()

    await userEvent.click(await screen.findByRole('button', { name: 'Exportar a Excel' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo generar el archivo')
    expect(screen.getByRole('region', { name: 'Costos operativos' })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(descargas).toEqual(['Liquidacion_Lote_42.xlsx'])
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('una anulada avisa con su registro y no se exporta', async () => {
    fetchMock.mockResolvedValueOnce(json(200, ANULADA))
    renderPagina()

    expect(await screen.findByText('LIQUIDACIÓN ANULADA')).toBeInTheDocument()
    expect(screen.getByText('Motivo: Error en el precio por kg')).toBeInTheDocument()
    expect(screen.getByText(/Por Ana/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Exportar a Excel' })).toBeDisabled()
  })

  it('si otro usuario la anuló muestra el estado actual', async () => {
    fetchMock
      .mockResolvedValueOnce(json(200, ACTIVA))
      .mockResolvedValueOnce(problema(409, 'exportacion-no-permitida', 'Está anulada.'))
      .mockResolvedValueOnce(json(200, ANULADA))
    renderPagina()

    await userEvent.click(await screen.findByRole('button', { name: 'Exportar a Excel' }))

    expect(await screen.findByText('LIQUIDACIÓN ANULADA')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Exportar a Excel' })).toBeDisabled()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('un siniestro total indica que no hubo venta', async () => {
    fetchMock.mockResolvedValueOnce(json(200, SINIESTRO))
    renderPagina()

    expect(await screen.findByText('Este lote no registró venta.')).toBeInTheDocument()
    const ingresos = within(screen.getByRole('region', { name: 'Ingresos' }))
    expect(ingresos.getAllByText('—')).toHaveLength(2)
    expect(ingresos.getByText(pesos(0))).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Exportar a Excel' })).toBeEnabled()
  })

  it('informa si la liquidación no existe', async () => {
    fetchMock.mockResolvedValueOnce(
      problema(404, 'liquidacion-no-encontrada', 'No se encontró la liquidación con ID 42.'),
    )
    renderPagina()

    expect(await screen.findByRole('alert')).toHaveTextContent('No se encontró la liquidación')
  })
})
