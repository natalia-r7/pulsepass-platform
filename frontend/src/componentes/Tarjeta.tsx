import clsx from 'clsx'
import type { ReactNode } from 'react'

export function Tarjeta({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className={clsx('rounded-2xl bg-white/90 p-6 shadow-sm ring-1 ring-slate-200/80 backdrop-blur dark:bg-white/5 dark:ring-white/10', className)}>
      {children}
    </div>
  )
}
