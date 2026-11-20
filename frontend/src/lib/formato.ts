const moneda = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })
const fechaHora = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' })
const fecha = new Intl.DateTimeFormat('es-CO', { dateStyle: 'long' })

export function formatearCOP(valor: number): string {
  return moneda.format(valor)
}

/** El backend envía LocalDateTime sin zona (hora de Bogotá); se muestra tal cual. */
export function formatearFechaHora(valor: string): string {
  const d = new Date(valor)
  return Number.isNaN(d.getTime()) ? valor : fechaHora.format(d)
}

export function formatearFecha(valor: string): string {
  const d = new Date(`${valor}T00:00:00`)
  return Number.isNaN(d.getTime()) ? valor : fecha.format(d)
}
