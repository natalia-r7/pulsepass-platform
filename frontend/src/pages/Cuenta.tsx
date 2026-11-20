import { Navigate } from 'react-router'
import { Icono } from '../componentes/Icono'
import { ResultadoConsulta } from '../componentes/ResultadoConsulta'
import { Tarjeta } from '../componentes/Tarjeta'
import { Titulo } from '../componentes/Titulo'
import { useAuth } from '../funcionalidades/auth/useAuth'
import { useUsuarioPorEmail, useUsuarioPorUsername } from '../funcionalidades/usuarios/hooks'
import { RUTAS } from '../lib/rutas'

export default function Cuenta() {
  const { sesion } = useAuth()
  const usuario = sesion.estado === 'autenticado' ? sesion.usuario : null
  const porEmail = useUsuarioPorEmail(usuario?.email)
  const porUsername = useUsuarioPorUsername(usuario?.username)
  if (!usuario) return <Navigate to={RUTAS.login} replace />
  return (
    <div className="space-y-6">
      <Titulo>Mi cuenta</Titulo>
      <Tarjeta>
        <h2 className="font-semibold">Datos de acceso</h2>
        <dl className="mt-3 grid gap-3 text-sm sm:grid-cols-3">
          <div><dt className="text-slate-500">Usuario</dt><dd className="font-semibold">{usuario.username}</dd></div>
          <div><dt className="text-slate-500">Correo</dt><dd className="font-semibold break-all">{usuario.email}</dd></div>
          <div><dt className="text-slate-500">Roles</dt><dd className="font-semibold">{usuario.roles.join(', ')}</dd></div>
        </dl>
      </Tarjeta>
      <Tarjeta>
        <h2 className="font-semibold">Perfil</h2>
        <ResultadoConsulta consulta={porEmail}>
          {(u) => (
            <dl className="mt-3 grid gap-3 text-sm sm:grid-cols-3">
              <div><dt className="text-slate-500">Nombre</dt><dd className="font-semibold">{u.firstName} {u.lastName}</dd></div>
              <div><dt className="text-slate-500">Teléfono</dt><dd className="font-semibold">{u.phone ?? 'No informado'}</dd></div>
              <div><dt className="text-slate-500">Ciudad</dt><dd className="font-semibold">{u.city ?? 'No informada'}</dd></div>
              <div><dt className="text-slate-500">Estado</dt><dd className="font-semibold">{u.active ? 'Activa' : 'Inactiva'}</dd></div>
            </dl>
          )}
        </ResultadoConsulta>
        {porUsername.isSuccess && (
          <p className="mt-3 flex items-center gap-1.5 text-xs font-semibold text-emerald-700 dark:text-emerald-300">
            <Icono nombre="check" className="h-4 w-4" />
            Cuenta verificada
          </p>
        )}
      </Tarjeta>
    </div>
  )
}
