import type { UseQueryResult } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { esApiError, mensajeDeError } from '../lib/http'
import { Cargando, EstadoError } from './Estados'

interface Props<T> {
  consulta: UseQueryResult<T>
  /** Contenido cuando la API responde 404 (o 403 si se indica). */
  noEncontrado?: ReactNode
  ocultar403?: boolean
  children: (datos: T) => ReactNode
}

/** Estados explícitos de cada pantalla: cargando, error y éxito (FE-014). */
export function ResultadoConsulta<T>({ consulta, noEncontrado, ocultar403 = false, children }: Props<T>) {
  if (consulta.isPending) return <Cargando />
  if (consulta.isError) {
    const e = consulta.error
    if (noEncontrado && esApiError(e) && (e.status === 404 || (ocultar403 && e.status === 403))) return <>{noEncontrado}</>
    return <EstadoError mensaje={mensajeDeError(e)} onReintentar={() => void consulta.refetch()} />
  }
  return <>{children(consulta.data)}</>
}
