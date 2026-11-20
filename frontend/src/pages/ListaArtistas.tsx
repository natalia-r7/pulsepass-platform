import { Link } from 'react-router'
import { EstadoVacio } from '../componentes/Estados'
import { ResultadoConsulta } from '../componentes/ResultadoConsulta'
import { Titulo } from '../componentes/Titulo'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { RUTAS } from '../lib/rutas'

export default function ListaArtistas() {
  const artistas = useArtistasActivos()
  return (
    <div>
      <Titulo>Artistas</Titulo>
      <ResultadoConsulta consulta={artistas}>
        {(lista) =>
          lista.length === 0 ? (
            <EstadoVacio mensaje="No hay artistas activos." />
          ) : (
            <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {lista.map((a) => (
                <li key={a.id} className="rounded-xl bg-white p-5 shadow-sm ring-1 ring-slate-200 dark:bg-slate-900 dark:ring-slate-800">
                  <div aria-hidden="true" className="mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-marca-50 text-lg font-bold text-marca-700">
                    {a.stageName.charAt(0)}
                  </div>
                  <h2 className="font-semibold">
                    <Link to={RUTAS.artista(a.id)} className="hover:underline">{a.stageName}</Link>
                  </h2>
                  <p className="text-sm text-slate-600 dark:text-slate-400">{[a.genre, a.country].filter(Boolean).join(' · ') || 'Sin datos adicionales'}</p>
                </li>
              ))}
            </ul>
          )
        }
      </ResultadoConsulta>
    </div>
  )
}
