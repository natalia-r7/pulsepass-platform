import type { ReactNode } from 'react'

export interface Columna<T> {
  encabezado: string
  celda: (fila: T) => ReactNode
}

interface PropsTabla<T> {
  columnas: Columna<T>[]
  filas: T[]
  clave: (fila: T) => string
  titulo: string
}

export function Tabla<T>({ columnas, filas, clave, titulo }: PropsTabla<T>) {
  return (
    <div className="overflow-x-auto rounded-xl ring-1 ring-slate-200 dark:ring-slate-800">
      <table className="min-w-full divide-y divide-slate-200 bg-white text-sm dark:divide-slate-800 dark:bg-slate-900">
        <caption className="sr-only">{titulo}</caption>
        <thead className="bg-slate-100 dark:bg-slate-800">
          <tr>
            {columnas.map((c) => (
              <th key={c.encabezado} scope="col" className="px-4 py-3 text-left font-semibold">
                {c.encabezado}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
          {filas.map((fila) => (
            <tr key={clave(fila)}>
              {columnas.map((c) => (
                <td key={c.encabezado} className="px-4 py-3">
                  {c.celda(fila)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
