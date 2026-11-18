# Fixtures y ambiente E2E — PulsePass Fase 5

Esta guía prepara la base PostgreSQL **exclusiva** de las pruebas E2E manuales con Postman (PRD E2E
§5.1 y §15). Explica cómo crear el contenedor, arrancar la aplicación contra él, comprobar que se usa
la base correcta, aplicar los fixtures de `seed-e2e.sql` y repetir o restaurar el ambiente.

> **Regla de oro.** Todo se hace sobre el contenedor `pulsepass-e2e-db`, el volumen
> `pulsepass-e2e-data` y la base `pulsepass_e2e`. Ningún paso toca otra base de datos ni otro
> contenedor. En particular, **nunca** se modifica, detiene ni borra `pulsepass-db` (la base de
> desarrollo) ni su volumen.

## 1. Prerrequisitos

- Docker Desktop en marcha. La imagen `postgres:17` se descarga sola la primera vez.
- Java 21 y el wrapper de Maven del proyecto (`backend/mvnw`).
- `openssl`, para generar los secretos de la sesión.
- El puerto `127.0.0.1:55432` (base E2E) y el `8080` (aplicación) libres.
- No hace falta `psql` en el equipo: se usa el del contenedor con `docker exec`.

Sin cambios en el backend (F5-H): la aplicación se configura solo con variables de entorno. Ni el
código ni `application.yml` se modifican.

## 2. Convenciones de una ejecución (`runId`)

Cada ejecución elige un `runId` **en minúsculas, alfanumérico y de 1 a 20 caracteres**
(`^[a-z0-9]{1,20}$`), por ejemplo `r202611181530`. El límite garantiza que `E2E-{runId}-DRAFT` no
supere los 30 caracteres de `eventCode` y `venueCode`.

| Dato | Patrón |
|---|---|
| Emails de los actores | `{actor}.{runId}@example.test` (dominio reservado, PRD §5.1) |
| Locales (SQL) | `E2E-{runId}-V0`, `E2E-{runId}-V1` y, opcional, `E2E-{runId}-VX` |
| Eventos (API, ADMIN) | `E2E-{runId}-DENY`, `-MAIN`, `-DRAFT` y `-SOLD` |

`seed-e2e.sql` deriva los emails de `staff`, `admin` e `inactive` a partir del `runId`. Si se
registran con otro email, el seed no los encuentra y falla sin cambiar nada.

## 3. Secretos de la sesión

Se generan **en cada sesión de terminal** y solo viven en ella (P5-02-b). Nunca se escriben en el
repositorio, la colección, el ambiente de Postman ni la bitácora.

```bash
export PULSEPASS_E2E_DB_PASSWORD="$(openssl rand -hex 16)"
export PULSEPASS_JWT_SECRET_BASE64="$(openssl rand -base64 32)"
```

La clave JWT es exclusiva de E2E (§15.1). Si se cambia, los tokens emitidos antes dejan de valer y
hay que repetir los logins.

## 4. Crear la base E2E (primera vez)

```bash
docker volume create --label com.pulsepass.purpose=e2e pulsepass-e2e-data
```

```bash
docker run -d --name pulsepass-e2e-db \
  --label com.pulsepass.purpose=e2e \
  -e POSTGRES_DB=pulsepass_e2e \
  -e POSTGRES_USER=pulsepass_e2e \
  -e POSTGRES_PASSWORD="$PULSEPASS_E2E_DB_PASSWORD" \
  -p 127.0.0.1:55432:5432 \
  -v pulsepass-e2e-data:/var/lib/postgresql/data \
  postgres:17
```

- Si ya existe un contenedor con ese nombre, `docker run` **falla**: no sobrescribe nada.
- El puerto `55432` solo escucha en `127.0.0.1` y no choca con el `5432` de desarrollo.
- **La contraseña inicial queda en los metadatos del contenedor** (`POSTGRES_PASSWORD` en
  `docker inspect`) y no cambia aunque después se actualice con `\password`. No compartir ni
  adjuntar como evidencia la salida completa de `docker inspect` sin revisar antes si contiene
  secretos; usar los comandos con `--format` de esta guía, que solo muestran los campos necesarios.

