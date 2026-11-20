import { http, HttpResponse } from 'msw'
import { andrea, API, artistas, errorResponse, eventoCMF, resumenCMF, sede, ticketAndrea } from './datos'

/** Respuestas por defecto; cada prueba sobrescribe con servidor.use(...). */
export const handlers = [
  http.get(`${API}/events/published`, () => HttpResponse.json([resumenCMF])),
  http.get(`${API}/events/by-artist`, () => HttpResponse.json([resumenCMF])),
  http.get(`${API}/events/:code`, ({ params }) =>
    params.code === 'CMF-2026'
      ? HttpResponse.json(eventoCMF)
      : HttpResponse.json(errorResponse(404, 'Not Found', 'Event not found'), { status: 404 }),
  ),
  http.get(`${API}/venues/active`, () => HttpResponse.json([sede])),
  http.get(`${API}/venues/:code`, () => HttpResponse.json(sede)),
  http.get(`${API}/artists/active`, () => HttpResponse.json(artistas)),
  http.get(`${API}/artists/:id`, () => HttpResponse.json(artistas[0])),
  http.get(`${API}/auth/me`, () => HttpResponse.json(andrea)),
  http.get(`${API}/tickets/by-user`, () => HttpResponse.json([ticketAndrea])),
  http.get(`${API}/tickets/:code`, () => HttpResponse.json(ticketAndrea)),
]
