import type { ComponentType } from 'react'
import { createBrowserRouter, type RouteObject } from 'react-router'
import Layout from './layouts/Layout'
import { RutaPorRol, RutaProtegida, SoloAnonimo } from './layouts/Guardas'
import Inicio from './pages/Inicio'
import ErrorPagina from './pages/ErrorPagina'

/** Carga perezosa de todas las páginas salvo Layout e Inicio (FE-012). */
const lazy = (cargar: () => Promise<{ default: ComponentType }>) => async () => ({ Component: (await cargar()).default })

export const rutas: RouteObject[] = [
  {
    path: '/',
    Component: Layout,
    errorElement: <ErrorPagina />,
    children: [
      { index: true, Component: Inicio },
      { path: 'eventos', lazy: lazy(() => import('./pages/CatalogoEventos')) },
      { path: 'eventos/:eventCode', lazy: lazy(() => import('./pages/DetalleEvento')) },
      { path: 'artistas', lazy: lazy(() => import('./pages/ListaArtistas')) },
      { path: 'artistas/:id', lazy: lazy(() => import('./pages/DetalleArtista')) },
      { path: 'sedes', lazy: lazy(() => import('./pages/ListaSedes')) },
      { path: 'sedes/:code', lazy: lazy(() => import('./pages/DetalleSede')) },
      {
        Component: SoloAnonimo,
        children: [
          { path: 'login', lazy: lazy(() => import('./pages/Login')) },
          { path: 'registro', lazy: lazy(() => import('./pages/Registro')) },
        ],
      },
      {
        Component: RutaProtegida,
        children: [
          { path: 'cuenta', lazy: lazy(() => import('./pages/Cuenta')) },
          {
            element: <RutaPorRol roles={['USER']} />,
            children: [
              { path: 'mis-tickets', lazy: lazy(() => import('./pages/MisTickets')) },
              { path: 'mis-tickets/:ticketCode', lazy: lazy(() => import('./pages/DetalleTicket')) },
              { path: 'compra/confirmacion/:ticketCode', lazy: lazy(() => import('./pages/ConfirmacionCompra')) },
            ],
          },
          {
            element: <RutaPorRol roles={['STAFF', 'ADMIN']} />,
            children: [
              { path: 'staff/validacion', lazy: lazy(() => import('./pages/ValidacionIngreso')) },
              { path: 'staff/eventos/:eventCode/tickets', lazy: lazy(() => import('./pages/TicketsPagadosEvento')) },
            ],
          },
          {
            element: <RutaPorRol roles={['ADMIN']} />,
            children: [
              { path: 'admin/eventos', lazy: lazy(() => import('./pages/AdminEventos')) },
              { path: 'admin/eventos/nuevo', lazy: lazy(() => import('./pages/AdminCrearEvento')) },
              { path: 'admin/eventos/:eventCode', lazy: lazy(() => import('./pages/AdminGestionEvento')) },
            ],
          },
        ],
      },
      { path: '403', lazy: lazy(() => import('./pages/SinPermisos')) },
      { path: '*', lazy: lazy(() => import('./pages/NoEncontrada')) },
    ],
  },
]

export function crearRouter() {
  return createBrowserRouter(rutas)
}
