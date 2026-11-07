import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import type { Liquidacion } from './types.ts'
import VerLiquidacionPage from './VerLiquidacionPage.tsx'

const REGISTRO = {
  idAnulacion: 7,
  motivo: 'Error en el precio por kg',
  fechaHoraAnulacion: '2026-10-30T12:00:00Z',
  usuarioResponsable: 'Ana',
}

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

const ANULADA: Liquidacion = {
  ...ACTIVA,
  estado: 'ANULADA',
  registroAnulacion: REGISTRO,
  accionesDisponibles: ['VER_DESGLOSE'],
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
    fetchMock.mockResolvedValue(json(200, ANULADA))

    renderEn('42')

    expect(await screen.findByText('Motivo: Error en el precio por kg')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Anular liquidación' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Generar nueva liquidación' })).toHaveAttribute(
      'href',
      `/lotes/${ACTIVA.idLote}/liquidacion`,
    )
  })

  async function abrirAnulacion() {
    fetchMock.mockResolvedValueOnce(json(200, ACTIVA))
    renderEn('42')
    await userEvent.click(await screen.findByRole('button', { name: 'Anular liquidación' }))
  }

  async function llegarAConfirmacion() {
    await abrirAnulacion()
    await userEvent.type(screen.getByLabelText('Motivo de anulación'), '  Error en el precio  ')
    await userEvent.type(screen.getByLabelText('Usuario responsable'), 'Jorge')
    await userEvent.click(screen.getByRole('button', { name: 'Continuar' }))
  }

  it('anula tras capturar el motivo y confirmar', async () => {
    await llegarAConfirmacion()
    expect(screen.getByText(/del Lote L-2026-04 del Galpón Oeste/)).toBeInTheDocument()
    expect(screen.getByText('Error en el precio')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(1)

    fetchMock
      .mockResolvedValueOnce(
        json(200, { idLiquidacion: 42, estado: 'ANULADA', registroAnulacion: REGISTRO }),
      )
      .mockResolvedValueOnce(json(200, ANULADA))
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar anulación' }))

    expect(await screen.findByText('ANULADA')).toBeInTheDocument()
    expect(screen.getByText('Motivo: Error en el precio por kg')).toBeInTheDocument()
    const [url, opciones] = fetchMock.mock.calls[1]
    expect(url).toBe('http://localhost:8080/api/v1/liquidaciones/42/anulacion')
    expect(opciones?.method).toBe('PUT')
    expect(JSON.parse(opciones?.body as string)).toEqual({
      motivo: '  Error en el precio  ',
      usuario: 'Jorge',
    })
  })

  it('no avanza con un motivo corto o de solo espacios', async () => {
    await abrirAnulacion()

    await userEvent.type(screen.getByLabelText('Motivo de anulación'), '   A   ')
    await userEvent.click(screen.getByRole('button', { name: 'Continuar' }))

    expect(screen.getByText('Escribe al menos 10 caracteres (faltan 9).')).toBeInTheDocument()
    expect(screen.getByText('1 / 500 · mínimo 10')).toBeInTheDocument()
    expect(screen.getByText('Escribe al menos 3 caracteres.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Confirmar anulación' })).not.toBeInTheDocument()
  })

  it('cancelar en la confirmación vuelve a la vista sin anular', async () => {
    await llegarAConfirmacion()

    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(screen.getByRole('button', { name: 'Anular liquidación' })).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('si otro usuario se adelantó avisa y muestra el registro existente', async () => {
    await llegarAConfirmacion()
    fetchMock
      .mockResolvedValueOnce(
        json(409, {
          type: 'https://avicontrol.edu.co/errors/liquidacion-ya-anulada',
          title: 'Liquidación ya anulada',
          status: 409,
          detail: 'La Liquidación 42 ya fue anulada.',
          registroAnulacion: REGISTRO,
        }),
      )
      .mockResolvedValueOnce(json(200, ANULADA))

    await userEvent.click(screen.getByRole('button', { name: 'Confirmar anulación' }))

    expect(await screen.findByRole('status')).toHaveTextContent('ya fue anulada')
    expect(await screen.findByText('Motivo: Error en el precio por kg')).toBeInTheDocument()
  })

  it('vuelve a la captura con el error de campo del backend', async () => {
    await llegarAConfirmacion()
    fetchMock.mockResolvedValueOnce(
      json(400, {
        type: 'https://avicontrol.edu.co/errors/validacion',
        title: 'Error de validación',
        status: 400,
        detail: 'La solicitud contiene parámetros inválidos.',
        errores: [{ campo: 'usuario', mensaje: 'El usuario responsable es obligatorio.' }],
      }),
    )

    await userEvent.click(screen.getByRole('button', { name: 'Confirmar anulación' }))

    expect(await screen.findByText('El usuario responsable es obligatorio.')).toBeInTheDocument()
    expect(screen.getByLabelText('Motivo de anulación')).toHaveValue('  Error en el precio  ')
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
