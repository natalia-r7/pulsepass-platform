import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { z } from 'zod'
import { Boton } from '../componentes/Boton'
import { Campo } from '../componentes/Campo'
import { EstadoVacio } from '../componentes/Estados'
import { Tarjeta } from '../componentes/Tarjeta'
import { Titulo } from '../componentes/Titulo'
import { leerRecientes } from '../funcionalidades/eventos/recientes'
import { RUTAS } from '../lib/rutas'

const BusquedaSchema = z.object({ codigo: z.string().trim().min(1, 'Ingresa el código del evento') })

export default function AdminEventos() {
  const navigate = useNavigate()
  const recientes = leerRecientes()
  const { register, handleSubmit, formState } = useForm<z.infer<typeof BusquedaSchema>>({ resolver: zodResolver(BusquedaSchema) })
  return (
    <div className="space-y-6">
      <Titulo accion={<Link to={RUTAS.adminCrear} className="inline-flex min-h-11 items-center rounded-lg bg-marca-600 px-4 font-semibold text-white hover:bg-marca-700">Crear evento</Link>}>
        Administración de eventos
      </Titulo>
      <Tarjeta>
        <p className="mb-3 text-sm text-slate-600 dark:text-slate-400">
          Busca un evento por su código para gestionarlo, incluidos los borradores.
        </p>
        <form noValidate className="flex flex-wrap items-end gap-3" onSubmit={(e) => void handleSubmit((d) => navigate(RUTAS.adminEvento(d.codigo)))(e)}>
          <Campo className="min-w-60 flex-1" etiqueta="Código del evento" {...register('codigo')} error={formState.errors.codigo?.message} />
          <Boton type="submit">Buscar</Boton>
        </form>
      </Tarjeta>
      <section>
        <h2 className="mb-3 text-lg font-semibold">Consultados en esta sesión</h2>
        {recientes.length === 0 ? (
          <EstadoVacio mensaje="Aún no has creado ni consultado eventos en esta sesión." />
        ) : (
          <ul className="flex flex-wrap gap-2">
            {recientes.map((c) => (
              <li key={c}>
                <Link to={RUTAS.adminEvento(c)} className="inline-block rounded-full bg-white px-4 py-2 text-sm ring-1 ring-slate-200 hover:ring-marca-500 dark:bg-slate-900 dark:ring-slate-800">{c}</Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
