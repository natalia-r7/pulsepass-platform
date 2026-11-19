import { z } from 'zod'
import { traducirMensaje } from './mensajes'

/** Contrato uniforme de errores del backend (PRD Controladores §13). */
export const ErrorResponseSchema = z.object({
  timestamp: z.string(),
  status: z.number(),
  error: z.string(),
  message: z.string(),
  details: z.record(z.string(), z.string()).default({}),
})

/** Error tipado que reciben las páginas; status 0 = fallo de red. */
export class ApiError extends Error {
  readonly status: number
  readonly details: Record<string, string>

  constructor(status: number, message: string, details: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

export const MENSAJE_RED = 'No pudimos conectar con el servidor.'
export const MENSAJE_INESPERADO = 'Ocurrió un error inesperado. Inténtalo más tarde.'

const BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? '/api'

let obtenerToken: () => string | null = () => null
let alRecibir401: () => void = () => {}

/** AuthContext registra aquí cómo leer el token y qué hacer ante un 401. */
export function configurarHttp(opciones: { obtenerToken: () => string | null; alRecibir401: () => void }) {
  obtenerToken = opciones.obtenerToken
  alRecibir401 = opciones.alRecibir401
}

interface OpcionesPeticion {
  metodo?: 'GET' | 'POST' | 'PATCH'
  cuerpo?: unknown
  /** En el login un 401 significa credenciales inválidas, no sesión expirada. */
  ignorar401?: boolean
  /** Token explícito (p. ej. justo después del login, antes de guardar la sesión). */
  token?: string
}

async function convertirError(respuesta: Response): Promise<ApiError> {
  let cuerpo: unknown = null
  try {
    cuerpo = await respuesta.json()
  } catch {
    cuerpo = null
  }
  const resultado = ErrorResponseSchema.safeParse(cuerpo)
  if (respuesta.status >= 500) return new ApiError(respuesta.status, MENSAJE_INESPERADO)
  if (resultado.success) {
    return new ApiError(respuesta.status, traducirMensaje(resultado.data.message), resultado.data.details)
  }
  return new ApiError(respuesta.status, MENSAJE_INESPERADO)
}

/** fetch + Bearer + validación zod de la respuesta + ErrorResponse → ApiError. */
export async function peticion<T>(ruta: string, esquema: z.ZodType<T>, opciones: OpcionesPeticion = {}): Promise<T> {
  const encabezados: Record<string, string> = { Accept: 'application/json' }
  const token = opciones.token ?? obtenerToken()
  if (token) encabezados.Authorization = `Bearer ${token}`
  if (opciones.cuerpo !== undefined) encabezados['Content-Type'] = 'application/json'

  let respuesta: Response
  try {
    respuesta = await fetch(`${BASE_URL}${ruta}`, {
      method: opciones.metodo ?? 'GET',
      headers: encabezados,
      body: opciones.cuerpo === undefined ? undefined : JSON.stringify(opciones.cuerpo),
    })
  } catch {
    throw new ApiError(0, MENSAJE_RED)
  }

  if (!respuesta.ok) {
    const error = await convertirError(respuesta)
    if (respuesta.status === 401 && !opciones.ignorar401) alRecibir401()
    throw error
  }

  const datos: unknown = await respuesta.json()
  const resultado = esquema.safeParse(datos)
  if (!resultado.success) throw new ApiError(-1, 'La respuesta del servidor no tiene el formato esperado.')
  return resultado.data
}

export function esApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

/** Mensaje de interfaz para un error de mutación (PRD §13). */
export function mensajeDeError(error: unknown): string {
  if (!esApiError(error)) return MENSAJE_INESPERADO
  if (error.status === 403) return 'No tienes permisos para realizar esta acción.'
  return error.message
}
