# Checklist de ejecución manual E2E — PulsePass Fase 5

Secuencia oficial E0–E7 aprobada en P5-01 y ampliada en P5-02 a P5-06. Cada casilla es **una acción**: un envío en Bruno (botón Send), un reenvío previsto, un paso SQL o un reinicio de la aplicación. Se ejecutan **en este orden**, sin el Collection Runner de Bruno, Newman, Postman CLI ni scripts (PRD §16.4). El cliente de ejecución es Bruno en lugar de Postman (desviación D-01, `tests/e2e/README.md`); la colección oficial sigue siendo la Postman Collection v2.1 del repositorio, sin cambios.

- **Envío (S):** caso de la colección; su descripción (pestaña Docs en Bruno) detalla precondiciones, autorización, respuesta esperada y revisión manual.
- **Reenvío (R):** volver a enviar una petición existente para comprobar un estado; no es un caso nuevo.
- **Condicional (C):** solo si hace falta (un token con 15 minutos o más). Si no se envía, su fila queda NOT RUN con la observación «no necesario».
- **Acción externa (A-xx):** fuera del cliente de ejecución; se registra en la tabla de acciones de la bitácora.

Reglas que aplican siempre:

- Antes de cada petición protegida, comprobar qué token usa y que no ha caducado (15 minutos). Si caducó, repetir su login con la petición existente y anotarlo como reenvío no previsto.
- Todo resultado empieza en NOT RUN. Un caso con comprobación posterior pendiente sigue en NOT RUN hasta esa comprobación; nunca se marca PASS antes.
- Un FAIL o un 500 inesperado: registrar el defecto y **detenerse** (F5-H). Un prerrequisito fallido: BLOCKED en los casos dependientes, nunca PASS (FR-E2E-015).
- Evidencias solo textuales: ni JWT, ni contraseñas, ni capturas (P5-07-d).
- Prohibido usar el Collection Runner de Bruno y añadir scripts, tests o assertions: cada petición se envía a mano con Send.
- La restauración R-B afecta **exclusivamente** a `pulsepass-e2e-db` y `pulsepass-e2e-data`. Prohibido tocar `pulsepass-db` o su volumen (solo `docker inspect` de lectura).


## E0 — Preparación (antes de cualquier petición)

