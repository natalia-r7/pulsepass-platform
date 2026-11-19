import clsx from 'clsx'
import { useId, type ComponentProps } from 'react'

const BASE =
  'mt-1 block w-full rounded-lg border bg-white px-3 py-2 text-sm text-slate-900 focus-visible:outline-2 focus-visible:outline-marca-600 dark:bg-slate-900 dark:text-slate-100'

interface PropsCampo extends ComponentProps<'input'> {
  etiqueta: string
  error?: string
}

export function Campo({ etiqueta, error, className, id, ...resto }: PropsCampo) {
  const generado = useId()
  const idCampo = id ?? generado
  return (
    <div className={className}>
      <label htmlFor={idCampo} className="block text-sm font-medium text-slate-700 dark:text-slate-300">
        {etiqueta}
      </label>
      <input
        id={idCampo}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${idCampo}-error` : undefined}
        className={clsx(BASE, error ? 'border-red-500' : 'border-slate-300 dark:border-slate-700')}
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
      <label htmlFor={idCampo} className="block text-sm font-medium text-slate-700 dark:text-slate-300">
        {etiqueta}
      </label>
      <select
        id={idCampo}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${idCampo}-error` : undefined}
        className={clsx(BASE, error ? 'border-red-500' : 'border-slate-300 dark:border-slate-700')}
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
