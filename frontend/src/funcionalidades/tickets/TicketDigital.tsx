import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { BadgeTicket } from '../../componentes/Badge'
import { PrecioCOP } from '../../componentes/PrecioCOP'
import { formatearFechaHora } from '../../lib/formato'
import { RUTAS } from '../../lib/rutas'
import { useEvento } from '../eventos/hooks'
import { ETIQUETA_TIPO, type TicketResponse } from './tipos'

/**
 * Entrada digital. El nombre y la fecha del evento se piden a la API; si el evento ya no es público
 * (agotado, cancelado…), se muestra solo su código. Sin QR: fuera de alcance (PRD §2.4).
 */
export function TicketDigital({ ticket, acciones }: { ticket: TicketResponse; acciones?: ReactNode }) {
  const evento = useEvento(ticket.eventCode)
  const nombre = evento.data?.name ?? ticket.eventCode
  return (
    <article className="mx-auto w-full max-w-xl overflow-hidden rounded-3xl bg-white shadow-xl shadow-marca-600/10 ring-1 ring-slate-200/80 dark:bg-white/5 dark:ring-white/10">
      <div className="fondo-degradado relative px-6 py-6 text-white">
        <div aria-hidden="true" className="absolute -right-10 -top-10 h-32 w-32 rounded-full bg-white/15 blur-2xl" />
        <p className="text-xs font-semibold uppercase tracking-widest text-white/80">Entrada {ETIQUETA_TIPO[ticket.type]}</p>
        <h2 className="mt-1 text-2xl font-extrabold leading-tight">
          <Link to={RUTAS.evento(ticket.eventCode)} className="hover:underline">
            {nombre}
          </Link>
        </h2>
        {evento.data && (
          <p className="mt-1 text-sm text-white/85">
            {formatearFechaHora(evento.data.eventDate)}
            {evento.data.venueName ? ` · ${evento.data.venueName}` : ''}
          </p>
        )}
      </div>
      {/* Línea perforada entre la cabecera y el talón */}
      <div aria-hidden="true" className="relative h-0">
        <span className="absolute -left-4 -top-4 h-8 w-8 rounded-full bg-slate-50 dark:bg-[#0b0618]" />
        <span className="absolute -right-4 -top-4 h-8 w-8 rounded-full bg-slate-50 dark:bg-[#0b0618]" />
        <span className="absolute inset-x-6 top-0 border-t-2 border-dashed border-slate-200 dark:border-white/15" />
      </div>
      <div className="grid gap-4 px-6 py-6 sm:grid-cols-2">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Código</p>
          <p className="font-mono text-xl font-bold tracking-wider">{ticket.ticketCode}</p>
        </div>
        <div className="sm:text-right">
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Estado</p>
          <BadgeTicket estado={ticket.status} />
        </div>
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Precio</p>
          <p className="text-lg font-bold">
            <PrecioCOP valor={ticket.price} />
          </p>
        </div>
        <div className="sm:text-right">
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Comprada</p>
          <p className="text-sm font-medium">{formatearFechaHora(ticket.purchaseDate)}</p>
        </div>
      </div>
      {acciones && <div className="flex flex-wrap gap-3 border-t border-slate-100 px-6 py-4 dark:border-white/10">{acciones}</div>}
    </article>
  )
}