- [ ] **1.** **A-01** — Cliente de ejecución: **Bruno** (aplicación de escritorio, desviación D-01 de `tests/e2e/README.md`), instalado por la persona que ejecuta. Comprobar que Bruno trabaja **solo en local**, sin iniciar sesión ni crear cuenta y sin sincronizar la colección ni el ambiente con la nube; que la carpeta de la colección de Bruno está **fuera del repositorio** (por ejemplo, `~/PulsePass-E2E-Bruno`); y que los valores secretos se guardarán cifrados en el almacén local de Bruno, fuera del archivo del ambiente (se verifica en A-12). Anotar la versión de Bruno y el modo usado. **Si no puede garantizarse, detenerse aquí, registrar BLOCKED en esta acción y en todos los casos, y documentar el bloqueo.** No introducir ningún secreto antes de cumplir este punto.
- [ ] **2.** **A-02** — En Bruno, importar la colección `tests/e2e/postman/PulsePass-Manual-E2E.postman_collection.json` (Import Collection → Postman Collection) en la carpeta de A-01, **fuera del repositorio**; después, el ambiente `tests/e2e/postman/PulsePass-local.postman_environment.json` (Configure environments → Import environment), y seleccionar `PulsePass-local`. Nunca abrir `tests/e2e/` como colección de Bruno. Comprobar: 10 carpetas (00–09) en orden y 124 peticiones; autenticación de la colección «No Auth» y, en una muestra, la autenticación de peticiones sin token y con Bearer (`E2E-AUTH-02`: `{{userAToken}}`); cuerpos de una muestra y el cuerpo literal de TC-HTTP-003 (`{"username": `, sin corregir); descripciones completas en la pestaña Docs (`E2E-TKT-01`, `TC-AUTH-009`, `TC-AUTH-011b`); ninguna petición con Script, Tests ni Assert; 53 variables en el ambiente, con valor solo `baseUrl`, `expectedIssuer`, `expectedTokenTtlSeconds` y `expectedForeignResourceStatus`; y `git status` limpio en el repositorio (los archivos originales no cambian). Si solo cambia el cuerpo de TC-HTTP-003, anotarlo y registrar BLOCKED en TC-HTTP-003; si falla cualquier otra comprobación o la importación, registrar BLOCKED en esta acción y en todos los casos.
- [ ] **3.** **A-03** — Completar la cabecera de `registro-ejecucion-manual.md` (fecha, ejecutor, commit, versiones).
- [ ] **4.** **A-04** — Anotar el estado de la base de desarrollo, solo lectura (`tests/e2e/fixtures/README.md` §6, punto 3): `docker inspect pulsepass-db --format '{{.State.Status}} {{range .Mounts}}{{.Name}}{{end}}'`. **Prohibido** cualquier otro comando sobre `pulsepass-db` o su volumen.
- [ ] **5.** **A-05** — Restaurar desde cero **solo** la base E2E con R-B (`tests/e2e/fixtures/README.md` §9, pasos 1–5): detener la aplicación si está en marcha; verificar con `docker inspect` que el contenedor es `/pulsepass-e2e-db`, con la etiqueta `com.pulsepass.purpose=e2e`, `POSTGRES_DB=pulsepass_e2e` y el volumen `pulsepass-e2e-data` (etiqueta `e2e`); solo si todo coincide, `docker rm -f pulsepass-e2e-db` (sin `-v`) y `docker volume rm pulsepass-e2e-data`. Si algo no coincide, detenerse. Nunca `prune`, comodines ni `pulsepass-db`.
- [ ] **6.** **A-06** — Generar los secretos de la sesión en la terminal de la aplicación (`tests/e2e/fixtures/README.md` §3). No copiarlos en ningún archivo, en la bitácora ni en Bruno.
- [ ] **7.** **A-07** — Crear el volumen y el contenedor E2E (`tests/e2e/fixtures/README.md` §4) y esperar a `pg_isready`.
- [ ] **8.** **A-08** — Arrancar la aplicación desde `backend/` (`tests/e2e/fixtures/README.md` §5), en la misma terminal.
- [ ] **9.** **A-09** — Comprobaciones previas (`tests/e2e/fixtures/README.md` §6, puntos 1–2): el log muestra `jdbc:postgresql://127.0.0.1:55432/pulsepass_e2e (PostgreSQL 17.x)` y `Successfully applied 4 migrations`; `current_database()` = `pulsepass_e2e`; V1–V4 con `success = t`. Si falla, todo queda BLOCKED (PRD §15.3).
- [ ] **10.** **A-10** — Elegir el `runId` (`^[a-z0-9]{1,20}$`) y rellenar en el ambiente `runId` y los valores derivados (P5-03-c): emails `{actor}.{runId}@example.test` y usernames `{actor}-{runId}` de `userA`, `userB`, `userMinor`, `staff`, `admin`, `inactiveUser` y `roleInjection` (actores `usera`, `userb`, `userminor`, `staff`, `admin`, `inactive` y `roleinjection`); `venueActiveCode` = `E2E-{runId}-V0`; `capacityOneVenueCode` = `E2E-{runId}-V1`; `eventToPublishCode` y `publishedEventCode` = `E2E-{runId}-MAIN`; `draftEventCode` = `E2E-{runId}-DRAFT`; `soldOutEventCode` = `E2E-{runId}-SOLD`; `denyEventCode` = `E2E-{runId}-DENY`. No rellenar aquí `staffIdentifier` ni `adminIdentifier`: son secretas y se rellenan en A-12.
- [ ] **11.** **A-11** — Aplicar el paso `venues` del seed con el `runId` (`tests/e2e/fixtures/README.md` §7). Decidir y anotar si se usa `-v inactive_venue=on` (crea VX): sin VX, TC-BIZ-015d quedará BLOCKED, nunca PASS.
- [ ] **12.** **A-12** — Antes de introducir ningún valor, marcar en Bruno como **secretas** las 20 variables `secret` del ambiente: `adminIdentifier`, `staffIdentifier`, `userAPassword`, `userBPassword`, `userMinorPassword`, `staffPassword`, `adminPassword`, `inactiveUserPassword`, `roleInjectionPassword`, `userAToken`, `userBToken`, `staffToken`, `adminToken`, `userMinorToken`, `inactiveUserToken`, `roleInjectionToken`, `expiredTestToken`, `otherKeyTestToken`, `otherIssuerTestToken` y `tamperedToken`. Después, introducir solo en esas variables secretas `staffIdentifier` y `adminIdentifier` (emails de STAFF y ADMIN) y las contraseñas de los 7 actores (≥ 12 caracteres, ≤ 72 bytes). Abrir el archivo del ambiente que Bruno guarda en la carpeta de A-01 y confirmar que **no** contiene ningún valor secreto (solo los nombres); si lo contiene, borrar el valor, detenerse y registrar BLOCKED. Los tokens que se copien más adelante van siempre a estas variables secretas. Solo después de A-01.
- [ ] **13.** [S] `CONN-01` · GET venues activos (conectividad) — HTTP 200; comprobar: incluye V0 y V1; nunca VX, que es inactivo.

## E1 — Flujo A: descubrimiento público (paso 3)

- [ ] **14.** [S] `E2E-VEN-02` · GET venues activos — HTTP 200; comprobar: solo venues activos; V0 y V1.
- [ ] **15.** [S] `E2E-VEN-01` · GET venue por código — HTTP 200; comprobar: V0 con `capacity = 100`.
- [ ] **16.** [S] `E2E-ART-03` · GET artistas activos — HTTP 200; comprobar: solo artistas activos. Copiar `artistId` y `artistStageName` (p. ej., Solar Beat).
- [ ] **17.** [S] `E2E-ART-01` · GET artista por id — HTTP 200; comprobar: mismo artista.
- [ ] **18.** [S] `E2E-ART-02` · GET artista por stage name — HTTP 200; comprobar: mismo `id` que E2E-ART-01.

