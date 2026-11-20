import clsx from 'clsx'
import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import { Icono } from '../componentes/Icono'
import { useAuth } from '../funcionalidades/auth/useAuth'
import { RUTAS } from '../lib/rutas'

interface Enlace {
  a: string
  texto: string
  fin?: boolean
}

const claseEnlace = ({ isActive }: { isActive: boolean }) =>
  clsx(
    'flex min-h-11 items-center rounded-full px-4 text-sm font-semibold transition focus-visible:outline-2 focus-visible:outline-marca-600',
    isActive
      ? 'bg-marca-600/10 text-marca-700 dark:bg-white/10 dark:text-white'
      : 'text-slate-600 hover:bg-marca-50 hover:text-marca-700 dark:text-slate-300 dark:hover:bg-white/10 dark:hover:text-white',
  )

function useTemaOscuro(): [boolean, () => void] {
  const [oscuro, setOscuro] = useState(() => window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false)
  useEffect(() => {
    document.documentElement.classList.toggle('dark', oscuro)
  }, [oscuro])
  return [oscuro, () => setOscuro((v) => !v)]
}

export default function Layout() {
  const { sesion, tieneRol, cerrarSesion } = useAuth()
  const navigate = useNavigate()
  const [menuAbierto, setMenuAbierto] = useState(false)
  const [oscuro, alternarTema] = useTemaOscuro()

  const enlaces: Enlace[] = [
    { a: RUTAS.eventos, texto: 'Eventos' },
    { a: RUTAS.artistas, texto: 'Artistas' },
    { a: RUTAS.sedes, texto: 'Sedes' },
  ]
  if (tieneRol('USER')) enlaces.push({ a: RUTAS.misTickets, texto: 'Mis tickets' })
  if (tieneRol('STAFF', 'ADMIN')) enlaces.push({ a: RUTAS.validacion, texto: 'Validar ingreso' })
  if (tieneRol('ADMIN')) enlaces.push({ a: RUTAS.adminEventos, texto: 'Administrar eventos' })

  // Primero se sale de la ruta protegida y después se cierra la sesión; si no, la guarda
  // redirige a /login antes de llegar a / (PRD §12.2).
  const salir = async () => {
    setMenuAbierto(false)
    await navigate(RUTAS.inicio)
    cerrarSesion('manual')
  }

  const cuenta =
    sesion.estado === 'autenticado' ? (
      <>
        <NavLink to={RUTAS.cuenta} className={claseEnlace} onClick={() => setMenuAbierto(false)}>
          Mi cuenta ({sesion.usuario.username})
        </NavLink>
        <button type="button" onClick={() => void salir()} className={claseEnlace({ isActive: false })}>
          Salir
        </button>
      </>
    ) : (
      <>
        <NavLink to={RUTAS.login} className={claseEnlace} onClick={() => setMenuAbierto(false)}>
          Ingresar
        </NavLink>
        <Link
          to={RUTAS.registro}
          onClick={() => setMenuAbierto(false)}
          className="flex min-h-11 items-center justify-center rounded-full bg-marca-600 px-5 text-sm font-semibold text-white shadow-sm hover:bg-marca-700 dark:bg-marca-500 dark:hover:bg-marca-600"
        >
          Registrarse
        </Link>
      </>
    )

  return (
    <div className="flex min-h-screen flex-col">
      <a href="#contenido" className="sr-only focus:not-sr-only focus:absolute focus:left-2 focus:top-2 focus:z-50 focus:rounded-full focus:bg-white focus:px-4 focus:py-2 focus:text-marca-700">
        Saltar al contenido
      </a>
      <header className="sticky top-0 z-40 border-b border-slate-200/70 bg-white/75 backdrop-blur-xl dark:border-white/10 dark:bg-[#0b0618]/75">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
          <Link
            to={RUTAS.inicio}
            aria-label="PulsePass, inicio"
            className="rounded-md font-titulo text-2xl font-extrabold tracking-tight text-slate-900 focus-visible:outline-2 focus-visible:outline-marca-600 dark:text-white"
          >
            Pulse<span className="text-marca-600 dark:text-marca-400">Pass</span>
          </Link>
          <nav aria-label="Principal" className="hidden flex-1 items-center gap-1 md:flex">
            {enlaces.map((e) => (
              <NavLink key={e.a} to={e.a} className={claseEnlace} end={e.fin}>
                {e.texto}
              </NavLink>
            ))}
          </nav>
          <div className="hidden items-center gap-1 md:flex">
            <button
              type="button"
              onClick={alternarTema}
              aria-label={oscuro ? 'Activar modo claro' : 'Activar modo oscuro'}
              className="flex h-11 w-11 items-center justify-center rounded-full text-lg hover:bg-marca-50 dark:hover:bg-white/10"
            >
              <Icono nombre={oscuro ? 'sun' : 'moon'} className="h-5 w-5" />
            </button>
            {cuenta}
          </div>
          <button
            type="button"
            className="flex h-11 w-11 items-center justify-center rounded-full text-xl hover:bg-marca-50 md:hidden dark:hover:bg-white/10"
            aria-expanded={menuAbierto}
            aria-controls="menu-movil"
            aria-label={menuAbierto ? 'Cerrar menú' : 'Abrir menú'}
            onClick={() => setMenuAbierto((v) => !v)}
          >
            <Icono nombre={menuAbierto ? 'x' : 'menu'} className="h-6 w-6" />
          </button>
        </div>
        {menuAbierto && (
          <nav id="menu-movil" aria-label="Menú móvil" className="space-y-1 border-t border-slate-200/70 px-4 pb-5 pt-3 md:hidden dark:border-white/10">
            {enlaces.map((e) => (
              <NavLink key={e.a} to={e.a} className={claseEnlace} onClick={() => setMenuAbierto(false)}>
                {e.texto}
              </NavLink>
            ))}
            <hr className="my-2 border-slate-200 dark:border-white/10" />
            {cuenta}
            <button type="button" onClick={alternarTema} className={claseEnlace({ isActive: false })}>
              {oscuro ? 'Modo claro' : 'Modo oscuro'}
            </button>
          </nav>
        )}
      </header>
      <main id="contenido" className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
        <Outlet />
      </main>
      <footer className="mt-12 border-t border-slate-200/70 py-8 text-sm text-slate-600 dark:border-white/10 dark:text-slate-400">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-4">
          <p>© 2026 PulsePass</p>
          <p className="space-x-3">
            <Link to={RUTAS.sedes} className="hover:underline">Sedes</Link>
            <Link to={RUTAS.artistas} className="hover:underline">Artistas</Link>
          </p>
        </div>
      </footer>
    </div>
  )
}
