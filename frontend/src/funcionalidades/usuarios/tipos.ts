import { z } from 'zod'

/** UserResponse real (sin hash ni fecha de nacimiento). */
export const UserResponseSchema = z.object({
  id: z.number(),
  username: z.string(),
  email: z.string(),
  active: z.boolean(),
  firstName: z.string(),
  lastName: z.string(),
  phone: z.string().nullish(),
  city: z.string().nullish(),
})

const bytesUtf8 = (texto: string) => new TextEncoder().encode(texto).length

export const RegistroSchema = z.object({
  username: z.string().trim().min(1, 'Ingresa un nombre de usuario').max(50, 'Máximo 50 caracteres'),
  email: z.email('Ingresa un correo válido').max(254, 'Máximo 254 caracteres'),
  firstName: z.string().trim().min(1, 'Ingresa tus nombres').max(100, 'Máximo 100 caracteres'),
  lastName: z.string().trim().min(1, 'Ingresa tus apellidos').max(100, 'Máximo 100 caracteres'),
  phone: z.string().max(30, 'Máximo 30 caracteres'),
  city: z.string().max(100, 'Máximo 100 caracteres'),
  birthDate: z
    .string()
    .min(1, 'Ingresa tu fecha de nacimiento')
    .refine((v) => !Number.isNaN(new Date(v).getTime()), 'Fecha no válida')
    .refine((v) => new Date(v).getTime() <= Date.now(), 'La fecha no puede ser futura'),
  password: z
    .string()
    .min(12, 'La contraseña debe tener al menos 12 caracteres')
    .refine((v) => bytesUtf8(v) <= 72, 'La contraseña no puede superar 72 bytes'),
})

export type UserResponse = z.infer<typeof UserResponseSchema>
export type RegistroForm = z.infer<typeof RegistroSchema>
