# Pruebas E2E manuales — PulsePass Fase 5

Pruebas HTTP de caja negra, ejecutadas a mano contra la aplicación real conectada a su base PostgreSQL
exclusiva de E2E (PRD E2E, pp. 112–137). Este documento resume cómo preparar y ejecutar las pruebas, el estado
de la ejecución oficial del 19-11-2026, las desviaciones respecto al PRD y el estado de la Definition of Done (§24).

## Entregables

| Archivo | Contenido |
|---|---|
| `postman/PulsePass-Manual-E2E.postman_collection.json` | Colección oficial, Postman Collection v2.1: 124 peticiones, 22 rutas, sin scripts |
| `postman/PulsePass-local.postman_environment.json` | Plantilla del ambiente: 53 variables, sin secretos |
| `fixtures/README.md` y `fixtures/seed-e2e.sql` | Base E2E en Docker, seed y restauración |
| `checklist-e2e-manual.md` | Secuencia oficial de ejecución E0–E7 (173 acciones) |
| `registro-ejecucion-manual.md` | Bitácora de resultados (todo empieza en NOT RUN) |

## Cómo ejecutar las pruebas

1. **Preparar la base E2E:** `fixtures/README.md` (contenedor `pulsepass-e2e-db`, secretos de la sesión,
   arranque de la aplicación, comprobaciones previas, seed y restauración R-B). Nunca se toca la base de
   desarrollo `pulsepass-db`.
2. **Seguir `checklist-e2e-manual.md` en orden** (E0–E7, 173 acciones): cada casilla es un Send, un reenvío, un
   paso SQL o un reinicio. Cliente: Bruno (D-01), con la colección importada fuera del repositorio.
3. **Copiar variables a mano:** los valores derivados del `runId` (A-10), los secretos solo en variables
   secretas de Bruno (A-12), y los tokens y códigos que indica la sección «Copiar» de cada petición. Los tokens
   duran 15 minutos: si caducan, repetir el login y anotarlo como reenvío no previsto.
4. **Comparar cada respuesta** con la descripción de la petición (pestaña Docs): código HTTP, campos del DTO o
   de `ErrorResponse`, estado de negocio y, cuando se indica, la comprobación posterior.
5. **Registrar en `registro-ejecucion-manual.md`** cada Send: HTTP esperado y observado, estado, resultado
   (PASS, FAIL, BLOCKED o NOT RUN) y evidencia textual, sin tokens ni contraseñas. Ante un FAIL o un 500,
   detenerse (F5-H).

## Estado de la ejecución oficial (19-11-2026)

Ejecución **parcial** (D-02). Detalle en `registro-ejecucion-manual.md`, sección «Resumen de la ejecución».

| Indicador | Resultado |
|---|---|
| Acciones externas | A-01 a A-13, A-25 y A-26 en PASS; el resto, NOT RUN |
| Casos | 16 PASS, 0 FAIL, 0 BLOCKED y 108 NOT RUN, de 124 |
| Cobertura nominal | 8/22 rutas (venues, artists, registro, login y `/me`) |
| Cobertura de seguridad | 3/46 casos |
| Defectos | Ninguno en los casos ejecutados |
| Suite automatizada (`./mvnw clean test` en `backend/`, repositorio original, 19-11-2026) | 664 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS en 48,6 s, finalizada a las 14:34:48 |

## Desviaciones respecto al PRD

### D-01 — Cliente de ejecución: Bruno en lugar de Postman

**Qué dice el PRD.** La ejecución manual se hace «desde Postman con botón Send» (§1, Decisión MVP) y
«exclusivamente desde la interfaz de Postman» (§16.1).

**Qué se hace.** La colección y el ambiente oficiales siguen siendo los archivos **Postman Collection v2.1** de
`postman/` (FR-E2E-001, DoD §24), sin ningún cambio. Lo único que cambia es el **cliente** con el que se
importan y se envían las peticiones: **Bruno**, una aplicación de escritorio de código abierto. El procedimiento
no cambia: cada petición se envía a mano, en el orden del checklist, y se revisa visualmente. Siguen prohibidos
los scripts, `pm.test`, Newman y Postman CLI (§16.4); también el **Collection Runner de Bruno**.

**Por qué.** Las pruebas exigen que ningún secreto (contraseñas y tokens) salga del equipo (FR-E2E-013,
NFR-E2E-002) y la acción A-01 del checklist exige trabajar solo en local, sin sincronizar la colección ni el
ambiente con la nube:

- El cliente ligero de Postman, sin iniciar sesión, no permite importar colecciones ni usar ambientes y
  variables, de las que depende toda la colección (`{{baseUrl}}`, tokens y códigos).
- Con una cuenta de Postman, la colección, el ambiente y posiblemente el historial de peticiones (con
  contraseñas en los cuerpos y tokens en las cabeceras) se guardan en el espacio de trabajo en la nube, lo que
  no permite garantizar A-01.
- Bruno importa Postman Collection v2.1 y ambientes de Postman y funciona sin cuenta y sin conexión. Según su
  documentación, los valores secretos se guardan cifrados en un almacén local, fuera del archivo del ambiente;
  A-12 comprobará que no aparecen en el archivo del ambiente.

**Condiciones de uso** (checklist, A-01, A-02 y A-12):

1. Bruno lo instala la persona que ejecuta; se anota su versión en la bitácora.
2. La colección de Bruno se crea en una carpeta **fuera del repositorio**; nunca se abre `tests/e2e/` como
   colección de Bruno. Tras importar, `git status` debe seguir limpio.
