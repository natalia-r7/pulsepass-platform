import clsx from 'clsx'

type TipoAviso = 'exito' | 'error' | 'info'

const TONO: Record<TipoAviso, string> = {
  exito: 'border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950 dark:text-emerald-200',
  error: 'border-red-200 bg-red-50 text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-200',
  info: 'border-sky-200 bg-sky-50 text-sky-800 dark:border-sky-900 dark:bg-sky-950 dark:text-sky-200',
}

/** Mensaje en línea; el texto del backend se renderiza como texto, nunca como HTML. */
export function Aviso({ tipo, mensaje }: { tipo: TipoAviso; mensaje: string }) {
  return (
    <div
      role={tipo === 'error' ? 'alert' : 'status'}
      aria-live={tipo === 'error' ? 'assertive' : 'polite'}
      className={clsx('rounded-xl border px-4 py-3 text-sm font-medium', TONO[tipo])}
    >
      {mensaje}
    </div>
  )
}
