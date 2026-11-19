import { Link } from 'react-router'
import { BadgeEvento } from '../../../componentes/Badge'
import { formatearFechaHora } from '../../../lib/formato'
import { RUTAS } from '../../../lib/rutas'
import { ETIQUETA_CATEGORIA, type EventSummaryResponse } from '../tipos'

export function TarjetaEvento({ evento }: { evento: EventSummaryResponse }) {
  return (
    <li className="rounded-xl bg-white p-5 shadow-sm ring-1 ring-slate-200 transition hover:ring-marca-500 dark:bg-slate-900 dark:ring-slate-800">
      <div className="flex items-start justify-between gap-2">
        <span className="text-xs font-semibold uppercase tracking-wide text-acento-500">{ETIQUETA_CATEGORIA[evento.category]}</span>
        <BadgeEvento estado={evento.status} />
      </div>
      <h3 className="mt-2 text-lg font-semibold">
        <Link to={RUTAS.evento(evento.eventCode)} className="hover:underline focus-visible:outline-2 focus-visible:outline-marca-600">
          {evento.name}
        </Link>
      </h3>
      <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{formatearFechaHora(evento.eventDate)}</p>
      <p className="text-sm text-slate-600 dark:text-slate-400">Sede {evento.venueCode}</p>
    </li>
  )
}

export function ListaEventos({ eventos }: { eventos: EventSummaryResponse[] }) {
  return (
    <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {eventos.map((e) => (
        <TarjetaEvento key={e.eventCode} evento={e} />
      ))}
    </ul>
  )
}
