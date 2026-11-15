# PulsePass — Backend · Fase 1: Persistencia

Capa de persistencia de PulsePass, plataforma de gestión y venta de entradas para eventos,
implementada según el **PRD de Persistencia** (`docs/proyecto Pulsepass.pdf`, páginas 1–24).

Esta fase cubre el modelo relacional, las migraciones Flyway, las entidades JPA, los
repositorios Spring Data y las pruebas de integración contra PostgreSQL real. Quedan fuera de
alcance (PRD §18): API REST, capa de servicios, seguridad, frontend y reglas de negocio como el
cálculo de `SOLD_OUT`.

## Tecnologías

| Tecnología | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.2 |
| Maven (mediante Maven Wrapper `./mvnw`) | 3.10.0 |
| Spring Data JPA / Hibernate | Hibernate 7.4.13 |
| PostgreSQL | 17 (imagen `postgres:17`) |
| Flyway | 12.4.0 |
| Testcontainers | 2.0.5 |
| JUnit (Jupiter) | 6.0.3 — ver [DEV-001](#desviación-dev-001-junit-6) |
| AssertJ | 3.27.7 |

No se usa H2 ni Lombok.

## Requisitos para ejecutar

* Java 21.
* Docker en ejecución: las pruebas levantan PostgreSQL 17 con Testcontainers.
* No hace falta instalar Maven ni PostgreSQL.

## Estructura

```
backend/
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/            Maven Wrapper
└── src/
    ├── main/java/com/pulsepass/
    │   ├── PulsepassApplication.java
    │   ├── domain/                  Entidades JPA
    │   │   └── enums/               EventCategory, EventStatus, TicketType, TicketStatus
    │   └── repository/              Repositorios Spring Data JPA
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/            V1, V2 y V3 (Flyway)
    └── test/java/com/pulsepass/
        ├── TestcontainersConfiguration.java
        └── persistence/             Pruebas de integración y PersistenceFixtures
```

## Modelo de dominio

| Entidad | Tabla | Relaciones |
|---|---|---|
| `Venue` | `venues` | — |
| `Event` | `events` | `@ManyToOne` → `Venue` (cada evento tiene exactamente un venue) · `@ManyToMany` → `Artist` mediante `event_artists` |
| `Artist` | `artists` | No declara la relación: la N:M es unidireccional desde `Event.artists` |
| `User` | `users` | `@OneToOne(mappedBy = "user")` → `UserProfile`, con cascade `PERSIST` y `MERGE` |
| `UserProfile` | `user_profiles` | `@OneToOne` → `User` (lado propietario: FK `user_id`) |
| `Ticket` | `tickets` | `@ManyToOne` → `User` y `@ManyToOne` → `Event` |

* Cardinalidades del PRD (§6): Venue 1:N Event · Event N:M Artist · User 1:1 UserProfile ·
  User 1:N Ticket · Event 1:N Ticket.
* `Ticket` es una entidad propia y no un `@ManyToMany` entre `User` y `Event`, porque tiene datos
  propios: código, tipo, precio, estado y fecha de compra (BR-006).
* `@ManyToOne` (`Event.venue`, `Ticket.user`, `Ticket.event`), `@ManyToMany` (`Event.artists`) y
  el lado propietario `UserProfile.user` usan `FetchType.LAZY`.
* `User.profile` es el lado inverso (`mappedBy = "user"`) y no declara `fetch`: usa el valor
  predeterminado de `@OneToOne` (EAGER), y en el lado inverso Hibernate necesita consultar
  `user_profiles` para saber si el perfil existe, así que el perfil se carga junto con el usuario.
* El perfil se asocia con `User.setProfile(...)`, que sincroniza ambos lados de la relación.
* Los enums se guardan por nombre (`@Enumerated(EnumType.STRING)`, BR-008).
* Los precios usan `BigDecimal` con `NUMERIC(12,2)` (BR-007, NFR-008).

## Migraciones (Flyway)

Flyway es el único responsable del esquema; Hibernate solo lo valida
(`spring.jpa.hibernate.ddl-auto: validate`). Una migración aplicada nunca se modifica: cualquier
cambio va en una versión nueva.

| Migración | Contenido |
|---|---|
| `V1__create_schema.sql` | Las 7 tablas con claves, restricciones e índices |
| `V2__insert_initial_artists.sql` | Los 5 artistas iniciales: Solar Beat, Neon Waves, Caribbean Sound, Ocean Drive y Digital Pulse |
| `V3__add_streaming_url_to_event.sql` | Columna opcional `events.streaming_url VARCHAR(500)` (FR-EVT-006) |

La migración V4 (credenciales y roles) pertenece a la Fase 4 (Seguridad) y no forma parte de
esta fase.

### Restricciones de V1

| Tipo | Restricciones |
|---|---|
| Claves primarias | `pk_venues`, `pk_events`, `pk_artists`, `pk_event_artists` (compuesta), `pk_users`, `pk_user_profiles`, `pk_tickets` |
| Claves foráneas (`ON DELETE NO ACTION`) | `fk_events_venue`, `fk_event_artists_event`, `fk_event_artists_artist`, `fk_user_profiles_user`, `fk_tickets_user`, `fk_tickets_event` |
| UNIQUE | `uk_venues_code`, `uk_events_event_code`, `uk_artists_stage_name`, `uk_users_username`, `uk_users_email`, `uk_user_profiles_user_id`, `uk_tickets_ticket_code` |
| CHECK | `ck_venues_capacity`, `ck_events_category`, `ck_events_status`, `ck_events_minimum_age`, `ck_tickets_type`, `ck_tickets_status`, `ck_tickets_price` |
| Índices | `ix_events_venue_id`, `ix_event_artists_artist_id`, `ix_tickets_user_id`, `ix_tickets_event_id` |

## Consultas

Las consultas simples usan *query methods*; las complejas, JPQL con `@Query` (NFR-007, §14).
No se usa SQL nativo.

| Repositorio | Método | Mecanismo | Requisito |
|---|---|---|---|
| `VenueRepository` | `findByCode` | Query method | FR-VEN-001, AC-001 |
| `ArtistRepository` | `findByStageName` | Query method | Lectura de los artistas de V2 |
| `EventRepository` | `findByEventCode` | Query method | AC-002 |
| `EventRepository` | `findByStatusOrderByEventDateAsc` | Query method | FR-EVT-005, AC-006 |
| `EventRepository` | `findByVenue_Code` | Query method (navega la relación) | FR-VEN-004 |
| `EventRepository` | `findByArtistStageName` | JPQL con `JOIN` y `DISTINCT` | FR-ART-004, FR-SRC-001, AC-007 |
| `EventRepository` | `findByVenueCityAndArtistStageName` | JPQL con varias asociaciones | FR-SRC-002 |
| `EventRepository` | `findRecommended` | JPQL con filtros, `DISTINCT` y orden | FR-SRC-003 |
| `UserRepository` | `findByEmailIgnoreCase` | Query method | §14 |
| `TicketRepository` | `findByUser_EmailIgnoreCase` | Query method (navega la relación) | FR-TKT-006 |
| `TicketRepository` | `findByUser_EmailIgnoreCaseAndStatus` | Query method (navega la relación) | FR-TKT-006 |
| `TicketRepository` | `findByEvent_EventCodeAndStatus` | Query method (navega la relación) | FR-TKT-007 |
| `TicketRepository` | `countByEventEventCodeAndStatus` | JPQL con `COUNT` | FR-TKT-008, AC-008 |
| `TicketRepository` | `findByEvent_EventDateAfterOrderByEvent_EventDateAsc` | Query method | FR-SRC-004 |

`findById`, `save` y `saveAndFlush` se heredan de `JpaRepository`.

## Pruebas

### Cómo ejecutarlas

Con Docker en ejecución, desde la carpeta `backend`:

```bash
./mvnw clean test
```

Para ejecutar una sola clase:

```bash
./mvnw test -Dtest=EventSearchIT
```

Maven Surefire ejecuta tanto las clases `*Test` como las `*IT`, por lo que `./mvnw clean test`
incluye todas las pruebas de integración (QT-010).

### Qué cubren

Las siete clases del paquete `persistence` usan `@DataJpaTest` con
`@Import(TestcontainersConfiguration.class)`: Flyway construye el esquema sobre PostgreSQL 17 y
Hibernate lo valida antes de ejecutar las pruebas. `PulsepassApplicationTests` usa
`@SpringBootTest` con la misma configuración y comprueba que arranca la aplicación completa.

| Clase | Pruebas | Cubre |
|---|---|---|
| `FlywayMigrationIT` | 4 | QT-001, QT-002, NFR-002..NFR-005 y el contenido de V2 |
| `VenuePersistenceTest` | 6 | FR-VEN-001..003, AC-001, QT-009 |
| `EventRepositoryIT` | 9 | FR-EVT-001..006, FR-VEN-004, AC-002, AC-006, QT-003, QT-007 |
| `EventArtistIT` | 4 | FR-ART-001..004, AC-003, QT-005, QT-008 |
| `UserProfileIT` | 6 | FR-USR-001..004, AC-004, QT-004, QT-007 |
| `TicketRepositoryIT` | 12 | FR-TKT-001..008, AC-005, AC-008, QT-006..QT-009 |
| `EventSearchIT` | 4 | FR-SRC-001..004, AC-007, QT-008 |
| `PulsepassApplicationTests` | 1 | Arranque completo de la aplicación |

Al cierre de la Fase 1, `./mvnw clean test` terminó con `Tests run: 46, Failures: 0, Errors: 0,
Skipped: 0` y `BUILD SUCCESS`.

* Las violaciones de restricciones se comprueban con el SQLSTATE y el nombre real de la
  restricción que informa PostgreSQL.
* Las lecturas con JPA de datos guardados por la propia prueba van precedidas de `flush()` y
  `clear()`, para leerlos de PostgreSQL y no de la caché de JPA. Las lecturas con `JdbcTemplate`
  no usan esa caché y solo necesitan el `flush` previo.
* `PersistenceFixtures` solo construye objetos con los datos de referencia del PRD (§16); los
  valores que el PRD no define están marcados como `DATO DE PRUEBA`.

## Trazabilidad

| Requisito | Implementación | Prueba |
|---|---|---|
| FR-VEN-001..003 | `Venue`, `VenueRepository`, `uk_venues_code`, `ck_venues_capacity` | `VenuePersistenceTest` |
| FR-VEN-004 | `EventRepository.findByVenue_Code` | `EventRepositoryIT` |
| FR-EVT-001..005 | `Event`, `EventRepository`, `uk_events_event_code`; estado y categoría como `@Enumerated(STRING)`, respaldados por `ck_events_status` y `ck_events_category` | `EventRepositoryIT` |
| FR-EVT-006 | `V3__add_streaming_url_to_event.sql`, `Event.streamingUrl` | `EventRepositoryIT`, `FlywayMigrationIT` |
| FR-ART-001..003 | `Artist`, `ArtistRepository`, `Event.artists` (`Set`), `uk_artists_stage_name`; respaldo en la base de datos: `pk_event_artists` | `EventArtistIT` |
| FR-ART-004 | `EventRepository.findByArtistStageName` | `EventArtistIT` |
| FR-USR-001..004 | `User`, `UserProfile`, `UserRepository`, `uk_users_username`, `uk_users_email`, `uk_user_profiles_user_id` | `UserProfileIT` |
| FR-TKT-001..005 | `Ticket`, `TicketRepository`, `uk_tickets_ticket_code`, `ck_tickets_price`; tipo y estado como `@Enumerated(STRING)`, respaldados por `ck_tickets_type` y `ck_tickets_status` | `TicketRepositoryIT` |
| FR-TKT-006..008 | `TicketRepository` (consultas por email, por evento y conteo) | `TicketRepositoryIT` |
| FR-SRC-001..004 | `EventRepository` (JPQL) y `TicketRepository.findByEvent_EventDateAfterOrderByEvent_EventDateAsc` | `EventSearchIT` |
| NFR-002 / NFR-003 | Migraciones V1–V3 | `FlywayMigrationIT` |
| NFR-004 / NFR-005 | `TestcontainersConfiguration` (`postgres:17`) | Todas las pruebas de integración |

Las restricciones `ck_events_status`, `ck_events_category`, `ck_tickets_type`, `ck_tickets_status`
y `pk_event_artists` forman parte del esquema, pero las pruebas no las ejercitan directamente:
FR-EVT-003/004 y FR-TKT-004/005 se verifican comprobando que el valor se guarda como texto, y
AC-003 se verifica con el comportamiento del `Set` en JPA. Los valores fuera del catálogo no se
pueden crear porque el tipo enum de Java lo impide.

## Decisiones clave

* **Configuración:** `ddl-auto: validate` (PRD §13) y `open-in-view: false`. En esta fase no se
  configura ninguna conexión de base de datos: las pruebas la reciben de Testcontainers. Sin
  secretos en el repositorio.
* **Identificadores:** `BIGINT GENERATED BY DEFAULT AS IDENTITY`.
* **Enums:** columnas `VARCHAR(20)` con `CHECK` y los valores exactos del PRD.
* **Restricciones con nombre explícito** y claves foráneas con `ON DELETE NO ACTION`; sin valores
  por defecto en la base de datos.
* **`events.description` es `VARCHAR(500)`:** la longitud se toma del contador 0/500 del diseño
  del frontend, no de una especificación explícita del PRD.
* **`tickets.purchase_date` admite NULL:** un ticket `RESERVED` todavía no se ha comprado.
* **Artistas de V2:** `country` y `genre` quedan en NULL porque el PRD no los proporciona.
  `active = true` es una **inferencia** de los escenarios de los PRD posteriores (Servicios, E2E y
  Frontend), no un valor indicado textualmente para V2.
* **Datos de prueba separados por PRD:** las pruebas usan solo los datos de referencia del PRD de
  Persistencia (§16), sin mezclarlos con los escenarios de otros PRD.

### Desviación DEV-001: JUnit 6

Los PRD de Servicios, Controladores y Seguridad indican **JUnit 5**. El proyecto usa **JUnit 6.0.3
(Jupiter)**, la versión que gestiona Spring Boot 4.1.2.

* **Motivo:** el PRD exige Spring Boot 4.x, que incluye Spring Framework 7.0 (7.0.10 en este
  proyecto), y Spring Framework 7.0 establece JUnit 6 como requisito mínimo.
* **Impacto:** ninguno en la forma de escribir las pruebas. JUnit 6 conserva el modelo Jupiter y
  el paquete `org.junit.jupiter.api` (`@Test`, `@ExtendWith`, `@BeforeEach`). Las pruebas de la
  Fase 1 solo usan `@Test` y `@DisplayName` de ese paquete.

## Limitaciones conocidas

* **Email sin distinguir mayúsculas:** `uk_users_email` sí distingue mayúsculas; solo la búsqueda
  (`findByEmailIgnoreCase`) las ignora. La regla de unicidad sin distinguir mayúsculas corresponde
  a la Fase 2 (Servicios).
* **`purchase_date` y ordenamiento (consideración para la Fase 2):** ninguna consulta de la Fase 1
  ordena por `purchase_date`. El PRD de Servicios (§32) propone
  `findByUserEmailIgnoreCaseOrderByPurchaseDateDesc`; en PostgreSQL, el orden descendente coloca
  por defecto los NULL primero (`NULLS FIRST`), así que los tickets `RESERVED` sin fecha
  aparecerían al principio. La Fase 2 deberá decidir cómo tratarlos y asignar la fecha de compra
  a los tickets `PAID` (P1-D1).
* **Comodines en `findRecommended`:** los caracteres `%` y `_` del texto de búsqueda no se
  escapan, porque el PRD no define ese caso.
* **`SOLD_OUT`** no se calcula en esta fase (BR-010).
* **Rechazos de Hibernate antes del INSERT:** las pruebas de relaciones obligatorias (`venue`,
  `user`, `event`) esperan que Hibernate detecte el valor null antes de enviar la sentencia, lo
  que ocurre mientras el proyecto no incluya Bean Validation.
* **Código de pruebas:** los métodos auxiliares que verifican las restricciones de PostgreSQL
  están repetidos en varias clases de prueba; se mantienen así de forma intencional.
