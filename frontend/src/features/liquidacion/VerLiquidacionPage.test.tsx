import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import type { Liquidacion } from './types.ts'
import VerLiquidacionPage from './VerLiquidacionPage.tsx'

const ACTIVA: Liquidacion = {
  idLiquidacion: 42,
  estado: 'ACTIVA',
  idLote: 'b0000000-0000-0000-0000-000000000004',
  idGalpon: 'a0000000-0000-0000-0000-000000000004',
  nombreLote: 'L-2026-04',
  nombreGalpon: 'Galpón Oeste',
  siniestroTotal: true,
  precioKgCop: null,
  pollosVendidos: 0,
  pesoTotalKg: 0,
  pesoPromedioKg: null,
  ventaBrutaCop: 0,
  mortalidadAves: 8000,
  porcentajeMortalidad: 100,
  subtotalesCategoria: {
    alimento: 21_000_000,
    insumosMedicos: 1_800_000,
    costoPoblacion: 10_000_000,
  },
  costosOperativosCop: 32_800_000,
  utilidadNetaCop: -32_800_000,
  fechaHoraGeneracion: '2026-10-29T15:00:00Z',
  usuarioResponsable: 'Jorge',
  fechaSincronizacionM1: '2026-10-20T13:00:01Z',
  fechaSincronizacionM2: '2026-10-20T14:20:01Z',
  registroAnulacion: null,
  accionesDisponibles: ['VER_DESGLOSE', 'ANULAR'],
}

function json(status: number, cuerpo: unknown) {
  return new Response(JSON.stringify(cuerpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('VerLiquidacionPage', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  function renderEn(id: string) {
    const router = createMemoryRouter(
      [{ path: '/liquidaciones/:idLiquidacion', element: <VerLiquidacionPage /> }],
      { initialEntries: [`/liquidaciones/${id}`] },
    )
    render(<RouterProvider router={router} />)
  }

  it('muestra una liquidación activa de siniestro total con sus acciones', async () => {
    fetchMock.mockResolvedValue(json(200, ACTIVA))

    renderEn('42')

    expect(await screen.findByText('ACTIVA')).toBeInTheDocument()
    expect(screen.getByText('Siniestro total')).toBeInTheDocument()
    expect(screen.getByText(/-\s?\$\s?32\.800\.000/)).toHaveClass('text-error')
    expect(screen.getByRole('button', { name: 'Anular liquidación' })).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/liquidaciones/42',
      expect.anything(),
    )
  })

  it('una anulada muestra el motivo y no ofrece anular', async () => {
    fetchMock.mockResolvedValue(
      json(200, {
        ...ACTIVA,
        estado: 'ANULADA',
        registroAnulacion: {
          idAnulacion: 7,
          motivo: 'Error en el precio por kg',
          fechaHoraAnulacion: '2026-10-30T12:00:00Z',
          usuarioResponsable: 'Ana',
        },
        accionesDisponibles: ['VER_DESGLOSE'],
      }),
    )

    renderEn('42')

    expect(await screen.findByText('Motivo: Error en el precio por kg')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Anular liquidación' })).not.toBeInTheDocument()
  })

  it('informa si no existe', async () => {
    fetchMock.mockResolvedValue(
      json(404, {
        type: 'https://avicontrol.edu.co/errors/liquidacion-no-encontrada',
        title: 'Recurso no encontrado',
        status: 404,
        detail: 'No se encontró la liquidación con ID 999.',
      }),
    )

    renderEn('999')

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se encontró la liquidación con ID 999.',
    )
  })
})
