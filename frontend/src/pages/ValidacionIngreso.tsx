import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router'
import { z } from 'zod'
import { Aviso } from '../componentes/Aviso'
import { BadgeTicket } from '../componentes/Badge'
import { Boton } from '../componentes/Boton'
import { Campo } from '../componentes/Campo'
import { PrecioCOP } from '../componentes/PrecioCOP'
import { ResultadoConsulta } from '../componentes/ResultadoConsulta'
import { Tarjeta } from '../componentes/Tarjeta'
import { Titulo } from '../componentes/Titulo'
import { useTicket, useUsarTicket } from '../funcionalidades/tickets/hooks'
import { ETIQUETA_TIPO } from '../funcionalidades/tickets/tipos'
import { EstadoVacio } from '../componentes/Estados'
import { mensajeDeError } from '../lib/http'
import { RUTAS } from '../lib/rutas'

const BusquedaSchema = z.object({ codigo: z.string().trim().min(1, 'Ingresa el código del ticket') })
const EventoSchema = z.object({ codigo: z.string().trim().min(1, 'Ingresa el código del evento') })

export default function ValidacionIngreso() {
  const [codigo, setCodigo] = useState<string>('')
  const navigate = useNavigate()
  const busqueda = useForm<z.infer<typeof BusquedaSchema>>({ resolver: zodResolver(BusquedaSchema) })
  const evento = useForm<z.infer<typeof EventoSchema>>({ resolver: zodResolver(EventoSchema) })

  return (
    <div className="space-y-6">
      <Titulo>Validación de ingreso</Titulo>
      <Tarjeta>
        <form noValidate className="flex flex-wrap items-end gap-3" onSubmit={(e) => void busqueda.handleSubmit((d) => setCodigo(d.codigo))(e)}>
          <Campo className="min-w-60 flex-1" etiqueta="Código del ticket" {...busqueda.register('codigo')} error={busqueda.formState.errors.codigo?.message} />
          <Boton type="submit">Buscar</Boton>
        </form>
      </Tarjeta>
      {codigo && <ResultadoTicket key={codigo} codigo={codigo} />}
      <Tarjeta>
        <h2 className="font-semibold">Tickets pagados por evento</h2>
        <form noValidate className="mt-3 flex flex-wrap items-end gap-3" onSubmit={(e) => void evento.handleSubmit((d) => navigate(RUTAS.ticketsPagados(d.codigo)))(e)}>
          <Campo className="min-w-60 flex-1" etiqueta="Código del evento" {...evento.register('codigo')} error={evento.formState.errors.codigo?.message} />
          <Boton type="submit" variante="secundario">Ver tickets pagados</Boton>
        </form>
      </Tarjeta>
    </div>
  )
}

function ResultadoTicket({ codigo }: { codigo: string }) {
  const ticket = useTicket(codigo)
  const usar = useUsarTicket()
  return (
    <ResultadoConsulta consulta={ticket} noEncontrado={<EstadoVacio mensaje={`No existe un ticket con el código ${codigo}.`} />}>
      {(t) => (
        <Tarjeta>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-xl font-semibold">Ticket {t.ticketCode}</h2>
            <BadgeTicket estado={t.status} />
          </div>
          <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
            {t.eventCode} · {ETIQUETA_TIPO[t.type]} · <PrecioCOP valor={t.price} /> · {t.userEmail}
          </p>
          <div className="mt-4 space-y-3">
            {usar.isSuccess && <Aviso tipo="exito" mensaje="Ingreso registrado: el ticket quedó como usado." />}
            {usar.isError && <Aviso tipo="error" mensaje={mensajeDeError(usar.error)} />}
            <Boton onClick={() => usar.mutate(t.ticketCode)} cargando={usar.isPending}>Marcar como usado</Boton>
          </div>
        </Tarjeta>
      )}
    </ResultadoConsulta>
  )
}
