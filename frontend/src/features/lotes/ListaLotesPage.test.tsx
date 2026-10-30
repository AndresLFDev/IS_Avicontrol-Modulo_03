import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import ListaLotesPage from './ListaLotesPage.tsx'
import type { LotePage, LoteResumen } from './types.ts'

const SYNC = {
  modulo1: { ultimaSincronizacionExitosa: '2026-10-20T13:07:01Z', desactualizada: false },
  modulo2: { ultimaSincronizacionExitosa: null, desactualizada: true },
}

function pagina(cambios: Partial<LotePage>): LotePage {
  return {
    content: [],
    page: 0,
    size: 6,
    totalElements: 0,
    totalPages: 0,
    mensaje: 'No hay lotes disponibles para consultar',
    sincronizacion: SYNC,
    ...cambios,
  }
}

function respuesta(cuerpo: unknown) {
  return new Response(JSON.stringify(cuerpo), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('ListaLotesPage', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  function renderPagina() {
    render(
      <MemoryRouter>
        <ListaLotesPage />
      </MemoryRouter>,
    )
  }

  it('muestra el mensaje de vacío y el estado de sincronización', async () => {
    fetchMock.mockResolvedValue(respuesta(pagina({})))

    renderPagina()

    expect(await screen.findByText('No hay lotes disponibles para consultar')).toBeInTheDocument()
    expect(screen.getByText(/Módulo 2: sin sincronizar \(desactualizado\)/)).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/lotes?page=0&size=6',
      expect.anything(),
    )
  })

  it('filtra por etapa desde la primera página', async () => {
    fetchMock.mockImplementation(async () => respuesta(pagina({})))
    renderPagina()
    await screen.findByText('No hay lotes disponibles para consultar')

    await userEvent.click(screen.getByRole('button', { name: 'Por liquidar' }))

    expect(fetchMock).toHaveBeenLastCalledWith(
      'http://localhost:8080/api/v1/lotes?page=0&size=6&etapa=POR_LIQUIDAR',
      expect.anything(),
    )
    expect(screen.getByRole('button', { name: 'Por liquidar' })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
  })

  it('pagina cuando hay más de 6 lotes', async () => {
    const fila: LoteResumen = {
      idLote: 'b0000000-0000-0000-0000-000000000001',
      nombreLote: 'L-2026-01',
      fechaIngreso: '2026-08-01',
      idGalpon: 'a0000000-0000-0000-0000-000000000001',
      nombreGalpon: 'Galpón Norte',
      etapa: 'PRODUCTIVO',
      siniestroTotal: false,
      accion: 'GENERAR_LIQUIDACION',
      accionHabilitada: false,
    }
    fetchMock.mockImplementation(async () =>
      respuesta(
        pagina({ content: [fila], totalElements: 7, totalPages: 2, mensaje: '1 de 7 lotes' }),
      ),
    )
    renderPagina()

    expect(await screen.findByText('Mostrando 1 de 7 lotes')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Siguiente' }))

    expect(fetchMock).toHaveBeenLastCalledWith(
      'http://localhost:8080/api/v1/lotes?page=1&size=6',
      expect.anything(),
    )
  })

  it('muestra el error del servidor', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    renderPagina()

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo contactar al servidor')
  })
})
