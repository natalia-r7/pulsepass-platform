import { useSearchParams } from 'react-router'
import { Campo, Selector } from '../componentes/Campo'
import { EstadoVacio } from '../componentes/Estados'
import { ResultadoConsulta } from '../componentes/ResultadoConsulta'
import { Titulo } from '../componentes/Titulo'
import { ListaEventos } from '../funcionalidades/eventos/componentes/TarjetaEvento'
import { useEventosPorArtista, useEventosPublicados } from '../funcionalidades/eventos/hooks'
import { ETIQUETA_CATEGORIA, EventCategorySchema } from '../funcionalidades/eventos/tipos'

export default function CatalogoEventos() {
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''
  const categoria = EventCategorySchema.safeParse(params.get('categoria')).data
  const artista = params.get('artista') ?? ''

  const publicados = useEventosPublicados()
  const porArtista = useEventosPorArtista(artista || undefined)
  const consulta = artista ? porArtista : publicados

  const actualizar = (clave: string, valor: string) => {
    const nuevos = new URLSearchParams(params)
    if (valor) nuevos.set(clave, valor)
    else nuevos.delete(clave)
    setParams(nuevos, { replace: true })
  }

  return (
    <div>
      <Titulo>Cartelera</Titulo>
      <form role="search" className="mb-6 grid gap-4 sm:grid-cols-3" onSubmit={(e) => e.preventDefault()}>
        <Campo etiqueta="Buscar por nombre" type="search" value={q} onChange={(e) => actualizar('q', e.target.value)} />
        <Selector etiqueta="Categoría" value={categoria ?? ''} onChange={(e) => actualizar('categoria', e.target.value)}>
          <option value="">Todas</option>
          {EventCategorySchema.options.map((c) => (
            <option key={c} value={c}>{ETIQUETA_CATEGORIA[c]}</option>
          ))}
        </Selector>
        <Campo etiqueta="Artista (nombre artístico exacto)" type="search" defaultValue={artista} onBlur={(e) => actualizar('artista', e.target.value.trim())} />
      </form>
      <ResultadoConsulta consulta={consulta}>
        {(lista) => {
          const filtrados = lista.filter(
            (e) => (!q || e.name.toLowerCase().includes(q.toLowerCase())) && (!categoria || e.category === categoria),
          )
          return filtrados.length === 0 ? <EstadoVacio mensaje="No hay eventos que coincidan con la búsqueda." /> : <ListaEventos eventos={filtrados} />
        }}
      </ResultadoConsulta>
    </div>
  )
}