Esperar a que acepte conexiones:

```bash
docker exec pulsepass-e2e-db pg_isready -U pulsepass_e2e -d pulsepass_e2e
```

### Volver en otra sesión

El contenedor y el volumen se conservan. En una terminal nueva hay secretos nuevos, así que la
contraseña de la base se actualiza de forma interactiva con `\password`, por el socket local del
contenedor. Así la contraseña no aparece en los argumentos de ningún proceso, y psql envía al
servidor solo su hash, nunca el texto plano.

```bash
docker start pulsepass-e2e-db
```

Copiar la contraseña nueva al portapapeles sin mostrarla en pantalla (macOS):

```bash
printf '%s' "$PULSEPASS_E2E_DB_PASSWORD" | pbcopy
```

Abrir psql de forma interactiva:

```bash
docker exec -it pulsepass-e2e-db psql -X -U pulsepass_e2e -d pulsepass_e2e
```

Dentro de psql, ejecutar `\password pulsepass_e2e`, pegar la contraseña en las dos peticiones (no se
muestra al escribir) y salir con `\q`. Después, vaciar el portapapeles:

```bash
printf '' | pbcopy
```

## 5. Arrancar la aplicación contra la base E2E

Desde `backend/`, en la misma terminal donde se exportaron los secretos:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:55432/pulsepass_e2e
export SPRING_DATASOURCE_USERNAME=pulsepass_e2e
export SPRING_DATASOURCE_PASSWORD="$PULSEPASS_E2E_DB_PASSWORD"
./mvnw spring-boot:run
```

Variables opcionales, solo para el bloque JWT (E7, TC-AUTH-010/011): `PULSEPASS_JWT_ACCESS_TOKEN_TTL`
(por defecto `PT15M`) y `PULSEPASS_JWT_ISSUER` (por defecto `pulsepass-api`). Cambiarlas exige
reiniciar la aplicación; al terminar ese bloque se vuelve a la configuración original.

Flyway aplica V1–V4 en el primer arranque. Las migraciones no se modifican nunca.

## 6. Comprobaciones previas (obligatorias antes de cada sesión)

Anotar el resultado de cada una en la bitácora. Si alguna falla, **detenerse**: los casos quedan
BLOCKED (PRD §15.3).

1. **El log de arranque** muestra la base E2E y la versión 17:
   `Database: jdbc:postgresql://127.0.0.1:55432/pulsepass_e2e (PostgreSQL 17.x)`.
   En el primer arranque también `Successfully applied 4 migrations`; después, `Schema "public" is up to date`.
2. **La base y el historial de Flyway**:

   ```bash
   docker exec pulsepass-e2e-db psql -X -U pulsepass_e2e -d pulsepass_e2e -c "SELECT current_database(), current_user, version();" -c "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank;"
   ```

   Esperado: `pulsepass_e2e`, `pulsepass_e2e`, PostgreSQL 17.x y las versiones 1–4 con `success = t`.
3. **La base de desarrollo no se tocó** (solo lectura; si no existe, `docker inspect` lo indica y
   no hay nada que comprobar):

   ```bash
   docker inspect pulsepass-db --format '{{.State.Status}} {{range .Mounts}}{{.Name}}{{end}}'
   ```

   El estado y el volumen deben ser los mismos antes y después de la sesión.
4. **Conectividad HTTP**: `GET http://localhost:8080/api/venues/active` responde 200.

## 7. Pasos de `seed-e2e.sql`

Un paso por ejecución, cada uno en **una sola transacción**:

```bash
docker exec -i pulsepass-e2e-db psql -X -U pulsepass_e2e -d pulsepass_e2e -v step=<paso> -v run_id=<runId> < tests/e2e/fixtures/seed-e2e.sql
```

(Desde la raíz del repositorio. No usar `-1`: el archivo gestiona su propia transacción.)

