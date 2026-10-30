import { createBrowserRouter, type RouteObject } from 'react-router'
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
      { path: '*', element: <NoEncontradoPage /> },
    ],
  },
]

export const router = createBrowserRouter(routes)
