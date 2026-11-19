import clsx from 'clsx'
import type { ComponentProps } from 'react'

type Variante = 'primario' | 'secundario' | 'peligro' | 'fantasma'

const VARIANTES: Record<Variante, string> = {
  primario: 'bg-marca-600 text-white hover:bg-marca-700',
  secundario:
    'border border-slate-300 bg-white text-slate-800 hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:hover:bg-slate-800',
  peligro: 'bg-red-600 text-white hover:bg-red-700',
  fantasma: 'text-marca-600 hover:bg-marca-50 dark:text-marca-500 dark:hover:bg-slate-800',
}

interface PropsBoton extends ComponentProps<'button'> {
  variante?: Variante
  cargando?: boolean
}

export function Boton({ variante = 'primario', cargando = false, className, disabled, children, ...resto }: PropsBoton) {
  return (
    <button
      type="button"
      {...resto}
      disabled={disabled || cargando}
      aria-busy={cargando || undefined}
      className={clsx(
        'inline-flex min-h-11 items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-semibold transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-marca-600 disabled:cursor-not-allowed disabled:opacity-50',
        VARIANTES[variante],
        className,
      )}
    >
      {cargando ? 'Procesando…' : children}
    </button>
  )
}
