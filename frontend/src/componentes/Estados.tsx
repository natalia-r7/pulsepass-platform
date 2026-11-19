import { Boton } from './Boton'

export function Cargando({ mensaje = 'Cargando…' }: { mensaje?: string }) {
  return (
    <div aria-busy="true" role="status" className="space-y-3">
      <span className="sr-only">{mensaje}</span>
      <div className="h-6 w-1/3 animate-pulse rounded bg-slate-200 dark:bg-slate-800" />
      <div className="h-24 animate-pulse rounded-xl bg-slate-200 dark:bg-slate-800" />
      <div className="h-24 animate-pulse rounded-xl bg-slate-200 dark:bg-slate-800" />
    </div>
  )
}

export function EstadoVacio({ mensaje }: { mensaje: string }) {
  return (
    <div className="rounded-xl border border-dashed border-slate-300 p-8 text-center text-slate-600 dark:border-slate-700 dark:text-slate-400">
      <p aria-hidden="true" className="text-3xl">∅</p>
      <p className="mt-2">{mensaje}</p>
    </div>
  )
}

export function EstadoError({ mensaje, onReintentar }: { mensaje: string; onReintentar?: () => void }) {
  return (
    <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-6 text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-200">
      <p>{mensaje}</p>
      {onReintentar && (
        <Boton variante="secundario" className="mt-3" onClick={onReintentar}>
          Reintentar
        </Boton>
      )}
    </div>
  )
}
