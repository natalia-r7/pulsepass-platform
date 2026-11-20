import { useParams } from 'react-router'
import { ResultadoConsulta } from '../componentes/ResultadoConsulta'
import { Tarjeta } from '../componentes/Tarjeta'
import { useSede } from '../funcionalidades/sedes/hooks'
import NoEncontrada from './NoEncontrada'

export default function DetalleSede() {
  const { code = '' } = useParams()
  const sede = useSede(code)
  return (
    <ResultadoConsulta consulta={sede} noEncontrado={<NoEncontrada />}>
      {(s) => (
        <Tarjeta>
          <h1 className="text-3xl font-bold">{s.name}</h1>
          <dl className="mt-4 grid gap-4 text-sm sm:grid-cols-2">
            <div><dt className="text-slate-500">Código</dt><dd className="font-semibold">{s.code}</dd></div>
            <div><dt className="text-slate-500">Ciudad</dt><dd className="font-semibold">{s.city}</dd></div>
            <div><dt className="text-slate-500">Dirección</dt><dd className="font-semibold">{s.address ?? 'No informada'}</dd></div>
            <div><dt className="text-slate-500">Capacidad</dt><dd className="font-semibold">{s.capacity}</dd></div>
          </dl>
        </Tarjeta>
      )}
    </ResultadoConsulta>
  )
}
