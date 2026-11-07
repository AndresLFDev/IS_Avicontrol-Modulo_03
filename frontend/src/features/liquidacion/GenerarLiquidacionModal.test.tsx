import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import GenerarLiquidacionModal from './GenerarLiquidacionModal.tsx'
import type { LiquidacionPrevia, LoteParaLiquidar } from './types.ts'

const ID_LOTE = 'b0000000-0000-0000-0000-000000000003'

const PREVIA: LiquidacionPrevia = {
  esVistaPrevia: true,
  aviso: 'Vista previa · sin generar',
  idLote: ID_LOTE,
  idGalpon: 'a0000000-0000-0000-0000-000000000003',
  nombreLote: 'L-2026-03',
  nombreGalpon: 'Galpón Este',
  siniestroTotal: false,
  precioKgCop: 4500,
  pollosVendidos: 8500,
  pesoTotalKg: 23800,
  pesoPromedioKg: 2.8,
  ventaBrutaCop: 107_100_000,
  mortalidadAves: 500,
  porcentajeMortalidad: 5.56,
  subtotalesCategoria: {
    alimento: 64_800_000,
    insumosMedicos: 2_200_000,
    costoPoblacion: 18_000_000,
  },
  costosOperativosCop: 85_000_000,
  utilidadNetaCop: 22_100_000,
  fechaSincronizacionM1: '2026-10-20T13:00:01Z',
  fechaSincronizacionM2: '2026-10-20T14:20:01Z',
  huellaDatos: 'huella-1',
}

function json(status: number, cuerpo: unknown) {
  return new Response(JSON.stringify(cuerpo), {
    status,
    headers: {
      'Content-Type': status < 400 ? 'application/json' : 'application/problem+json',
    },
  })
}

function cuerpoEnviado(llamada: number) {
  return JSON.parse(fetchMock.mock.calls[llamada][1]!.body as string)
}

const fetchMock = vi.fn<typeof fetch>()

function renderPagina(
  lote: LoteParaLiquidar | null = {
    nombreLote: 'L-2026-03',
    nombreGalpon: 'Galpón Este',
    siniestroTotal: false,
  },
) {
  const router = createMemoryRouter(
    [
      { path: '/', element: <p>Lista de lotes</p> },
      { path: '/lotes/:idLote/liquidacion', element: <GenerarLiquidacionModal /> },
      { path: '/liquidaciones/:idLiquidacion', element: <p>Liquidación generada</p> },
    ],
    { initialEntries: [{ pathname: `/lotes/${ID_LOTE}/liquidacion`, state: lote }] },
  )
  render(<RouterProvider router={router} />)
}

