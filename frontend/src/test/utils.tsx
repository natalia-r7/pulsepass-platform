import { QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { http, HttpResponse } from 'msw'
import { AuthProvider } from '../funcionalidades/auth/AuthContext'
import { crearQueryClient } from '../lib/queryClient'
import { rutas } from '../router'
import { API } from './mocks/datos'
import { servidor } from './mocks/servidor'

export const TOKEN_PRUEBA = 'token-de-prueba'

/** Simula una sesión guardada: el AuthProvider la restaura con GET /api/auth/me. */
export function conSesion(usuario: { id: number; username: string; email: string; roles: string[] }) {
  sessionStorage.setItem('pulsepass.sesion', JSON.stringify({ token: TOKEN_PRUEBA, expiraEn: Date.now() + 900_000 }))
  servidor.use(http.get(`${API}/auth/me`, () => HttpResponse.json(usuario)))
}

export function renderConProveedores(ruta: string, estado?: unknown) {
  const queryClient = crearQueryClient()
  const router = createMemoryRouter(rutas, { initialEntries: [{ pathname: ruta, state: estado }] })
  const resultado = render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>,
  )
  return { ...resultado, router, queryClient }
}
