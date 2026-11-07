import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import HistorialTable from './HistorialTable.tsx'
import { useHistorial } from './useHistorial.ts'

const ESPERA_BUSQUEDA_MS = 300

/** Aplica los cambios a los filtros de la URL; todo cambio vuelve a la primera página salvo `page`. */
function conCambios(actuales: URLSearchParams, cambios: Record<string, string>) {
  const siguientes = new URLSearchParams(actuales)
  siguientes.delete('page')
  for (const [nombre, valor] of Object.entries(cambios)) {
    if (valor) siguientes.set(nombre, valor)
    else siguientes.delete(nombre)
  }
  return siguientes
}

/**
 * P5 — Historial de liquidaciones (M3-CU06). Los filtros viven en la URL para conservarlos al
 * volver de la vista de una Liquidación.
 */
export default function HistorialPage() {
  const [params, setParams] = useSearchParams()
  const search = params.get('search') ?? ''
  const desde = params.get('desde') ?? ''
  const hasta = params.get('hasta') ?? ''
  const page = Math.max(Number(params.get('page')) || 0, 0)

  const [texto, setTexto] = useState(search)
  const [searchAnterior, setSearchAnterior] = useState(search)
  if (search !== searchAnterior) {
    // La URL cambió desde fuera (p. ej. el enlace del menú): el campo la sigue
    setSearchAnterior(search)
    setTexto(search)
  }

  const rangoInvertido = desde !== '' && hasta !== '' && desde > hasta
  const { datos, cargando, error } = useHistorial({ search, desde, hasta, page }, !rangoInvertido)

  // La búsqueda se envía cuando el usuario deja de escribir
  useEffect(() => {
    if (texto === search) return
    const espera = setTimeout(() => {
      setParams((actuales) => conCambios(actuales, { search: texto }), { replace: true })
    }, ESPERA_BUSQUEDA_MS)
    return () => clearTimeout(espera)
  }, [texto, search, setParams])

  function actualizar(cambios: Record<string, string>) {
    setParams((actuales) => conCambios(actuales, cambios), { replace: true })
  }

  const vacia = datos !== null && datos.content.length === 0
  const consulta = params.toString()

  return (
    <section className="mx-auto max-w-6xl">
      <h1 className="text-3xl font-bold">Historial de Liquidaciones</h1>

      <div className="mt-4 flex flex-wrap items-end gap-3">
        <label className="flex min-w-64 flex-1 flex-col text-sm font-medium">
          Galpón o lote
          <input
            type="search"
            value={texto}
            maxLength={100}
            onChange={(e) => setTexto(e.target.value)}
            placeholder="Buscar por nombre o UUID…"
            className="mt-1 rounded-md border border-gray-300 bg-white px-3 py-1 font-normal"
          />
        </label>
        <label className="flex flex-col text-sm font-medium">
          Desde
          <input
            type="date"
            value={desde}
            aria-invalid={rangoInvertido}
            onChange={(e) => actualizar({ desde: e.target.value })}
            className="mt-1 rounded-md border border-gray-300 bg-white px-3 py-1 font-normal aria-invalid:border-error"
          />
        </label>
        <label className="flex flex-col text-sm font-medium">
          Hasta
          <input
            type="date"
            value={hasta}
            aria-invalid={rangoInvertido}
            onChange={(e) => actualizar({ hasta: e.target.value })}
            className="mt-1 rounded-md border border-gray-300 bg-white px-3 py-1 font-normal aria-invalid:border-error"
          />
        </label>
      </div>

      {rangoInvertido && (
        <p role="alert" className="mt-3 text-sm text-error">
          La fecha inicial debe ser menor o igual a la fecha final.
        </p>
      )}
      {error && (
        <p role="alert" className="mt-6 text-error">
          {error}
        </p>
      )}
      {cargando && !datos && !error && <p className="mt-6 text-gray-600">Cargando historial…</p>}

      {datos && !vacia && (
        <>
          <div className="mt-4">
            <HistorialTable
              liquidaciones={datos.content}
              volverA={consulta ? `/historial?${consulta}` : '/historial'}
            />
          </div>
          <div className="mt-4 flex items-center justify-between text-sm text-gray-600">
            <span>
              {datos.content.length} de {datos.totalElements} liquidaciones
            </span>
            {datos.totalPages > 1 && (
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  disabled={page === 0 || cargando}
                  onClick={() => actualizar({ page: page > 1 ? String(page - 1) : '' })}
                  className="rounded-md border px-3 py-1 disabled:opacity-40"
                >
                  Anterior
                </button>
                <span>
                  Página {page + 1} de {datos.totalPages}
                </span>
                <button
                  type="button"
                  disabled={page + 1 >= datos.totalPages || cargando}
                  onClick={() => actualizar({ page: String(page + 1) })}
                  className="rounded-md border px-3 py-1 disabled:opacity-40"
                >
                  Siguiente
                </button>
              </div>
            )}
          </div>
        </>
      )}

      {vacia && <p className="mt-10 text-center text-gray-600">{datos.mensaje}</p>}
    </section>
  )
}
