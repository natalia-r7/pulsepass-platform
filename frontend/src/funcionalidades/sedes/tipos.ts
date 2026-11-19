import { z } from 'zod'

/** VenueResponse real: no incluye el campo active (solo se listan sedes activas). */
export const VenueResponseSchema = z.object({
  id: z.number(),
  code: z.string(),
  name: z.string(),
  city: z.string(),
  address: z.string().nullish(),
  capacity: z.number(),
})

export type VenueResponse = z.infer<typeof VenueResponseSchema>
