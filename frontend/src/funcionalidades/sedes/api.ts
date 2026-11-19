import { z } from 'zod'
import { peticion } from '../../lib/http'
import { VenueResponseSchema } from './tipos'

export const sedesApi = {
  activas: () => peticion('/venues/active', z.array(VenueResponseSchema)),
  porCodigo: (code: string) => peticion(`/venues/${encodeURIComponent(code)}`, VenueResponseSchema),
}
