import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { Aviso } from '../componentes/Aviso'
import { BadgeEvento } from '../componentes/Badge'
import { Boton } from '../componentes/Boton'
import { Selector } from '../componentes/Campo'
import { EstadoVacio } from '../componentes/Estados'
import { Modal } from '../componentes/Modal'
import { ResultadoConsulta } from '../componentes/ResultadoConsulta'
import { Tarjeta } from '../componentes/Tarjeta'
import { Titulo } from '../componentes/Titulo'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { useAsociarArtista, useEvento, usePublicarEvento } from '../funcionalidades/eventos/hooks'
import { recordarEvento } from '../funcionalidades/eventos/recientes'
import { ETIQUETA_CATEGORIA } from '../funcionalidades/eventos/tipos'
import { formatearFechaHora } from '../lib/formato'
import { mensajeDeError } from '../lib/http'
import { RUTAS } from '../lib/rutas'

export default function AdminGestionEvento() {
  const { eventCode = '' } = useParams()
  const evento = useEvento(eventCode)
  useEffect(() => {
    if (evento.isSuccess) recordarEvento(evento.data.eventCode)
  }, [evento.isSuccess, evento.data])

  return (
    <ResultadoConsulta consulta={evento} noEncontrado={<EstadoVacio mensaje={`No existe un evento con el código ${eventCode}.`} />}>
      {(e) => (
        <div className="space-y-6">
          <Titulo accion={<BadgeEvento estado={e.status} />}>{e.name}</Titulo>
          <Tarjeta>
            <dl className="grid gap-3 text-sm sm:grid-cols-3">
              <div><dt className="text-slate-500">Código</dt><dd className="font-semibold">{e.eventCode}</dd></div>
              <div><dt className="text-slate-500">Categoría</dt><dd className="font-semibold">{ETIQUETA_CATEGORIA[e.category]}</dd></div>
              <div><dt className="text-slate-500">Fecha</dt><dd className="font-semibold">{formatearFechaHora(e.eventDate)}</dd></div>
              <div><dt className="text-slate-500">Edad mínima</dt><dd className="font-semibold">{e.minimumAge}</dd></div>
              <div><dt className="text-slate-500">Sede</dt><dd className="font-semibold">{e.venueName ?? e.venueCode}</dd></div>
            </dl>
            <div className="mt-4 flex flex-wrap gap-4 text-sm">
              <Link to={RUTAS.evento(e.eventCode)} className="font-semibold text-marca-600 hover:underline">Ver página pública</Link>
              <Link to={RUTAS.ticketsPagados(e.eventCode)} className="font-semibold text-marca-600 hover:underline">Tickets pagados</Link>
            </div>
          </Tarjeta>
          <Publicar eventCode={e.eventCode} esBorrador={e.status === 'DRAFT'} />
          <AsociarArtista eventCode={e.eventCode} />
        </div>
      )}
    </ResultadoConsulta>
  )
}

function Publicar({ eventCode, esBorrador }: { eventCode: string; esBorrador: boolean }) {
  const publicar = usePublicarEvento(eventCode)
  const [abierto, setAbierto] = useState(false)
  return (
    <Tarjeta>
      <h2 className="font-semibold">Publicación</h2>
      {publicar.isSuccess && <div className="mt-3"><Aviso tipo="exito" mensaje="Evento publicado." /></div>}
      {publicar.isError && <div className="mt-3"><Aviso tipo="error" mensaje={mensajeDeError(publicar.error)} /></div>}
      <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
        {esBorrador ? 'El evento está en borrador y no aparece en la cartelera.' : 'El evento ya no está en borrador.'}
      </p>
      <Boton className="mt-3" onClick={() => setAbierto(true)}>Publicar evento</Boton>
      <Modal abierto={abierto} titulo="Publicar evento" onCerrar={() => setAbierto(false)}>
        <p>¿Publicar {eventCode}? Aparecerá en la cartelera pública.</p>
        <div className="mt-4 flex justify-end gap-2">
          <Boton variante="secundario" onClick={() => setAbierto(false)}>Volver</Boton>
          <Boton cargando={publicar.isPending} onClick={() => publicar.mutate(undefined, { onSettled: () => setAbierto(false) })}>Publicar</Boton>
        </div>
      </Modal>
    </Tarjeta>
  )
}

function AsociarArtista({ eventCode }: { eventCode: string }) {
  const artistas = useArtistasActivos()
  const asociar = useAsociarArtista(eventCode)
  const [seleccion, setSeleccion] = useState('')
  const [asociados, setAsociados] = useState<string[]>([])
  const artista = artistas.data?.find((a) => String(a.id) === seleccion)
  return (
    <Tarjeta>
      <h2 className="font-semibold">Asociar artista</h2>
      <div className="mt-3 space-y-3">
        {asociar.isError && <Aviso tipo="error" mensaje={mensajeDeError(asociar.error)} />}
        {asociados.length > 0 && <Aviso tipo="exito" mensaje={`Asociados en esta sesión: ${asociados.join(', ')}.`} />}
        <ResultadoConsulta consulta={artistas}>
          {(lista) => (
            <Selector etiqueta="Artista activo" value={seleccion} onChange={(ev) => setSeleccion(ev.target.value)}>
              <option value="">Selecciona un artista</option>
              {lista.map((a) => (
                <option key={a.id} value={a.id}>{a.stageName}</option>
              ))}
            </Selector>
          )}
        </ResultadoConsulta>
        <Boton
          disabled={!artista}
          cargando={asociar.isPending}
          onClick={() =>
            artista &&
            asociar.mutate(
              { id: artista.id, stageName: artista.stageName },
              { onSuccess: () => setAsociados((prev) => [...prev, artista.stageName]) },
            )
          }
        >
          Asociar
        </Boton>
      </div>
    </Tarjeta>
  )
}
