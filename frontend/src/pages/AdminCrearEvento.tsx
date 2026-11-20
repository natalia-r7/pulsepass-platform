import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router'
import { Aviso } from '../componentes/Aviso'
import { Boton } from '../componentes/Boton'
import { Campo, Selector } from '../componentes/Campo'
import { Tarjeta } from '../componentes/Tarjeta'
import { Titulo } from '../componentes/Titulo'
import { useCrearEvento } from '../funcionalidades/eventos/hooks'
import { recordarEvento } from '../funcionalidades/eventos/recientes'
import { CrearEventoSchema, ETIQUETA_CATEGORIA, EventCategorySchema, type CrearEventoForm } from '../funcionalidades/eventos/tipos'
import { useSedesActivas } from '../funcionalidades/sedes/hooks'
import { esApiError, mensajeDeError } from '../lib/http'
import { RUTAS } from '../lib/rutas'

const CAMPOS: (keyof CrearEventoForm)[] = ['eventCode', 'name', 'description', 'category', 'eventDate', 'minimumAge', 'venueCode']

export default function AdminCrearEvento() {
  const navigate = useNavigate()
  const crear = useCrearEvento()
  const sedes = useSedesActivas()
  const { register, handleSubmit, formState, setError } = useForm<CrearEventoForm>({
    resolver: zodResolver(CrearEventoSchema),
    defaultValues: { description: '', category: 'MUSIC', minimumAge: 0, venueCode: '' },
  })
  const e = formState.errors

  const enviar = handleSubmit((datos) => {
    crear.mutate(datos, {
      onSuccess: (evento) => {
        recordarEvento(evento.eventCode)
        void navigate(RUTAS.adminEvento(evento.eventCode))
      },
      onError: (err) => {
        if (esApiError(err) && err.status === 400) {
          for (const [clave, mensaje] of Object.entries(err.details)) {
            if ((CAMPOS as string[]).includes(clave)) setError(clave as keyof CrearEventoForm, { message: mensaje })
          }
        }
      },
    })
  })

  return (
    <div className="mx-auto max-w-2xl">
      <Titulo>Crear evento</Titulo>
      <Tarjeta>
        {crear.isError && <div className="mb-4"><Aviso tipo="error" mensaje={mensajeDeError(crear.error)} /></div>}
        <form noValidate onSubmit={(ev) => void enviar(ev)} className="grid gap-4 sm:grid-cols-2">
          <Campo etiqueta="Código del evento" {...register('eventCode')} error={e.eventCode?.message} />
          <Campo etiqueta="Nombre" {...register('name')} error={e.name?.message} />
          <Selector etiqueta="Categoría" {...register('category')} error={e.category?.message}>
            {EventCategorySchema.options.map((c) => (
              <option key={c} value={c}>{ETIQUETA_CATEGORIA[c]}</option>
            ))}
          </Selector>
          <Campo etiqueta="Fecha y hora" type="datetime-local" {...register('eventDate')} error={e.eventDate?.message} />
          <Campo etiqueta="Edad mínima" type="number" min={0} {...register('minimumAge', { valueAsNumber: true })} error={e.minimumAge?.message} />
          <Selector etiqueta="Sede" {...register('venueCode')} error={e.venueCode?.message ?? (sedes.isError ? 'No se pudieron cargar las sedes' : undefined)}>
            <option value="">{sedes.isPending ? 'Cargando sedes…' : 'Selecciona una sede'}</option>
            {sedes.data?.map((s) => (
              <option key={s.code} value={s.code}>{s.name} · {s.city} ({s.code})</option>
            ))}
          </Selector>
          <div className="sm:col-span-2">
            <label htmlFor="descripcion" className="block text-sm font-medium text-slate-700 dark:text-slate-300">Descripción (opcional)</label>
            <textarea
              id="descripcion"
              rows={4}
              {...register('description')}
              aria-invalid={e.description ? true : undefined}
              className="mt-1.5 block w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm shadow-sm focus-visible:border-marca-500 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-marca-500/20 dark:border-white/15 dark:bg-white/5"
            />
            {e.description && <p className="mt-1 text-sm text-red-600">{e.description.message}</p>}
          </div>
          <Boton type="submit" className="sm:col-span-2" cargando={crear.isPending}>Crear evento (borrador)</Boton>
        </form>
      </Tarjeta>
    </div>
  )
}
