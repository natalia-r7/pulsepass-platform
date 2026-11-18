# Registro de ejecución manual E2E — PulsePass Fase 5

Bitácora de la ejecución oficial en Postman (PRD §13.4, §16.2 y §16.3). Se rellena **a mano** después de cada Send, siguiendo `checklist-e2e-manual.md`. **Todos los resultados empiezan en NOT RUN.** No incluir JWT, contraseñas, secretos ni capturas; solo datos no sensibles (códigos, ids, estados, mensajes).

## Cabecera de la ejecución

| Campo | Valor |
|---|---|
| Fecha | pendiente |
| Ejecutor | pendiente |
| Commit del repositorio | pendiente |
| `runId` | pendiente |
| Postman (versión y modo local sin sincronización, A-01) | pendiente |
| PostgreSQL (versión observada en A-09) | pendiente |
| Java | pendiente |
| Restauración R-B aplicada (A-05) | pendiente |
| VX creado (`inactive_venue=on`, A-11) | pendiente |
| Estado de `pulsepass-db` antes (A-04) y después (A-25) | pendiente |

## Criterios de resultado

- **PASS:** HTTP, JSON y condición de negocio o seguridad coinciden con lo esperado, incluida la comprobación posterior cuando la hay.
- **FAIL:** cualquier diferencia, o un 500 inesperado. Registrar el defecto y detenerse (F5-H).
- **BLOCKED:** falta un fixture, actor, token o respuesta previa. Los casos dependientes también quedan BLOCKED, nunca PASS.
- **NOT RUN:** estado inicial y de todo lo no ejecutado. Un caso con comprobación posterior pendiente sigue en NOT RUN con la observación «pendiente de …» hasta ejecutarla.

Las filas de tipo **Reenvío** y **Condicional** son evidencia del caso que verifican: no se cuentan en los totales de casos.

## Acciones fuera de Postman

| Orden | Acción | Descripción (ver checklist) | Hora | Resultado | Observación |
|---|---|---|---|---|---|
| 1 | A-01 | Postman solo en local, sin sincronización (si no, detenerse: BLOCKED) | pendiente | NOT RUN | pendiente |
| 2 | A-02 | Importar la colección y el ambiente; seleccionar `PulsePass-local` | pendiente | NOT RUN | pendiente |
| 3 | A-03 | Completar la cabecera de la bitácora | pendiente | NOT RUN | pendiente |
| 4 | A-04 | Anotar el estado de `pulsepass-db` (solo `docker inspect`) | pendiente | NOT RUN | pendiente |
| 5 | A-05 | R-B: restaurar solo `pulsepass-e2e-db` y `pulsepass-e2e-data` | pendiente | NOT RUN | pendiente |
| 6 | A-06 | Generar los secretos de la sesión (solo en la terminal) | pendiente | NOT RUN | pendiente |
| 7 | A-07 | Crear el volumen y el contenedor E2E | pendiente | NOT RUN | pendiente |
| 8 | A-08 | Arrancar la aplicación | pendiente | NOT RUN | pendiente |
| 9 | A-09 | Comprobaciones previas (Flyway, base y versión) | pendiente | NOT RUN | pendiente |
| 10 | A-10 | Elegir el `runId` y rellenar los valores derivados | pendiente | NOT RUN | pendiente |
| 11 | A-11 | Seed `venues` (anotar si se usa `inactive_venue=on`) | pendiente | NOT RUN | pendiente |
| 12 | A-12 | Contraseñas solo como valores locales | pendiente | NOT RUN | pendiente |
| 27 | A-13 | SQL `promote` (paso 7) | pendiente | NOT RUN | pendiente |
| 48 | A-14 | Preparar `tamperedToken` (TC-AUTH-009) | pendiente | NOT RUN | pendiente |
| 55 | A-15 | SQL `deactivate` (paso 11) | pendiente | NOT RUN | pendiente |
| 145 | A-16 | SQL `revoke-staff` (paso 36) | pendiente | NOT RUN | pendiente |
| 149 | A-17 | Reinicio a (1 de 6): TTL PT1M | pendiente | NOT RUN | pendiente |
| 152 | A-18 | Esperar al menos 70 segundos | pendiente | NOT RUN | pendiente |
| 154 | A-19 | Reinicio e (2 de 6): configuración normal | pendiente | NOT RUN | pendiente |
| 157 | A-20 | Reinicio g (3 de 6): K2 en línea | pendiente | NOT RUN | pendiente |
| 161 | A-21 | Reinicio j (4 de 6): configuración normal (K1) | pendiente | NOT RUN | pendiente |
| 164 | A-22 | Reinicio l (5 de 6): emisor `pulsepass-e2e-alt` | pendiente | NOT RUN | pendiente |
| 168 | A-23 | Reinicio o (6 de 6): configuración normal | pendiente | NOT RUN | pendiente |
| 169 | A-24 | Vaciar los tres tokens de prueba | pendiente | NOT RUN | pendiente |
| 170 | A-25 | Comprobar que `pulsepass-db` no cambió | pendiente | NOT RUN | pendiente |
| 171 | A-26 | Detener la aplicación y la base E2E (sin borrarla) | pendiente | NOT RUN | pendiente |
| 172 | A-27 | Sin secretos en archivos versionables | pendiente | NOT RUN | pendiente |
| 173 | A-28 | Consolidar la bitácora | pendiente | NOT RUN | pendiente |

## Bitácora de envíos

