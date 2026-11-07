import type { ReactNode } from 'react'
import { NavLink, Outlet } from 'react-router'
import AvisoDatosSimulados from './AvisoDatosSimulados.tsx'

const CLASE_ENLACE =
  'flex size-11 items-center justify-center rounded-xl text-white hover:bg-white/20 aria-[current=page]:bg-white aria-[current=page]:text-black'

function Icono({ children }: { children: ReactNode }) {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="size-6"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {children}
    </svg>
  )
}

function EnlaceMenu({
  to,
  nombre,
  end,
  children,
}: {
  to: string
  nombre: string
  end?: boolean
  children: ReactNode
}) {
  return (
    <NavLink to={to} end={end} aria-label={nombre} title={nombre} className={CLASE_ENLACE}>
      <Icono>{children}</Icono>
    </NavLink>
  )
}

// El indicador de sincronización con M1 y M2 (RT-09) se agrega con el plan 001.
export default function Layout() {
  return (
    <div className="flex h-screen bg-black">
      <aside className="flex w-16 shrink-0 flex-col items-center gap-6 py-6">
        <span
          title="AviControl · Módulo 3: Liquidación de Lote y Análisis de Rentabilidad"
          className="flex size-11 items-center justify-center rounded-xl bg-white text-xl font-bold text-black"
        >
          <span aria-hidden="true">A</span>
          <span className="sr-only">AviControl</span>
        </span>
        <nav aria-label="Principal" className="flex flex-col gap-3">
          <EnlaceMenu to="/" end nombre="Lotes">
            <path d="M3 11.5 12 4l9 7.5" />
            <path d="M5.5 9.5V20h13V9.5" />
            <path d="M10 20v-6h4v6" />
          </EnlaceMenu>
          <EnlaceMenu to="/historial" nombre="Historial">
            <path d="M4 20h16" />
            <path d="M7 20v-6M12 20V9M17 20v-9" />
            <path d="m6 9 5-4 3 2 5-4" />
          </EnlaceMenu>
        </nav>
      </aside>
      <div className="flex min-w-0 flex-1 flex-col overflow-hidden bg-fondo">
        <AvisoDatosSimulados />
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
