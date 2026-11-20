import { describe, expect, it } from 'vitest'
import { traducirMensaje } from './mensajes'

describe('traducirMensaje', () => {
  it('traduce la reutilización de un ticket y conserva su código', () => {
    expect(traducirMensaje('Only PAID tickets can be used: TKT-1E59B2')).toBe(
      'Este ticket no se puede usar: ya fue utilizado o no está pagado (TKT-1E59B2).',
    )
  })

  it('traduce la edad mínima con el formato real del backend', () => {
    expect(traducirMensaje('User does not meet minimum age for event DEMO-FEST-2026')).toBe(
      'No cumples la edad mínima requerida para este evento (for event DEMO-FEST-2026).',
    )
  })

  it('traduce un mensaje sin detalle variable', () => {
    expect(traducirMensaje('Authentication is required')).toBe('Debes iniciar sesión para continuar.')
  })

  it('deja intacto un mensaje desconocido', () => {
    expect(traducirMensaje('Something new happened')).toBe('Something new happened')
  })
})
