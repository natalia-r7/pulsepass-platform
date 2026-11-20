import { z } from 'zod'

const CLAVE = 'pulsepass.eventosRecientes'
const Esquema = z.array(z.string()).max(20)

/** GAP-01: no hay listado completo; se recuerdan en la pestaña los códigos creados o consultados. */
export function leerRecientes(): string[] {
  try {
    return Esquema.safeParse(JSON.parse(sessionStorage.getItem(CLAVE) ?? '[]')).data ?? []
  } catch {
    return []
  }
}

export function recordarEvento(eventCode: string): void {
  try {
    const lista = [eventCode, ...leerRecientes().filter((c) => c !== eventCode)].slice(0, 20)
    sessionStorage.setItem(CLAVE, JSON.stringify(lista))
  } catch {
    /* sin almacenamiento */
  }
}
