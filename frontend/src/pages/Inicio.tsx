import { Link } from 'react-router'
import { EstadoVacio } from '../componentes/Estados'
import { ResultadoConsulta } from '../componentes/ResultadoConsulta'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { ListaEventos } from '../funcionalidades/eventos/componentes/TarjetaEvento'
import { useEventosPublicados } from '../funcionalidades/eventos/hooks'
import { RUTAS } from '../lib/rutas'

export default function Inicio() {
  const eventos = useEventosPublicados()
  const artistas = useArtistasActivos()
  return (
    <div className="space-y-10">
      <section className="rounded-2xl bg-gradient-to-br from-marca-600 to-marca-700 px-6 py-12 text-white sm:px-10">
        <h1 className="text-3xl font-bold sm:text-4xl">Vive la música, el deporte y la cultura</h1>
        <p className="mt-3 max-w-xl text-marca-50">Descubre eventos publicados, compra tus entradas y gestiona tus tickets en un solo lugar.</p>
        <Link to={RUTAS.eventos} className="mt-6 inline-flex min-h-11 items-center rounded-lg bg-white px-5 font-semibold text-marca-700 hover:bg-marca-50">
          Ver cartelera
        </Link>
      </section>
      <section aria-labelledby="proximos">
        <h2 id="proximos" className="mb-4 text-xl font-bold">Próximos eventos</h2>
        <ResultadoConsulta consulta={eventos}>
          {(lista) => (lista.length === 0 ? <EstadoVacio mensaje="No hay eventos publicados por ahora." /> : <ListaEventos eventos={lista.slice(0, 6)} />)}
        </ResultadoConsulta>
      </section>
      <section aria-labelledby="artistas">
        <h2 id="artistas" className="mb-4 text-xl font-bold">Artistas</h2>
        <ResultadoConsulta consulta={artistas}>
          {(lista) =>
            lista.length === 0 ? (
              <EstadoVacio mensaje="No hay artistas activos." />
            ) : (
              <ul className="flex flex-wrap gap-2">
                {lista.slice(0, 12).map((a) => (
                  <li key={a.id}>
                    <Link to={RUTAS.artista(a.id)} className="inline-block rounded-full bg-white px-4 py-2 text-sm shadow-sm ring-1 ring-slate-200 hover:ring-marca-500 dark:bg-slate-900 dark:ring-slate-800">
                      {a.stageName}
                    </Link>
                  </li>
                ))}
              </ul>
            )
          }
        </ResultadoConsulta>
      </section>
    </div>
  )
}