## E2 — Flujo B: registro, privilegios, login, /me y autenticación (pasos 4–12)

- [ ] **19.** [S] `E2E-USR-01` · POST registrar USER_A — HTTP 201; comprobar: 201; sin JWT. Anotar el `id` de USER_A.
- [ ] **20.** [S] `E2E-USR-01-B` · POST registrar USER_B — HTTP 201; comprobar: 201.
- [ ] **21.** [S] `E2E-USR-01-STAFF` · POST registrar STAFF — HTTP 201; comprobar: 201.
- [ ] **22.** [S] `E2E-USR-01-ADMIN` · POST registrar ADMIN — HTTP 201; comprobar: 201.
- [ ] **23.** [S] `E2E-USR-01-INACTIVE` · POST registrar INACTIVE_USER — HTTP 201; comprobar: 201.
- [ ] **24.** [S] `TC-SEC-016` · POST registrar ROLE_INJECTION con role=ADMIN inyectado — HTTP 201; comprobar: 201; el rol se comprueba en E2E-AUTH-02-ROLEINJ.
- [ ] **25.** [S] `TC-BIZ-006a` · POST registro con username duplicado — HTTP 409; comprobar: 409; anotar el mensaje observado.
- [ ] **26.** [S] `TC-BIZ-006b` · POST registro con email duplicado — HTTP 409; comprobar: 409; anotar el mensaje observado.
- [ ] **27.** **A-13** — Paso 7: aplicar el paso SQL `promote` con el `runId` (`tests/e2e/fixtures/README.md` §7). STAFF queda solo con STAFF y ADMIN solo con ADMIN.
- [ ] **28.** [S] `E2E-AUTH-01` · POST login USER_A (username) — HTTP 200; comprobar: 200; `expiresIn = 900`. Copiar `accessToken` a `userAToken`.
- [ ] **29.** [R] `E2E-AUTH-02` · GET /me USER_A · control previo de TC-AUTH-003 — HTTP 200; comprobar: 200; anotar el `id` (= el de E2E-USR-01).
- [ ] **30.** [S] `TC-AUTH-003` · POST login USER_A (email) — HTTP 200; comprobar: 200. Copiar `accessToken` a `userAToken` (sobrescribe).
- [ ] **31.** [S] `E2E-AUTH-01-B` · POST login USER_B — HTTP 200; comprobar: 200. Copiar a `userBToken`.
- [ ] **32.** [S] `E2E-AUTH-01-STAFF` · POST login STAFF — HTTP 200; comprobar: 200. Copiar a `staffToken`.
- [ ] **33.** [S] `E2E-AUTH-01-ADMIN` · POST login ADMIN — HTTP 200; comprobar: 200. Copiar a `adminToken`.
- [ ] **34.** [S] `E2E-AUTH-01-ROLEINJ` · POST login ROLE_INJECTION — HTTP 200; comprobar: 200. Copiar a `roleInjectionToken`.
- [ ] **35.** [S] `E2E-AUTH-01-INACTIVE` · POST login INACTIVE_USER — HTTP 200; comprobar: 200. Copiar a `inactiveUserToken` y anotar la hora del login.
- [ ] **36.** [S] `E2E-AUTH-02` · GET /me USER_A — HTTP 200; comprobar: 200; mismo `id` que el control previo (cierra TC-AUTH-003).
- [ ] **37.** [S] `E2E-AUTH-02-B` · GET /me USER_B — HTTP 200; comprobar: `roles = ["USER"]`.
- [ ] **38.** [S] `E2E-AUTH-02-STAFF` · GET /me STAFF — HTTP 200; comprobar: `roles = ["STAFF"]`.
- [ ] **39.** [S] `E2E-AUTH-02-ADMIN` · GET /me ADMIN — HTTP 200; comprobar: `roles = ["ADMIN"]`.
- [ ] **40.** [S] `E2E-AUTH-02-ROLEINJ` · GET /me ROLE_INJECTION — HTTP 200; comprobar: `roles = ["USER"]` (cierra TC-SEC-016; ADMIN sería defecto).
- [ ] **41.** [S] `E2E-AUTH-02-INACTIVE` · GET /me INACTIVE_USER — HTTP 200; comprobar: 200; control de TC-AUTH-013.
- [ ] **42.** [S] `TC-BIZ-006a-CHK` · GET by-email del email nuevo (ADMIN) — HTTP 404; comprobar: 404 (cierra TC-BIZ-006a).
- [ ] **43.** [S] `TC-BIZ-006b-CHK` · GET by-username del username nuevo (ADMIN) — HTTP 404; comprobar: 404 (cierra TC-BIZ-006b).
- [ ] **44.** [S] `TC-AUTH-004` · POST login con contraseña incorrecta — HTTP 401; comprobar: 401 «Invalid credentials».
- [ ] **45.** [S] `TC-AUTH-005` · POST login con identificador inexistente — HTTP 401; comprobar: 401 idéntico a TC-AUTH-004.
- [ ] **46.** [S] `TC-AUTH-007` · GET /me sin Authorization — HTTP 401; comprobar: 401.
- [ ] **47.** [S] `TC-AUTH-008` · GET /me con Bearer malformado — HTTP 401; comprobar: 401.
- [ ] **48.** **A-14** — Preparar `tamperedToken` como indica la descripción de TC-AUTH-009 (un carácter del segundo segmento, que no sea el último, por otro distinto de base64url; sin añadir ni quitar caracteres ni puntos). Solo valor local.
- [ ] **49.** [S] `TC-AUTH-009` · GET /me con JWT alterado — HTTP 401; comprobar: 401.
- [ ] **50.** [S] `TC-AUTH-012` · GET /me con token en query string — HTTP 401; comprobar: 401. No capturar la URL resuelta: contiene el token.
- [ ] **51.** [S] `TC-AUTH-014` · POST crear evento con token USER — HTTP 403; comprobar: 403. Resultado: NOT RUN «pendiente de TC-SEC-001-CHK».
- [ ] **52.** [S] `TC-AUTH-015` · GET ruta pública con Bearer inválido — HTTP 401; comprobar: 401.
- [ ] **53.** [C] `E2E-AUTH-01-INACTIVE` · POST login INACTIVE_USER · renovación si el token de INACTIVE tiene ≥ 15 min (TC-AUTH-013a) — HTTP 200; comprobar: 200; copiar a `inactiveUserToken` y anotar la hora.
- [ ] **54.** [C] `E2E-AUTH-02-INACTIVE` · GET /me INACTIVE_USER · control de la renovación — HTTP 200; comprobar: 200.
- [ ] **55.** **A-15** — Paso 11: aplicar el paso SQL `deactivate` (`tests/e2e/fixtures/README.md` §7), con el token de INACTIVE emitido hace menos de 15 minutos.
- [ ] **56.** [S] `TC-AUTH-013a` · GET /me con el token del usuario desactivado — HTTP 401; comprobar: 401 con el token anterior; anotar el mensaje.
- [ ] **57.** [S] `TC-AUTH-013b` · POST login del usuario desactivado — HTTP 401; comprobar: 401 genérico.
- [ ] **58.** [S] `E2E-USR-02` · GET usuario por email (propio) — HTTP 200; comprobar: 200; email de A.
- [ ] **59.** [S] `E2E-USR-03` · GET usuario por username (propio) — HTTP 200; comprobar: 200; username de A.
- [ ] **60.** [S] `TC-SEC-006` · GET usuario A por email (USER_B) — HTTP 403; comprobar: 403.
- [ ] **61.** [S] `TC-SEC-007` · GET usuario A por username (USER_B) — HTTP 403; comprobar: 403.

