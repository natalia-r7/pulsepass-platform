import { Link } from 'react-router'
import { Icono } from '../../../componentes/Icono'
import { RUTAS } from '../../../lib/rutas'
import { useSedesActivas } from '../../sedes/hooks'
import { ETIQUETA_CATEGORIA, type EventCategory, type EventStatus, type EventSummaryResponse } from '../tipos'

/** Cabecera por categoría: mapa de clases completas (PRD §24.4, sin clases dinámicas). */
const CABECERA: Record<EventCategory, string> = {
  MUSIC: 'from-marca-700 via-marca-600 to-acento-600',
  SPORTS: 'from-indigo-700 via-violet-600 to-sky-500',
  TECHNOLOGY: 'from-violet-800 via-indigo-600 to-cyan-500',
  EDUCATION: 'from-purple-700 via-violet-600 to-indigo-500',
  CULTURE: 'from-fuchsia-700 via-purple-600 to-rose-500',
  ENTERTAINMENT: 'from-pink-600 via-fuchsia-600 to-violet-600',
}

/** Disponibilidad según el estado real del evento; la API no informa cupos restantes (GAP-04). */
const DISPONIBILIDAD: Record<EventStatus, { texto: string; clase: string }> = {
  PUBLISHED: { texto: 'Entradas disponibles', clase: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-500/15 dark:text-emerald-300' },
  SOLD_OUT: { texto: 'Agotado', clase: 'bg-amber-100 text-amber-800 dark:bg-amber-500/15 dark:text-amber-300' },
  DRAFT: { texto: 'Borrador', clase: 'bg-slate-200 text-slate-700 dark:bg-white/10 dark:text-slate-300' },
  CANCELLED: { texto: 'Cancelado', clase: 'bg-red-100 text-red-800 dark:bg-red-500/15 dark:text-red-300' },
  FINISHED: { texto: 'Finalizado', clase: 'bg-slate-200 text-slate-700 dark:bg-white/10 dark:text-slate-300' },
}

const dia = new Intl.DateTimeFormat('es-CO', { day: '2-digit' })
const mes = new Intl.DateTimeFormat('es-CO', { month: 'short' })
const fechaLarga = new Intl.DateTimeFormat('es-CO', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })
const hora = new Intl.DateTimeFormat('es-CO', { hour: 'numeric', minute: '2-digit' })

export function TarjetaEvento({ evento, sede }: { evento: EventSummaryResponse; sede?: { name: string; city: string } }) {
  const fecha = new Date(evento.eventDate)
  const fechaValida = !Number.isNaN(fecha.getTime())
  const disponibilidad = DISPONIBILIDAD[evento.status]
  return (
    <li className="group relative flex flex-col overflow-hidden rounded-2xl bg-white ring-1 ring-slate-200 transition hover:ring-marca-400 dark:bg-white/5 dark:ring-white/10 dark:hover:ring-marca-400">
      <div className={`relative h-28 bg-gradient-to-br ${CABECERA[evento.category]}`}>
        <span className="absolute left-4 top-4 rounded-full bg-black/25 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-white backdrop-blur">
          {ETIQUETA_CATEGORIA[evento.category]}
        </span>
        {fechaValida && (
          <div className="absolute -bottom-6 right-4 flex h-16 w-16 flex-col items-center justify-center rounded-xl bg-white text-center shadow-md ring-1 ring-slate-200 dark:bg-[#150c2b] dark:ring-white/10">
            <span className="text-2xl font-extrabold leading-none text-marca-700 dark:text-white">{dia.format(fecha)}</span>
            <span className="text-xs font-semibold uppercase text-slate-500 dark:text-slate-400">{mes.format(fecha).replace('.', '')}</span>
          </div>
        )}
      </div>
      <div className="flex flex-1 flex-col gap-2 p-5 pt-4">
        <h3 className="pr-16 text-lg font-bold leading-snug">
          <Link
            to={RUTAS.evento(evento.eventCode)}
            className="after:absolute after:inset-0 hover:text-marca-700 focus-visible:outline-2 focus-visible:outline-marca-600 dark:hover:text-marca-300"
          >
            {evento.name}
          </Link>
        </h3>
        {fechaValida && (
          <p className="text-sm text-slate-600 capitalize dark:text-slate-400">
            {fechaLarga.format(fecha)} · {hora.format(fecha)}
          </p>
        )}
        <p className="flex items-center gap-1.5 text-sm text-slate-600 dark:text-slate-400">
          <Icono nombre="mapPin" className="h-4 w-4 shrink-0 text-marca-600 dark:text-marca-400" />
          {sede ? `${sede.name} · ${sede.city}` : `Sede ${evento.venueCode}`}
        </p>
        <div className="mt-auto pt-2">
          <span className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${disponibilidad.clase}`}>{disponibilidad.texto}</span>
        </div>
      </div>
    </li>
  )
}

export function ListaEventos({ eventos }: { eventos: EventSummaryResponse[] }) {
  // Nombre y ciudad de la sede desde la lista de sedes activas (ya en caché); si no está, se muestra el código.
  const sedes = useSedesActivas()
  const porCodigo = new Map((sedes.data ?? []).map((s) => [s.code, s]))
  return (
    <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
      {eventos.map((e) => (
        <TarjetaEvento key={e.eventCode} evento={e} sede={porCodigo.get(e.venueCode)} />
      ))}
    </ul>
  )
}
