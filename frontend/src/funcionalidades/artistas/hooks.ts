import { useQuery } from '@tanstack/react-query'
import { artistasApi } from './api'

export function useArtistasActivos() {
  return useQuery({ queryKey: ['artistas', 'activos'], queryFn: artistasApi.activos, staleTime: 300_000 })
}

export function useArtista(id: number | null) {
  return useQuery({
    queryKey: ['artista', id],
    queryFn: () => artistasApi.porId(id ?? 0),
    enabled: id !== null,
    staleTime: 300_000,
  })
}

export function useArtistaPorNombre(stageName: string) {
  return useQuery({
    queryKey: ['artista', 'nombre', stageName],
    queryFn: () => artistasApi.porNombre(stageName),
    enabled: stageName.trim().length > 0,
    staleTime: 300_000,
  })
}