## E3 — Flujo C: catálogo (pasos 13–21)

- [ ] **62.** [S] `TC-SEC-001-ANON` · POST crear evento sin token — HTTP 401; comprobar: 401. Resultado: NOT RUN «pendiente de TC-SEC-001-CHK».
- [ ] **63.** [S] `TC-SEC-001-USER` · POST crear evento con token USER — HTTP 403; comprobar: 403. Resultado: NOT RUN «pendiente de TC-SEC-001-CHK».
- [ ] **64.** [S] `TC-SEC-001-CHK` · GET evento DENY (ADMIN) — HTTP 404; comprobar: 404. Con este 404 se cierran TC-SEC-001-ANON, TC-SEC-001-USER y TC-AUTH-014.
- [ ] **65.** [S] `E2E-EVT-01` · POST crear evento MAIN (ADMIN) — HTTP 201; comprobar: 201; `status = DRAFT`, `minimumAge = 18`.
- [ ] **66.** [S] `E2E-EVT-02-PRE` · GET MAIN antes de publicar (anónimo) — HTTP 404; comprobar: 404.
- [ ] **67.** [S] `E2E-EVT-03-PRE` · GET publicados antes de publicar MAIN — HTTP 200; comprobar: sin MAIN.
- [ ] **68.** [S] `TC-SEC-003` · POST asociar artista a MAIN (USER_A) — HTTP 403; comprobar: 403. Resultado: NOT RUN «pendiente de E2E-EVT-02-ADMIN».
- [ ] **69.** [S] `E2E-EVT-02-ADMIN` · GET MAIN (ADMIN) — HTTP 200; comprobar: DRAFT, `artists = []` (cierra TC-SEC-003). Estado del paso 15.
- [ ] **70.** [S] `E2E-EVT-05` · POST asociar artista a MAIN (ADMIN) — HTTP 200; comprobar: 200; con el artista.
- [ ] **71.** [R] `E2E-EVT-02-ADMIN` · GET MAIN (ADMIN) · estado del paso 16 — HTTP 200; comprobar: DRAFT, con el artista.
- [ ] **72.** [S] `TC-SEC-002-USER` · PATCH publicar MAIN (USER_A) — HTTP 403; comprobar: 403. Resultado: NOT RUN «pendiente del reenvío de E2E-EVT-02-ADMIN».
- [ ] **73.** [S] `TC-SEC-002-STAFF` · PATCH publicar MAIN (STAFF) — HTTP 403; comprobar: 403. Resultado: NOT RUN «pendiente del reenvío de E2E-EVT-02-ADMIN».
- [ ] **74.** [R] `E2E-EVT-02-ADMIN` · GET MAIN (ADMIN) · estado del paso 17 — HTTP 200; comprobar: sigue DRAFT, con el artista (cierra TC-SEC-002).
- [ ] **75.** [S] `E2E-EVT-04` · PATCH publicar MAIN (ADMIN) — HTTP 200; comprobar: 200; `status = PUBLISHED`.
- [ ] **76.** [S] `E2E-EVT-02` · GET evento publicado por código — HTTP 200; comprobar: 200 sin token; PUBLISHED, con el artista.
- [ ] **77.** [S] `E2E-EVT-03` · GET eventos publicados — HTTP 200; comprobar: incluye MAIN.
- [ ] **78.** [S] `E2E-EVT-06` · GET eventos por artista — HTTP 200; comprobar: incluye MAIN.
- [ ] **79.** [S] `E2E-EVT-04-REPEAT` · PATCH volver a publicar MAIN (ADMIN) — HTTP 409; comprobar: 409.
- [ ] **80.** [S] `E2E-EVT-01-DRAFT` · POST crear evento DRAFT (ADMIN) — HTTP 201; comprobar: 201; DRAFT.
- [ ] **81.** [S] `E2E-EVT-05-DRAFT` · POST asociar artista a DRAFT (ADMIN) — HTTP 200; comprobar: 200.
- [ ] **82.** [S] `E2E-EVT-05-DRAFT-REPEAT` · POST repetir asociación (ADMIN) — HTTP 409; comprobar: 409.
- [ ] **83.** [S] `TC-SEC-015` · GET DRAFT (ADMIN) — HTTP 200; comprobar: 200; DRAFT, el artista una sola vez.
- [ ] **84.** [S] `TC-SEC-004` · GET DRAFT (anónimo) — HTTP 404; comprobar: 404.
- [ ] **85.** [S] `TC-SEC-005` · GET eventos por artista (anónimo) — HTTP 200; comprobar: incluye MAIN y no DRAFT.
- [ ] **86.** [S] `E2E-EVT-01-SOLD` · POST crear evento SOLD (ADMIN) — HTTP 201; comprobar: 201; V1.
- [ ] **87.** [S] `E2E-EVT-04-SOLD` · PATCH publicar SOLD (ADMIN) — HTTP 200; comprobar: 200; PUBLISHED.

