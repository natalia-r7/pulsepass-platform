import { z } from 'zod'

export const EventCategorySchema = z.enum(['MUSIC', 'SPORTS', 'TECHNOLOGY', 'EDUCATION', 'CULTURE', 'ENTERTAINMENT'])
export const EventStatusSchema = z.enum(['DRAFT', 'PUBLISHED', 'SOLD_OUT', 'CANCELLED', 'FINISHED'])

/** EventResponse real del backend. No incluye artistas (brecha GAP-09, ver README). */
export const EventResponseSchema = z.object({
  id: z.number(),
  eventCode: z.string(),
  name: z.string(),
  description: z.string().nullish(),
  category: EventCategorySchema,
  status: EventStatusSchema,
  eventDate: z.string(),
  minimumAge: z.number(),
  venueCode: z.string(),
  venueName: z.string().nullish(),
})

export const EventSummaryResponseSchema = z.object({
  id: z.number(),
  eventCode: z.string(),
  name: z.string(),
  category: EventCategorySchema,
  status: EventStatusSchema,
  eventDate: z.string(),
  venueCode: z.string(),
})

export const CrearEventoSchema = z.object({
  eventCode: z.string().trim().min(1, 'Ingresa el código del evento').max(30, 'Máximo 30 caracteres'),
  name: z.string().trim().min(1, 'Ingresa el nombre').max(150, 'Máximo 150 caracteres'),
  description: z.string().max(500, 'Máximo 500 caracteres'),
  category: EventCategorySchema,
  eventDate: z.string().min(1, 'Ingresa la fecha y hora'),
  minimumAge: z.number({ error: 'Ingresa un número' }).int('Debe ser un entero').min(0, 'Debe ser mayor o igual a 0'),
  venueCode: z.string().min(1, 'Selecciona una sede'),
})

export type EventCategory = z.infer<typeof EventCategorySchema>
export type EventStatus = z.infer<typeof EventStatusSchema>
export type EventResponse = z.infer<typeof EventResponseSchema>
export type EventSummaryResponse = z.infer<typeof EventSummaryResponseSchema>
export type CrearEventoForm = z.infer<typeof CrearEventoSchema>

export const ETIQUETA_CATEGORIA: Record<EventCategory, string> = {
  MUSIC: 'Música',
  SPORTS: 'Deportes',
  TECHNOLOGY: 'Tecnología',
  EDUCATION: 'Educación',
  CULTURE: 'Cultura',
  ENTERTAINMENT: 'Entretenimiento',
}

export const ETIQUETA_ESTADO_EVENTO: Record<EventStatus, string> = {
  DRAFT: 'Borrador',
  PUBLISHED: 'Publicado',
  SOLD_OUT: 'Agotado',
  CANCELLED: 'Cancelado',
  FINISHED: 'Finalizado',
}
