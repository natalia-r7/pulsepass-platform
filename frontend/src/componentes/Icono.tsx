import type { ComponentProps } from 'react'

/*
 * Iconos SVG locales con los trazos de Lucide (https://lucide.dev), licencia ISC:
 * Copyright (c) for portions of Lucide are held by Cole Bemis 2013-2022 as part of Feather (MIT).
 * All other copyright (c) for Lucide are held by Lucide Contributors 2022.
 * Solo se incluyen los iconos que usa la interfaz, sin añadir dependencias.
 */
const TRAZOS = {
  mapPin: (
    <>
      <path d="M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0" />
      <circle cx="12" cy="10" r="3" />
    </>
  ),
  calendar: (
    <>
      <path d="M8 2v4" />
      <path d="M16 2v4" />
      <rect width="18" height="18" x="3" y="4" rx="2" />
      <path d="M3 10h18" />
    </>
  ),
  menu: (
    <>
      <path d="M4 6h16" />
      <path d="M4 12h16" />
      <path d="M4 18h16" />
    </>
  ),
  x: (
    <>
      <path d="M18 6 6 18" />
      <path d="m6 6 12 12" />
    </>
  ),
  sun: (
    <>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2" />
      <path d="M12 20v2" />
      <path d="m4.93 4.93 1.41 1.41" />
      <path d="m17.66 17.66 1.41 1.41" />
      <path d="M2 12h2" />
      <path d="M20 12h2" />
      <path d="m6.34 17.66-1.41 1.41" />
      <path d="m19.07 4.93-1.41 1.41" />
    </>
  ),
  moon: <path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z" />,
  check: <path d="M20 6 9 17l-5-5" />,
  ticket: (
    <>
      <path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z" />
      <path d="M13 5v2" />
      <path d="M13 17v2" />
      <path d="M13 11v2" />
    </>
  ),
} as const

export type NombreIcono = keyof typeof TRAZOS

interface PropsIcono extends ComponentProps<'svg'> {
  nombre: NombreIcono
  /** Texto para lectores de pantalla; sin él, el icono es decorativo (aria-hidden). */
  etiqueta?: string
}

export function Icono({ nombre, etiqueta, className = 'h-4 w-4', ...resto }: PropsIcono) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      role={etiqueta ? 'img' : undefined}
      aria-label={etiqueta}
      aria-hidden={etiqueta ? undefined : true}
      focusable="false"
      {...resto}
    >
      {TRAZOS[nombre]}
    </svg>
  )
}
