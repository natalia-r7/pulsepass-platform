import { Link } from 'react-router'
import { EstadoVacio } from '../componentes/Estados'
import { ResultadoConsulta } from '../componentes/ResultadoConsulta'
import { Titulo } from '../componentes/Titulo'
import { useSedesActivas } from '../funcionalidades/sedes/hooks'
import { RUTAS } from '../lib/rutas'

export default function ListaSedes() {
  const sedes = useSedesActivas()
  return (
    <div>
      <Titulo>Sedes</Titulo>
      <ResultadoConsulta consulta={sedes}>
        {(lista) =>
          lista.length === 0 ? (
            <EstadoVacio mensaje="No hay sedes activas." />
          ) : (
            <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {lista.map((s) => (
                <li key={s.code} className="rounded-xl bg-white p-5 shadow-sm ring-1 ring-slate-200 dark:bg-slate-900 dark:ring-slate-800">
                  <h2 className="font-semibold">
                    <Link to={RUTAS.sede(s.code)} className="hover:underline">{s.name}</Link>
                  </h2>
                  <p className="text-sm text-slate-600 dark:text-slate-400">{s.city} · Capacidad {s.capacity}</p>
                </li>
              ))}
            </ul>
          )
        }
      </ResultadoConsulta>
    </div>
  )
}
