import { Link, useParams } from 'react-router'
import { EstadoVacio } from '../componentes/Estados'
import { ResultadoConsulta } from '../componentes/ResultadoConsulta'
import { useTicket } from '../funcionalidades/tickets/hooks'
import { TicketDigital } from '../funcionalidades/tickets/TicketDigital'
import { RUTAS } from '../lib/rutas'

export default function ConfirmacionCompra() {
  const { ticketCode = '' } = useParams()
  const ticket = useTicket(ticketCode)
  return (
    <ResultadoConsulta consulta={ticket} ocultar403 noEncontrado={<EstadoVacio mensaje="Ticket no disponible." />}>
      {(t) => (
        <div className="space-y-6">
          <div className="text-center">
            <p aria-hidden="true" className="fondo-degradado mx-auto flex h-14 w-14 items-center justify-center rounded-full text-2xl text-white shadow-lg shadow-marca-600/30">
              ✓
            </p>
            <h1 className="mt-4 text-3xl font-extrabold">¡Compra confirmada!</h1>
            <p className="mt-2 text-slate-600 dark:text-slate-400">Tu entrada está lista. Preséntala con su código al ingresar.</p>
          </div>
          <TicketDigital
            ticket={t}
            acciones={
              <>
                <Link to={RUTAS.ticket(t.ticketCode)} className="font-semibold text-marca-600 hover:underline dark:text-marca-300">Ver ticket</Link>
                <Link to={RUTAS.misTickets} className="font-semibold text-marca-600 hover:underline dark:text-marca-300">Ir a mis tickets</Link>
              </>
            }
          />
        </div>
      )}
    </ResultadoConsulta>
  )
}
