import { z } from 'zod'

export const RolSchema = z.enum(['USER', 'STAFF', 'ADMIN'])

export const AuthResponseSchema = z.object({
  accessToken: z.string().min(1),
  tokenType: z.literal('Bearer'),
  expiresIn: z.number().positive(),
})

/** GET /api/auth/me real: id, username, email y roles sin prefijo ROLE_. */
export const UsuarioAutenticadoSchema = z.object({
  id: z.number(),
  username: z.string(),
  email: z.string(),
  roles: z.array(RolSchema),
})

/** Lo que se guarda en sessionStorage; se valida al arrancar (FE-002). */
export const SesionGuardadaSchema = z.object({
  token: z.string().min(1),
  expiraEn: z.number(),
})

export const LoginSchema = z.object({
  identifier: z.string().trim().min(1, 'Ingresa tu usuario o correo'),
  password: z.string().min(1, 'Ingresa tu contraseña'),
})

export type Rol = z.infer<typeof RolSchema>
export type UsuarioAutenticado = z.infer<typeof UsuarioAutenticadoSchema>
export type LoginForm = z.infer<typeof LoginSchema>

export type Sesion =
  | { estado: 'cargando' }
  | { estado: 'anonimo' }
  | { estado: 'autenticado'; token: string; expiraEn: number; usuario: UsuarioAutenticado }
