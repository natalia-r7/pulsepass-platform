import clsx from 'clsx'
import type { ReactNode } from 'react'

export function Tarjeta({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className={clsx('rounded-xl bg-white p-5 shadow-sm ring-1 ring-slate-200 dark:bg-slate-900 dark:ring-slate-800', className)}>
      {children}
    </div>
  )
}