| # | Tipo | Caso | Paso | Actor | Método y ruta | Datos no sensibles | HTTP esperado | Qué comprobar | Fecha y hora | HTTP observado | JSON / estado observado | Resultado | Evidencia / observación |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 13 | Caso | `CONN-01` | E0, paso 2 | ANONYMOUS | `GET /api/venues/active` | pendiente | 200 | incluye V0 y V1; nunca VX, que es inactivo | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 14 | Caso | `E2E-VEN-02` | E1, paso 3 | ANONYMOUS | `GET /api/venues/active` | pendiente | 200 | solo venues activos; V0 y V1 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 15 | Caso | `E2E-VEN-01` | E1, paso 3 | ANONYMOUS | `GET /api/venues/{{venueActiveCode}}` | pendiente | 200 | V0 con `capacity = 100` | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 16 | Caso | `E2E-ART-03` | E1, paso 3 | ANONYMOUS | `GET /api/artists/active` | pendiente | 200 | solo artistas activos | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 17 | Caso | `E2E-ART-01` | E1, paso 3 | ANONYMOUS | `GET /api/artists/{{artistId}}` | pendiente | 200 | mismo artista | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 18 | Caso | `E2E-ART-02` | E1, paso 3 | ANONYMOUS | `GET /api/artists/by-stage-name?stageName={{artistStageName}}` | pendiente | 200 | mismo `id` que E2E-ART-01 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 19 | Caso | `E2E-USR-01` | E2, paso 4 | ANONYMOUS | `POST /api/users` | pendiente | 201 | 201; sin JWT | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 20 | Caso | `E2E-USR-01-B` | E2, paso 4 | ANONYMOUS | `POST /api/users` | pendiente | 201 | 201 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 21 | Caso | `E2E-USR-01-STAFF` | E2, paso 4 | ANONYMOUS | `POST /api/users` | pendiente | 201 | 201 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 22 | Caso | `E2E-USR-01-ADMIN` | E2, paso 4 | ANONYMOUS | `POST /api/users` | pendiente | 201 | 201 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 23 | Caso | `E2E-USR-01-INACTIVE` | E2, paso 4 | ANONYMOUS | `POST /api/users` | pendiente | 201 | 201 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 24 | Caso | `TC-SEC-016` | E2, paso 5 | ANONYMOUS | `POST /api/users` | pendiente | 201 | 201; el rol se comprueba en E2E-AUTH-02-ROLEINJ | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 25 | Caso | `TC-BIZ-006a` | E2, paso 6 | ANONYMOUS | `POST /api/users` | pendiente | 409 | 409; anotar el mensaje observado | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 26 | Caso | `TC-BIZ-006b` | E2, paso 6 | ANONYMOUS | `POST /api/users` | pendiente | 409 | 409; anotar el mensaje observado | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 28 | Caso | `E2E-AUTH-01` | E2, paso 8 | ANONYMOUS | `POST /api/auth/login` | pendiente | 200 | 200; `expiresIn = 900` | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 29 | Reenvío | `E2E-AUTH-02` · control previo de TC-AUTH-003 | E2, paso 8 | USER_A | `GET /api/auth/me` | pendiente | 200 | 200; anotar el `id` (= el de E2E-USR-01) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 30 | Caso | `TC-AUTH-003` | E2, paso 8 | ANONYMOUS | `POST /api/auth/login` | pendiente | 200 | 200 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 31 | Caso | `E2E-AUTH-01-B` | E2, paso 8 | ANONYMOUS | `POST /api/auth/login` | pendiente | 200 | 200 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 32 | Caso | `E2E-AUTH-01-STAFF` | E2, paso 8 | ANONYMOUS | `POST /api/auth/login` | pendiente | 200 | 200 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 33 | Caso | `E2E-AUTH-01-ADMIN` | E2, paso 8 | ANONYMOUS | `POST /api/auth/login` | pendiente | 200 | 200 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 34 | Caso | `E2E-AUTH-01-ROLEINJ` | E2, paso 8 | ANONYMOUS | `POST /api/auth/login` | pendiente | 200 | 200 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 35 | Caso | `E2E-AUTH-01-INACTIVE` | E2, paso 8 | ANONYMOUS | `POST /api/auth/login` | pendiente | 200 | 200 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 36 | Caso | `E2E-AUTH-02` | E2, paso 9 | USER_A | `GET /api/auth/me` | pendiente | 200 | 200; mismo `id` que el control previo (cierra TC-AUTH-003) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 37 | Caso | `E2E-AUTH-02-B` | E2, paso 9 | USER_B | `GET /api/auth/me` | pendiente | 200 | `roles = ["USER"]` | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 38 | Caso | `E2E-AUTH-02-STAFF` | E2, paso 9 | STAFF | `GET /api/auth/me` | pendiente | 200 | `roles = ["STAFF"]` | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 39 | Caso | `E2E-AUTH-02-ADMIN` | E2, paso 9 | ADMIN | `GET /api/auth/me` | pendiente | 200 | `roles = ["ADMIN"]` | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 40 | Caso | `E2E-AUTH-02-ROLEINJ` | E2, paso 9 | ROLE_INJECTION | `GET /api/auth/me` | pendiente | 200 | `roles = ["USER"]` (cierra TC-SEC-016; ADMIN sería defecto) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 41 | Caso | `E2E-AUTH-02-INACTIVE` | E2, paso 9 | INACTIVE_USER | `GET /api/auth/me` | pendiente | 200 | 200; control de TC-AUTH-013 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 42 | Caso | `TC-BIZ-006a-CHK` | E2, verificación del paso 6 (después del paso 8, con el token de ADMIN) | ADMIN | `GET /api/users/by-email?email=dupuser.{{runId}}@example.test` | pendiente | 404 | 404 (cierra TC-BIZ-006a) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 43 | Caso | `TC-BIZ-006b-CHK` | E2, verificación del paso 6 (después del paso 8, con el token de ADMIN) | ADMIN | `GET /api/users/by-username?username=dupemail-{{runId}}` | pendiente | 404 | 404 (cierra TC-BIZ-006b) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 44 | Caso | `TC-AUTH-004` | E2, paso 10 | ANONYMOUS | `POST /api/auth/login` | pendiente | 401 | 401 «Invalid credentials» | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 45 | Caso | `TC-AUTH-005` | E2, paso 10 | ANONYMOUS | `POST /api/auth/login` | pendiente | 401 | 401 idéntico a TC-AUTH-004 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 46 | Caso | `TC-AUTH-007` | E2, paso 10 | ANONYMOUS | `GET /api/auth/me` | pendiente | 401 | 401 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 47 | Caso | `TC-AUTH-008` | E2, paso 10 | token inválido (literal) | `GET /api/auth/me` | pendiente | 401 | 401 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 49 | Caso | `TC-AUTH-009` | E2, paso 10 | USER_A (token alterado) | `GET /api/auth/me` | pendiente | 401 | 401 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 50 | Caso | `TC-AUTH-012` | E2, paso 10 | ANONYMOUS | `GET /api/auth/me?access_token={{userAToken}}` | pendiente | 401 | 401 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 51 | Caso | `TC-AUTH-014` | E2, paso 10 | USER_A | `POST /api/events` | pendiente | 403 | 403 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 52 | Caso | `TC-AUTH-015` | E2, paso 10 | token inválido (literal) | `GET /api/venues/active` | pendiente | 401 | 401 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 53 | Condicional | `E2E-AUTH-01-INACTIVE` · renovación si el token de INACTIVE tiene ≥ 15 min (TC-AUTH-013a) | E2, paso 11 | ANONYMOUS | `POST /api/auth/login` | pendiente | 200 | 200; copiar a `inactiveUserToken` y anotar la hora | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 54 | Condicional | `E2E-AUTH-02-INACTIVE` · control de la renovación | E2, paso 11 | INACTIVE_USER | `GET /api/auth/me` | pendiente | 200 | 200 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 56 | Caso | `TC-AUTH-013a` | E2, paso 11 | INACTIVE_USER | `GET /api/auth/me` | pendiente | 401 | 401 con el token anterior; anotar el mensaje | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 57 | Caso | `TC-AUTH-013b` | E2, paso 11 | ANONYMOUS | `POST /api/auth/login` | pendiente | 401 | 401 genérico | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 58 | Caso | `E2E-USR-02` | E2, paso 12 | USER_A | `GET /api/users/by-email?email={{userAEmail}}` | pendiente | 200 | 200; email de A | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 59 | Caso | `E2E-USR-03` | E2, paso 12 | USER_A | `GET /api/users/by-username?username={{userAUsername}}` | pendiente | 200 | 200; username de A | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 60 | Caso | `TC-SEC-006` | E2, paso 12 | USER_B | `GET /api/users/by-email?email={{userAEmail}}` | pendiente | 403 | 403 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 61 | Caso | `TC-SEC-007` | E2, paso 12 | USER_B | `GET /api/users/by-username?username={{userAUsername}}` | pendiente | 403 | 403 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 62 | Caso | `TC-SEC-001-ANON` | E3, paso 13 | ANONYMOUS | `POST /api/events` | pendiente | 401 | 401 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 63 | Caso | `TC-SEC-001-USER` | E3, paso 13 | USER_A | `POST /api/events` | pendiente | 403 | 403 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 64 | Caso | `TC-SEC-001-CHK` | E3, paso 13 (después de TC-SEC-001-ANON y TC-SEC-001-USER) | ADMIN | `GET /api/events/{{denyEventCode}}` | pendiente | 404 | 404 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 65 | Caso | `E2E-EVT-01` | E3, paso 14 | ADMIN | `POST /api/events` | pendiente | 201 | 201; `status = DRAFT`, `minimumAge = 18` | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 66 | Caso | `E2E-EVT-02-PRE` | E3, paso 14 (después de E2E-EVT-01) | ANONYMOUS | `GET /api/events/{{publishedEventCode}}` | pendiente | 404 | 404 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 67 | Caso | `E2E-EVT-03-PRE` | E3, paso 14 | ANONYMOUS | `GET /api/events/published` | pendiente | 200 | sin MAIN | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 68 | Caso | `TC-SEC-003` | E3, paso 15 | USER_A | `POST /api/events/{{eventToPublishCode}}/artists/{{artistId}}` | pendiente | 403 | 403 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 69 | Caso | `E2E-EVT-02-ADMIN` | E3, paso 15 | ADMIN | `GET /api/events/{{eventToPublishCode}}` | pendiente | 200 | DRAFT, `artists = []` (cierra TC-SEC-003) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 70 | Caso | `E2E-EVT-05` | E3, paso 16 | ADMIN | `POST /api/events/{{eventToPublishCode}}/artists/{{artistId}}` | pendiente | 200 | 200; con el artista | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 71 | Reenvío | `E2E-EVT-02-ADMIN` · estado del paso 16 | E3, paso 16 | ADMIN | `GET /api/events/{{eventToPublishCode}}` | pendiente | 200 | DRAFT, con el artista | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 72 | Caso | `TC-SEC-002-USER` | E3, paso 17 | USER_A | `PATCH /api/events/{{eventToPublishCode}}/publish` | pendiente | 403 | 403 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 73 | Caso | `TC-SEC-002-STAFF` | E3, paso 17 | STAFF | `PATCH /api/events/{{eventToPublishCode}}/publish` | pendiente | 403 | 403 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 74 | Reenvío | `E2E-EVT-02-ADMIN` · estado del paso 17 | E3, paso 17 | ADMIN | `GET /api/events/{{eventToPublishCode}}` | pendiente | 200 | sigue DRAFT, con el artista (cierra TC-SEC-002) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 75 | Caso | `E2E-EVT-04` | E3, paso 18 | ADMIN | `PATCH /api/events/{{eventToPublishCode}}/publish` | pendiente | 200 | 200; `status = PUBLISHED` | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 76 | Caso | `E2E-EVT-02` | E3, paso 18 | ANONYMOUS | `GET /api/events/{{publishedEventCode}}` | pendiente | 200 | 200 sin token; PUBLISHED, con el artista | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 77 | Caso | `E2E-EVT-03` | E3, paso 18 | ANONYMOUS | `GET /api/events/published` | pendiente | 200 | incluye MAIN | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 78 | Caso | `E2E-EVT-06` | E3, paso 18 | ANONYMOUS | `GET /api/events/by-artist?stageName={{artistStageName}}` | pendiente | 200 | incluye MAIN | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 79 | Caso | `E2E-EVT-04-REPEAT` | E3, paso 19 (después de E2E-EVT-04) | ADMIN | `PATCH /api/events/{{eventToPublishCode}}/publish` | pendiente | 409 | 409 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 80 | Caso | `E2E-EVT-01-DRAFT` | E3, paso 20 | ADMIN | `POST /api/events` | pendiente | 201 | 201; DRAFT | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 81 | Caso | `E2E-EVT-05-DRAFT` | E3, paso 20 | ADMIN | `POST /api/events/{{draftEventCode}}/artists/{{artistId}}` | pendiente | 200 | 200 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 82 | Caso | `E2E-EVT-05-DRAFT-REPEAT` | E3, paso 20 | ADMIN | `POST /api/events/{{draftEventCode}}/artists/{{artistId}}` | pendiente | 409 | 409 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 83 | Caso | `TC-SEC-015` | E3, paso 20 | ADMIN | `GET /api/events/{{draftEventCode}}` | pendiente | 200 | 200; DRAFT, el artista una sola vez | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 84 | Caso | `TC-SEC-004` | E3, paso 20 | ANONYMOUS | `GET /api/events/{{draftEventCode}}` | pendiente | 404 | 404 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 85 | Caso | `TC-SEC-005` | E3, paso 20 | ANONYMOUS | `GET /api/events/by-artist?stageName={{artistStageName}}` | pendiente | 200 | incluye MAIN y no DRAFT | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 86 | Caso | `E2E-EVT-01-SOLD` | E3, paso 21 | ADMIN | `POST /api/events` | pendiente | 201 | 201; V1 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 87 | Caso | `E2E-EVT-04-SOLD` | E3, paso 21 | ADMIN | `PATCH /api/events/{{soldOutEventCode}}/publish` | pendiente | 200 | 200; PUBLISHED | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 88 | Caso | `E2E-TKT-01` | E4, paso 22 | USER_A | `POST /api/tickets` | pendiente | 201 | 201; PAID | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 89 | Caso | `TC-SEC-008` | E4, paso 22 (después de la 1.ª compra de E2E-TKT-01, T1, y antes de la 2.ª) | USER_A | `POST /api/tickets` | pendiente | 403 | 403 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 90 | Caso | `TC-SEC-008-CHK` | E4, paso 22 | ADMIN | `GET /api/tickets/by-user?email={{userBEmail}}` | pendiente | 200 | `[]` (cierra TC-SEC-008) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 91 | Reenvío | `E2E-TKT-01` · envío 2 de 4 | E4, paso 23 | USER_A | `POST /api/tickets` | pendiente | 201 | 201; copiar a `ticketUsableCode` (T2) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 92 | Reenvío | `E2E-TKT-01` · envío 3 de 4 | E4, paso 23 | USER_A | `POST /api/tickets` | pendiente | 201 | 201; copiar a `ticket3Code` (T3) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 93 | Reenvío | `E2E-TKT-01` · envío 4 de 4 (no enviar más) | E4, paso 23 | USER_A | `POST /api/tickets` | pendiente | 201 | 201; copiar a `ticket4Code` (T4) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 94 | Caso | `E2E-TKT-02` | E4, paso 24 | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | pendiente | 200 | T1 `PAID` | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 95 | Caso | `E2E-TKT-03` | E4, paso 24 | USER_A | `GET /api/tickets/by-user?email={{userAEmail}}` | pendiente | 200 | T1–T4, todos PAID | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 96 | Caso | `TC-SEC-009` | E4, paso 25 | USER_B | `GET /api/tickets/{{ticketCancelableCode}}` | pendiente | 403 | 403 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 97 | Caso | `TC-SEC-010` | E4, paso 25 | USER_B | `GET /api/tickets/by-user?email={{userAEmail}}` | pendiente | 403 | 403 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 98 | Caso | `TC-SEC-011` | E4, paso 25 | USER_B | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | pendiente | 403 | 403 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 99 | Reenvío | `E2E-TKT-02` · integridad de TC-SEC-011 (paso 25) | E4, paso 25 | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | pendiente | 200 | T1 sigue `PAID` (cierra TC-SEC-011) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 100 | Caso | `TC-SEC-013` | E4, paso 26 | USER_A | `GET /api/events/{{publishedEventCode}}/tickets/paid` | pendiente | 403 | 403 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 101 | Caso | `E2E-TKT-05` | E4, paso 27 | USER_A | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | pendiente | 200 | 200; CANCELLED | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 102 | Reenvío | `E2E-TKT-02` · relectura tras cancelar (paso 27) | E4, paso 27 | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | pendiente | 200 | T1 `CANCELLED` (cierra E2E-TKT-05) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 103 | Caso | `TC-BIZ-011` | E4, paso 27 (después de E2E-TKT-05 y de reenviar E2E-TKT-02) | USER_A | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | pendiente | 409 | 409; T1 sigue CANCELLED | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 104 | Caso | `E2E-TKT-04` | E4, paso 28 | STAFF | `GET /api/events/{{publishedEventCode}}/tickets/paid` | pendiente | 200 | T2, T3 y T4; sin T1 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 105 | Caso | `TC-SEC-012` | E4, paso 29 (después de E2E-TKT-04, paso 28, y antes de E2E-TKT-06) | USER_B | `PATCH /api/tickets/{{ticketUsableCode}}/use` | pendiente | 403 | 403 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 106 | Caso | `E2E-TKT-02-T2` | E4, paso 29 | USER_A | `GET /api/tickets/{{ticketUsableCode}}` | pendiente | 200 | T2 `PAID` (cierra TC-SEC-012) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 107 | Caso | `E2E-TKT-06` | E4, paso 30 | STAFF | `PATCH /api/tickets/{{ticketUsableCode}}/use` | pendiente | 200 | 200; USED | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 108 | Reenvío | `E2E-TKT-02-T2` · estado del paso 30 | E4, paso 30 | USER_A | `GET /api/tickets/{{ticketUsableCode}}` | pendiente | 200 | T2 `USED` (cierra E2E-TKT-06) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 109 | Caso | `TC-BIZ-012` | E4, paso 31 (después de E2E-TKT-04, paso 28) | STAFF | `PATCH /api/tickets/{{ticket4Code}}/use` | pendiente | 200 | 200; T4 USED | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 110 | Caso | `TC-BIZ-013` | E4, paso 31 | STAFF | `PATCH /api/tickets/{{ticket4Code}}/use` | pendiente | 409 | 409; T4 sigue USED | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 111 | Caso | `TC-BIZ-014-DRAFT` | E4, paso 32 | USER_A | `POST /api/tickets` | pendiente | 409 | 409 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 112 | Reenvío | `E2E-TKT-03` · integridad de TC-BIZ-014-DRAFT (paso 32) | E4, paso 32 | USER_A | `GET /api/tickets/by-user?email={{userAEmail}}` | pendiente | 200 | los mismos 4 tickets: T1 CANCELLED, T2 USED, T3 PAID, T4 USED (cierra TC-BIZ-014-DRAFT) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 113 | Caso | `E2E-TKT-01-SOLD` | E4, paso 33 | USER_A | `POST /api/tickets` | pendiente | 201 | 201; PAID | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 114 | Caso | `TC-BIZ-014-SOLDOUT` | E4, paso 33 | USER_B | `POST /api/tickets` | pendiente | 409 | 409 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 115 | Reenvío | `TC-SEC-008-CHK` · estado del paso 33 | E4, paso 33 | ADMIN | `GET /api/tickets/by-user?email={{userBEmail}}` | pendiente | 200 | sigue `[]` (cierra TC-BIZ-014-SOLDOUT) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 116 | Caso | `E2E-EVT-02-SOLD-ANON` | E4, paso 33 | ANONYMOUS | `GET /api/events/{{soldOutEventCode}}` | pendiente | 404 | 404 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 117 | Caso | `E2E-EVT-02-SOLD-ADMIN` | E4, paso 33 | ADMIN | `GET /api/events/{{soldOutEventCode}}` | pendiente | 200 | `status = SOLD_OUT` (cierra E2E-TKT-01-SOLD) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 118 | Caso | `E2E-USR-01-MINOR` | E4, paso 34 | ANONYMOUS | `POST /api/users` | pendiente | 201 | 201; `birthDate = 2013-06-01` | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 119 | Caso | `E2E-AUTH-01-MINOR` | E4, paso 34 | ANONYMOUS | `POST /api/auth/login` | pendiente | 200 | 200 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 120 | Caso | `TC-BIZ-014-AGE` | E4, paso 34 | USER_MINOR | `POST /api/tickets` | pendiente | 409 | 409 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 121 | Caso | `E2E-TKT-03-MINOR` | E4, paso 34 | USER_MINOR | `GET /api/tickets/by-user?email={{userMinorEmail}}` | pendiente | 200 | `[]` (cierra TC-BIZ-014-AGE) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 122 | Caso | `TC-HTTP-001` | E5, paso 35 | ANONYMOUS | `POST /api/users` | pendiente | 400 | 400 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 123 | Caso | `TC-HTTP-002` | E5, paso 35 | ANONYMOUS | `POST /api/users` | pendiente | 400 | 400 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 124 | Caso | `TC-HTTP-003` | E5, paso 35 | ANONYMOUS | `POST /api/users` | pendiente | 400 | 400 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 125 | Caso | `TC-HTTP-004` | E5, paso 35 | ADMIN | `POST /api/events` | pendiente | 400 | 400 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 126 | Caso | `TC-HTTP-005` | E5, paso 35 | ADMIN | `POST /api/events` | pendiente | 400 | 400 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 127 | Caso | `TC-HTTP-006` | E5, paso 35 | ANONYMOUS | `GET /api/artists/by-stage-name` | pendiente | 400 | 400 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 128 | Caso | `TC-HTTP-007` | E5, paso 35 | ANONYMOUS | `GET /api/artists/abc` | pendiente | 400 | 400 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 129 | Caso | `TC-HTTP-008` | E5, paso 35 | ANONYMOUS | `GET /api/venues/E2E-{{runId}}-NOPE` | pendiente | 404 | 404 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 130 | Caso | `TC-HTTP-009` | E5, paso 35 | ANONYMOUS | `GET /api/artists/999999999` | pendiente | 404 | 404 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 131 | Caso | `TC-HTTP-010` | E5, paso 35 | ANONYMOUS | `GET /api/artists/by-stage-name?stageName=NoExiste-{{runId}}` | pendiente | 404 | 404 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 132 | Caso | `TC-HTTP-011` | E5, paso 35 | ADMIN | `GET /api/events/E2E-{{runId}}-NOPE` | pendiente | 404 | 404 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 133 | Caso | `TC-HTTP-012` | E5, paso 35 | USER_A | `GET /api/tickets/NOEXISTE-{{runId}}` | pendiente | 404 | 404 (no 403) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 134 | Caso | `TC-HTTP-013` | E5, paso 35 | ADMIN | `POST /api/events/{{publishedEventCode}}/artists/999999999` | pendiente | 404 | 404 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 135 | Reenvío | `E2E-EVT-02` · integridad de TC-HTTP-013 | E5, paso 35 | ANONYMOUS | `GET /api/events/{{publishedEventCode}}` | pendiente | 200 | MAIN PUBLISHED con el mismo artista (cierra TC-HTTP-013) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 136 | Caso | `TC-HTTP-014` | E5, paso 35 | USER_A | `POST /api/tickets` | pendiente | 404 | 404 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 137 | Caso | `TC-BIZ-015a` | E5, paso 35 | ADMIN | `POST /api/events` | pendiente | 409 | 409 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 138 | Reenvío | `E2E-EVT-02` · integridad de TC-BIZ-015a | E5, paso 35 | ANONYMOUS | `GET /api/events/{{publishedEventCode}}` | pendiente | 200 | MAIN sigue PUBLISHED, sin cambios (cierra TC-BIZ-015a) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 139 | Caso | `TC-BIZ-015b` | E5, paso 35 | ADMIN | `POST /api/events` | pendiente | 409 | 409 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 140 | Caso | `TC-BIZ-015c` | E5, paso 35 | ADMIN | `POST /api/events` | pendiente | 404 | 404 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 141 | Caso | `TC-BIZ-015d` | E5, paso 35 | ADMIN | `POST /api/events` | pendiente | 409 | 409 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 142 | Caso | `TC-BIZ-015-CHK` | E5, paso 35 (después de todos los intentos) | ADMIN | `GET /api/events/E2E-{{runId}}-BAD` | pendiente | 404 | 404 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 143 | Condicional | `E2E-AUTH-01-STAFF` · renovación si `staffToken` tiene ≥ 15 min (antes del SQL) | E6, paso 36 | ANONYMOUS | `POST /api/auth/login` | pendiente | 200 | 200; copiar a `staffToken` | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 144 | Condicional | `E2E-AUTH-02-STAFF` · control de la renovación | E6, paso 36 | STAFF | `GET /api/auth/me` | pendiente | 200 | `roles = ["STAFF"]` | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 146 | Caso | `TC-SEC-017-ME` | E6, paso 36 (después del SQL `revoke-staff`) | STAFF | `GET /api/auth/me` | pendiente | 200 | 200; `roles = []` | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 147 | Caso | `TC-SEC-017` | E6, paso 36 | STAFF | `PATCH /api/tickets/{{ticket3Code}}/use` | pendiente | 403 | 403 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 148 | Caso | `TC-SEC-017-CHK` | E6, paso 36 | ADMIN | `GET /api/tickets/{{ticket3Code}}` | pendiente | 200 | T3 sigue `PAID` (cierra TC-SEC-017) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 150 | Caso | `TC-AUTH-010-LOGIN` | E7, paso 37 (b; reinicio a: 1 de 6) | ANONYMOUS | `POST /api/auth/login` | pendiente | 200 | 200; `expiresIn = 60` | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 151 | Caso | `TC-AUTH-010-CTRL` | E7, paso 37 (c) | USER_A (token de 1 min) | `GET /api/auth/me` | pendiente | 200 | 200 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 153 | Caso | `TC-AUTH-010` | E7, paso 37 (d) | USER_A (token de 1 min) | `GET /api/auth/me` | pendiente | 401 | 401 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 155 | Caso | `TC-AUTH-011a-LOGIN` | E7, paso 37 (f; reinicio e: 2 de 6) | ANONYMOUS | `POST /api/auth/login` | pendiente | 200 | 200; `expiresIn = 900` | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 156 | Caso | `TC-AUTH-011a-CTRL` | E7, paso 37 (f) | USER_A (token firmado con K1) | `GET /api/auth/me` | pendiente | 200 | 200 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 158 | Caso | `TC-AUTH-011a` | E7, paso 37 (h; reinicio g: 3 de 6) | USER_A (token firmado con K1) | `GET /api/auth/me` | pendiente | 401 | 401 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 159 | Reenvío | `E2E-AUTH-01` · control i (con K2) | E7, paso 37 | ANONYMOUS | `POST /api/auth/login` | pendiente | 200 | 200; copiar el nuevo `accessToken` a `userAToken` | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 160 | Reenvío | `E2E-AUTH-02` · control i (con K2) | E7, paso 37 | USER_A | `GET /api/auth/me` | pendiente | 200 | 200 (cierra TC-AUTH-011a) | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 162 | Caso | `TC-AUTH-011b-LOGIN` | E7, paso 37 (k; reinicio j: 4 de 6) | ANONYMOUS | `POST /api/auth/login` | pendiente | 200 | 200; `expiresIn = 900` | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 163 | Caso | `TC-AUTH-011b-CTRL` | E7, paso 37 (k) | USER_A (token con el emisor por defecto) | `GET /api/auth/me` | pendiente | 200 | 200 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 165 | Caso | `TC-AUTH-011b` | E7, paso 37 (m; reinicio l: 5 de 6) | USER_A (token con el emisor por defecto) | `GET /api/auth/me` | pendiente | 401 | 401 | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 166 | Reenvío | `E2E-AUTH-01` · control n (emisor alternativo) | E7, paso 37 | ANONYMOUS | `POST /api/auth/login` | pendiente | 200 | 200; copiar el nuevo `accessToken` a `userAToken` | pendiente | pendiente | pendiente | NOT RUN | pendiente |
| 167 | Reenvío | `E2E-AUTH-02` · control n (emisor alternativo) | E7, paso 37 | USER_A | `GET /api/auth/me` | pendiente | 200 | 200 (cierra TC-AUTH-011b) | pendiente | pendiente | pendiente | NOT RUN | pendiente |

