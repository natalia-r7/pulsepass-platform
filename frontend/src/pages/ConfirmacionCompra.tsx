import { Link, useParams } from 'react-router'
import { BadgeTicket } from '../componentes/Badge'
import { EstadoVacio } from '../componentes/Estados'
import { PrecioCOP } from '../componentes/PrecioCOP'
import { ResultadoConsulta } from '../componentes/ResultadoConsulta'
import { Tarjeta } from '../componentes/Tarjeta'
import { useTicket } from '../funcionalidades/tickets/hooks'
import { ETIQUETA_TIPO } from '../funcionalidades/tickets/tipos'
import { RUTAS } from '../lib/rutas'

export default function ConfirmacionCompra() {
  const { ticketCode = '' } = useParams()
  const ticket = useTicket(ticketCode)
  return (
    <ResultadoConsulta consulta={ticket} ocultar403 noEncontrado={<EstadoVacio mensaje="Ticket no disponible." />}>
      {(t) => (
        <Tarjeta className="mx-auto max-w-lg text-center">
          <p aria-hidden="true" className="text-5xl">✓</p>
          <h1 className="mt-2 text-2xl font-bold">¡Compra confirmada!</h1>
          <p className="mt-2 text-slate-600 dark:text-slate-400">Tu ticket {t.ticketCode} para {t.eventCode} está listo.</p>
          <div className="mt-4 flex items-center justify-center gap-3">
            <span>{ETIQUETA_TIPO[t.type]}</span>
            <span className="font-semibold"><PrecioCOP valor={t.price} /></span>
            <BadgeTicket estado={t.status} />
          </div>
          <div className="mt-6 flex flex-wrap justify-center gap-4">
            <Link to={RUTAS.ticket(t.ticketCode)} className="font-semibold text-marca-600 hover:underline">Ver ticket</Link>
            <Link to={RUTAS.misTickets} className="font-semibold text-marca-600 hover:underline">Ir a mis tickets</Link>
          </div>
        </Tarjeta>
      )}
    </ResultadoConsulta>
  )
}
