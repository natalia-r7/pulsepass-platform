# PulsePass — Frontend (Fase 6)

SPA en React que consume la API REST segura de PulsePass (PRD Frontend, `docs/proyecto Pulsepass.pdf`, pp. 138–200).
El backend no se modificó: el frontend usa los DTO y endpoints reales de las Fases 3 y 4.

> **Estado:** entrega del 19-11-2026. La aplicación compila, pasa lint y 46 pruebas automatizadas con la API simulada (MSW).
> **No se ha verificado contra el backend real** y quedan requisitos pendientes (ver más abajo). El DoD §30.2 **no** se da por cumplido.

## Stack

| Pieza | Versión instalada |
|---|---|
| Node | 22.22+ requerido (probado con 26.13.1) |
| Vite (plantilla `react-ts`) | 8.3 |
| React / React DOM | 19.3.1 |
| TypeScript | 7.0, `strict` y `noUncheckedIndexedAccess` |
| Tailwind CSS (`@tailwindcss/vite`) | 4.3.5 |
| React Router (`react-router`, `createBrowserRouter`) | 8.5.2 |
| TanStack Query | 5.108 |
| React Hook Form + `@hookform/resolvers` + zod | 7.92 / 7.0 / 4.6 |
| Vitest + Testing Library + MSW | 5.1 / 16.3 / 3.0 |
| Lint | oxlint 1.9x (ver desviaciones) |

HTTP con `fetch` nativo encapsulado en `src/lib/http.ts`; sin Axios.

## Comandos

```bash
npm install          # dependencias
npm run dev          # http://localhost:5173 (proxy /api → http://localhost:8080)
npm run build        # tsc estricto + build de producción
npm run lint         # oxlint
npm test             # Vitest + Testing Library + MSW (sin red real)
```

La API debe estar en `http://localhost:8080` (`backend/`, `./mvnw spring-boot:run` con las variables de `backend/.env.example`).

## Variables de entorno

| Variable | Valor | Uso |
|---|---|---|
| `VITE_API_BASE_URL` | `/api` (por defecto) | URL base de la API. En desarrollo pasa por el proxy de Vite. No es un secreto. |

Ver `.env.example`. No hay secretos en el bundle ni en el repositorio.

## Roles de demostración

Los usuarios se crean con la API (registro) y los roles STAFF/ADMIN se asignan con la semilla local de la Fase 4
(`tests/e2e/fixtures/`). No hay credenciales en el repositorio.

| Rol | Puede |
|---|---|
| Anónimo | Cartelera, detalle de eventos, artistas, sedes, registro e inicio de sesión |
| USER | Lo anterior + comprar, mis tickets, detalle y cancelación, mi cuenta |
| STAFF | Validar ingreso (marcar tickets como usados) y tickets pagados por evento |
| ADMIN | Lo de STAFF + crear, buscar, publicar eventos y asociar artistas |

## Rutas principales (20)

`/`, `/eventos`, `/eventos/:eventCode`, `/artistas`, `/artistas/:id`, `/sedes`, `/sedes/:code`, `/login`, `/registro`,
`/cuenta`, `/mis-tickets`, `/mis-tickets/:ticketCode`, `/compra/confirmacion/:ticketCode`, `/staff/validacion`,
`/staff/eventos/:eventCode/tickets`, `/admin/eventos`, `/admin/eventos/nuevo`, `/admin/eventos/:eventCode`, `/403` y `*`.
Todas se cargan con `lazy` salvo `Layout` e `Inicio`. Las guardas `RutaProtegida`, `RutaPorRol` y `SoloAnonimo` están en
`src/layouts/Guardas.tsx` y son solo experiencia de usuario: la seguridad la aplica el servidor.

## Estructura

```
src/
├── main.tsx            QueryClientProvider · AuthProvider · RouterProvider (react-router/dom)
├── router.tsx          createBrowserRouter con lazy, guardas y errorElement
├── index.css           Tailwind v4 + tokens @theme (§9.1) + variante dark por clase
├── lib/                http.ts (Bearer, ApiError, zod), rutas.ts, formato.ts (es-CO, COP), queryClient.ts
├── layouts/            Layout (navbar por rol, menú móvil, modo oscuro), Guardas
├── componentes/        Boton, Campo/Selector, Badge, Tarjeta, Modal, Tabla<T>, Aviso, PrecioCOP, estados
├── funcionalidades/    auth, eventos, artistas, sedes, usuarios, tickets: tipos.ts (zod) · api.ts · hooks.ts
├── pages/              una página por ruta
└── test/               setup, MSW (handlers y datos de prueba) y renderConProveedores
```

## Cobertura API → interfaz (§10.3)

Las 23 operaciones tienen una forma de invocarse desde la interfaz:

| # | Operación | Hook | Página |
|---|---|---|---|
| 1 | `GET /api/venues/{code}` | `useSede` | DetalleSede, DetalleEvento |
| 2 | `GET /api/venues/active` | `useSedesActivas` | ListaSedes, AdminCrearEvento |
| 3 | `POST /api/events` | `useCrearEvento` | AdminCrearEvento |
| 4 | `GET /api/events/{eventCode}` | `useEvento` | DetalleEvento, AdminGestionEvento |
| 5 | `GET /api/events/published` | `useEventosPublicados` | Inicio, CatalogoEventos |
| 6 | `PATCH /api/events/{eventCode}/publish` | `usePublicarEvento` | AdminGestionEvento |
| 7 | `POST /api/events/{eventCode}/artists/{artistId}` | `useAsociarArtista` | AdminGestionEvento |
| 8 | `GET /api/events/by-artist?stageName=` | `useEventosPorArtista` | CatalogoEventos (`?artista=`), DetalleArtista |
| 9 | `GET /api/artists/{id}` | `useArtista` | DetalleArtista |
| 10 | `GET /api/artists/by-stage-name?stageName=` | `useArtistaPorNombre` (hook disponible) | — ver pendientes |
| 11 | `GET /api/artists/active` | `useArtistasActivos` | Inicio, ListaArtistas, AdminGestionEvento |
| 12 | `POST /api/users` | `useRegistrarUsuario` | Registro |
| 13 | `GET /api/users/by-email?email=` | `useUsuarioPorEmail` | Cuenta |
| 14 | `GET /api/users/by-username?username=` | `useUsuarioPorUsername` | Cuenta |
| 15 | `POST /api/tickets` | `useComprarTicket` | DetalleEvento (email de la sesión) |
| 16 | `GET /api/tickets/{ticketCode}` | `useTicket` | DetalleTicket, ConfirmacionCompra, ValidacionIngreso |
| 17 | `GET /api/tickets/by-user?email=` | `useMisTickets` | MisTickets |
| 18 | `GET /api/events/{eventCode}/tickets/paid` | `useTicketsPagadosPorEvento` | TicketsPagadosEvento |
| 19 | `PATCH /api/tickets/{ticketCode}/cancel` | `useCancelarTicket` | MisTickets, DetalleTicket |
| 20 | `PATCH /api/tickets/{ticketCode}/use` | `useUsarTicket` | ValidacionIngreso, TicketsPagadosEvento |
| A1 | `POST /api/auth/login` | `iniciarSesion` (AuthContext) | Login |
| A2 | `GET /api/auth/me` | AuthContext | Layout, Cuenta |
| A3 | `POST /api/users` (con password) | `useRegistrarUsuario` | Registro |

## Sesión y errores

- Token en memoria y en `sessionStorage` (nunca `localStorage`, cookies ni URL); se valida con zod al arrancar y se
  descarta si expiró. Viaja solo en `Authorization: Bearer`. Los roles salen de `GET /api/auth/me`.
- Cierre local al vencer `expiresIn`; cualquier 401 fuera del login cierra la sesión, limpia la caché y lleva a
  `/login` con «Tu sesión expiró».
- `ApiError` uniforme: red → «No pudimos conectar con el servidor.»; 400 → `details` a los campos; 401 en login →
  «Credenciales inválidas.»; 403 → página `/403` (guardas) o «No disponible»/aviso; 404 → «no disponible» o 404;
  409 → mensaje del backend como texto; 500 → mensaje genérico.

## Pruebas (resultado del 19-11-2026)

`npm test`: **4 archivos, 46 pruebas, 46 PASS** (API simulada con MSW, sin red real). `npm run lint`: 0 avisos.
`npm run build`: correcto.

Cubiertas de la §21: TEST-FE-LIB-001..005, TEST-FE-AUTH-001..011, TEST-FE-EVT-001..006, TEST-FE-ART-001..003,
TEST-FE-VEN-001, TEST-FE-TKT-001..010, TEST-FE-STF-001..004, TEST-FE-ADM-001..006, TEST-FE-GEN-001, 003 y 004, más
401 → «Tu sesión expiró», 500 y error de red. **No cubierta:** TEST-FE-GEN-002 (errorElement).

## Brechas del backend (aceptadas, sin modificar el backend)

| ID | Brecha | Decisión |
|---|---|---|
| GAP-01 | No hay `GET /api/events` | ADMIN busca por código y ve los códigos consultados en la pestaña |
| GAP-02 | Borradores solo para STAFF/ADMIN | 403/404 → «Evento no disponible» |
| GAP-03 | Sin tickets por ID de usuario | `by-user?email=` con el email de `/api/auth/me` |
| GAP-05 | Sin CORS | Proxy de Vite en desarrollo |
| **GAP-09 (nueva)** | **`EventResponse` no incluye artistas** | El detalle del evento no puede listar sus artistas; la gestión ADMIN muestra solo los asociados en la sesión. La relación se consulta desde el artista (`by-artist`). |
| — | `VenueResponse` no tiene `active`; `UserResponse` no tiene `birthDate` | Los esquemas zod siguen los DTO reales, no la tabla §10.2 |

## Desviaciones respecto al PRD

1. Carpeta `frontend/` en lugar de `pulsepass-web/`.
2. Lint con **oxlint** (incluido en la plantilla actual de Vite) en lugar de ESLint; **Prettier no configurado**.
3. Notificaciones como avisos en línea (`role="status"`/`alert`, `aria-live`) en lugar de toasts que desaparecen a los 4 s.
4. Los errores de carga de página se muestran dentro de la página; `errorElement` solo captura errores del router.
5. Modo oscuro por clase en `<html>`, con valor inicial de `prefers-color-scheme`; no se guarda la preferencia.
6. Las fuentes Poppins/Inter no se cargan (no hay archivos locales); se usa la pila de respaldo del sistema.

## Pendientes (no implementados o no verificados)

- **Verificación manual contra el backend real** (login, compra, cancelación, validación y publicación): no realizada.
- **Fidelidad visual** con las 32 imágenes de §9.0: no verificada (las imágenes no están en el repositorio, `docs/diseno/` no existe).
  Solo se revisaron a mano Inicio y Login en escritorio y móvil (375 px).
- Barra de compra fija en móvil (`MovilEvento.png`) y menú desplegable de usuario en la navbar.
- `GET /api/artists/by-stage-name` sin uso en pantalla (hook listo; el filtro `?artista=` usa `by-artist`).
- TEST-FE-GEN-002 y prueba explícita de invalidación de caché (QT-FE-008).
- Auditoría de accesibilidad y rendimiento (Lighthouse/axe, QT-FE-011, NFR-FE-003).
- Prettier + `prettier-plugin-tailwindcss`.