## E4 — Flujo D: compra, propiedad y transiciones (pasos 22–34)

- [ ] **88.** [S] `E2E-TKT-01` · POST comprar ticket (USER_A, MAIN) — HTTP 201; comprobar: 201; PAID. Envío 1 de 4: copiar `ticketCode` a `ticketCancelableCode` (T1).
- [ ] **89.** [S] `TC-SEC-008` · POST comprar con el email de USER_B (USER_A) — HTTP 403; comprobar: 403. Resultado: NOT RUN «pendiente de TC-SEC-008-CHK».
- [ ] **90.** [S] `TC-SEC-008-CHK` · GET tickets de USER_B (ADMIN) — HTTP 200; comprobar: `[]` (cierra TC-SEC-008). Estado del paso 22.
- [ ] **91.** [R] `E2E-TKT-01` · POST comprar ticket (USER_A, MAIN) · envío 2 de 4 — HTTP 201; comprobar: 201; copiar a `ticketUsableCode` (T2).
- [ ] **92.** [R] `E2E-TKT-01` · POST comprar ticket (USER_A, MAIN) · envío 3 de 4 — HTTP 201; comprobar: 201; copiar a `ticket3Code` (T3).
- [ ] **93.** [R] `E2E-TKT-01` · POST comprar ticket (USER_A, MAIN) · envío 4 de 4 (no enviar más) — HTTP 201; comprobar: 201; copiar a `ticket4Code` (T4).
- [ ] **94.** [S] `E2E-TKT-02` · GET ticket por código (propietario) — HTTP 200; comprobar: T1 `PAID`.
- [ ] **95.** [S] `E2E-TKT-03` · GET tickets por usuario (propio) — HTTP 200; comprobar: T1–T4, todos PAID.
- [ ] **96.** [S] `TC-SEC-009` · GET ticket T1 de USER_A (USER_B) — HTTP 403; comprobar: 403.
- [ ] **97.** [S] `TC-SEC-010` · GET tickets de USER_A (USER_B) — HTTP 403; comprobar: 403.
- [ ] **98.** [S] `TC-SEC-011` · PATCH cancelar T1 de USER_A (USER_B) — HTTP 403; comprobar: 403. Resultado: NOT RUN «pendiente del reenvío de E2E-TKT-02».
- [ ] **99.** [R] `E2E-TKT-02` · GET ticket por código (propietario) · integridad de TC-SEC-011 (paso 25) — HTTP 200; comprobar: T1 sigue `PAID` (cierra TC-SEC-011).
- [ ] **100.** [S] `TC-SEC-013` · GET tickets pagados de MAIN (USER_A) — HTTP 403; comprobar: 403.
- [ ] **101.** [S] `E2E-TKT-05` · PATCH cancelar ticket (propietario) — HTTP 200; comprobar: 200; CANCELLED. Resultado: NOT RUN «pendiente del reenvío de E2E-TKT-02».
- [ ] **102.** [R] `E2E-TKT-02` · GET ticket por código (propietario) · relectura tras cancelar (paso 27) — HTTP 200; comprobar: T1 `CANCELLED` (cierra E2E-TKT-05).
- [ ] **103.** [S] `TC-BIZ-011` · PATCH cancelar T1 otra vez (USER_A) — HTTP 409; comprobar: 409; T1 sigue CANCELLED.
- [ ] **104.** [S] `E2E-TKT-04` · GET tickets pagados del evento (STAFF) — HTTP 200; comprobar: T2, T3 y T4; sin T1. Punto de control del paso 28: después de cancelar T1, antes de cualquier uso; STAFF conserva su rol.
- [ ] **105.** [S] `TC-SEC-012` · PATCH usar T2 de USER_A (USER_B) — HTTP 403; comprobar: 403. Resultado: NOT RUN «pendiente de E2E-TKT-02-T2».
- [ ] **106.** [S] `E2E-TKT-02-T2` · GET ticket T2 (propietario) — HTTP 200; comprobar: T2 `PAID` (cierra TC-SEC-012). Estado del paso 29.
- [ ] **107.** [S] `E2E-TKT-06` · PATCH marcar ticket como usado (STAFF) — HTTP 200; comprobar: 200; USED. Resultado: NOT RUN «pendiente del reenvío de E2E-TKT-02-T2».
- [ ] **108.** [R] `E2E-TKT-02-T2` · GET ticket T2 (propietario) · estado del paso 30 — HTTP 200; comprobar: T2 `USED` (cierra E2E-TKT-06).
- [ ] **109.** [S] `TC-BIZ-012` · PATCH usar T4 (STAFF) — HTTP 200; comprobar: 200; T4 USED.
- [ ] **110.** [S] `TC-BIZ-013` · PATCH usar T4 otra vez (STAFF) — HTTP 409; comprobar: 409; T4 sigue USED.
- [ ] **111.** [S] `TC-BIZ-014-DRAFT` · POST comprar en DRAFT (USER_A) — HTTP 409; comprobar: 409. Resultado: NOT RUN «pendiente del reenvío de E2E-TKT-03».
- [ ] **112.** [R] `E2E-TKT-03` · GET tickets por usuario (propio) · integridad de TC-BIZ-014-DRAFT (paso 32) — HTTP 200; comprobar: los mismos 4 tickets: T1 CANCELLED, T2 USED, T3 PAID, T4 USED (cierra TC-BIZ-014-DRAFT).
- [ ] **113.** [S] `E2E-TKT-01-SOLD` · POST comprar en SOLD → T5 (USER_A) — HTTP 201; comprobar: 201; PAID. Un solo envío: copiar `ticketCode` a `ticket5Code` (T5). Resultado: NOT RUN «pendiente de E2E-EVT-02-SOLD-ADMIN».
- [ ] **114.** [S] `TC-BIZ-014-SOLDOUT` · POST comprar en SOLD agotado (USER_B) — HTTP 409; comprobar: 409. Resultado: NOT RUN «pendiente del reenvío de TC-SEC-008-CHK».
- [ ] **115.** [R] `TC-SEC-008-CHK` · GET tickets de USER_B (ADMIN) · estado del paso 33 — HTTP 200; comprobar: sigue `[]` (cierra TC-BIZ-014-SOLDOUT).
- [ ] **116.** [S] `E2E-EVT-02-SOLD-ANON` · GET SOLD agotado (anónimo) — HTTP 404; comprobar: 404.
- [ ] **117.** [S] `E2E-EVT-02-SOLD-ADMIN` · GET SOLD agotado (ADMIN) — HTTP 200; comprobar: `status = SOLD_OUT` (cierra E2E-TKT-01-SOLD).
- [ ] **118.** [S] `E2E-USR-01-MINOR` · POST registrar USER_MINOR — HTTP 201; comprobar: 201; `birthDate = 2013-06-01`.
- [ ] **119.** [S] `E2E-AUTH-01-MINOR` · POST login USER_MINOR — HTTP 200; comprobar: 200. Copiar a `userMinorToken`.
- [ ] **120.** [S] `TC-BIZ-014-AGE` · POST comprar en MAIN sin edad mínima (USER_MINOR) — HTTP 409; comprobar: 409. Resultado: NOT RUN «pendiente de E2E-TKT-03-MINOR».
- [ ] **121.** [S] `E2E-TKT-03-MINOR` · GET tickets propios (USER_MINOR) — HTTP 200; comprobar: `[]` (cierra TC-BIZ-014-AGE).

