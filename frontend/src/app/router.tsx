import { createBrowserRouter, type RouteObject } from 'react-router'
import GenerarLiquidacionModal from '../features/liquidacion/GenerarLiquidacionModal.tsx'
import VerLiquidacionPage from '../features/liquidacion/VerLiquidacionPage.tsx'
import ListaLotesPage from '../features/lotes/ListaLotesPage.tsx'
import HistorialPage from '../features/reportes/HistorialPage.tsx'
import VerDesglosePage from '../features/reportes/VerDesglosePage.tsx'
import Layout from './Layout.tsx'
import NoEncontradoPage from './NoEncontradoPage.tsx'

// Todas las rutas del módulo. Cada feature agrega aquí sus páginas (P1–P5) a medida que se implementan los planes.
export const routes: RouteObject[] = [
  {
    path: '/',
    element: <Layout />,
    children: [
      {
        // La lista queda montada detrás de la ventana: conserva sus filtros y su página
        element: <ListaLotesPage />,
        children: [
          { index: true },
          { path: 'lotes/:idLote/liquidacion', element: <GenerarLiquidacionModal /> },
        ],
      },
      { path: 'liquidaciones/:idLiquidacion', element: <VerLiquidacionPage /> },
      { path: 'liquidaciones/:idLiquidacion/desglose', element: <VerDesglosePage /> },
      { path: 'historial', element: <HistorialPage /> },
      { path: '*', element: <NoEncontradoPage /> },
    ],
  },
]

export const router = createBrowserRouter(routes)