3. Antes de ejecutar se comprueba la importación: 10 carpetas y 124 peticiones en orden, autenticación,
   cuerpos (incluido el JSON truncado de TC-HTTP-003), descripciones y ausencia de scripts. Si algo no
   coincide, se registra BLOCKED.
4. Las 20 variables `secret` se marcan como secretas en Bruno antes de introducir ningún valor, y se verifica
   que el archivo del ambiente no contiene los valores.

**Impacto.** Ninguno sobre el backend (F5-H), la colección, el ambiente ni `seed-e2e.sql`; solo se actualiza la
redacción de `fixtures/README.md`. La bitácora registra el cliente en su cabecera. Si la importación en Bruno
no reproduce la colección fielmente, los casos afectados quedan BLOCKED y la desviación se revisa antes de
continuar.

**Aprobación.** Aprobada en la revisión del proyecto el 19 de noviembre de 2026, antes de la ejecución
oficial.

### D-02 — Ejecución manual parcial por el plazo de entrega

**Qué dice el PRD.** El DoD (§24) exige 22/22 rutas probadas manualmente y la ejecución de los escenarios de
seguridad, propiedad, estados y errores.

**Qué se hace.** Por el plazo de entrega (19-11-2026, 17:00), la ejecución oficial se detuvo tras E1 y parte de
E2: 16 de 124 casos en PASS. Los casos no ejecutados quedan en **NOT RUN** en la bitácora; **no se declaran
aprobados** y el DoD no se da por cumplido.

**Qué queda pendiente.** Completar desde el checklist, con un `runId` nuevo y restauración R-B. La colección y el
procedimiento no requieren cambios. Casos en NOT RUN (108 de 124; detalle fila a fila en la bitácora):

| Etapa | NOT RUN | Qué falta |
|---|---|---|
| E2 | 27 | `E2E-USR-01-INACTIVE`; TC-SEC-016 (su `/me`, `E2E-AUTH-02-ROLEINJ`); duplicados TC-BIZ-006a/b y sus comprobaciones; el control previo y TC-AUTH-003; los `/me` de USER_B, STAFF, ADMIN e INACTIVE; los negativos de autenticación (TC-AUTH-004, 005, 007, 008, 009, 012, 013a/b, 014 y 015); `E2E-USR-02`, `E2E-USR-03`, TC-SEC-006 y TC-SEC-007 |
| E3 | 24 | Visibilidad de borradores y ciclo de vida de eventos |
| E4 | 26 | Compra de tickets T1–T5 y propiedad |
| E5 | 19 | Errores 400, 404 y 409 con `ErrorResponse` |
| E6 | 3 | Retirada del rol STAFF |
| E7 | 9 | Bloque JWT con reinicios (incluido TC-AUTH-011) |

También quedan en NOT RUN las acciones A-14 a A-24, A-27 y A-28. Criterios del DoD (§24) que siguen sin cumplirse:
2 (parcial, 8/22 rutas), 4, 5, 6, 7 y 8; ver la tabla del DoD más abajo.

**Aprobación.** Aprobada por la responsable del proyecto el 19 de noviembre de 2026, como decisión de ejecutar
parcialmente las pruebas manuales por el plazo de entrega. **No es una aprobación del DoD** ni equivale a ejecutar
las pruebas pendientes.

## Definition of Done (§24)

| # | Criterio | Estado |
|---|---|---|
| 1 | Colección v2.1 válida e importable, sin scripts | **Cumplido** (validada contra el esquema v2.1 e importada en Bruno sin diferencias) |
| 2 | 22/22 rutas incluidas y probadas manualmente | **Parcial:** 22/22 incluidas; 8/22 probadas (D-02) |
| 3 | Logins de A, B, STAFF y ADMIN; JWT en variables, sin hardcodear | **Cumplido** |
| 4 | Casos 400, 401, 403, 404 y 409 ejecutados | **No cumplido:** definidos en la colección, no ejecutados (D-02) |
| 5 | Visibilidad de borradores y propiedad de usuarios y tickets | **No cumplido** (NOT RUN) |
| 6 | Compra ajena bloqueada y sin mutación | **No cumplido** (NOT RUN) |
| 7 | Estados de eventos y tickets comprobados tras las operaciones | **No cumplido** (NOT RUN) |
| 8 | `ErrorResponse` comprobado | **No cumplido** como caso previsto; solo un 401 incidental |
| 9 | Bitácora con esperado, observado, resultado y observación | **Cumplido** |
| 10 | Variables y evidencias sin contraseñas ni tokens | **Cumplido** |
| 11 | README con preparación, secuencia, copia de variables y comparación | **Cumplido** (este README, `fixtures/README.md` y el checklist) |
| 12 | Defectos y bloqueos registrados; nada aprobado sin ejecutar | **Cumplido** |
| 13 | Matriz requisito → caso → ruta → resultado | **Cumplido** (en la bitácora) |

## Limitaciones conocidas que afectan a E2E

Heredadas de la Fase 4 y documentadas en `backend/README.md`: STAFF tiene alcance global; no hay revocación de
tokens ni límite de intentos de login; la compra en un borrador responde 409 y revela que existe; la diferencia
403/404 revela si existe un código de ticket. TC-AUTH-011 se diseñó con reinicios del servidor en lugar de tokens
construidos offline (F5-F); no llegó a ejecutarse.