## E5 — Regresión de errores (paso 35)

- [ ] **122.** [S] `TC-HTTP-001` · POST registro con cuerpo vacío — HTTP 400; comprobar: 400.
- [ ] **123.** [S] `TC-HTTP-002` · POST registro con email mal formado — HTTP 400; comprobar: 400.
- [ ] **124.** [S] `TC-HTTP-003` · POST registro con JSON inválido — HTTP 400; comprobar: 400. No corregir el cuerpo truncado.
- [ ] **125.** [S] `TC-HTTP-004` · POST crear evento con categoría inválida (ADMIN) — HTTP 400; comprobar: 400. Resultado: NOT RUN «pendiente de TC-BIZ-015-CHK».
- [ ] **126.** [S] `TC-HTTP-005` · POST crear evento sin campos obligatorios (ADMIN) — HTTP 400; comprobar: 400. Resultado: NOT RUN «pendiente de TC-BIZ-015-CHK».
- [ ] **127.** [S] `TC-HTTP-006` · GET artista por nombre sin parámetro — HTTP 400; comprobar: 400.
- [ ] **128.** [S] `TC-HTTP-007` · GET artista con id no numérico — HTTP 400; comprobar: 400.
- [ ] **129.** [S] `TC-HTTP-008` · GET venue inexistente — HTTP 404; comprobar: 404.
- [ ] **130.** [S] `TC-HTTP-009` · GET artista inexistente por id — HTTP 404; comprobar: 404.
- [ ] **131.** [S] `TC-HTTP-010` · GET artista inexistente por nombre — HTTP 404; comprobar: 404.
- [ ] **132.** [S] `TC-HTTP-011` · GET evento inexistente (ADMIN) — HTTP 404; comprobar: 404.
- [ ] **133.** [S] `TC-HTTP-012` · GET ticket inexistente (USER_A) — HTTP 404; comprobar: 404 (no 403).
- [ ] **134.** [S] `TC-HTTP-013` · POST asociar artista inexistente a MAIN (ADMIN) — HTTP 404; comprobar: 404. Resultado: NOT RUN «pendiente del reenvío de E2E-EVT-02».
- [ ] **135.** [R] `E2E-EVT-02` · GET evento publicado por código · integridad de TC-HTTP-013 — HTTP 200; comprobar: MAIN PUBLISHED con el mismo artista (cierra TC-HTTP-013).
- [ ] **136.** [S] `TC-HTTP-014` · POST comprar en evento inexistente (USER_A) — HTTP 404; comprobar: 404.
- [ ] **137.** [S] `TC-BIZ-015a` · POST crear evento con código duplicado (ADMIN) — HTTP 409; comprobar: 409. Resultado: NOT RUN «pendiente del reenvío de E2E-EVT-02».
- [ ] **138.** [R] `E2E-EVT-02` · GET evento publicado por código · integridad de TC-BIZ-015a — HTTP 200; comprobar: MAIN sigue PUBLISHED, sin cambios (cierra TC-BIZ-015a).
- [ ] **139.** [S] `TC-BIZ-015b` · POST crear evento con fecha pasada (ADMIN) — HTTP 409; comprobar: 409. Resultado: NOT RUN «pendiente de TC-BIZ-015-CHK».
- [ ] **140.** [S] `TC-BIZ-015c` · POST crear evento en venue inexistente (ADMIN) — HTTP 404; comprobar: 404. Resultado: NOT RUN «pendiente de TC-BIZ-015-CHK».
- [ ] **141.** [S] `TC-BIZ-015d` · POST crear evento en venue inactivo (ADMIN) — HTTP 409; comprobar: 409. Solo si A-11 creó VX; si no, BLOCKED sin enviar (nunca PASS). Si se envía: NOT RUN «pendiente de TC-BIZ-015-CHK».
- [ ] **142.** [S] `TC-BIZ-015-CHK` · GET evento BAD (ADMIN) — HTTP 404; comprobar: 404. Cierra TC-HTTP-004, TC-HTTP-005 y TC-BIZ-015b/c/d.

