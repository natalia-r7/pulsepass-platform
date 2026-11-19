import { Link } from 'react-router'
import { RUTAS } from '../lib/rutas'

export default function NoEncontrada() {
  return (
    <div className="py-16 text-center">
      <p className="text-6xl font-bold text-marca-600">404</p>
      <h1 className="mt-4 text-2xl font-bold">Página no encontrada</h1>
      <p className="mt-2 text-slate-600 dark:text-slate-400">El recurso que buscas no existe o no está disponible.</p>
      <Link to={RUTAS.inicio} className="mt-6 inline-block font-semibold text-marca-600 hover:underline">Volver al inicio</Link>
    </div>
  )
}
