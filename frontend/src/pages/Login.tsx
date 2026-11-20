import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router'
import { z } from 'zod'
import { Aviso } from '../componentes/Aviso'
import { Boton } from '../componentes/Boton'
import { Campo } from '../componentes/Campo'
import { Tarjeta } from '../componentes/Tarjeta'
import { useAuth } from '../funcionalidades/auth/useAuth'
import { LoginSchema, type LoginForm } from '../funcionalidades/auth/tipos'
import { esApiError, mensajeDeError } from '../lib/http'
import { RUTAS } from '../lib/rutas'

const EstadoNavegacion = z.object({ desde: z.string().startsWith('/').optional(), registrado: z.boolean().optional() })

export default function Login() {
  const { iniciarSesion, motivoCierre } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const estado = EstadoNavegacion.safeParse(location.state).data ?? {}
  const [error, setError] = useState<string | null>(null)
  const { register, handleSubmit, formState } = useForm<LoginForm>({ resolver: zodResolver(LoginSchema) })

  const enviar = handleSubmit(async (datos) => {
    setError(null)
    try {
      await iniciarSesion(datos)
      void navigate(estado.desde ?? RUTAS.inicio, { replace: true })
    } catch (e) {
      setError(esApiError(e) && e.status === 401 ? 'Credenciales inválidas.' : mensajeDeError(e))
    }
  })

  return (
    <div className="mx-auto max-w-md">
      <Tarjeta>
        <h1 className="text-2xl font-bold">Iniciar sesión</h1>
        <div className="mt-4 space-y-3">
          {estado.registrado && <Aviso tipo="exito" mensaje="Cuenta creada. Ya puedes iniciar sesión." />}
          {motivoCierre === 'expirada' && <Aviso tipo="info" mensaje="Tu sesión expiró. Inicia sesión de nuevo." />}
          {error && <Aviso tipo="error" mensaje={error} />}
        </div>
        <form noValidate onSubmit={(e) => void enviar(e)} className="mt-4 space-y-4">
          <Campo etiqueta="Usuario o correo" autoComplete="username" {...register('identifier')} error={formState.errors.identifier?.message} />
          <Campo etiqueta="Contraseña" type="password" autoComplete="current-password" {...register('password')} error={formState.errors.password?.message} />
          <Boton type="submit" className="w-full" cargando={formState.isSubmitting}>Ingresar</Boton>
        </form>
        <p className="mt-4 text-sm">
          ¿No tienes cuenta? <Link to={RUTAS.registro} className="font-semibold text-marca-600 hover:underline">Regístrate</Link>
        </p>
      </Tarjeta>
    </div>
  )
}