## E6 — Retirada del rol STAFF (paso 36): último uso de STAFF

- [ ] **143.** [C] `E2E-AUTH-01-STAFF` · POST login STAFF · renovación si `staffToken` tiene ≥ 15 min (antes del SQL) — HTTP 200; comprobar: 200; copiar a `staffToken`.
- [ ] **144.** [C] `E2E-AUTH-02-STAFF` · GET /me STAFF · control de la renovación — HTTP 200; comprobar: `roles = ["STAFF"]`.
- [ ] **145.** **A-16** — Aplicar el paso SQL `revoke-staff` (`tests/e2e/fixtures/README.md` §7). No volver a hacer login de STAFF.
- [ ] **146.** [S] `TC-SEC-017-ME` · GET /me STAFF sin rol (token anterior) — HTTP 200; comprobar: 200; `roles = []`.
- [ ] **147.** [S] `TC-SEC-017` · PATCH usar T3 con el token anterior de STAFF — HTTP 403; comprobar: 403. Resultado: NOT RUN «pendiente de TC-SEC-017-CHK».
- [ ] **148.** [S] `TC-SEC-017-CHK` · GET ticket T3 (ADMIN) — HTTP 200; comprobar: T3 sigue `PAID` (cierra TC-SEC-017).

## E7 — Bloque JWT (paso 37): solo USER_A y seis reinicios

