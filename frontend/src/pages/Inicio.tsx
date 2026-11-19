import { Link } from 'react-router'
import { EstadoVacio } from '../componentes/Estados'
import { ResultadoConsulta } from '../componentes/ResultadoConsulta'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { ListaEventos } from '../funcionalidades/eventos/componentes/TarjetaEvento'
import { useEventosPublicados } from '../funcionalidades/eventos/hooks'
import { ETIQUETA_CATEGORIA, EventCategorySchema } from '../funcionalidades/eventos/tipos'
import { RUTAS } from '../lib/rutas'

export default function Inicio() {
  const eventos = useEventosPublicados()
  const artistas = useArtistasActivos()
  return (
    <div className="space-y-14">
      <section className="fondo-degradado overflow-hidden rounded-3xl px-6 py-12 text-white sm:px-12 sm:py-16">
        <div className="max-w-2xl">
          <h1 className="text-4xl font-extrabold leading-tight sm:text-5xl">Encuentra tu próximo evento</h1>
          <p className="mt-4 max-w-xl text-base text-white/85 sm:text-lg">
            Conciertos, festivales, deporte y cultura. Compra tus entradas y consúltalas desde tu cuenta.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link
              to={RUTAS.eventos}
              className="inline-flex min-h-12 items-center rounded-full bg-white px-6 font-semibold text-marca-700 transition hover:bg-marca-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
            >
              Ver eventos
            </Link>
            <Link
              to={RUTAS.artistas}
              className="inline-flex min-h-12 items-center rounded-full px-6 font-semibold text-white ring-1 ring-white/50 transition hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
            >
              Artistas
            </Link>
          </div>
        </div>
      </section>

      <section aria-labelledby="categorias">
        <h2 id="categorias" className="mb-4 text-2xl font-bold">Categorías</h2>
        <ul className="flex flex-wrap gap-2">
          {EventCategorySchema.options.map((c) => (
            <li key={c}>
              <Link
                to={`${RUTAS.eventos}?categoria=${c}`}
                className="inline-flex min-h-11 items-center rounded-full bg-white px-5 text-sm font-semibold text-slate-700 ring-1 ring-slate-200 transition hover:bg-marca-50 hover:text-marca-700 hover:ring-marca-300 focus-visible:outline-2 focus-visible:outline-marca-600 dark:bg-white/5 dark:text-slate-200 dark:ring-white/10 dark:hover:bg-white/10"
              >
                {ETIQUETA_CATEGORIA[c]}
              </Link>
            </li>
          ))}
        </ul>
      </section>

      <section aria-labelledby="proximos">
        <div className="mb-4 flex items-end justify-between gap-3">
          <h2 id="proximos" className="text-2xl font-bold">Próximos eventos</h2>
          <Link to={RUTAS.eventos} className="text-sm font-semibold text-marca-600 hover:underline dark:text-marca-300">
            Ver cartelera
          </Link>
        </div>
        <ResultadoConsulta consulta={eventos}>
          {(lista) => (lista.length === 0 ? <EstadoVacio mensaje="No hay eventos publicados por ahora." /> : <ListaEventos eventos={lista.slice(0, 6)} />)}
        </ResultadoConsulta>
      </section>

      <section aria-labelledby="artistas">
        <h2 id="artistas" className="mb-4 text-2xl font-bold">Artistas</h2>
        <ResultadoConsulta consulta={artistas}>
          {(lista) =>
            lista.length === 0 ? (
              <EstadoVacio mensaje="No hay artistas activos." />
            ) : (
              <ul className="flex flex-wrap gap-3">
                {lista.slice(0, 12).map((a) => (
                  <li key={a.id}>
                    <Link
                      to={RUTAS.artista(a.id)}
                      className="inline-flex min-h-11 items-center gap-2 rounded-full bg-white py-1.5 pl-1.5 pr-4 text-sm font-semibold ring-1 ring-slate-200 transition hover:ring-marca-400 dark:bg-white/5 dark:ring-white/10"
                    >
                      <span aria-hidden="true" className="flex h-8 w-8 items-center justify-center rounded-full bg-marca-100 text-xs font-bold text-marca-700 dark:bg-marca-500/20 dark:text-marca-300">
                        {a.stageName.charAt(0)}
                      </span>
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
