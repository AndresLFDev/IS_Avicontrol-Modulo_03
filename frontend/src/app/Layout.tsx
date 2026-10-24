import { Outlet } from 'react-router'

// El indicador de sincronización con M1 y M2 (RT-09) se agrega con el plan 001.
export default function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <header className="flex items-baseline gap-4 bg-primario px-6 py-3 text-white">
        <span className="text-xl font-bold">AviControl</span>
        <span>Módulo 3 · Liquidación de Lote y Análisis de Rentabilidad</span>
      </header>
      <main className="flex-1 p-6">
        <Outlet />
      </main>
    </div>
  )
}
