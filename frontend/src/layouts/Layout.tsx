import clsx from 'clsx'
import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import { useAuth } from '../funcionalidades/auth/useAuth'
import { RUTAS } from '../lib/rutas'

interface Enlace {
  a: string
  texto: string
  fin?: boolean
}

const claseEnlace = ({ isActive }: { isActive: boolean }) =>
  clsx(
    'block rounded-md px-3 py-2 text-sm font-medium focus-visible:outline-2 focus-visible:outline-marca-600',
    isActive
      ? 'bg-marca-50 text-marca-700 dark:bg-slate-800 dark:text-marca-500'
      : 'text-slate-700 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800',
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
          className="block rounded-md bg-marca-600 px-3 py-2 text-sm font-semibold text-white hover:bg-marca-700"
        >
          Registrarse
        </Link>
      </>
    )

  return (
    <div className="flex min-h-screen flex-col">
      <a href="#contenido" className="sr-only focus:not-sr-only focus:absolute focus:left-2 focus:top-2 focus:z-50 focus:rounded focus:bg-white focus:px-3 focus:py-2">
        Saltar al contenido
      </a>
      <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/90 backdrop-blur dark:border-slate-800 dark:bg-slate-950/90">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
          <Link to={RUTAS.inicio} className="font-titulo text-lg font-bold text-marca-600 dark:text-marca-500">
            ♪ PulsePass
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
              className="rounded-md px-2 py-2 text-sm hover:bg-slate-100 dark:hover:bg-slate-800"
            >
              {oscuro ? '☀' : '☾'}
            </button>
            {cuenta}
          </div>
          <button
            type="button"
            className="rounded-md px-3 py-2 text-xl md:hidden"
            aria-expanded={menuAbierto}
            aria-controls="menu-movil"
            aria-label={menuAbierto ? 'Cerrar menú' : 'Abrir menú'}
            onClick={() => setMenuAbierto((v) => !v)}
          >
            {menuAbierto ? '✕' : '☰'}
          </button>
        </div>
        {menuAbierto && (
          <nav id="menu-movil" aria-label="Menú móvil" className="space-y-1 border-t border-slate-200 px-4 py-3 md:hidden dark:border-slate-800">
            {enlaces.map((e) => (
              <NavLink key={e.a} to={e.a} className={claseEnlace} onClick={() => setMenuAbierto(false)}>
                {e.texto}
              </NavLink>
            ))}
            <hr className="border-slate-200 dark:border-slate-800" />
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
      <footer className="border-t border-slate-200 py-6 text-sm text-slate-600 dark:border-slate-800 dark:text-slate-400">
        <div className="mx-auto flex max-w-6xl flex-wrap justify-between gap-2 px-4">
          <p>© 2026 PulsePass · Caso de estudio académico</p>
          <p className="space-x-3">
            <Link to={RUTAS.sedes} className="hover:underline">Sedes</Link>
            <Link to={RUTAS.artistas} className="hover:underline">Artistas</Link>
          </p>
        </div>
      </footer>
    </div>
  )
}
