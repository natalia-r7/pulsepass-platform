import { Navigate, Outlet, useLocation } from 'react-router'
import { z } from 'zod'
import { Cargando } from '../componentes/Estados'
import { useAuth } from '../funcionalidades/auth/useAuth'
import type { Rol } from '../funcionalidades/auth/tipos'
import { RUTAS } from '../lib/rutas'

/** Solo experiencia de usuario: la seguridad real la aplica el servidor (FE-004). */
export function RutaProtegida() {
  const { sesion } = useAuth()
  const location = useLocation()
  if (sesion.estado === 'cargando') return <Cargando />
  if (sesion.estado === 'anonimo') return <Navigate to={RUTAS.login} replace state={{ desde: location.pathname }} />
  return <Outlet />
}

export function RutaPorRol({ roles }: { roles: Rol[] }) {
  const { sesion } = useAuth()
  const location = useLocation()
  if (sesion.estado === 'cargando') return <Cargando />
  if (sesion.estado === 'anonimo') return <Navigate to={RUTAS.login} replace state={{ desde: location.pathname }} />
  if (!roles.some((r) => sesion.usuario.roles.includes(r))) return <Navigate to={RUTAS.sinPermisos} replace />
  return <Outlet />
}

const DesdeSchema = z.object({ desde: z.string().startsWith('/') })

/** Con sesión, /login y /registro llevan a la ruta de origen (state.desde) o al inicio. */
export function SoloAnonimo() {
  const { sesion } = useAuth()
  const location = useLocation()
  if (sesion.estado === 'cargando') return <Cargando />
  if (sesion.estado === 'autenticado') {
    const desde = DesdeSchema.safeParse(location.state).data?.desde
    return <Navigate to={desde ?? RUTAS.inicio} replace />
  }
  return <Outlet />
}
