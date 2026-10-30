import { createBrowserRouter, type RouteObject } from 'react-router'
import GenerarLiquidacionPage from '../features/liquidacion/GenerarLiquidacionPage.tsx'
import VerLiquidacionPage from '../features/liquidacion/VerLiquidacionPage.tsx'
import ListaLotesPage from '../features/lotes/ListaLotesPage.tsx'
import Layout from './Layout.tsx'
import NoEncontradoPage from './NoEncontradoPage.tsx'

// Todas las rutas del módulo. Cada feature agrega aquí sus páginas (P1–P5) a medida que se implementan los planes.
export const routes: RouteObject[] = [
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <ListaLotesPage /> },
      { path: 'lotes/:idLote/liquidacion', element: <GenerarLiquidacionPage /> },
      { path: 'liquidaciones/:idLiquidacion', element: <VerLiquidacionPage /> },
      { path: '*', element: <NoEncontradoPage /> },
    ],
  },
]

export const router = createBrowserRouter(routes)