### Reenvíos no previstos (renovación de tokens u otros)

| Tras el # | Petición | Motivo | Hora | HTTP observado | Resultado | Observación |
|---|---|---|---|---|---|---|
| — | — | — | — | — | — | — |

## Casos con comprobación posterior

Estos casos **no** pueden marcarse PASS hasta ejecutar la comprobación indicada.

| Caso | Se cierra con |
|---|---|
| `TC-AUTH-014` | TC-SEC-001-CHK |
| `TC-SEC-001-ANON` | TC-SEC-001-CHK |
| `TC-SEC-001-USER` | TC-SEC-001-CHK |
| `TC-SEC-003` | E2E-EVT-02-ADMIN |
| `TC-SEC-002-USER` | reenvío de E2E-EVT-02-ADMIN |
| `TC-SEC-002-STAFF` | reenvío de E2E-EVT-02-ADMIN |
| `TC-SEC-008` | TC-SEC-008-CHK |
| `TC-SEC-011` | reenvío de E2E-TKT-02 |
| `E2E-TKT-05` | reenvío de E2E-TKT-02 |
| `TC-SEC-012` | E2E-TKT-02-T2 |
| `E2E-TKT-06` | reenvío de E2E-TKT-02-T2 |
| `TC-BIZ-014-DRAFT` | reenvío de E2E-TKT-03 |
| `E2E-TKT-01-SOLD` | E2E-EVT-02-SOLD-ADMIN |
| `TC-BIZ-014-SOLDOUT` | reenvío de TC-SEC-008-CHK |
| `TC-BIZ-014-AGE` | E2E-TKT-03-MINOR |
| `TC-HTTP-004` | TC-BIZ-015-CHK |
| `TC-HTTP-005` | TC-BIZ-015-CHK |
| `TC-HTTP-013` | reenvío de E2E-EVT-02 |
| `TC-BIZ-015a` | reenvío de E2E-EVT-02 |
| `TC-BIZ-015b` | TC-BIZ-015-CHK |
| `TC-BIZ-015c` | TC-BIZ-015-CHK |
| `TC-BIZ-015d` | TC-BIZ-015-CHK |
| `TC-SEC-017` | TC-SEC-017-CHK |
| `TC-AUTH-011a` | control i |
| `TC-AUTH-011b` | control n |
| `TC-AUTH-003` | E2E-AUTH-02 (mismo `id` que el control previo) |
| `TC-SEC-016` | E2E-AUTH-02-ROLEINJ (`roles = ["USER"]`) |
| `TC-BIZ-006a` / `TC-BIZ-006b` | TC-BIZ-006a-CHK / TC-BIZ-006b-CHK (404) |
| `TC-AUTH-013a` | precondición: E2E-AUTH-02-INACTIVE (200) con un token de menos de 15 min |

