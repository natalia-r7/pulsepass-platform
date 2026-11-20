import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { Aviso } from '../componentes/Aviso'
import { BadgeEvento } from '../componentes/Badge'
import { Boton } from '../componentes/Boton'
import { Selector } from '../componentes/Campo'
import { EstadoVacio } from '../componentes/Estados'
import { Icono } from '../componentes/Icono'
import { Modal } from '../componentes/Modal'
import { ResultadoConsulta } from '../componentes/ResultadoConsulta'
import { Tarjeta } from '../componentes/Tarjeta'
import { useAuth } from '../funcionalidades/auth/useAuth'
import { useEvento } from '../funcionalidades/eventos/hooks'
import { ETIQUETA_CATEGORIA } from '../funcionalidades/eventos/tipos'
import { useSede } from '../funcionalidades/sedes/hooks'
import { useComprarTicket } from '../funcionalidades/tickets/hooks'
import { ETIQUETA_TIPO, TicketTypeSchema, type TicketType } from '../funcionalidades/tickets/tipos'
import { formatearFechaHora } from '../lib/formato'
import { mensajeDeError } from '../lib/http'
import { RUTAS } from '../lib/rutas'

export default function DetalleEvento() {
  const { eventCode = '' } = useParams()
  const evento = useEvento(eventCode)
  return (
    <ResultadoConsulta consulta={evento} ocultar403 noEncontrado={<EstadoVacio mensaje="Evento no disponible." />}>
      {(e) => (
        <article className="grid gap-6 lg:grid-cols-3">
          <div className="space-y-6 lg:col-span-2">
            <header className="fondo-degradado relative overflow-hidden rounded-3xl px-6 py-10 text-white sm:px-8">
              <div className="relative flex flex-wrap items-center gap-3">
                <span className="rounded-full bg-black/25 px-3 py-1 text-xs font-semibold uppercase tracking-wide">{ETIQUETA_CATEGORIA[e.category]}</span>
                <BadgeEvento estado={e.status} />
              </div>
              <h1 className="relative mt-4 text-3xl font-extrabold leading-tight sm:text-5xl">{e.name}</h1>
              <p className="relative mt-3 flex items-center gap-2 text-white/90">
                <Icono nombre="calendar" className="h-4 w-4" />
                {formatearFechaHora(e.eventDate)}
              </p>
            </header>
            {e.description && <p className="whitespace-pre-line text-lg text-slate-700 dark:text-slate-300">{e.description}</p>}
            <dl className="grid grid-cols-2 gap-4 text-sm">
              <div className="rounded-2xl bg-white/90 p-4 ring-1 ring-slate-200/80 dark:bg-white/5 dark:ring-white/10">
                <dt className="text-slate-500">Edad mínima</dt>
                <dd className="text-lg font-bold">{e.minimumAge === 0 ? 'Todo público' : `${e.minimumAge} años`}</dd>
              </div>
              <div className="rounded-2xl bg-white/90 p-4 ring-1 ring-slate-200/80 dark:bg-white/5 dark:ring-white/10">
                <dt className="text-slate-500">Código</dt>
                <dd className="font-mono text-lg font-bold">{e.eventCode}</dd>
              </div>
            </dl>
          </div>
          <aside className="space-y-4">
            <InfoSede code={e.venueCode} nombre={e.venueName ?? e.venueCode} />
            <PanelCompra eventCode={e.eventCode} publicado={e.status === 'PUBLISHED'} />
          </aside>
        </article>
      )}
    </ResultadoConsulta>
  )
}

function InfoSede({ code, nombre }: { code: string; nombre: string }) {
  const sede = useSede(code)
  return (
    <Tarjeta>
      <h2 className="font-semibold">Sede</h2>
      <p className="mt-1">
        <Link to={RUTAS.sede(code)} className="text-marca-600 hover:underline dark:text-marca-500">{nombre}</Link>
      </p>
      {sede.data && (
        <p className="text-sm text-slate-600 dark:text-slate-400">
          {sede.data.city} · Capacidad {sede.data.capacity}
        </p>
      )}
    </Tarjeta>
  )
}

function PanelCompra({ eventCode, publicado }: { eventCode: string; publicado: boolean }) {
  const { sesion, tieneRol } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [abierto, setAbierto] = useState(false)
  const [tipo, setTipo] = useState<TicketType>('GENERAL')
  const compra = useComprarTicket(eventCode)

  const pulsarComprar = () => {
    if (sesion.estado !== 'autenticado') {
      void navigate(RUTAS.login, { state: { desde: location.pathname } })
      return
    }
    compra.reset()
    setAbierto(true)
  }

  const confirmar = () => {
    compra.mutate(tipo, {
      onSuccess: (ticket) => void navigate(RUTAS.confirmacion(ticket.ticketCode)),
    })
  }

  const puedeComprar = sesion.estado !== 'autenticado' || tieneRol('USER')

  return (
    <Tarjeta>
      <h2 className="font-semibold">Entradas</h2>
      {!publicado && <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">Este evento no está disponible para la compra.</p>}
      {publicado && !puedeComprar && (
        <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">La compra solo está disponible para cuentas de asistente (USER).</p>
      )}
      <Boton className="mt-4 w-full" disabled={!publicado || !puedeComprar} onClick={pulsarComprar}>
        Comprar
      </Boton>
      <Modal abierto={abierto} titulo="Comprar entrada" onCerrar={() => setAbierto(false)}>
        <div className="space-y-4">
          <Selector etiqueta="Tipo de entrada" value={tipo} onChange={(e) => setTipo(TicketTypeSchema.parse(e.target.value))}>
            {TicketTypeSchema.options.map((t) => (
              <option key={t} value={t}>{ETIQUETA_TIPO[t]}</option>
            ))}
          </Selector>
          <p className="text-sm text-slate-600 dark:text-slate-400">El precio lo fija el sistema y se muestra en la confirmación.</p>
          {compra.isError && <Aviso tipo="error" mensaje={mensajeDeError(compra.error)} />}
          <div className="flex justify-end gap-2">
            <Boton variante="secundario" onClick={() => setAbierto(false)}>Cancelar</Boton>
            <Boton onClick={confirmar} cargando={compra.isPending}>Confirmar compra</Boton>
          </div>
        </div>
      </Modal>
    </Tarjeta>
  )
}
