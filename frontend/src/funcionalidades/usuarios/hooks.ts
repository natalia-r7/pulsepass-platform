import { useMutation, useQuery } from '@tanstack/react-query'
import { usuariosApi } from './api'
import type { RegistroForm } from './tipos'

export function useRegistrarUsuario() {
  return useMutation({ mutationFn: (datos: RegistroForm) => usuariosApi.registrar(datos) })
}

export function useUsuarioPorEmail(email: string | undefined) {
  return useQuery({
    queryKey: ['usuario', email],
    queryFn: () => usuariosApi.porEmail(email ?? ''),
    enabled: Boolean(email),
    staleTime: 60_000,
  })
}

export function useUsuarioPorUsername(username: string | undefined) {
  return useQuery({
    queryKey: ['usuario', 'username', username],
    queryFn: () => usuariosApi.porUsername(username ?? ''),
    enabled: Boolean(username),
    staleTime: 60_000,
  })
}
