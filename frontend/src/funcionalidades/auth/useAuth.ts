import { createContext, useContext } from 'react'
import type { LoginForm, Rol, Sesion } from './tipos'

export type MotivoCierre = 'expirada' | 'manual' | null

export interface ValorAuth {
  sesion: Sesion
  motivoCierre: MotivoCierre
  iniciarSesion: (datos: LoginForm) => Promise<void>
  cerrarSesion: (motivo?: MotivoCierre) => void
  tieneRol: (...roles: Rol[]) => boolean
}

export const AuthContext = createContext<ValorAuth | null>(null)

export function useAuth(): ValorAuth {
  const valor = useContext(AuthContext)
  if (!valor) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return valor
}
