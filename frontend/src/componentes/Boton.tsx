import clsx from 'clsx'
import type { ComponentProps } from 'react'

type Variante = 'primario' | 'secundario' | 'peligro' | 'fantasma'

const VARIANTES: Record<Variante, string> = {
  primario: 'bg-marca-600 text-white shadow-sm hover:bg-marca-700 dark:bg-marca-500 dark:hover:bg-marca-600',
  secundario:
    'border border-slate-300 bg-white text-slate-800 hover:border-marca-400 hover:bg-marca-50 dark:border-white/15 dark:bg-white/5 dark:text-slate-100 dark:hover:bg-white/10',
  peligro: 'bg-red-600 text-white shadow-sm hover:bg-red-700',
  fantasma: 'text-marca-600 hover:bg-marca-50 dark:text-marca-300 dark:hover:bg-white/10',
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
        'inline-flex min-h-11 items-center justify-center gap-2 rounded-full px-5 py-2 text-sm font-semibold transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-marca-600 disabled:cursor-not-allowed disabled:opacity-50 disabled:shadow-none',
        VARIANTES[variante],
        className,
      )}
    >
      {cargando ? 'Procesando…' : children}
    </button>
  )
}
