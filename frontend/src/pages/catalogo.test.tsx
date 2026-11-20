import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { API, eventoCMF } from '../test/mocks/datos'
import { servidor } from '../test/mocks/servidor'
import { renderConProveedores } from '../test/utils'

describe('Catálogo, artistas, sedes y transversales', () => {
  it('TEST-FE-EVT-001: la cartelera lista eventos publicados sin login', async () => {
    renderConProveedores('/eventos')
    expect(await screen.findByRole('link', { name: 'Caribbean Music Fest 2026' })).toBeInTheDocument()
  })

  it('TEST-FE-EVT-002: lista vacía → estado vacío', async () => {
    servidor.use(http.get(`${API}/events/published`, () => HttpResponse.json([])))
    renderConProveedores('/eventos')
    expect(await screen.findByText('No hay eventos que coincidan con la búsqueda.')).toBeInTheDocument()
  })

  it('TEST-FE-EVT-003: el filtro por texto actualiza la URL', async () => {
    const { router } = renderConProveedores('/eventos')
    await screen.findByRole('link', { name: 'Caribbean Music Fest 2026' })
    await userEvent.type(screen.getByLabelText('Buscar por nombre'), 'zzz')
    await waitFor(() => expect(router.state.location.search).toBe('?q=zzz'))
    expect(screen.getByText('No hay eventos que coincidan con la búsqueda.')).toBeInTheDocument()
  })

  it('TEST-FE-EVT-004: el detalle muestra el evento y la sede', async () => {
    renderConProveedores('/eventos/CMF-2026')
    expect(await screen.findByRole('heading', { name: 'Caribbean Music Fest 2026' })).toBeInTheDocument()
    expect(screen.getByText('18 años')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Marina Convention Center' })).toBeInTheDocument()
  })

  it('TEST-FE-EVT-005: 404 → "Evento no disponible"', async () => {
    renderConProveedores('/eventos/NO-EXISTE')
    expect(await screen.findByText('Evento no disponible.')).toBeInTheDocument()
  })

  it('TEST-FE-EVT-006: evento SOLD_OUT → compra deshabilitada', async () => {
    servidor.use(http.get(`${API}/events/:code`, () => HttpResponse.json({ ...eventoCMF, status: 'SOLD_OUT' })))
    renderConProveedores('/eventos/CMF-2026')
    expect(await screen.findByRole('button', { name: 'Comprar' })).toBeDisabled()
  })

  it('TEST-FE-ART-001/002: lista y detalle de artista con eventos', async () => {
    renderConProveedores('/artistas/1')
    expect(await screen.findByRole('heading', { name: 'Solar Beat' })).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Caribbean Music Fest 2026' })).toBeInTheDocument()
  })

  it('TEST-FE-ART-003: id no numérico → 404 sin llamar a la API', async () => {
    let llamadas = 0
    servidor.use(http.get(`${API}/artists/:id`, () => { llamadas++; return HttpResponse.json({}) }))
    renderConProveedores('/artistas/abc')
    expect(await screen.findByText('Página no encontrada')).toBeInTheDocument()
    expect(llamadas).toBe(0)
  })

  it('TEST-FE-VEN-001: lista de sedes y detalle', async () => {
    renderConProveedores('/sedes')
    await userEvent.click(await screen.findByRole('link', { name: 'Marina Convention Center' }))
    expect(await screen.findByText('Santa Marta')).toBeInTheDocument()
  })

  it('TEST-FE-GEN-001: ruta inexistente → 404', async () => {
    renderConProveedores('/no-existe')
    expect(await screen.findByText('Página no encontrada')).toBeInTheDocument()
  })

  it('TEST-FE-GEN-003: navbar anónima muestra Ingresar y no Mis tickets', async () => {
    renderConProveedores('/')
    expect((await screen.findAllByRole('link', { name: 'Ingresar' })).length).toBeGreaterThan(0)
    expect(screen.queryByRole('link', { name: 'Mis tickets' })).not.toBeInTheDocument()
  })

  it('TEST-FE-GEN-004: el menú móvil abre y cierra con teclado', async () => {
    renderConProveedores('/')
    const boton = await screen.findByRole('button', { name: 'Abrir menú' })
    boton.focus()
    await userEvent.keyboard('{Enter}')
    expect(screen.getByRole('navigation', { name: 'Menú móvil' })).toBeInTheDocument()
    await userEvent.keyboard('{Enter}')
    expect(screen.queryByRole('navigation', { name: 'Menú móvil' })).not.toBeInTheDocument()
  })
})