## Cobertura nominal de las 22 rutas

Una fila por ruta con su caso nominal (§7). La cobertura de seguridad se cuenta aparte (§16.3).

| # | Ruta | Caso nominal | HTTP esperado | Resultado |
|---|---|---|---|---|
| 1 | `GET /api/venues/{{venueActiveCode}}` | `E2E-VEN-01` | 200 | NOT RUN |
| 2 | `GET /api/venues/active` | `E2E-VEN-02` | 200 | NOT RUN |
| 3 | `GET /api/artists/{{artistId}}` | `E2E-ART-01` | 200 | NOT RUN |
| 4 | `GET /api/artists/by-stage-name?stageName={{artistStageName}}` | `E2E-ART-02` | 200 | NOT RUN |
| 5 | `GET /api/artists/active` | `E2E-ART-03` | 200 | NOT RUN |
| 6 | `POST /api/users` | `E2E-USR-01` | 201 | NOT RUN |
| 7 | `GET /api/users/by-email?email={{userAEmail}}` | `E2E-USR-02` | 200 | NOT RUN |
| 8 | `GET /api/users/by-username?username={{userAUsername}}` | `E2E-USR-03` | 200 | NOT RUN |
| 9 | `POST /api/events` | `E2E-EVT-01` | 201 | NOT RUN |
| 10 | `GET /api/events/{{publishedEventCode}}` | `E2E-EVT-02` | 200 | NOT RUN |
| 11 | `GET /api/events/published` | `E2E-EVT-03` | 200 | NOT RUN |
| 12 | `PATCH /api/events/{{eventToPublishCode}}/publish` | `E2E-EVT-04` | 200 | NOT RUN |
| 13 | `POST /api/events/{{eventToPublishCode}}/artists/{{artistId}}` | `E2E-EVT-05` | 200 | NOT RUN |
| 14 | `GET /api/events/by-artist?stageName={{artistStageName}}` | `E2E-EVT-06` | 200 | NOT RUN |
| 15 | `POST /api/tickets` | `E2E-TKT-01` | 201 | NOT RUN |
| 16 | `GET /api/tickets/{{ticketCancelableCode}}` | `E2E-TKT-02` | 200 | NOT RUN |
| 17 | `GET /api/tickets/by-user?email={{userAEmail}}` | `E2E-TKT-03` | 200 | NOT RUN |
| 18 | `GET /api/events/{{publishedEventCode}}/tickets/paid` | `E2E-TKT-04` | 200 | NOT RUN |
| 19 | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | `E2E-TKT-05` | 200 | NOT RUN |
| 20 | `PATCH /api/tickets/{{ticketUsableCode}}/use` | `E2E-TKT-06` | 200 | NOT RUN |
| 21 | `POST /api/auth/login` | `E2E-AUTH-01` | 200 | NOT RUN |
| 22 | `GET /api/auth/me` | `E2E-AUTH-02` | 200 | NOT RUN |