| `step` | Momento (orden E0–E7 de P5-01) | Efecto | Precondición |
|---|---|---|---|
| `venues` | E0, tras el primer arranque | Inserta `E2E-{runId}-V0` (capacidad 100) y `E2E-{runId}-V1` (capacidad 1). Con `-v inactive_venue=on`, también `E2E-{runId}-VX` (inactivo, opcional) | No existen locales de ese `runId` |
| `promote` | E2, paso 7, tras registrar STAFF y ADMIN por la API | `staff.{runId}` queda **solo** con STAFF y `admin.{runId}`, **solo** con ADMIN | Ambos activos, con credencial y roles exactamente `{USER}` |
| `deactivate` | E2, paso 11 (TC-AUTH-013), tras su login | `inactive.{runId}` pasa a `active = false` | Activo y roles exactamente `{USER}` |
| `revoke-staff` | E6 (TC-SEC-017), último uso de STAFF | `staff.{runId}` pierde STAFF y queda sin roles | Roles exactamente `{STAFF}` |

**Salida correcta**: `BEGIN`, la fila de `set_config`, `DO`, dos tablas de evidencia (los locales y
los usuarios de ese `runId`, con sus roles y `active`) y `COMMIT`. Código de salida 0.

**Salida con error**: `ERROR: ...` con el motivo y código de salida **3**. No se confirma nada
(`COMMIT` no llega a ejecutarse). Errores posibles:

| Mensaje | Causa |
|---|---|
| `Not the E2E database` | Se apuntó a otra base |
| `Flyway migrations V1..V4 are not applied` | La aplicación no ha arrancado aún contra esta base |
| `Invalid step` / `Invalid run_id` / `Invalid inactive_venue` | Variable ausente o con formato incorrecto |
| `Venues for run_id ... already exist` | `runId` reutilizado: elegir otro |
| `query returned no rows` | El actor no está registrado con el email esperado |
| `must have exactly roles ...` | El paso ya se aplicó o el usuario no está en el estado esperado |

## 8. Catálogo de datos de una ejecución

Valores marcados como DATO DE PRUEBA: el PRD no los define.

**Actores** (todos registrados por `POST /api/users`; contraseñas solo en valores locales de Postman):

| Actor | Email | Origen del privilegio |
|---|---|---|
| USER_A | `usera.{runId}@example.test` | Registro (USER), `birthDate` 1998-05-15 |
| USER_B | `userb.{runId}@example.test` | Registro (USER), `birthDate` 1998-05-15 |
| USER_MINOR | `userminor.{runId}@example.test` | Registro (USER), `birthDate` 2013-06-01: 17 años el 2030-12-12 |
| STAFF | `staff.{runId}@example.test` | Registro + `promote`: solo STAFF |
| ADMIN | `admin.{runId}@example.test` | Registro + `promote`: solo ADMIN |
| INACTIVE_USER | `inactive.{runId}@example.test` | Registro + `deactivate` |
| ROLE_INJECTION | `roleinjection.{runId}@example.test` | Cuenta **auxiliar** de seguridad, no un actor funcional: se registra con `"role":"ADMIN"` en el cuerpo y debe quedar solo con USER (TC-SEC-016) |

**Locales** (SQL, paso `venues`): `E2E-{runId}-V0` «PulsePass E2E Sede Principal», capacidad 100;
`E2E-{runId}-V1` «PulsePass E2E Sede Aforo 1», capacidad 1; opcional `E2E-{runId}-VX` «PulsePass
E2E Sede Inactiva», capacidad 50, inactivo. Todos en Bogotá. El artista viene de V2 (p. ej. Solar Beat).

**Eventos** (API, ADMIN, fecha 2030-12-12):

