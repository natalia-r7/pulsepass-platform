import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { BadgeTicket } from '../componentes/Badge'
import { Boton } from '../componentes/Boton'
import { EstadoVacio } from '../componentes/Estados'
import { PrecioCOP } from '../componentes/PrecioCOP'
import { ResultadoConsulta } from '../componentes/ResultadoConsulta'
import { Tarjeta } from '../componentes/Tarjeta'
import { useTicket } from '../funcionalidades/tickets/hooks'
import { ModalCancelar } from '../funcionalidades/tickets/ModalCancelar'
import { ETIQUETA_TIPO } from '../funcionalidades/tickets/tipos'
import { formatearFechaHora } from '../lib/formato'
import { RUTAS } from '../lib/rutas'

export default function DetalleTicket() {
  const { ticketCode = '' } = useParams()
  const ticket = useTicket(ticketCode)
  const [cancelando, setCancelando] = useState<string | null>(null)
  return (
    <ResultadoConsulta consulta={ticket} ocultar403 noEncontrado={<EstadoVacio mensaje="Ticket no disponible." />}>
      {(t) => (
        <Tarjeta className="mx-auto max-w-xl">
          <div className="flex items-center justify-between gap-3">
            <h1 className="text-2xl font-bold">Ticket {t.ticketCode}</h1>
            <BadgeTicket estado={t.status} />
          </div>
          <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
            <div><dt className="text-slate-500">Evento</dt><dd className="font-semibold"><Link to={RUTAS.evento(t.eventCode)} className="hover:underline">{t.eventCode}</Link></dd></div>
            <div><dt className="text-slate-500">Tipo</dt><dd className="font-semibold">{ETIQUETA_TIPO[t.type]}</dd></div>
            <div><dt className="text-slate-500">Precio</dt><dd className="font-semibold"><PrecioCOP valor={t.price} /></dd></div>
            <div><dt className="text-slate-500">Fecha de compra</dt><dd className="font-semibold">{formatearFechaHora(t.purchaseDate)}</dd></div>
          </dl>
          {t.status === 'PAID' && (
            <Boton variante="peligro" className="mt-6" onClick={() => setCancelando(t.ticketCode)}>Cancelar ticket</Boton>
          )}
          <ModalCancelar ticketCode={cancelando} onCerrar={() => setCancelando(null)} />
        </Tarjeta>
      )}
    </ResultadoConsulta>
  )
}