Rutas nominales: 0/22 PASS (actualizar al consolidar).

## Cobertura de seguridad

Casos TC-AUTH y TC-SEC (PRD §8.2 y §8.3), contados aparte de la cobertura nominal. Los casos del PRD que la colección ejecuta con una petición `E2E-*` se indican como `TC-… → petición`.

| Caso | Actor | Ruta | HTTP esperado | Resultado |
|---|---|---|---|---|
| `TC-AUTH-001` → `E2E-USR-01` | ANONYMOUS | `POST /api/users` | 201 | NOT RUN |
| `TC-SEC-016` | ANONYMOUS | `POST /api/users` | 201 | NOT RUN |
| `TC-AUTH-002` → `E2E-AUTH-01` | ANONYMOUS | `POST /api/auth/login` | 200 | NOT RUN |
| `TC-AUTH-003` | ANONYMOUS | `POST /api/auth/login` | 200 | NOT RUN |
| `TC-AUTH-006` → `E2E-AUTH-02` | USER_A | `GET /api/auth/me` | 200 | NOT RUN |
| `TC-AUTH-004` | ANONYMOUS | `POST /api/auth/login` | 401 | NOT RUN |
| `TC-AUTH-005` | ANONYMOUS | `POST /api/auth/login` | 401 | NOT RUN |
| `TC-AUTH-007` | ANONYMOUS | `GET /api/auth/me` | 401 | NOT RUN |
| `TC-AUTH-008` | token inválido (literal) | `GET /api/auth/me` | 401 | NOT RUN |
| `TC-AUTH-009` | USER_A (token alterado) | `GET /api/auth/me` | 401 | NOT RUN |
| `TC-AUTH-012` | ANONYMOUS | `GET /api/auth/me?access_token={{userAToken}}` | 401 | NOT RUN |
| `TC-AUTH-014` | USER_A | `POST /api/events` | 403 | NOT RUN |
| `TC-AUTH-015` | token inválido (literal) | `GET /api/venues/active` | 401 | NOT RUN |
| `TC-AUTH-013a` | INACTIVE_USER | `GET /api/auth/me` | 401 | NOT RUN |
| `TC-AUTH-013b` | ANONYMOUS | `POST /api/auth/login` | 401 | NOT RUN |
| `TC-SEC-006` | USER_B | `GET /api/users/by-email?email={{userAEmail}}` | 403 | NOT RUN |
| `TC-SEC-007` | USER_B | `GET /api/users/by-username?username={{userAUsername}}` | 403 | NOT RUN |
| `TC-SEC-001-ANON` | ANONYMOUS | `POST /api/events` | 401 | NOT RUN |
| `TC-SEC-001-USER` | USER_A | `POST /api/events` | 403 | NOT RUN |
| `TC-SEC-001-CHK` | ADMIN | `GET /api/events/{{denyEventCode}}` | 404 | NOT RUN |
| `TC-SEC-003` | USER_A | `POST /api/events/{{eventToPublishCode}}/artists/{{artistId}}` | 403 | NOT RUN |
| `TC-SEC-002-USER` | USER_A | `PATCH /api/events/{{eventToPublishCode}}/publish` | 403 | NOT RUN |
| `TC-SEC-002-STAFF` | STAFF | `PATCH /api/events/{{eventToPublishCode}}/publish` | 403 | NOT RUN |
| `TC-SEC-015` | ADMIN | `GET /api/events/{{draftEventCode}}` | 200 | NOT RUN |
| `TC-SEC-004` | ANONYMOUS | `GET /api/events/{{draftEventCode}}` | 404 | NOT RUN |
| `TC-SEC-005` | ANONYMOUS | `GET /api/events/by-artist?stageName={{artistStageName}}` | 200 | NOT RUN |
| `TC-SEC-008` | USER_A | `POST /api/tickets` | 403 | NOT RUN |
| `TC-SEC-008-CHK` | ADMIN | `GET /api/tickets/by-user?email={{userBEmail}}` | 200 | NOT RUN |
| `TC-SEC-009` | USER_B | `GET /api/tickets/{{ticketCancelableCode}}` | 403 | NOT RUN |
| `TC-SEC-010` | USER_B | `GET /api/tickets/by-user?email={{userAEmail}}` | 403 | NOT RUN |
| `TC-SEC-011` | USER_B | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 403 | NOT RUN |
| `TC-SEC-013` | USER_A | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 403 | NOT RUN |
| `TC-SEC-012` | USER_B | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 403 | NOT RUN |
| `TC-SEC-014` → `E2E-TKT-06` | STAFF | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 200 | NOT RUN |
| `TC-SEC-017-ME` | STAFF | `GET /api/auth/me` | 200 | NOT RUN |
| `TC-SEC-017` | STAFF | `PATCH /api/tickets/{{ticket3Code}}/use` | 403 | NOT RUN |
| `TC-SEC-017-CHK` | ADMIN | `GET /api/tickets/{{ticket3Code}}` | 200 | NOT RUN |
| `TC-AUTH-010-LOGIN` | ANONYMOUS | `POST /api/auth/login` | 200 | NOT RUN |
| `TC-AUTH-010-CTRL` | USER_A (token de 1 min) | `GET /api/auth/me` | 200 | NOT RUN |
| `TC-AUTH-010` | USER_A (token de 1 min) | `GET /api/auth/me` | 401 | NOT RUN |
| `TC-AUTH-011a-LOGIN` | ANONYMOUS | `POST /api/auth/login` | 200 | NOT RUN |
| `TC-AUTH-011a-CTRL` | USER_A (token firmado con K1) | `GET /api/auth/me` | 200 | NOT RUN |
| `TC-AUTH-011a` | USER_A (token firmado con K1) | `GET /api/auth/me` | 401 | NOT RUN |
| `TC-AUTH-011b-LOGIN` | ANONYMOUS | `POST /api/auth/login` | 200 | NOT RUN |
| `TC-AUTH-011b-CTRL` | USER_A (token con el emisor por defecto) | `GET /api/auth/me` | 200 | NOT RUN |
| `TC-AUTH-011b` | USER_A (token con el emisor por defecto) | `GET /api/auth/me` | 401 | NOT RUN |