| Evento | Local | Uso |
|---|---|---|
| `E2E-{runId}-DENY` | V0 | Solo intentos denegados (TC-SEC-001): nunca debe existir |
| `E2E-{runId}-MAIN` | V0 | `minimumAge = 18`; se asocia el artista y se publica; compras T1–T4 y USER_MINOR |
| `E2E-{runId}-DRAFT` | V0 | Se asocia el artista y queda en DRAFT (TC-SEC-004/005/015) |
| `E2E-{runId}-SOLD` | V1 | `minimumAge = 0`; se publica y se agota con una compra |

**Tickets** (los códigos los genera el servidor; se copian de cada respuesta):

| Ticket | Comprador / evento | Casos |
|---|---|---|
| T1 | USER_A / MAIN | TC-SEC-009 y 011, después TC-BIZ-010 y 011 (cancelación) |
| T2 | USER_A / MAIN | TC-SEC-012 (403), después TC-SEC-014 (STAFF lo usa) |
| T3 | USER_A / MAIN | TC-SEC-017 (STAFF sin rol: 403) |
| T4 | USER_A / MAIN | TC-BIZ-012 y 013 (uso y segundo uso) |
| T5 | USER_A / SOLD | **Ticket generado en el escenario SOLD_OUT**: su compra agota V1 y el evento pasa a SOLD_OUT (TC-BIZ-014) |

## 9. Repetir y restaurar

### R-A — Repetir sin borrar nada (por defecto)

Elegir un `runId` nuevo y empezar en E0 con el paso `venues`. Los datos de ejecuciones anteriores
quedan en la base sin interferir, porque cada ejecución usa sus propios emails y códigos (§15.2).

### R-B — Restaurar desde cero

Borra **solo** el contenedor y el volumen E2E. Hacerlo en este orden y detenerse si alguna
verificación no coincide:

1. Detener la aplicación (`Ctrl+C` en su terminal).
2. Verificar el contenedor. Debe mostrar `/pulsepass-e2e-db e2e pulsepass-e2e-data` y, en sus
   variables, `POSTGRES_DB=pulsepass_e2e`:

   ```bash
   docker inspect pulsepass-e2e-db --format '{{.Name}} {{index .Config.Labels "com.pulsepass.purpose"}} {{range .Mounts}}{{.Name}}{{end}}'
   ```

   ```bash
   docker inspect pulsepass-e2e-db --format '{{range .Config.Env}}{{println .}}{{end}}' | grep '^POSTGRES_DB='
   ```

3. Verificar el volumen. Debe mostrar `pulsepass-e2e-data e2e`:

   ```bash
   docker volume inspect pulsepass-e2e-data --format '{{.Name}} {{index .Labels "com.pulsepass.purpose"}}'
   ```

4. Anotar el estado de la base de desarrollo (sección 6, punto 3).
5. Borrar por nombre exacto (el contenedor **sin** `-v`):

   ```bash
   docker rm -f pulsepass-e2e-db
   ```

   ```bash
   docker volume rm pulsepass-e2e-data
   ```

6. Repetir la sección 4 (crear), la 5 (arrancar: Flyway aplica V1–V4), la 6 (comprobaciones, incluida
   la de la base de desarrollo, que debe seguir igual) y el paso `venues` con un `runId` nuevo.

### Prohibido

- `docker system prune`, `docker volume prune`, `docker container prune` o `docker image prune`.
- `docker rm` o `docker volume rm` con comodines, `xargs` o listas generadas.
- `docker compose down -v`.
- `DROP DATABASE`, `TRUNCATE` o `DELETE` masivos: no hacen falta, el contenedor es exclusivo.
- Cualquier comando sobre `pulsepass-db` o su volumen que no sea el `docker inspect` de lectura.
- Ejecutar `seed-e2e.sql` contra una base que no sea `pulsepass_e2e` (el propio script lo impide).

## 10. Al terminar la sesión

1. Detener la aplicación (`Ctrl+C`).
2. Detener la base sin borrarla:

   ```bash
   docker stop pulsepass-e2e-db
   ```

3. Cerrar la terminal: los secretos exportados desaparecen con ella.
4. Comprobar que no quedan tokens ni contraseñas en archivos versionables (`git status`).
