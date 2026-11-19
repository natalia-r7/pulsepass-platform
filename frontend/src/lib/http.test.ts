import { http as mswHttp, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { z } from 'zod'
import { API, errorResponse } from '../test/mocks/datos'
import { servidor } from '../test/mocks/servidor'
import { formatearCOP } from './formato'
import { ApiError, configurarHttp, MENSAJE_INESPERADO, peticion } from './http'

describe('lib/http', () => {
  it('TEST-FE-LIB-001: añade Authorization Bearer cuando hay sesión', async () => {
    let autorizacion: string | null = null
    let url = ''
    servidor.use(
      mswHttp.get(`${API}/prueba`, ({ request }) => {
        autorizacion = request.headers.get('Authorization')
        url = request.url
        return HttpResponse.json({ ok: true })
      }),
    )
    configurarHttp({ obtenerToken: () => 'abc', alRecibir401: () => {} })
    await peticion('/prueba', z.object({ ok: z.boolean() }))
    expect(autorizacion).toBe('Bearer abc')
    expect(url).not.toContain('abc') // el token nunca viaja en la URL
  })

  it('TEST-FE-LIB-002: ErrorResponse válido → ApiError con status y details', async () => {
    servidor.use(
      mswHttp.post(`${API}/prueba`, () =>
        HttpResponse.json(errorResponse(400, 'Bad Request', 'Validation failed', { name: 'Name is required' }), { status: 400 }),
      ),
    )
    const error = await peticion('/prueba', z.unknown(), { metodo: 'POST', cuerpo: {} }).catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 400, message: 'Validation failed', details: { name: 'Name is required' } })
  })

  it('TEST-FE-LIB-003: cuerpo de error inválido → ApiError genérico', async () => {
    servidor.use(mswHttp.get(`${API}/prueba`, () => HttpResponse.text('<html>', { status: 409 })))
    const error = await peticion('/prueba', z.unknown()).catch((e: unknown) => e)
    expect(error).toMatchObject({ status: 409, message: MENSAJE_INESPERADO })
  })

  it('TEST-FE-LIB-004: 401 → dispara cierre de sesión', async () => {
    const alRecibir401 = vi.fn()
    configurarHttp({ obtenerToken: () => 'vencido', alRecibir401 })
    servidor.use(
      mswHttp.get(`${API}/prueba`, () => HttpResponse.json(errorResponse(401, 'Unauthorized', 'Authentication is required'), { status: 401 })),
    )
    await expect(peticion('/prueba', z.unknown())).rejects.toBeInstanceOf(ApiError)
    expect(alRecibir401).toHaveBeenCalledTimes(1)
  })

  it('500 → mensaje genérico sin detalles internos', async () => {
    servidor.use(mswHttp.get(`${API}/prueba`, () => HttpResponse.json(errorResponse(500, 'Internal Server Error', 'NullPointer…'), { status: 500 })))
    await expect(peticion('/prueba', z.unknown())).rejects.toMatchObject({ status: 500, message: MENSAJE_INESPERADO })
  })

  it('error de red → status 0 y mensaje de conexión', async () => {
    servidor.use(mswHttp.get(`${API}/prueba`, () => HttpResponse.error()))
    await expect(peticion('/prueba', z.unknown())).rejects.toMatchObject({ status: 0, message: 'No pudimos conectar con el servidor.' })
  })

  it('TEST-FE-LIB-005: PrecioCOP formatea 250000 como COP', () => {
    const texto = formatearCOP(250000).replace(/\s/g, ' ')
    expect(texto).toMatch(/\$\s?250\.000/)
  })
})
