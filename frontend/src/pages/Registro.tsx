import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { Aviso } from '../componentes/Aviso'
import { Boton } from '../componentes/Boton'
import { Campo } from '../componentes/Campo'
import { Tarjeta } from '../componentes/Tarjeta'
import { useRegistrarUsuario } from '../funcionalidades/usuarios/hooks'
import { RegistroSchema, type RegistroForm } from '../funcionalidades/usuarios/tipos'
import { esApiError, mensajeDeError } from '../lib/http'
import { RUTAS } from '../lib/rutas'

const CAMPOS: (keyof RegistroForm)[] = ['username', 'email', 'firstName', 'lastName', 'phone', 'city', 'birthDate', 'password']

export default function Registro() {
  const navigate = useNavigate()
  const registro = useRegistrarUsuario()
  const { register, handleSubmit, formState, setError } = useForm<RegistroForm>({
    resolver: zodResolver(RegistroSchema),
    defaultValues: { phone: '', city: '' },
  })

  const enviar = handleSubmit((datos) => {
    registro.mutate(datos, {
      onSuccess: () => void navigate(RUTAS.login, { state: { registrado: true } }),
      onError: (e) => {
        if (esApiError(e) && e.status === 400) {
          for (const [clave, mensaje] of Object.entries(e.details)) {
            if ((CAMPOS as string[]).includes(clave)) setError(clave as keyof RegistroForm, { message: mensaje })
          }
        }
      },
    })
  })

  const errorGeneral = registro.isError
    ? esApiError(registro.error) && registro.error.status === 409
      ? 'No fue posible crear la cuenta: el usuario o el correo ya están registrados.'
      : mensajeDeError(registro.error)
    : null
  const e = formState.errors

  return (
    <div className="mx-auto max-w-2xl">
      <Tarjeta>
        <h1 className="text-2xl font-bold">Crear cuenta</h1>
        {errorGeneral && <div className="mt-4"><Aviso tipo="error" mensaje={errorGeneral} /></div>}
        <form noValidate onSubmit={(ev) => void enviar(ev)} className="mt-4 grid gap-4 sm:grid-cols-2">
          <Campo etiqueta="Nombre de usuario" autoComplete="username" {...register('username')} error={e.username?.message} />
          <Campo etiqueta="Correo electrónico" type="email" autoComplete="email" {...register('email')} error={e.email?.message} />
          <Campo etiqueta="Nombres" autoComplete="given-name" {...register('firstName')} error={e.firstName?.message} />
          <Campo etiqueta="Apellidos" autoComplete="family-name" {...register('lastName')} error={e.lastName?.message} />
          <Campo etiqueta="Teléfono (opcional)" type="tel" {...register('phone')} error={e.phone?.message} />
          <Campo etiqueta="Ciudad (opcional)" {...register('city')} error={e.city?.message} />
          <Campo etiqueta="Fecha de nacimiento" type="date" {...register('birthDate')} error={e.birthDate?.message} />
          <Campo etiqueta="Contraseña (mínimo 12 caracteres)" type="password" autoComplete="new-password" {...register('password')} error={e.password?.message} />
          <Boton type="submit" className="sm:col-span-2" cargando={registro.isPending}>Registrarme</Boton>
        </form>
        <p className="mt-4 text-sm">
          ¿Ya tienes cuenta? <Link to={RUTAS.login} className="font-semibold text-marca-600 hover:underline">Inicia sesión</Link>
        </p>
      </Tarjeta>
    </div>
  )
}
