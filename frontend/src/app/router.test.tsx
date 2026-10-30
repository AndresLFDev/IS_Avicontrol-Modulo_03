import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { routes } from './router.tsx'

function renderEn(ruta: string) {
  render(<RouterProvider router={createMemoryRouter(routes, { initialEntries: [ruta] })} />)
}

describe('router', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn<typeof fetch>(() => new Promise(() => {})),
    )
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('muestra la lista de lotes en la raíz', () => {
    renderEn('/')
    expect(screen.getByText('AviControl')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Lista de Lotes' })).toBeInTheDocument()
  })

  it('muestra la página de no encontrado en una ruta desconocida', () => {
    renderEn('/no-existe')
    expect(screen.getByRole('heading', { name: 'Página no encontrada' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Volver al inicio' })).toHaveAttribute('href', '/')
  })
})