Casos de seguridad: 0/46 PASS (actualizar al consolidar).

## Matriz requisito → caso → ruta → resultado

La ruta de cada caso figura en la bitácora de envíos y en las tablas de cobertura.

| Requisito | Descripción | Casos y evidencias | Resultado |
|---|---|---|---|
| FR-E2E-001/002 | Colección importable, ambiente editable, 22 rutas | A-02; CONN-01; cobertura nominal (22) | NOT RUN |
| FR-E2E-003/004 | Login y /me de cada actor; un token por actor | E2E-AUTH-01, TC-AUTH-003, E2E-AUTH-01-B/-STAFF/-ADMIN; E2E-AUTH-02, E2E-AUTH-02-B/-STAFF/-ADMIN | NOT RUN |
| FR-E2E-005/006 | HTTP y JSON comparados; códigos copiados entre peticiones | Todas las filas; copias de `artistId`, T1–T5 y tokens | NOT RUN |
| FR-E2E-007/008 | Matriz de autorización; estado antes y después | Cobertura de seguridad; reenvíos de integridad (E2E-EVT-02-ADMIN, E2E-TKT-02, E2E-TKT-02-T2, E2E-TKT-03, TC-SEC-008-CHK, E2E-EVT-02) | NOT RUN |
| FR-E2E-009 | ErrorResponse de 5 campos, sin datos sensibles | TC-HTTP-001 a TC-HTTP-014 y todos los 4xx | NOT RUN |
| FR-E2E-010/016 | Datos controlados y repetibilidad | A-05 (R-B), A-10, A-11, A-13, A-15, A-16 | NOT RUN |
| FR-E2E-011/012 | Instrucciones paso a paso y bitácora | `checklist-e2e-manual.md`; esta bitácora | NOT RUN |
| FR-E2E-013 | Sin tokens ni contraseñas exportados | A-01, A-12, A-24, A-27 | NOT RUN |
| FR-E2E-014/015 | Regresión registrada; bloqueos marcados | Sección «Defectos»; criterios BLOCKED | NOT RUN |
| FR-E2E-017 | Coherencia con las Fases 3 y 4 | Toda la bitácora (sin cambios en el backend, F5-H) | NOT RUN |
| AC-01 | Registro y login | E2E-USR-01, E2E-AUTH-01, E2E-AUTH-02 | NOT RUN |
| AC-02 | ADMIN publica | E2E-EVT-01, E2E-EVT-05, E2E-EVT-04, E2E-EVT-02, E2E-EVT-06 | NOT RUN |
| AC-03 | USER sin privilegio | TC-AUTH-014, TC-SEC-001-USER, TC-SEC-002-USER, TC-SEC-003 (+ comprobaciones) | NOT RUN |
| AC-04 | Protección de propiedad | TC-SEC-009, TC-SEC-010, TC-SEC-011 (+ reenvío de E2E-TKT-02) | NOT RUN |
| AC-05 | Compra ajena | TC-SEC-008, TC-SEC-008-CHK | NOT RUN |
| AC-06 | Uso por STAFF | E2E-TKT-06, E2E-TKT-02-T2, TC-BIZ-012, TC-BIZ-013 | NOT RUN |
| AC-07 | JWT inválido | TC-AUTH-008, TC-AUTH-009, TC-AUTH-010, TC-AUTH-011a, TC-AUTH-011b | NOT RUN |
| AC-08 | Borradores | E2E-EVT-02-PRE, E2E-EVT-03-PRE, TC-SEC-004, TC-SEC-005, TC-SEC-015 | NOT RUN |
| AC-09 | ErrorResponse | TC-HTTP-001 a TC-HTTP-014 | NOT RUN |
| AC-10 | Verificación manual completa | Toda la bitácora, recuentos y cobertura nominal | NOT RUN |

