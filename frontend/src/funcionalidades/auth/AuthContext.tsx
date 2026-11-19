import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { configurarHttp } from '../../lib/http'
import { authApi } from './api'
import { SesionGuardadaSchema, type LoginForm, type Sesion } from './tipos'
import { AuthContext, type MotivoCierre, type ValorAuth } from './useAuth'

const CLAVE = 'pulsepass.sesion'


function leerGuardada(ahora: number) {
  try {
    const crudo = sessionStorage.getItem(CLAVE)
    if (!crudo) return null
    const r = SesionGuardadaSchema.safeParse(JSON.parse(crudo))
    if (!r.success || r.data.expiraEn <= ahora) {
      sessionStorage.removeItem(CLAVE)
      return null
    }
    return r.data
  } catch {
    return null
  }
}

interface PropsAuth {
  children: ReactNode
  /** Reloj inyectable para pruebas deterministas. */
  ahora?: () => number
}

export function AuthProvider({ children, ahora = Date.now }: PropsAuth) {
  const queryClient = useQueryClient()
  const [sesion, setSesion] = useState<Sesion>(() =>
    leerGuardada(ahora()) ? { estado: 'cargando' } : { estado: 'anonimo' },
  )
  const [motivoCierre, setMotivoCierre] = useState<MotivoCierre>(null)
  const tokenRef = useRef<string | null>(null)

  const cerrarSesion = useCallback(
    (motivo: MotivoCierre = 'manual') => {
      tokenRef.current = null
      try {
        sessionStorage.removeItem(CLAVE)
      } catch {
        /* sin almacenamiento disponible */
      }
      queryClient.clear()
      setMotivoCierre(motivo)
      setSesion({ estado: 'anonimo' })
    },
    [queryClient],
  )

  useEffect(() => {
    configurarHttp({
      obtenerToken: () => tokenRef.current,
      alRecibir401: () => {
        if (tokenRef.current) cerrarSesion('expirada')
      },
    })
  }, [cerrarSesion])

  // Restaurar la sesión guardada al recargar (FR-FE-AUTH-004).
  const restaurada = useRef(false)
  useEffect(() => {
    if (restaurada.current) return
    restaurada.current = true
    const guardada = leerGuardada(ahora())
    if (!guardada) return
    // Una sola restauración por montaje (el ref evita repetirla en StrictMode).
    authApi
      .me(guardada.token)
      .then((usuario) => {
        tokenRef.current = guardada.token
        setSesion({ estado: 'autenticado', token: guardada.token, expiraEn: guardada.expiraEn, usuario })
      })
      .catch(() => cerrarSesion('expirada'))
  }, [ahora, cerrarSesion])

  // Cierre local programado al vencer el token (PRD §12.2).
  useEffect(() => {
    if (sesion.estado !== 'autenticado') return
    const restante = sesion.expiraEn - ahora()
    const id = window.setTimeout(() => cerrarSesion('expirada'), Math.max(restante, 0))
    return () => window.clearTimeout(id)
  }, [sesion, ahora, cerrarSesion])

  const iniciarSesion = useCallback(
    async (datos: LoginForm) => {
      const auth = await authApi.login(datos)
      const usuario = await authApi.me(auth.accessToken)
      const expiraEn = ahora() + auth.expiresIn * 1000
      tokenRef.current = auth.accessToken
      try {
        sessionStorage.setItem(CLAVE, JSON.stringify({ token: auth.accessToken, expiraEn }))
      } catch {
        /* la sesión sigue en memoria */
      }
      setMotivoCierre(null)
      setSesion({ estado: 'autenticado', token: auth.accessToken, expiraEn, usuario })
    },
    [ahora],
  )

  const valor = useMemo<ValorAuth>(
    () => ({
      sesion,
      motivoCierre,
      iniciarSesion,
      cerrarSesion,
      tieneRol: (...roles) => sesion.estado === 'autenticado' && roles.some((r) => sesion.usuario.roles.includes(r)),
    }),
    [sesion, motivoCierre, iniciarSesion, cerrarSesion],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

