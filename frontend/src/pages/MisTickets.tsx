import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { BadgeTicket } from '../componentes/Badge'
import { Boton } from '../componentes/Boton'
import { Selector } from '../componentes/Campo'
import { EstadoVacio } from '../componentes/Estados'
import { PrecioCOP } from '../componentes/PrecioCOP'
import { ResultadoConsulta } from '../componentes/ResultadoConsulta'
import { Titulo } from '../componentes/Titulo'
import { useAuth } from '../funcionalidades/auth/useAuth'
import { useMisTickets } from '../funcionalidades/tickets/hooks'
import { ModalCancelar } from '../funcionalidades/tickets/ModalCancelar'
import { ETIQUETA_ESTADO_TICKET, ETIQUETA_TIPO, TicketStatusSchema } from '../funcionalidades/tickets/tipos'
import { formatearFechaHora } from '../lib/formato'
import { RUTAS } from '../lib/rutas'

export default function MisTickets() {
  const { sesion } = useAuth()
  const email = sesion.estado === 'autenticado' ? sesion.usuario.email : undefined
  const tickets = useMisTickets(email)
  const [params, setParams] = useSearchParams()
  const estado = TicketStatusSchema.safeParse(params.get('estado')).data
  const [aCancelar, setACancelar] = useState<string | null>(null)

  return (
    <div>
      <Titulo>Mis tickets</Titulo>
      <div className="mb-6 max-w-xs">
        <Selector
          etiqueta="Filtrar por estado"
          value={estado ?? ''}
          onChange={(e) => setParams(e.target.value ? { estado: e.target.value } : {}, { replace: true })}
        >
          <option value="">Todos</option>
          {TicketStatusSchema.options.map((s) => (
            <option key={s} value={s}>{ETIQUETA_ESTADO_TICKET[s]}</option>
          ))}
        </Selector>
      </div>
      <ResultadoConsulta consulta={tickets}>
        {(lista) => {
          const filtrados = estado ? lista.filter((t) => t.status === estado) : lista
          if (filtrados.length === 0) return <EstadoVacio mensaje="No tienes tickets con este filtro." />
          return (
            <ul className="space-y-3">
              {filtrados.map((t) => (
                <li key={t.ticketCode} className="flex flex-wrap items-center justify-between gap-3 overflow-hidden rounded-2xl border-l-8 border-marca-600 bg-white p-5 shadow-sm ring-1 ring-slate-200/80 dark:bg-white/5 dark:ring-white/10">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Entrada · {ETIQUETA_TIPO[t.type]}</p>
                    <p className="font-mono text-lg font-bold tracking-wider">
                      <Link to={RUTAS.ticket(t.ticketCode)} className="hover:text-marca-700 dark:hover:text-marca-300">{t.ticketCode}</Link>
                    </p>
                    <p className="text-sm text-slate-600 dark:text-slate-400">
                      {t.eventCode} · <PrecioCOP valor={t.price} /> · {formatearFechaHora(t.purchaseDate)}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <BadgeTicket estado={t.status} />
                    {t.status === 'PAID' && (
                      <Boton variante="peligro" onClick={() => setACancelar(t.ticketCode)}>Cancelar</Boton>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )
        }}
      </ResultadoConsulta>
      <ModalCancelar ticketCode={aCancelar} onCerrar={() => setACancelar(null)} />
    </div>
  )
}
