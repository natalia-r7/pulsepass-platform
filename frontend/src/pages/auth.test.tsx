import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { admin, andrea, API, errorResponse } from '../test/mocks/datos'
import { servidor } from '../test/mocks/servidor'
import { conSesion, renderConProveedores } from '../test/utils'

describe('Autenticación y guardas', () => {
  it('TEST-FE-AUTH-001: login válido → sesión y redirección a desde', async () => {
    servidor.use(
      http.post(`${API}/auth/login`, () => HttpResponse.json({ accessToken: 'jwt', tokenType: 'Bearer', expiresIn: 900 })),
      http.get(`${API}/tickets/by-user`, () => HttpResponse.json([])),
    )
    const { router } = renderConProveedores('/login', { desde: '/mis-tickets' })
    await userEvent.type(await screen.findByLabelText('Usuario o correo'), 'andrea')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'ContraseñaSegura1')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/mis-tickets'))
    expect(await screen.findByText(/Mi cuenta \(andrea\)/)).toBeInTheDocument()
    expect(JSON.parse(sessionStorage.getItem('pulsepass.sesion') ?? '{}')).toMatchObject({ token: 'jwt' })
    expect(localStorage.length).toBe(0)
  })

  it('TEST-FE-AUTH-002: login 401 → "Credenciales inválidas"', async () => {
    servidor.use(
      http.post(`${API}/auth/login`, () => HttpResponse.json(errorResponse(401, 'Unauthorized', 'Invalid credentials'), { status: 401 })),
    )
    renderConProveedores('/login')
    await userEvent.type(await screen.findByLabelText('Usuario o correo'), 'andrea')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'incorrecta')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))
    expect(await screen.findByText('Credenciales inválidas.')).toBeInTheDocument()
  })

  it('TEST-FE-AUTH-003: login sin campos → errores y 0 peticiones', async () => {
    let peticiones = 0
    servidor.use(http.post(`${API}/auth/login`, () => { peticiones++; return HttpResponse.json({}) }))
    renderConProveedores('/login')
    await userEvent.click(await screen.findByRole('button', { name: 'Ingresar' }))
    expect(await screen.findByText('Ingresa tu usuario o correo')).toBeInTheDocument()
    expect(screen.getByText('Ingresa tu contraseña')).toBeInTheDocument()
    expect(peticiones).toBe(0)
  })

  it('TEST-FE-AUTH-004: registro con contraseña corta → error y 0 peticiones', async () => {
    let peticiones = 0
    servidor.use(http.post(`${API}/users`, () => { peticiones++; return HttpResponse.json({}) }))
    renderConProveedores('/registro')
    await userEvent.type(await screen.findByLabelText(/^Contraseña/), '12345678901')
    await userEvent.click(screen.getByRole('button', { name: 'Registrarme' }))
    expect(await screen.findByText('La contraseña debe tener al menos 12 caracteres')).toBeInTheDocument()
    expect(peticiones).toBe(0)
  })

  async function llenarRegistro() {
    await userEvent.type(await screen.findByLabelText('Nombre de usuario'), 'andrea')
    await userEvent.type(screen.getByLabelText('Correo electrónico'), 'andrea@example.test')
    await userEvent.type(screen.getByLabelText('Nombres'), 'Andrea')
    await userEvent.type(screen.getByLabelText('Apellidos'), 'Gómez')
    await userEvent.type(screen.getByLabelText('Fecha de nacimiento'), '1995-05-10')
    await userEvent.type(screen.getByLabelText(/^Contraseña/), 'ContraseñaSegura1')
    await userEvent.click(screen.getByRole('button', { name: 'Registrarme' }))
  }

  it('TEST-FE-AUTH-005: registro 201 → redirección a /login con aviso', async () => {
    servidor.use(
      http.post(`${API}/users`, () =>
        HttpResponse.json({ id: 10, username: 'andrea', email: 'andrea@example.test', active: true, firstName: 'Andrea', lastName: 'Gómez', phone: null, city: null }, { status: 201 }),
      ),
    )
    const { router } = renderConProveedores('/registro')
    await llenarRegistro()
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(await screen.findByText('Cuenta creada. Ya puedes iniciar sesión.')).toBeInTheDocument()
  })

  it('TEST-FE-AUTH-006: registro 409 → mensaje genérico', async () => {
    servidor.use(
      http.post(`${API}/users`, () => HttpResponse.json(errorResponse(409, 'Conflict', 'Email already registered: andrea@example.test'), { status: 409 })),
    )
    renderConProveedores('/registro')
    await llenarRegistro()
    expect(await screen.findByText(/el usuario o el correo ya están registrados/)).toBeInTheDocument()
    expect(screen.queryByText(/andrea@example.test/)).not.toBeInTheDocument()
  })

  it('TEST-FE-AUTH-007: RutaProtegida: anónimo → /login con state.desde', async () => {
    const { router } = renderConProveedores('/cuenta')
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(router.state.location.state).toEqual({ desde: '/cuenta' })
  })

  it('TEST-FE-AUTH-008: RutaPorRol: USER en /admin → /403', async () => {
    conSesion(andrea)
    const { router } = renderConProveedores('/admin/eventos')
    await waitFor(() => expect(router.state.location.pathname).toBe('/403'))
    expect(await screen.findByText('No tienes permisos')).toBeInTheDocument()
  })

  it('TEST-FE-AUTH-009: RutaPorRol: ADMIN en /admin → renderiza', async () => {
    conSesion(admin)
    renderConProveedores('/admin/eventos')
    expect(await screen.findByRole('heading', { name: 'Administración de eventos' })).toBeInTheDocument()
  })

  it('TEST-FE-AUTH-010: cerrar sesión limpia token y caché', async () => {
    conSesion(andrea)
    const { router, queryClient } = renderConProveedores('/')
    const limpiar = vi.spyOn(queryClient, 'clear')
    await userEvent.click((await screen.findAllByRole('button', { name: 'Salir' }))[0]!)
    await waitFor(() => expect(router.state.location.pathname).toBe('/'))
    expect(sessionStorage.getItem('pulsepass.sesion')).toBeNull()
    expect(limpiar).toHaveBeenCalled()
    expect(screen.getAllByRole('link', { name: 'Ingresar' }).length).toBeGreaterThan(0)
  })

  it('TEST-FE-AUTH-010b: Salir desde una ruta protegida (/admin) lleva a / (PRD §12.2)', async () => {
    conSesion(admin)
    const { router } = renderConProveedores('/admin/eventos')
    await screen.findByRole('heading', { name: 'Administración de eventos' })
    await userEvent.click(screen.getAllByRole('button', { name: 'Salir' })[0]!)
    await waitFor(() => expect(sessionStorage.getItem('pulsepass.sesion')).toBeNull())
    await waitFor(() => expect(router.state.location.pathname).toBe('/'))
    expect(router.state.location.pathname).toBe('/')
  })

  it('TEST-FE-AUTH-011: token expirado al restaurar sesión → anónimo', async () => {
    sessionStorage.setItem('pulsepass.sesion', JSON.stringify({ token: 'viejo', expiraEn: Date.now() - 1000 }))
    renderConProveedores('/')
    expect((await screen.findAllByRole('link', { name: 'Ingresar' })).length).toBeGreaterThan(0)
    expect(sessionStorage.getItem('pulsepass.sesion')).toBeNull()
  })

  it('TEST-FE-AUTH (401 en ruta protegida): cierra sesión y avisa "Tu sesión expiró"', async () => {
    conSesion(andrea)
    servidor.use(
      http.get(`${API}/tickets/by-user`, () => HttpResponse.json(errorResponse(401, 'Unauthorized', 'Authentication is required'), { status: 401 })),
    )
    const { router } = renderConProveedores('/mis-tickets')
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(await screen.findByText('Tu sesión expiró. Inicia sesión de nuevo.')).toBeInTheDocument()
  })
})
