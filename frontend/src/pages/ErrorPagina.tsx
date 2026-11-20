import { Link, isRouteErrorResponse, useRouteError } from 'react-router'
import { RUTAS } from '../lib/rutas'

/** errorElement: mensaje seguro, sin stack (TEST-FE-GEN-002). */
export default function ErrorPagina() {
  const error = useRouteError()
  const es404 = isRouteErrorResponse(error) && error.status === 404
  return (
    <main className="mx-auto max-w-xl px-4 py-16 text-center">
      <h1 className="text-2xl font-bold">{es404 ? 'Página no encontrada' : 'Algo salió mal'}</h1>
      <p className="mt-2 text-slate-600 dark:text-slate-400">
        {es404 ? 'El recurso que buscas no existe.' : 'Ocurrió un error inesperado al cargar la página. Inténtalo más tarde.'}
      </p>
      <Link to={RUTAS.inicio} className="mt-6 inline-block font-semibold text-marca-600 hover:underline">Volver al inicio</Link>
    </main>
  )
}
