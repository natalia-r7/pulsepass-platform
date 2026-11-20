import { z } from 'zod'
import { peticion } from '../../lib/http'
import { TicketResponseSchema, type TicketType } from './tipos'

const p = encodeURIComponent

export const ticketsApi = {
  comprar: (datos: { userEmail: string; eventCode: string; type: TicketType }) =>
    peticion('/tickets', TicketResponseSchema, { metodo: 'POST', cuerpo: datos }),
  porCodigo: (ticketCode: string) => peticion(`/tickets/${p(ticketCode)}`, TicketResponseSchema),
  porUsuario: (email: string) => peticion(`/tickets/by-user?email=${p(email)}`, z.array(TicketResponseSchema)),
  pagadosPorEvento: (eventCode: string) =>
    peticion(`/events/${p(eventCode)}/tickets/paid`, z.array(TicketResponseSchema)),
  cancelar: (ticketCode: string) => peticion(`/tickets/${p(ticketCode)}/cancel`, TicketResponseSchema, { metodo: 'PATCH' }),
  usar: (ticketCode: string) => peticion(`/tickets/${p(ticketCode)}/use`, TicketResponseSchema, { metodo: 'PATCH' }),
}
