import { peticion } from '../../lib/http'
import { AuthResponseSchema, UsuarioAutenticadoSchema, type LoginForm } from './tipos'

export const authApi = {
  login: (datos: LoginForm) =>
    peticion('/auth/login', AuthResponseSchema, { metodo: 'POST', cuerpo: datos, ignorar401: true }),
  me: (token?: string) => peticion('/auth/me', UsuarioAutenticadoSchema, { token, ignorar401: token !== undefined }),
}
