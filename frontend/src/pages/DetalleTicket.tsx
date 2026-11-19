import { useState } from 'react'
import { useParams } from 'react-router'
import { Boton } from '../componentes/Boton'
import { EstadoVacio } from '../componentes/Estados'
import { ResultadoConsulta } from '../componentes/ResultadoConsulta'
import { useTicket } from '../funcionalidades/tickets/hooks'
import { ModalCancelar } from '../funcionalidades/tickets/ModalCancelar'
import { TicketDigital } from '../funcionalidades/tickets/TicketDigital'

export default function DetalleTicket() {
  const { ticketCode = '' } = useParams()
  const ticket = useTicket(ticketCode)
  const [cancelando, setCancelando] = useState<string | null>(null)
  return (
    <ResultadoConsulta consulta={ticket} ocultar403 noEncontrado={<EstadoVacio mensaje="Ticket no disponible." />}>
      {(t) => (
        <div className="space-y-6">
          <h1 className="sr-only">Ticket {t.ticketCode}</h1>
          <TicketDigital
            ticket={t}
            acciones={
              t.status === 'PAID' ? (
                <Boton variante="peligro" onClick={() => setCancelando(t.ticketCode)}>Cancelar ticket</Boton>
              ) : undefined
            }
          />
          <ModalCancelar ticketCode={cancelando} onCerrar={() => setCancelando(null)} />
        </div>
      )}
    </ResultadoConsulta>
  )
}
