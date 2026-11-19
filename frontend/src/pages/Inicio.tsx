import { Link } from 'react-router'
import { EstadoVacio } from '../componentes/Estados'
import { ResultadoConsulta } from '../componentes/ResultadoConsulta'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { ListaEventos } from '../funcionalidades/eventos/componentes/TarjetaEvento'
import { useEventosPublicados } from '../funcionalidades/eventos/hooks'
import { ETIQUETA_CATEGORIA, EventCategorySchema, type EventCategory } from '../funcionalidades/eventos/tipos'
import { RUTAS } from '../lib/rutas'

const ICONO_CATEGORIA: Record<EventCategory, string> = {
  MUSIC: '🎵',
  SPORTS: '🏟️',
  TECHNOLOGY: '💡',
  EDUCATION: '🎓',
  CULTURE: '🎭',
  ENTERTAINMENT: '🎉',
}

/** Barras decorativas del ecualizador (alturas fijas: sin clases dinámicas). */
const BARRAS = ['h-10', 'h-16', 'h-24', 'h-14', 'h-28', 'h-20', 'h-12', 'h-24', 'h-16', 'h-8']

export default function Inicio() {
  const eventos = useEventosPublicados()
  const artistas = useArtistasActivos()
  return (
    <div className="space-y-14">
      <section className="fondo-degradado relative overflow-hidden rounded-3xl px-6 py-14 text-white shadow-2xl shadow-marca-700/30 sm:px-12 sm:py-20">
        <div aria-hidden="true" className="pointer-events-none absolute -right-24 -top-24 h-72 w-72 rounded-full bg-white/15 blur-3xl" />
        <div aria-hidden="true" className="pointer-events-none absolute -bottom-32 left-1/3 h-72 w-72 rounded-full bg-acento-400/30 blur-3xl" />
        <div aria-hidden="true" className="pointer-events-none absolute bottom-0 right-6 hidden items-end gap-2 opacity-40 sm:flex">
          {BARRAS.map((alto, i) => (
            <span key={i} className={`w-3 rounded-t-full bg-white ${alto}`} />
          ))}
        </div>
        <div className="relative max-w-2xl">
          <p className="inline-flex items-center gap-2 rounded-full bg-white/15 px-4 py-1.5 text-xs font-semibold uppercase tracking-widest ring-1 ring-white/25">
            <span aria-hidden="true">●</span> En vivo · Temporada 2026
          </p>
          <h1 className="mt-5 text-4xl font-extrabold leading-tight sm:text-6xl">
            Siente la música.
            <br />
            Vive el momento.
          </h1>
          <p className="mt-4 max-w-xl text-base text-white/85 sm:text-lg">
            Conciertos, festivales, deporte y cultura. Encuentra tu próximo plan, compra tu entrada en segundos y
            llévala siempre contigo.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link
              to={RUTAS.eventos}
              className="inline-flex min-h-12 items-center rounded-full bg-white px-6 font-semibold text-marca-700 shadow-lg transition hover:bg-marca-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
            >
              Explorar eventos
            </Link>
            <Link
              to={RUTAS.artistas}
              className="inline-flex min-h-12 items-center rounded-full px-6 font-semibold text-white ring-1 ring-white/50 transition hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
            >
              Ver artistas
            </Link>
          </div>
        </div>
      </section>

      <section aria-labelledby="categorias">
        <h2 id="categorias" className="mb-4 text-2xl font-bold">Explora por categoría</h2>
        <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
          {EventCategorySchema.options.map((c) => (
            <li key={c}>
              <Link
                to={`${RUTAS.eventos}?categoria=${c}`}
                className="flex min-h-24 flex-col items-center justify-center gap-2 rounded-2xl bg-white/90 p-4 text-sm font-semibold shadow-sm ring-1 ring-slate-200/80 transition hover:-translate-y-0.5 hover:shadow-lg hover:ring-marca-400 focus-visible:outline-2 focus-visible:outline-marca-600 dark:bg-white/5 dark:ring-white/10"
              >
                <span aria-hidden="true" className="text-3xl">{ICONO_CATEGORIA[c]}</span>
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
            Ver toda la cartelera →
          </Link>
        </div>
        <ResultadoConsulta consulta={eventos}>
          {(lista) => (lista.length === 0 ? <EstadoVacio mensaje="No hay eventos publicados por ahora." /> : <ListaEventos eventos={lista.slice(0, 6)} />)}
        </ResultadoConsulta>
      </section>

      <section aria-labelledby="artistas">
        <h2 id="artistas" className="mb-4 text-2xl font-bold">Artistas en cartelera</h2>
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
                      className="inline-flex min-h-11 items-center gap-2 rounded-full bg-white/90 py-1.5 pl-1.5 pr-4 text-sm font-semibold shadow-sm ring-1 ring-slate-200/80 transition hover:ring-marca-400 dark:bg-white/5 dark:ring-white/10"
                    >
                      <span aria-hidden="true" className="fondo-degradado flex h-8 w-8 items-center justify-center rounded-full text-xs font-bold text-white">
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
