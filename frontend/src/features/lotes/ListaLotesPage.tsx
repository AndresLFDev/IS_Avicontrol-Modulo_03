import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { formatFechaHora } from '../../shared/format/formatFecha.ts'
import LotesTable from './LotesTable.tsx'
import { ETIQUETA_ETAPA, type EstadoFuente, type EtapaLote } from './types.ts'
import { useLotes } from './useLotes.ts'

const FILTROS: { etapa?: EtapaLote; etiqueta: string }[] = [
  { etiqueta: 'Todos' },
  ...(['POR_LIQUIDAR', 'EN_COSECHA', 'PRODUCTIVO', 'AISLAMIENTO'] as const).map((etapa) => ({
    etapa,
    etiqueta: ETIQUETA_ETAPA[etapa],
  })),
]

const ESPERA_BUSQUEDA_MS = 300

function Sincronizacion({ nombre, fuente }: { nombre: string; fuente: EstadoFuente }) {
  const cuando = fuente.ultimaSincronizacionExitosa
    ? formatFechaHora(fuente.ultimaSincronizacionExitosa)
    : 'sin sincronizar'
  return (
    <span className={fuente.desactualizada ? 'text-error' : undefined}>
      {nombre}: {cuando}
      {fuente.desactualizada && ' (desactualizado)'}
    </span>
  )
}

/** P1 — Lista de lotes (M3-CU01). */
export default function ListaLotesPage() {
  const navigate = useNavigate()
  const [etapa, setEtapa] = useState<EtapaLote>()
  const [texto, setTexto] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const { datos, cargando, error } = useLotes({ etapa, search, page })

  // La búsqueda se envía cuando el usuario deja de escribir
  useEffect(() => {
    if (texto === search) return
    const espera = setTimeout(() => {
      setSearch(texto)
      setPage(0)
    }, ESPERA_BUSQUEDA_MS)
    return () => clearTimeout(espera)
  }, [texto, search])

  function cambiarEtapa(nueva?: EtapaLote) {
    setEtapa(nueva)
    setPage(0)
  }

  const vacia = datos !== null && datos.content.length === 0

  return (
    <section className="mx-auto max-w-5xl">
      <h1 className="text-3xl font-bold">Lista de Lotes</h1>
      {datos && (
        <p className="mt-1 flex flex-wrap gap-x-4 text-sm text-gray-600">
          <span className="font-medium">Última sincronización</span>
          <Sincronizacion nombre="Módulo 1" fuente={datos.sincronizacion.modulo1} />
          <Sincronizacion nombre="Módulo 2" fuente={datos.sincronizacion.modulo2} />
        </p>
      )}

      <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap gap-2" role="group" aria-label="Filtrar por etapa">
          {FILTROS.map((filtro) => (
            <button
              key={filtro.etiqueta}
              type="button"
              aria-pressed={etapa === filtro.etapa}
              onClick={() => cambiarEtapa(filtro.etapa)}
              className="rounded-full border border-gray-400 px-3 py-1 text-sm aria-pressed:border-black aria-pressed:bg-black aria-pressed:text-white"
            >
              {filtro.etiqueta}
            </button>
          ))}
        </div>
        <input
          type="search"
          value={texto}
          maxLength={100}
          onChange={(e) => setTexto(e.target.value)}
          placeholder="Buscar por nombre o ID…"
          aria-label="Buscar lotes"
          className="w-64 rounded-md border border-gray-300 bg-white px-3 py-1 text-sm"
        />
      </div>

      {error && (
        <p role="alert" className="mt-6 text-error">
          {error}
        </p>
      )}
      {cargando && !datos && !error && <p className="mt-6 text-gray-600">Cargando lotes…</p>}

      {datos && !vacia && (
        <>
          <LotesTable
            lotes={datos.content}
            onGenerar={(lote) => navigate(`/lotes/${lote.idLote}/liquidacion`)}
          />
          <div className="mt-4 flex items-center justify-between text-sm text-gray-600">
            <span>Mostrando {datos.mensaje}</span>
            {datos.totalPages > 1 && (
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  disabled={page === 0 || cargando}
                  onClick={() => setPage(page - 1)}
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
                  onClick={() => setPage(page + 1)}
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
