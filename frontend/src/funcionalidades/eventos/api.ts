import { z } from 'zod'
import { peticion } from '../../lib/http'
import { EventResponseSchema, EventSummaryResponseSchema, type CrearEventoForm } from './tipos'

const p = encodeURIComponent

export const eventosApi = {
  publicados: () => peticion('/events/published', z.array(EventSummaryResponseSchema)),
  porCodigo: (eventCode: string) => peticion(`/events/${p(eventCode)}`, EventResponseSchema),
  porArtista: (stageName: string) =>
    peticion(`/events/by-artist?stageName=${p(stageName)}`, z.array(EventSummaryResponseSchema)),
  crear: (datos: CrearEventoForm) =>
    peticion('/events', EventResponseSchema, {
      metodo: 'POST',
      cuerpo: { ...datos, description: datos.description || null, eventDate: normalizarFecha(datos.eventDate) },
    }),
  publicar: (eventCode: string) => peticion(`/events/${p(eventCode)}/publish`, EventResponseSchema, { metodo: 'PATCH' }),
  asociarArtista: (eventCode: string, artistId: number) =>
    peticion(`/events/${p(eventCode)}/artists/${artistId}`, EventResponseSchema, { metodo: 'POST' }),
}

/** datetime-local da "2030-12-12T20:00"; LocalDateTime acepta ese formato con segundos. */
function normalizarFecha(valor: string): string {
  return valor.length === 16 ? `${valor}:00` : valor
}
