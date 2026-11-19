import { useParams } from 'react-router'
import { Aviso } from '../componentes/Aviso'
import { Boton } from '../componentes/Boton'
import { EstadoVacio } from '../componentes/Estados'
import { PrecioCOP } from '../componentes/PrecioCOP'
import { ResultadoConsulta } from '../componentes/ResultadoConsulta'
import { Tabla } from '../componentes/Tabla'
import { Titulo } from '../componentes/Titulo'
import { useTicketsPagadosPorEvento, useUsarTicket } from '../funcionalidades/tickets/hooks'
import { ETIQUETA_TIPO } from '../funcionalidades/tickets/tipos'
import { formatearFechaHora } from '../lib/formato'
import { mensajeDeError } from '../lib/http'

export default function TicketsPagadosEvento() {
  const { eventCode = '' } = useParams()
  const tickets = useTicketsPagadosPorEvento(eventCode)
  const usar = useUsarTicket()
  return (
    <div className="space-y-4">
      <Titulo>Tickets pagados · {eventCode}</Titulo>
      {usar.isSuccess && <Aviso tipo="exito" mensaje={`Ingreso registrado para ${usar.data.ticketCode}.`} />}
      {usar.isError && <Aviso tipo="error" mensaje={mensajeDeError(usar.error)} />}
      <ResultadoConsulta consulta={tickets} noEncontrado={<EstadoVacio mensaje="Evento no disponible." />}>
        {(lista) =>
          lista.length === 0 ? (
            <EstadoVacio mensaje="Este evento no tiene tickets pagados." />
          ) : (
            <Tabla
              titulo={`Tickets pagados del evento ${eventCode}`}
              filas={lista}
              clave={(t) => t.ticketCode}
              columnas={[
                { encabezado: 'Código', celda: (t) => t.ticketCode },
                { encabezado: 'Asistente', celda: (t) => t.userEmail },
                { encabezado: 'Tipo', celda: (t) => ETIQUETA_TIPO[t.type] },
                { encabezado: 'Precio', celda: (t) => <PrecioCOP valor={t.price} /> },
                { encabezado: 'Compra', celda: (t) => formatearFechaHora(t.purchaseDate) },
                {
                  encabezado: 'Acción',
                  celda: (t) => (
                    <Boton variante="secundario" onClick={() => usar.mutate(t.ticketCode)} disabled={usar.isPending}>
                      Marcar usado
                    </Boton>
                  ),
                },
              ]}
            />
          )
        }
      </ResultadoConsulta>
    </div>
  )
}
