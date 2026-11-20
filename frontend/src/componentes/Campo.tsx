import clsx from 'clsx'
import { useId, type ComponentProps } from 'react'

const BASE =
  'mt-1.5 block min-h-11 w-full rounded-xl border bg-white px-4 py-2.5 text-sm text-slate-900 shadow-sm transition focus-visible:border-marca-500 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-marca-500/20 dark:bg-white/5 dark:text-slate-100'

interface PropsCampo extends ComponentProps<'input'> {
  etiqueta: string
  error?: string
}

export function Campo({ etiqueta, error, className, id, ...resto }: PropsCampo) {
  const generado = useId()
  const idCampo = id ?? generado
  return (
    <div className={className}>
      <label htmlFor={idCampo} className="block text-sm font-semibold text-slate-700 dark:text-slate-200">
        {etiqueta}
      </label>
      <input
        id={idCampo}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${idCampo}-error` : undefined}
        className={clsx(BASE, error ? 'border-red-500' : 'border-slate-300 dark:border-white/15')}
        {...resto}
      />
      {error && (
        <p id={`${idCampo}-error`} className="mt-1 text-sm text-red-600 dark:text-red-400">
          {error}
        </p>
      )}
    </div>
  )
}

interface PropsSelector extends ComponentProps<'select'> {
  etiqueta: string
  error?: string
}

export function Selector({ etiqueta, error, className, id, children, ...resto }: PropsSelector) {
  const generado = useId()
  const idCampo = id ?? generado
  return (
    <div className={className}>
      <label htmlFor={idCampo} className="block text-sm font-semibold text-slate-700 dark:text-slate-200">
        {etiqueta}
      </label>
      <select
        id={idCampo}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${idCampo}-error` : undefined}
        className={clsx(BASE, error ? 'border-red-500' : 'border-slate-300 dark:border-white/15')}
        {...resto}
      >
        {children}
      </select>
      {error && (
        <p id={`${idCampo}-error`} className="mt-1 text-sm text-red-600 dark:text-red-400">
          {error}
        </p>
      )}
    </div>
  )
}
