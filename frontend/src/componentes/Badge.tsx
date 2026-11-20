import type { EventStatus } from '../funcionalidades/eventos/tipos'
import { ETIQUETA_ESTADO_EVENTO } from '../funcionalidades/eventos/tipos'
import type { TicketStatus } from '../funcionalidades/tickets/tipos'
import { ETIQUETA_ESTADO_TICKET } from '../funcionalidades/tickets/tipos'

const TONO_EVENTO: Record<EventStatus, string> = {
  DRAFT: 'bg-slate-200 text-slate-700',
  PUBLISHED: 'bg-emerald-100 text-emerald-800',
  SOLD_OUT: 'bg-amber-100 text-amber-800',
  CANCELLED: 'bg-red-100 text-red-800',
  FINISHED: 'bg-slate-300 text-slate-800',
}

const TONO_TICKET: Record<TicketStatus, string> = {
  RESERVED: 'bg-sky-100 text-sky-800',
  PAID: 'bg-emerald-100 text-emerald-800',
  USED: 'bg-slate-300 text-slate-800',
  CANCELLED: 'bg-red-100 text-red-800',
}

const BASE = 'inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-semibold ring-1 ring-inset ring-black/5'

export function BadgeEvento({ estado }: { estado: EventStatus }) {
  return <span className={`${BASE} ${TONO_EVENTO[estado]}`}>{ETIQUETA_ESTADO_EVENTO[estado]}</span>
}

export function BadgeTicket({ estado }: { estado: TicketStatus }) {
  return <span className={`${BASE} ${TONO_TICKET[estado]}`}>{ETIQUETA_ESTADO_TICKET[estado]}</span>
}
