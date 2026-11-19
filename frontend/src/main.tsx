import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router/dom'
import './index.css'
import { AuthProvider } from './funcionalidades/auth/AuthContext'
import { crearQueryClient } from './lib/queryClient'
import { crearRouter } from './router'

// Tema inicial antes del primer pintado, para evitar un parpadeo de claro a oscuro.
document.documentElement.classList.toggle('dark', window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false)

const queryClient = crearQueryClient()
const router = crearRouter()

const raiz = document.getElementById('root')
if (!raiz) throw new Error('No existe el elemento #root')

createRoot(raiz).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>
  </StrictMode>,
)
