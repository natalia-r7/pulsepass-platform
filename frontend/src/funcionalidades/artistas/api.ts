import { z } from 'zod'
import { peticion } from '../../lib/http'
import { ArtistResponseSchema } from './tipos'

export const artistasApi = {
  activos: () => peticion('/artists/active', z.array(ArtistResponseSchema)),
  porId: (id: number) => peticion(`/artists/${id}`, ArtistResponseSchema),
  porNombre: (stageName: string) =>
    peticion(`/artists/by-stage-name?stageName=${encodeURIComponent(stageName)}`, ArtistResponseSchema),
}
