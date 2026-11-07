/** Deja claro que lo que se ve no viene del backend. Solo aparece con VITE_USE_MOCKS=true. */
export default function AvisoDatosSimulados() {
  if (import.meta.env.VITE_USE_MOCKS !== 'true') return null

  async function reiniciar() {
    const { reiniciarDatosSimulados } = await import('../mocks/mockFetch.ts')
    reiniciarDatosSimulados()
    window.location.assign('/')
  }

  return (
    <div
      role="status"
      className="flex items-center justify-between gap-4 bg-amber-100 px-6 py-1 text-sm text-amber-900"
    >
      <span>Datos simulados: esta pantalla no está conectada al backend.</span>
      <button type="button" onClick={reiniciar} className="underline">
        Reiniciar datos
      </button>
    </div>
  )
}
