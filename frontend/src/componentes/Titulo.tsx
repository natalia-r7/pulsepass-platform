import type { ReactNode } from 'react'

export function Titulo({ children, accion }: { children: ReactNode; accion?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
      <h1 className="text-3xl font-extrabold sm:text-4xl">{children}</h1>
      {accion}
    </div>
  )
}
