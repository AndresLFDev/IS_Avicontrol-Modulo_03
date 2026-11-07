import { NavLink, Outlet } from 'react-router'
import AvisoDatosSimulados from './AvisoDatosSimulados.tsx'

const CLASE_ENLACE = 'hover:underline aria-[current=page]:font-bold aria-[current=page]:underline'

// El indicador de sincronización con M1 y M2 (RT-09) se agrega con el plan 001.
export default function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <header className="flex flex-wrap items-baseline gap-x-4 gap-y-1 bg-primario px-6 py-3 text-white">
        <span className="text-xl font-bold">AviControl</span>
        <span>Módulo 3 · Liquidación de Lote y Análisis de Rentabilidad</span>
        <nav aria-label="Principal" className="ml-auto flex gap-4">
          <NavLink to="/" end className={CLASE_ENLACE}>
            Lotes
          </NavLink>
          <NavLink to="/historial" className={CLASE_ENLACE}>
            Historial
          </NavLink>
        </nav>
      </header>
      <AvisoDatosSimulados />
      <main className="flex-1 p-6">
        <Outlet />
      </main>
    </div>
  )
}
