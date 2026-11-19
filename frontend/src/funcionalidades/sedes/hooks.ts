import { useQuery } from '@tanstack/react-query'
import { sedesApi } from './api'

export function useSedesActivas() {
  return useQuery({ queryKey: ['sedes', 'activas'], queryFn: sedesApi.activas, staleTime: 300_000 })
}

export function useSede(code: string | undefined) {
  return useQuery({
    queryKey: ['sede', code],
    queryFn: () => sedesApi.porCodigo(code ?? ''),
    enabled: Boolean(code),
    staleTime: 300_000,
  })
}