- [ ] **149.** **A-17** — Reinicio a (1 de 6): `PULSEPASS_JWT_ACCESS_TOKEN_TTL=PT1M ./mvnw spring-boot:run`, como indica TC-AUTH-010-LOGIN; comprobar con CONN-01 (200).
- [ ] **150.** [S] `TC-AUTH-010-LOGIN` · POST login USER_A con TTL de 1 minuto — HTTP 200; comprobar: 200; `expiresIn = 60`. Copiar a `expiredTestToken` y anotar la hora.
- [ ] **151.** [S] `TC-AUTH-010-CTRL` · GET /me con el token de 1 minuto (vigente) — HTTP 200; comprobar: 200. Inmediatamente.
- [ ] **152.** **A-18** — Esperar al menos 70 segundos desde el login de TC-AUTH-010-LOGIN (anotar la hora).
- [ ] **153.** [S] `TC-AUTH-010` · GET /me con el token caducado — HTTP 401; comprobar: 401.
- [ ] **154.** **A-19** — Reinicio e (2 de 6): `./mvnw spring-boot:run` (configuración normal: K1, PT15M, `pulsepass-api`); comprobar con CONN-01 (200).
- [ ] **155.** [S] `TC-AUTH-011a-LOGIN` · POST login USER_A antes de cambiar la clave — HTTP 200; comprobar: 200; `expiresIn = 900`. Copiar a `otherKeyTestToken`.
- [ ] **156.** [S] `TC-AUTH-011a-CTRL` · GET /me con el token firmado con K1 — HTTP 200; comprobar: 200.
- [ ] **157.** **A-20** — Reinicio g (3 de 6): `PULSEPASS_JWT_SECRET_BASE64="$(openssl rand -base64 32)" ./mvnw spring-boot:run` (K2 en línea: no se exporta ni se guarda); comprobar con CONN-01 (200).
- [ ] **158.** [S] `TC-AUTH-011a` · GET /me tras cambiar la clave — HTTP 401; comprobar: 401. Resultado: NOT RUN «pendiente del control i».
- [ ] **159.** [R] `E2E-AUTH-01` · POST login USER_A (username) · control i (con K2) — HTTP 200; comprobar: 200; copiar el nuevo `accessToken` a `userAToken`.
- [ ] **160.** [R] `E2E-AUTH-02` · GET /me USER_A · control i (con K2) — HTTP 200; comprobar: 200 (cierra TC-AUTH-011a).
- [ ] **161.** **A-21** — Reinicio j (4 de 6): `./mvnw spring-boot:run` (vuelve K1); comprobar con CONN-01 (200).
- [ ] **162.** [S] `TC-AUTH-011b-LOGIN` · POST login USER_A antes de cambiar el emisor — HTTP 200; comprobar: 200; `expiresIn = 900`. Copiar a `otherIssuerTestToken`.
- [ ] **163.** [S] `TC-AUTH-011b-CTRL` · GET /me con el token del emisor por defecto — HTTP 200; comprobar: 200.
- [ ] **164.** **A-22** — Reinicio l (5 de 6): `PULSEPASS_JWT_ISSUER=pulsepass-e2e-alt ./mvnw spring-boot:run` (misma K1); comprobar con CONN-01 (200).
- [ ] **165.** [S] `TC-AUTH-011b` · GET /me tras cambiar el emisor — HTTP 401; comprobar: 401. Resultado: NOT RUN «pendiente del control n».
- [ ] **166.** [R] `E2E-AUTH-01` · POST login USER_A (username) · control n (emisor alternativo) — HTTP 200; comprobar: 200; copiar el nuevo `accessToken` a `userAToken`.
- [ ] **167.** [R] `E2E-AUTH-02` · GET /me USER_A · control n (emisor alternativo) — HTTP 200; comprobar: 200 (cierra TC-AUTH-011b).
- [ ] **168.** **A-23** — Reinicio o (6 de 6): `./mvnw spring-boot:run` para volver a la configuración normal de la sesión.
- [ ] **169.** **A-24** — Vaciar a mano los valores locales de `expiredTestToken`, `otherKeyTestToken` y `otherIssuerTestToken`.

## Cierre de la sesión

- [ ] **170.** **A-25** — Repetir la lectura de A-04 sobre `pulsepass-db` (solo `docker inspect`): el estado y el volumen deben coincidir.
- [ ] **171.** **A-26** — Detener la aplicación (Ctrl+C) y la base E2E sin borrarla (`tests/e2e/fixtures/README.md` §10).
- [ ] **172.** **A-27** — Comprobar que no quedan tokens ni contraseñas en archivos versionables (`git status`); no exportar el ambiente con valores; vaciar los valores locales de contraseñas y tokens si el equipo es compartido.
- [ ] **173.** **A-28** — Consolidar la bitácora (recuentos, cobertura nominal, cobertura de seguridad y matriz) y entregarla para la consolidación de P5-08.

Fin del orden E0–E7. Consolidar en `registro-ejecucion-manual.md`.
