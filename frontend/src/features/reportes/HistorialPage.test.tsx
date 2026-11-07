import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import HistorialPage from './HistorialPage.tsx'
import type { HistorialItem, HistorialPage as Pagina } from './types.ts'

const ACTIVA: HistorialItem = {
  idLiquidacion: 42,
  fechaHoraGeneracion: '2026-09-28T16:42:00Z',
  idGalpon: 'a0000000-0000-0000-0000-000000000001',
  nombreGalpon: 'Galpón Norte',
  idLote: 'c0000000-0000-0000-0000-000000000004',
  nombreLote: 'L-2025-04',
  ventaBrutaCop: 139_200_000,
  porcentajeMortalidad: 4,
  costosOperativosCop: 60_200_000,
  utilidadNetaCop: 79_000_000,
  estado: 'ACTIVA',
}
const ANULADA: HistorialItem = {
  ...ACTIVA,
  idLiquidacion: 40,
  utilidadNetaCop: -5_000_000,
  estado: 'ANULADA',
}

function pagina(cambios: Partial<Pagina>): Pagina {
  return {
    content: [ACTIVA, ANULADA],
    page: 0,
    size: 10,
    totalElements: 2,
    totalPages: 1,
    mensaje: null,
    ...cambios,
  }
}

function respuesta(cuerpo: unknown) {
  return new Response(JSON.stringify(cuerpo), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}

const fetchMock = vi.fn<typeof fetch>()

function renderPagina(ruta = '/historial') {
  render(
    <MemoryRouter initialEntries={[ruta]}>
      <HistorialPage />
    </MemoryRouter>,
  )
}

function ultimaUrl() {
  return String(fetchMock.mock.lastCall?.[0])
}

describe('HistorialPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockImplementation(() => Promise.resolve(respuesta(pagina({}))))
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('muestra las columnas de cada fila y distingue las anuladas', async () => {
    renderPagina()

    expect(await screen.findByText('ANULADA')).toBeInTheDocument()
    expect(screen.getByText('ACTIVA')).toBeInTheDocument()
    expect(screen.getAllByText('Galpón Norte')).toHaveLength(2)
    expect(screen.getAllByText(/139\.200\.000/)).toHaveLength(2)
    expect(screen.getAllByText('4 %')).toHaveLength(2)
    expect(screen.getByText(/-\s*\$\s*5\.000\.000/)).toHaveClass('text-error')
    expect(screen.getByText('2 de 2 liquidaciones')).toBeInTheDocument()
    expect(ultimaUrl()).toContain('/api/v1/liquidaciones?page=0&size=10')
  })

  it('cada fila lleva a la vista de su Liquidación, no al desglose', async () => {
    renderPagina()

    const enlace = await screen.findByRole('link', { name: /L-2025-04, ANULADA/ })

    expect(enlace).toHaveAttribute('href', '/liquidaciones/40')
  })

  it('envía la búsqueda cuando el usuario deja de escribir', async () => {
    renderPagina()
    await screen.findByText('ANULADA')

    await userEvent.type(screen.getByLabelText('Galpón o lote'), 'norte')

    await vi.waitFor(() => expect(ultimaUrl()).toContain('search=norte'))
  })

  it('envía el rango de fechas y vuelve a la primera página', async () => {
    renderPagina('/historial?page=2')
    await screen.findByText('ANULADA')
    expect(ultimaUrl()).toContain('page=2')

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-07-01' } })

    await vi.waitFor(() => expect(ultimaUrl()).toContain('desde=2026-07-01'))
    expect(ultimaUrl()).toContain('page=0')
  })

  it('con el rango invertido avisa y no consulta hasta corregirlo', async () => {
    renderPagina('/historial?hasta=2026-09-01')
    await screen.findByText('ANULADA')
    fetchMock.mockClear()

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-09-30' } })

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'La fecha inicial debe ser menor o igual a la fecha final.',
    )
    expect(fetchMock).not.toHaveBeenCalled()

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-08-01' } })

    await vi.waitFor(() => expect(ultimaUrl()).toContain('desde=2026-08-01'))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('muestra el mensaje del backend cuando no hay resultados', async () => {
    const mensaje = 'No se encontraron liquidaciones para los criterios de búsqueda aplicados'
    fetchMock.mockResolvedValue(
      respuesta(pagina({ content: [], totalElements: 0, totalPages: 0, mensaje })),
    )
    renderPagina()

    expect(await screen.findByText(mensaje)).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('pagina con los botones', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(respuesta(pagina({ totalElements: 13, totalPages: 2 }))),
    )
    renderPagina()
    await screen.findByText('Página 1 de 2')
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: 'Siguiente' }))

    await vi.waitFor(() => expect(ultimaUrl()).toContain('page=1'))
    expect(await screen.findByText('Página 2 de 2')).toBeInTheDocument()
  })

  it('muestra el error si no hay conexión', async () => {
    fetchMock.mockRejectedValue(new TypeError('sin red'))
    renderPagina()

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo contactar')
  })
})
