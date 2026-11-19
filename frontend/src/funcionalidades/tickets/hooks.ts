import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../auth/useAuth'
import { ticketsApi } from './api'
import type { TicketResponse, TicketType } from './tipos'

export function useTicket(ticketCode: string | undefined) {
  return useQuery({
    queryKey: ['ticket', ticketCode],
    queryFn: () => ticketsApi.porCodigo(ticketCode ?? ''),
    enabled: Boolean(ticketCode),
    staleTime: 15_000,
  })
}

export function useMisTickets(email: string | undefined) {
  return useQuery({
    queryKey: ['tickets', 'usuario', email],
    queryFn: () => ticketsApi.porUsuario(email ?? ''),
    enabled: Boolean(email),
    staleTime: 15_000,
  })
}

export function useTicketsPagadosPorEvento(eventCode: string | undefined) {
  return useQuery({
    queryKey: ['tickets', 'pagados', eventCode],
    queryFn: () => ticketsApi.pagadosPorEvento(eventCode ?? ''),
    enabled: Boolean(eventCode),
    staleTime: 15_000,
  })
}

/** El email se toma de la sesión, nunca de un campo editable (PRD §24.3). */
export function useComprarTicket(eventCode: string) {
  const qc = useQueryClient()
  const { sesion } = useAuth()
  const email = sesion.estado === 'autenticado' ? sesion.usuario.email : ''
  return useMutation({
    mutationFn: (type: TicketType) => ticketsApi.comprar({ userEmail: email, eventCode, type }),
    onSuccess: (ticket) => {
      qc.setQueryData(['ticket', ticket.ticketCode], ticket)
      void qc.invalidateQueries({ queryKey: ['tickets', 'usuario', email] })
      void qc.invalidateQueries({ queryKey: ['evento', eventCode] })
    },
  })
}

function invalidarTicket(qc: ReturnType<typeof useQueryClient>, ticket: TicketResponse) {
  qc.setQueryData(['ticket', ticket.ticketCode], ticket)
  void qc.invalidateQueries({ queryKey: ['tickets', 'usuario', ticket.userEmail] })
  void qc.invalidateQueries({ queryKey: ['tickets', 'pagados', ticket.eventCode] })
}

export function useCancelarTicket() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (ticketCode: string) => ticketsApi.cancelar(ticketCode),
    onSuccess: (ticket) => invalidarTicket(qc, ticket),
  })
}

export function useUsarTicket() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (ticketCode: string) => ticketsApi.usar(ticketCode),
    onSuccess: (ticket) => invalidarTicket(qc, ticket),
  })
}