## Consolidación por carpeta

Recuento de casos (filas de tipo Caso). Los reenvíos y condicionales no se cuentan.

| Carpeta | Casos | PASS | FAIL | BLOCKED | NOT RUN |
|---|---|---|---|---|---|
| 00 Preparación / conectividad | 1 | 0 | 0 | 0 | 1 |
| 01 Venues (2 rutas) | 2 | 0 | 0 | 0 | 2 |
| 02 Artists (3 rutas) | 3 | 0 | 0 | 0 | 3 |
| 03 Auth / usuarios (registro, login, me) | 23 | 0 | 0 | 0 | 23 |
| 04 Usuarios / consulta y propiedad (2 rutas) | 6 | 0 | 0 | 0 | 6 |
| 05 Events / administración y visibilidad (6 rutas) | 24 | 0 | 0 | 0 | 24 |
| 06 Tickets / compra, consulta y ciclo de vida (6 rutas) | 24 | 0 | 0 | 0 | 24 |
| 07 Seguridad / escenarios negativos | 22 | 0 | 0 | 0 | 22 |
| 08 Validaciones y errores HTTP | 19 | 0 | 0 | 0 | 19 |
| **Total** | **124** | 0 | 0 | 0 | 124 |

## Defectos

| Defecto | Caso | Precondiciones | Pasos | HTTP esperado / observado | Requisito afectado | Estado |
|---|---|---|---|---|---|---|
| — | — | — | — | — | — | — |