describe('GenerarLiquidacionModal', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('al cerrar la ventana vuelve a la lista sin generar nada', async () => {
    renderPagina()

    await userEvent.click(screen.getByRole('button', { name: 'Cerrar' }))

    expect(await screen.findByText('Lista de lotes')).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('pasa de la captura a la vista previa y genera la liquidación', async () => {
    fetchMock
      .mockResolvedValueOnce(json(200, PREVIA))
      .mockResolvedValueOnce(json(201, { ...PREVIA, idLiquidacion: 42, estado: 'ACTIVA' }))
    renderPagina()

    expect(screen.getByText('Galpón Este · Lote L-2026-03')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Precio por kg (COP)'), '4500')
    await userEvent.type(screen.getByLabelText('Usuario responsable'), 'Jorge')
    await userEvent.click(screen.getByRole('button', { name: 'Siguiente' }))

    expect(await screen.findByText('Vista previa · sin generar')).toBeInTheDocument()
    expect(screen.getByText(/107\.100\.000/)).toBeInTheDocument()
    expect(cuerpoEnviado(0)).toEqual({ idLote: ID_LOTE, precioKgCop: 4500, usuario: 'Jorge' })

    await userEvent.click(screen.getByRole('button', { name: 'Generar liquidación' }))

    expect(await screen.findByText('Liquidación generada')).toBeInTheDocument()
    expect(cuerpoEnviado(1)).toMatchObject({ huellaDatos: 'huella-1', precioKgCop: 4500 })
  })

  it('volver conserva el precio digitado', async () => {
    fetchMock.mockResolvedValueOnce(json(200, PREVIA))
    renderPagina()
    await userEvent.type(screen.getByLabelText('Precio por kg (COP)'), '4500')
    await userEvent.type(screen.getByLabelText('Usuario responsable'), 'Jorge')
    await userEvent.click(screen.getByRole('button', { name: 'Siguiente' }))
    await screen.findByText('Vista previa · sin generar')

    await userEvent.click(screen.getByRole('button', { name: 'Volver' }))

    expect(screen.getByLabelText('Precio por kg (COP)')).toHaveValue(4500)
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('en siniestro total no pide precio', async () => {
    fetchMock.mockResolvedValueOnce(json(200, { ...PREVIA, siniestroTotal: true }))
    renderPagina({ nombreLote: 'L-2026-04', nombreGalpon: 'Galpón Oeste', siniestroTotal: true })

    expect(screen.queryByLabelText('Precio por kg (COP)')).not.toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Usuario responsable'), 'Jorge')
    await userEvent.click(screen.getByRole('button', { name: 'Siguiente' }))

    await screen.findByText('Vista previa · sin generar')
    expect(cuerpoEnviado(0).precioKgCop).toBeNull()
  })

  describe('al abrir la URL directamente', () => {
    const FILA_SINIESTRO = {
      idLote: ID_LOTE,
      nombreLote: 'L-2026-04',
      nombreGalpon: 'Galpón Oeste',
      siniestroTotal: true,
    }

    it('consulta el lote por UUID y no pide precio si es siniestro total', async () => {
      fetchMock.mockResolvedValueOnce(json(200, { content: [FILA_SINIESTRO] }))
      renderPagina(null)

      expect(screen.getByText('Cargando lote…')).toBeInTheDocument()
      expect(await screen.findByText('Galpón Oeste · Lote L-2026-04')).toBeInTheDocument()
      expect(fetchMock.mock.calls[0][0]).toContain(`search=${ID_LOTE}`)
      expect(screen.queryByLabelText('Precio por kg (COP)')).not.toBeInTheDocument()
    })

    it('avisa si el lote no se puede liquidar y no muestra el formulario', async () => {
      fetchMock.mockResolvedValueOnce(json(200, { content: [] }))
      renderPagina(null)

      expect(await screen.findByRole('alert')).toHaveTextContent('no está disponible')
      expect(screen.queryByLabelText('Usuario responsable')).not.toBeInTheDocument()
    })

    it('muestra el error si la consulta falla', async () => {
      fetchMock.mockRejectedValueOnce(new TypeError('sin red'))
      renderPagina(null)

      expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo contactar')
      expect(screen.queryByLabelText('Usuario responsable')).not.toBeInTheDocument()
    })
  })

  it('muestra el error de cada campo', async () => {
    fetchMock.mockResolvedValueOnce(
      json(400, {
        type: 'https://avicontrol.edu.co/errors/validacion',
        title: 'Error de validación',
        status: 400,
        detail: 'La solicitud contiene parámetros inválidos.',
        errores: [{ campo: 'precioKgCop', mensaje: 'El precio por kg debe ser mayor que 0' }],
      }),
    )
    renderPagina()

    await userEvent.click(screen.getByRole('button', { name: 'Siguiente' }))

    expect(await screen.findByText('El precio por kg debe ser mayor que 0')).toBeInTheDocument()
    expect(screen.queryByText('Vista previa · sin generar')).not.toBeInTheDocument()
  })

  it('lista las partidas sin precio', async () => {
    fetchMock.mockResolvedValueOnce(
      json(422, {
        type: 'https://avicontrol.edu.co/errors/lote-no-liquidable',
        title: 'Lote no apto para liquidar',
        status: 422,
        detail: 'Hay partidas de costo sin precio aplicable. No es posible liquidar.',
        causa: 'PARTIDAS_SIN_PRECIO',
        partidasPendientes: [
          { categoria: 'ALIMENTO', concepto: 'Engorde', referenciaOrigen: 'REC-2026-121' },
        ],
      }),
    )
    renderPagina()
    await userEvent.type(screen.getByLabelText('Precio por kg (COP)'), '4500')

    await userEvent.click(screen.getByRole('button', { name: 'Siguiente' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Alimento: Engorde (REC-2026-121)')
  })

  it('si los datos cambiaron recalcula la vista previa y avisa', async () => {
    fetchMock
      .mockResolvedValueOnce(json(200, PREVIA))
      .mockResolvedValueOnce(
        json(409, {
          type: 'https://avicontrol.edu.co/errors/vista-previa-desactualizada',
          title: 'Los datos cambiaron',
          status: 409,
        }),
      )
      .mockResolvedValueOnce(json(200, { ...PREVIA, ventaBrutaCop: 99_000_000, huellaDatos: 'h2' }))
    renderPagina()
    await userEvent.type(screen.getByLabelText('Precio por kg (COP)'), '4500')
    await userEvent.type(screen.getByLabelText('Usuario responsable'), 'Jorge')
    await userEvent.click(screen.getByRole('button', { name: 'Siguiente' }))
    await screen.findByText('Vista previa · sin generar')

    await userEvent.click(screen.getByRole('button', { name: 'Generar liquidación' }))

    expect(await screen.findByRole('status')).toHaveTextContent('cambiaron')
    expect(screen.getByText(/99\.000\.000/)).toBeInTheDocument()
  })
})
