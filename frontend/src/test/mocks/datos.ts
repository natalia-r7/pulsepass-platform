// DATO DE PRUEBA: escenario de referencia del PRD §22 (sin backend real).
export const API = 'http://localhost/api'

export const eventoCMF = {
  id: 1,
  eventCode: 'CMF-2026',
  name: 'Caribbean Music Fest 2026',
  description: 'Festival de música del Caribe',
  category: 'MUSIC',
  status: 'PUBLISHED',
  eventDate: '2026-12-12T20:00:00',
  minimumAge: 18,
  venueCode: 'VEN-SMR-01',
  venueName: 'Marina Convention Center',
} as const

export const resumenCMF = {
  id: 1,
  eventCode: 'CMF-2026',
  name: 'Caribbean Music Fest 2026',
  category: 'MUSIC',
  status: 'PUBLISHED',
  eventDate: '2026-12-12T20:00:00',
  venueCode: 'VEN-SMR-01',
} as const

export const sede = { id: 1, code: 'VEN-SMR-01', name: 'Marina Convention Center', city: 'Santa Marta', address: null, capacity: 3 }

export const artistas = [
  { id: 1, stageName: 'Solar Beat', country: 'Colombia', genre: 'Pop', active: true },
  { id: 2, stageName: 'Neon Waves', country: null, genre: null, active: true },
]

export const andrea = { id: 10, username: 'andrea', email: 'andrea@example.test', roles: ['USER'] }
export const staff = { id: 11, username: 'laura', email: 'laura@example.test', roles: ['STAFF'] }
export const admin = { id: 12, username: 'admin', email: 'admin@example.test', roles: ['ADMIN'] }

export const ticketAndrea = {
  id: 100,
  ticketCode: 'TCK-0001',
  type: 'VIP',
  price: 250000,
  status: 'PAID',
  purchaseDate: '2026-11-19T10:00:00',
  userEmail: 'andrea@example.test',
  eventCode: 'CMF-2026',
} as const

export function errorResponse(status: number, error: string, message: string, details: Record<string, string> = {}) {
  return { timestamp: '2026-11-19T15:00:00', status, error, message, details }
}
