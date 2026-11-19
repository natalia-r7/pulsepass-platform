import { useParams } from 'react-router'
import { EstadoVacio } from '../componentes/Estados'
import { ResultadoConsulta } from '../componentes/ResultadoConsulta'
import { Tarjeta } from '../componentes/Tarjeta'
import { useArtista } from '../funcionalidades/artistas/hooks'
import { ListaEventos } from '../funcionalidades/eventos/componentes/TarjetaEvento'
import { useEventosPorArtista } from '../funcionalidades/eventos/hooks'
import NoEncontrada from './NoEncontrada'

export default function DetalleArtista() {
  const { id = '' } = useParams()
  const numero = /^\d+$/.test(id) ? Number(id) : null
  const artista = useArtista(numero)
  // Un id no numérico es 404 sin llamar a la API (TEST-FE-ART-003).
  if (numero === null) return <NoEncontrada />
  return (
    <ResultadoConsulta consulta={artista} noEncontrado={<NoEncontrada />}>
      {(a) => (
        <div className="space-y-6">
          <Tarjeta>
            <h1 className="text-3xl font-bold">{a.stageName}</h1>
            <p className="mt-1 text-slate-600 dark:text-slate-400">{[a.genre, a.country].filter(Boolean).join(' · ') || 'Sin datos adicionales'}</p>
          </Tarjeta>
          <section>
            <h2 className="mb-4 text-xl font-bold">Eventos</h2>
            <EventosDeArtista stageName={a.stageName} />
          </section>
        </div>
      )}
    </ResultadoConsulta>
  )
}

function EventosDeArtista({ stageName }: { stageName: string }) {
  const eventos = useEventosPorArtista(stageName)
  return (
    <ResultadoConsulta consulta={eventos}>
      {(lista) => (lista.length === 0 ? <EstadoVacio mensaje="Este artista no tiene eventos publicados." /> : <ListaEventos eventos={lista} />)}
    </ResultadoConsulta>
  )
}
