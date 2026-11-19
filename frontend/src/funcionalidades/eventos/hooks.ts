import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventosApi } from './api'
import type { CrearEventoForm } from './tipos'

export function useEventosPublicados() {
  return useQuery({ queryKey: ['eventos', 'publicados'], queryFn: eventosApi.publicados, staleTime: 60_000 })
}

export function useEvento(eventCode: string | undefined) {
  return useQuery({
    queryKey: ['evento', eventCode],
    queryFn: () => eventosApi.porCodigo(eventCode ?? ''),
    enabled: Boolean(eventCode),
    staleTime: 30_000,
  })
}

export function useEventosPorArtista(stageName: string | undefined) {
  return useQuery({
    queryKey: ['eventos', 'artista', stageName],
    queryFn: () => eventosApi.porArtista(stageName ?? ''),
    enabled: Boolean(stageName),
    staleTime: 60_000,
  })
}

export function useCrearEvento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (datos: CrearEventoForm) => eventosApi.crear(datos),
    onSuccess: (evento) => qc.setQueryData(['evento', evento.eventCode], evento),
  })
}

export function usePublicarEvento(eventCode: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => eventosApi.publicar(eventCode),
    onSuccess: (evento) => {
      qc.setQueryData(['evento', eventCode], evento)
      void qc.invalidateQueries({ queryKey: ['eventos', 'publicados'] })
    },
  })
}

export function useAsociarArtista(eventCode: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (artista: { id: number; stageName: string }) => eventosApi.asociarArtista(eventCode, artista.id),
    onSuccess: (evento, artista) => {
      qc.setQueryData(['evento', eventCode], evento)
      void qc.invalidateQueries({ queryKey: ['eventos', 'artista', artista.stageName] })
    },
  })
}
