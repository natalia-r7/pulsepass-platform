import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { Aviso } from '../componentes/Aviso'
import { BadgeEvento } from '../componentes/Badge'
import { Boton } from '../componentes/Boton'
import { Selector } from '../componentes/Campo'
import { EstadoVacio } from '../componentes/Estados'
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
          <div className="space-y-4 lg:col-span-2">
            <div className="flex flex-wrap items-center gap-3">
              <span className="text-sm font-semibold uppercase text-acento-500">{ETIQUETA_CATEGORIA[e.category]}</span>
              <BadgeEvento estado={e.status} />
            </div>
            <h1 className="text-3xl font-bold">{e.name}</h1>
            <p className="text-slate-600 dark:text-slate-400">{formatearFechaHora(e.eventDate)}</p>
            {e.description && <p className="whitespace-pre-line">{e.description}</p>}
            <dl className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <dt className="text-slate-500">Edad mínima</dt>
                <dd className="font-semibold">{e.minimumAge === 0 ? 'Todo público' : `${e.minimumAge} años`}</dd>
              </div>
              <div>
                <dt className="text-slate-500">Código</dt>
                <dd className="font-semibold">{e.eventCode}</dd>
              </div>
            </dl>
            <p className="text-sm text-slate-500">
              Artistas: la API no incluye los artistas en el detalle del evento (brecha GAP-09). Consulta el perfil de cada artista.
            </p>
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
