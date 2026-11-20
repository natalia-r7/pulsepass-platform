import { QueryClient } from '@tanstack/react-query'
import { esApiError } from './http'

/** Sin reintentos para 4xx; uno para red y 5xx; las mutaciones nunca se reintentan (PRD §14.3). */
export function crearQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: (intentos, error) => {
          if (esApiError(error) && error.status >= 400 && error.status < 500) return false
          return intentos < 1
        },
        refetchOnWindowFocus: false,
      },
      mutations: { retry: false },
    },
  })
}
