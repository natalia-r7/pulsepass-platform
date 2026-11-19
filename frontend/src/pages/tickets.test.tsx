import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { admin, andrea, API, errorResponse, eventoCMF, staff, ticketAndrea } from '../test/mocks/datos'
import { servidor } from '../test/mocks/servidor'
import { conSesion, renderConProveedores } from '../test/utils'

describe('Tickets, STAFF y ADMIN', () => {
  it('TEST-FE-TKT-001/004: compra válida envía el email de la sesión y navega a la confirmación', async () => {
    conSesion(andrea)
    let cuerpo: unknown = null
    servidor.use(
      http.post(`${API}/tickets`, async ({ request }) => {
        cuerpo = await request.json()
        return HttpResponse.json({ ...ticketAndrea, ticketCode: 'TCK-NEW' }, { status: 201 })
      }),
    )
    const { router } = renderConProveedores('/eventos/CMF-2026')
    await userEvent.click(await screen.findByRole('button', { name: 'Comprar' }))
    await userEvent.selectOptions(screen.getByLabelText('Tipo de entrada'), 'VIP')
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar compra' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/compra/confirmacion/TCK-NEW'))
    expect(cuerpo).toEqual({ userEmail: 'andrea@example.test', eventCode: 'CMF-2026', type: 'VIP' })
  })

  it('TEST-FE-TKT-002: compra anónima redirige a login con desde', async () => {
    const { router } = renderConProveedores('/eventos/CMF-2026')
    await userEvent.click(await screen.findByRole('button', { name: 'Comprar' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(router.state.location.state).toEqual({ desde: '/eventos/CMF-2026' })
  })

  it('TEST-FE-TKT-003: compra 409 muestra el mensaje del backend en el modal', async () => {
    conSesion(andrea)
    servidor.use(
      http.post(`${API}/tickets`, () =>
        HttpResponse.json(errorResponse(409, 'Conflict', 'User does not meet minimum age for this event'), { status: 409 }),
      ),
    )
    const { router } = renderConProveedores('/eventos/CMF-2026')
    await userEvent.click(await screen.findByRole('button', { name: 'Comprar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar compra' }))
    expect(await screen.findByText('User does not meet minimum age for this event')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/eventos/CMF-2026')
  })

  it('TEST-FE-TKT-005/010: mis tickets filtra por estado y oculta Cancelar si no es PAID', async () => {
    conSesion(andrea)
    servidor.use(
      http.get(`${API}/tickets/by-user`, () =>
        HttpResponse.json([ticketAndrea, { ...ticketAndrea, id: 101, ticketCode: 'TCK-USED', status: 'USED' }]),
      ),
    )
    const { router } = renderConProveedores('/mis-tickets')
    const usado = (await screen.findByRole('link', { name: 'TCK-USED' })).closest('li')!
    expect(within(usado).queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument()
    const pagado = screen.getByRole('link', { name: 'TCK-0001' }).closest('li')!
    expect(within(pagado).getByRole('button', { name: 'Cancelar' })).toBeInTheDocument()
    await userEvent.selectOptions(screen.getByLabelText('Filtrar por estado'), 'USED')
    expect(router.state.location.search).toBe('?estado=USED')
    expect(screen.queryByRole('link', { name: 'TCK-0001' })).not.toBeInTheDocument()
  })

  it('TEST-FE-TKT-006: mis tickets vacío', async () => {
    conSesion(andrea)
    servidor.use(http.get(`${API}/tickets/by-user`, () => HttpResponse.json([])))
    renderConProveedores('/mis-tickets')
    expect(await screen.findByText('No tienes tickets con este filtro.')).toBeInTheDocument()
  })

  it('TEST-FE-TKT-007: cancelar PAID → 200 y lista actualizada', async () => {
    conSesion(andrea)
    let cancelado = false
    servidor.use(
      http.get(`${API}/tickets/by-user`, () => HttpResponse.json([{ ...ticketAndrea, status: cancelado ? 'CANCELLED' : 'PAID' }])),
      http.patch(`${API}/tickets/:code/cancel`, () => {
        cancelado = true
        return HttpResponse.json({ ...ticketAndrea, status: 'CANCELLED' })
      }),
    )
    renderConProveedores('/mis-tickets')
    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Sí, cancelar' }))
    expect(await screen.findByText('Cancelado', { selector: 'span' })).toBeInTheDocument()
  })

  it('TEST-FE-TKT-008: cancelar → 409 muestra mensaje y mantiene el modal', async () => {
    conSesion(andrea)
    servidor.use(
      http.patch(`${API}/tickets/:code/cancel`, () =>
        HttpResponse.json(errorResponse(409, 'Conflict', 'Only PAID tickets can be cancelled'), { status: 409 }),
      ),
    )
    renderConProveedores('/mis-tickets')
    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Sí, cancelar' }))
    expect(await screen.findByText('Only PAID tickets can be cancelled')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sí, cancelar' })).toBeInTheDocument()
  })

  it('TEST-FE-TKT-009: ticket ajeno 403 → "Ticket no disponible"', async () => {
    conSesion(andrea)
    servidor.use(http.get(`${API}/tickets/:code`, () => HttpResponse.json(errorResponse(403, 'Forbidden', 'Access denied'), { status: 403 })))
    renderConProveedores('/mis-tickets/TCK-CARLOS')
    expect(await screen.findByText('Ticket no disponible.')).toBeInTheDocument()
  })

  it('TEST-FE-STF-001/002: validar ingreso; la segunda vez muestra el 409', async () => {
    conSesion(staff)
    let usado = false
    servidor.use(
      http.patch(`${API}/tickets/:code/use`, () => {
        if (usado) return HttpResponse.json(errorResponse(409, 'Conflict', 'Only PAID tickets can be used'), { status: 409 })
        usado = true
        return HttpResponse.json({ ...ticketAndrea, status: 'USED' })
      }),
    )
    renderConProveedores('/staff/validacion')
    await userEvent.type(await screen.findByLabelText('Código del ticket'), 'TCK-0001')
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Marcar como usado' }))
    expect(await screen.findByText('Ingreso registrado: el ticket quedó como usado.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Marcar como usado' }))
    expect(await screen.findByText('Only PAID tickets can be used')).toBeInTheDocument()
  })

  it('TEST-FE-STF-003: ticket inexistente → 404 visible', async () => {
    conSesion(staff)
    servidor.use(http.get(`${API}/tickets/:code`, () => HttpResponse.json(errorResponse(404, 'Not Found', 'Ticket not found'), { status: 404 })))
    renderConProveedores('/staff/validacion')
    await userEvent.type(await screen.findByLabelText('Código del ticket'), 'NADA')
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))
    expect(await screen.findByText('No existe un ticket con el código NADA.')).toBeInTheDocument()
  })

  it('TEST-FE-STF-004: tickets pagados de un evento en tabla', async () => {
    conSesion(staff)
    servidor.use(http.get(`${API}/events/:code/tickets/paid`, () => HttpResponse.json([ticketAndrea])))
    renderConProveedores('/staff/eventos/CMF-2026/tickets')
    expect(await screen.findByRole('cell', { name: 'TCK-0001' })).toBeInTheDocument()
  })

  it('TEST-FE-ADM-001: crear evento con campos vacíos → errores y 0 peticiones', async () => {
    conSesion(admin)
    let peticiones = 0
    servidor.use(http.post(`${API}/events`, () => { peticiones++; return HttpResponse.json({}) }))
    renderConProveedores('/admin/eventos/nuevo')
    await userEvent.click(await screen.findByRole('button', { name: 'Crear evento (borrador)' }))
    expect(await screen.findByText('Ingresa el código del evento')).toBeInTheDocument()
    expect(screen.getByText('Selecciona una sede', { selector: 'p' })).toBeInTheDocument()
    expect(peticiones).toBe(0)
  })

  it('TEST-FE-ADM-002/003: crear evento 400 con details → error en el campo; 201 → gestión', async () => {
    conSesion(admin)
    let intento = 0
    servidor.use(
      http.post(`${API}/events`, () => {
        intento++
        if (intento === 1)
          return HttpResponse.json(errorResponse(400, 'Bad Request', 'Validation failed', { name: 'Name must not exceed 150 characters' }), { status: 400 })
        return HttpResponse.json({ ...eventoCMF, status: 'DRAFT' }, { status: 201 })
      }),
    )
    const { router } = renderConProveedores('/admin/eventos/nuevo')
    await userEvent.type(await screen.findByLabelText('Código del evento'), 'CMF-2026')
    await userEvent.type(screen.getByLabelText('Nombre'), 'Caribbean Music Fest 2026')
    await userEvent.type(screen.getByLabelText('Fecha y hora'), '2026-12-12T20:00')
    await screen.findByRole('option', { name: /Marina Convention Center/ })
    await userEvent.selectOptions(screen.getByLabelText('Sede'), 'VEN-SMR-01')
    await userEvent.click(screen.getByRole('button', { name: 'Crear evento (borrador)' }))
    expect(await screen.findByText('Name must not exceed 150 characters')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Crear evento (borrador)' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/admin/eventos/CMF-2026'))
  })

  it('TEST-FE-ADM-004/005: publicar evento → badge PUBLISHED; 409 visible', async () => {
    conSesion(admin)
    let publicado = false
    servidor.use(
      http.get(`${API}/events/:code`, () => HttpResponse.json({ ...eventoCMF, status: publicado ? 'PUBLISHED' : 'DRAFT' })),
      http.patch(`${API}/events/:code/publish`, () => {
        if (publicado) return HttpResponse.json(errorResponse(409, 'Conflict', 'Only DRAFT events can be published'), { status: 409 })
        publicado = true
        return HttpResponse.json({ ...eventoCMF, status: 'PUBLISHED' })
      }),
    )
    renderConProveedores('/admin/eventos/CMF-2026')
    expect(await screen.findByText('Borrador')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Publicar evento' }))
    await userEvent.click(screen.getByRole('button', { name: 'Publicar' }))
    expect(await screen.findByText('Publicado')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Publicar evento' }))
    await userEvent.click(screen.getByRole('button', { name: 'Publicar' }))
    expect(await screen.findByText('Only DRAFT events can be published')).toBeInTheDocument()
  })

  it('TEST-FE-ADM-006: asociar artista; duplicado → 409', async () => {
    conSesion(admin)
    let veces = 0
    servidor.use(
      http.post(`${API}/events/:code/artists/:id`, () => {
        veces++
        if (veces > 1) return HttpResponse.json(errorResponse(409, 'Conflict', 'Artist already associated'), { status: 409 })
        return HttpResponse.json(eventoCMF)
      }),
    )
    renderConProveedores('/admin/eventos/CMF-2026')
    await userEvent.selectOptions(await screen.findByLabelText('Artista activo'), '1')
    await userEvent.click(screen.getByRole('button', { name: 'Asociar' }))
    expect(await screen.findByText('Asociados en esta sesión: Solar Beat.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Asociar' }))
    expect(await screen.findByText('Artist already associated')).toBeInTheDocument()
  })
})
