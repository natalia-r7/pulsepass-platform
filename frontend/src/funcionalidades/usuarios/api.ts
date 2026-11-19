import { peticion } from '../../lib/http'
import { UserResponseSchema, type RegistroForm } from './tipos'

export const usuariosApi = {
  registrar: (datos: RegistroForm) =>
    peticion('/users', UserResponseSchema, {
      metodo: 'POST',
      cuerpo: { ...datos, phone: datos.phone || null, city: datos.city || null },
    }),
  porEmail: (email: string) => peticion(`/users/by-email?email=${encodeURIComponent(email)}`, UserResponseSchema),
  porUsername: (username: string) =>
    peticion(`/users/by-username?username=${encodeURIComponent(username)}`, UserResponseSchema),
}
