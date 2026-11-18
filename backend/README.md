# PulsePass — Backend · Fases 1 (Persistencia), 2 (Servicios), 3 (Controladores REST) y 4 (Seguridad)

Backend de PulsePass, plataforma de gestión y venta de entradas para eventos. Se implementa por
fases, cada una según su PRD dentro de `docs/proyecto Pulsepass.pdf`:

* **Fase 1 — Persistencia** (PRD de Persistencia, páginas 1–24): modelo relacional, migraciones
  Flyway, entidades JPA, repositorios Spring Data y pruebas de integración contra PostgreSQL real.
* **Fase 2 — Servicios** (PRD de la Capa de Servicios): contratos `Service`, DTO, mappers
  MapStruct, excepciones de negocio, reglas de negocio, transacciones y pruebas unitarias con
  Mockito.
* **Fase 3 — Controladores REST** (PRD de la Capa de Controladores REST): 20 endpoints sobre los
  servicios, validación de entrada con Bean Validation, contrato uniforme de errores y pruebas
  de contrato HTTP con `@WebMvcTest` y MockMvc.
* **Fase 4 — Seguridad** (PRD de la Capa de Seguridad con Spring Security y JJWT, páginas 86–111):
  registro con contraseña BCrypt, login con JWT firmado, API sin sesión, roles USER, STAFF y ADMIN,
  autorización por URL y propiedad de los recursos con `@PreAuthorize`.

Siguen fuera de alcance: las pruebas E2E con Postman (Fase 5) y el frontend.

## Estado de las fases

| Fase | PRD | Estado | Cierre |
|---|---|---|---|
| 1 — Persistencia | Persistencia | Cerrada | Commit `35b3ac6`, etiqueta `fase-1-persistencia` |
| 2 — Servicios | Capa de Servicios | Cerrada | Commit `c863210`, etiqueta `fase-2-servicios`; [DoD](#definition-of-done-de-la-fase-2) en este documento |
| 3 — Controladores REST | Capa de Controladores REST | Cerrada | Commit `2d78fce`, etiqueta `fase-3-controladores`; [DoD](#definition-of-done-de-la-fase-3) en este documento |
| 4 — Seguridad | Capa de Seguridad (Spring Security y JJWT) | Cerrada | Etiqueta `fase-4-seguridad` (commit de cierre de este README); [DoD](#definition-of-done-de-la-fase-4) en este documento |

## Tecnologías

| Tecnología | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.2 (Spring Framework 7.0.10) |
| Maven (mediante Maven Wrapper `./mvnw`) | 3.10.0 |
| Spring MVC (Fase 3) | `spring-webmvc` 7.0.10, con Tomcat embebido 11.0.27 |
| Bean Validation (Fase 3) | Jakarta Validation 3.1.1, Hibernate Validator 9.1.5.Final |
| Jackson (Fase 3, JSON) | 3.1.7 (`tools.jackson`) |
| Spring Security (Fase 4) | 7.1.2 |
| JJWT (Fase 4) | 0.13.0 (`jjwt-api`; `jjwt-impl` y `jjwt-jackson` en tiempo de ejecución) |
| Spring Data JPA / Hibernate | Hibernate 7.4.13 |
| PostgreSQL | 17 (imagen `postgres:17`) |
| Flyway | 12.4.0 |
| MapStruct (Fase 2) | 1.6.3 |
| Testcontainers | 2.0.5 |
| JUnit (Jupiter) | 6.0.3 — ver [DEV-001](#desviación-dev-001-junit-6) |
| Mockito (Fase 2) | 5.23.0 |
| AssertJ | 3.27.7 |

No se usa H2 ni Lombok. MapStruct es la única dependencia que agregó la Fase 2 (P2-D13): la
biblioteca `org.mapstruct:mapstruct` y su procesador de anotaciones en `maven-compiler-plugin`.
Mockito y `junit-jupiter-params` llegan como dependencias transitivas de
`spring-boot-starter-data-jpa-test` (a través de `spring-boot-starter-test`), que ya existía.

La Fase 3 agregó tres starters, con las versiones del BOM de Spring Boot 4.1.2 (P3-D1):
`spring-boot-starter-webmvc` y `spring-boot-starter-validation`, y en pruebas
`spring-boot-starter-webmvc-test` (`@WebMvcTest` y MockMvc). Jackson, Tomcat y Hibernate
Validator llegan a través de ellos.

La Fase 4 agregó `spring-boot-starter-security`, las tres dependencias de JJWT 0.13.0 que fija el
PRD (§14.1) y, en pruebas, `spring-boot-starter-security-test` en lugar de `spring-security-test`
(ver [DEV-002](#desviación-dev-002-spring-boot-starter-security-test)). `jjwt-jackson` trae
Jackson 2.21.7 solo en tiempo de ejecución para leer los claims; Spring MVC sigue usando Jackson 3.

## Requisitos para ejecutar

* Java 21.
* Docker en ejecución: las pruebas de integración levantan PostgreSQL 17 con Testcontainers.
* No hace falta instalar Maven ni PostgreSQL.
* Desde la Fase 4, la aplicación necesita la clave del JWT en `PULSEPASS_JWT_SECRET_BASE64` (ver
  [Variables de entorno](#variables-de-entorno)). Las pruebas usan una clave ficticia propia y no
  necesitan ninguna variable.

## Estructura

```
backend/
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/            Maven Wrapper
└── src/
    ├── main/java/com/pulsepass/
    │   ├── PulsepassApplication.java
    │   ├── config/                  ClockConfig: bean Clock con zona America/Bogota (Fase 2)
    │   ├── controller/              Venue, Artist, User, Event y TicketController (Fase 3);
    │   │                            AuthController (Fase 4)
    │   ├── domain/                  Entidades JPA; UserCredential, UserRole y UserRoleId (Fase 4)
    │   │   └── enums/               EventCategory, EventStatus, TicketType, TicketStatus; Role (Fase 4)
    │   ├── dto/                     DTO como record (Fase 2)
    │   │   ├── request/             CreateEventRequest, RegisterUserRequest, PurchaseTicketRequest
    │   │   │                        (con Bean Validation desde la Fase 3); LoginRequest (Fase 4)
    │   │   └── response/            VenueResponse, ArtistResponse, UserResponse, EventResponse,
    │   │                            EventSummaryResponse, TicketResponse; ErrorResponse (Fase 3);
    │   │                            AuthResponse y AuthenticatedUserResponse (Fase 4)
    │   ├── exception/               ResourceNotFoundException, DuplicateResourceException,
    │   │                            BusinessRuleException (Fase 2); GlobalExceptionHandler (Fase 3);
    │   │                            ErrorResponseFactory (Fase 4)
    │   ├── mapper/                  Mappers MapStruct (Fase 2)
    │   ├── repository/              Repositorios Spring Data JPA; UserCredential y UserRole (Fase 4)
    │   ├── security/                Fase 4: SecurityConfig, MethodSecurityConfig, JWT (JwtService,
    │   │                            JwtProperties, JwtConfig, AccessToken), JwtAuthenticationFilter,
    │   │                            CustomUserDetailsService, LoginUser, SecurityPrincipal,
    │   │                            RestAuthenticationEntryPoint, RestAccessDeniedHandler y RequestIdFilter
    │   ├── service/                 Interfaces de servicio, TicketPricingStrategy y
    │   │   │                        TicketCodeGenerator (Fase 2); AuthService y AuthorizationService (Fase 4)
    │   │   └── impl/                Implementaciones (Fase 2 y Fase 4)
    │   └── validation/              @MaxUtf8Bytes y su validador (Fase 4)
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/            V1, V2, V3 y V4 (Flyway)
    ├── .env.example                 Plantilla de variables de entorno, sin secretos (Fase 4)
    └── test/
        ├── resources/config/application.yml  Clave JWT ficticia de las pruebas (Fase 4)
        └── java/com/pulsepass/
            ├── PulsepassApplicationTests.java
            ├── TestcontainersConfiguration.java, TestPulsepassApplication.java
            ├── FixedClockTestConfiguration.java  Reloj fijo para las pruebas web (Fase 3)
            ├── persistence/             Pruebas de integración y PersistenceFixtures
            ├── mapper/                  Pruebas de los mappers reales (Fase 2)
            ├── service/                 ServiceFixtures (Fase 2)
            │   └── impl/                Pruebas unitarias de servicios y componentes (Fase 2 y Fase 4)
            ├── dto/request/             Pruebas de Bean Validation de los request DTO (Fase 3 y Fase 4)
            ├── exception/               GlobalExceptionHandlerTest (Fase 3)
            ├── controller/              Pruebas @WebMvcTest de los controladores y ControllerFixtures (Fase 3);
            │                            AuthControllerTest, EndpointSecurityMatrixTest y
            │                            EventDraftVisibilityTest (Fase 4)
            ├── security/                Pruebas de JWT, filtros y manejo de 401/403, y el soporte
            │                            WebSecurityTestConfiguration y @WithSecurityPrincipal (Fase 4)
            ├── validation/              MaxUtf8BytesValidatorTest (Fase 4)
            └── integration/             Pruebas de seguridad con la aplicación completa (Fase 4)
```

## Arquitectura

```
HTTP ──► RequestIdFilter                     request ID en el MDC                 [Fase 4]
     ──► Cadena de Spring Security            JwtAuthenticationFilter + reglas de URL; 401/403 con
             │                                ErrorResponse (EntryPoint / AccessDeniedHandler)  [Fase 4]
             ▼
         Controller (@RestController)        valida el cuerpo (@Valid) y delega   [Fase 3]
             │                ▲
             │                └── GlobalExceptionHandler (@RestControllerAdvice)  excepción → ErrorResponse
             ▼
         @PreAuthorize en la interfaz        propiedad con el bean authz           [Fase 4]
             ▼
         Service (interfaz) ──► ServiceImpl (@Service)                            [Fase 2]
                                 ├── Repository (Spring Data JPA)      acceso a datos, sin reglas
                                 ├── Mapper (MapStruct)                Entity → DTO
                                 ├── Clock (ClockConfig)               "ahora" inyectable
                                 ├── TicketPricingStrategy             precio (solo TicketService)
                                 └── TicketCodeGenerator               código de ticket (solo TicketService)
```

* Los controladores solo traducen HTTP a llamadas de servicio: no acceden a repositorios, no
  contienen reglas de negocio y no exponen entidades JPA (CTRL-001..004, CTRL-009). La
  validación estructural es suya (Bean Validation); las reglas de negocio, del servicio (§11.4).
* Los errores se traducen en un único punto, `GlobalExceptionHandler`; los controladores no
  tienen `try/catch` (CTRL-007, §15).
* Los servicios reciben y devuelven DTO; nunca exponen entidades JPA (SRV-001).
* Cada servicio tiene interfaz e implementación en `service.impl` (SRV-003), con dependencias
  `final` inyectadas por constructor (SRV-002, NFR-002).
* Las reglas de negocio viven en los servicios; los repositorios solo acceden a datos (SRV-005,
  NFR-006). No hay SQL en los servicios.
* `open-in-view: false`: el mapeo a DTO ocurre dentro de la transacción del servicio, donde las
  relaciones `LAZY` todavía se pueden cargar.
* La seguridad tiene dos niveles (Fase 4): las reglas de URL de `SecurityConfig` deciden quién
  puede llamar a cada ruta según su rol, y `@PreAuthorize` en las interfaces de `UserService` y
  `TicketService` decide si el recurso es suyo. Las reglas de negocio no cambian.

---

# Fase 1 — Persistencia

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

Las Fases 2 y 3 no agregaron ni modificaron migraciones. La migración V4 (credenciales y roles)
pertenece a la Fase 4 (Seguridad).

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

### Métodos agregados en la Fase 2 (PRD de Servicios §32)

La Fase 2 agregó 9 métodos sin modificar ni eliminar los anteriores (P2-D6). Los prueba
`ServiceRepositoryIT` contra PostgreSQL (P2-D8).

| Repositorio | Método | Mecanismo |
|---|---|---|
| `VenueRepository` | `findByActiveTrueOrderByNameAsc` | Query method |
| `EventRepository` | `existsByEventCode` | Query method |
| `ArtistRepository` | `findByStageNameIgnoreCase` | Query method (se conserva `findByStageName` exacto) |
| `ArtistRepository` | `findByActiveTrueOrderByStageNameAsc` | Query method |
| `UserRepository` | `findByUsername` | Query method |
| `UserRepository` | `existsByEmailIgnoreCase` | Query method |
| `UserRepository` | `existsByUsername` | Query method |
| `TicketRepository` | `findByTicketCode` | Query method |
| `TicketRepository` | `findByUserEmailIgnoreCaseOrderByPurchaseDateDesc` | JPQL con `ORDER BY purchaseDate DESC NULLS LAST` (P2-D7) |

---

# Fase 2 — Servicios

## Servicios y operaciones

| Servicio | Operación | Requisito | Transacción |
|---|---|---|---|
| `VenueService` | `findByCode` | FR-SVC-001 | `readOnly` |
| `VenueService` | `findActiveVenues` | FR-SVC-002 | `readOnly` |
| `EventService` | `create` | FR-SVC-003 | escritura |
| `EventService` | `findByCode` | FR-SVC-004 | `readOnly` |
| `EventService` | `findPublishedEvents` | FR-SVC-005 | `readOnly` |
| `EventService` | `publish` | FR-SVC-006 | escritura |
| `EventService` | `addArtist` | FR-SVC-007 | escritura |
| `EventService` | `findByArtist` | FR-SVC-008 | `readOnly` |
| `ArtistService` | `findById`, `findByStageName`, `findActiveArtists` | FR-SVC-009 | `readOnly` |
| `UserService` | `register` | FR-SVC-010 | escritura |
| `UserService` | `findByEmail` | FR-SVC-011 | `readOnly` |
| `UserService` | `findByUsername` | FR-SVC-012 | `readOnly` |
| `TicketService` | `purchase` | FR-SVC-013 | escritura |
| `TicketService` | `findByCode` | FR-SVC-014 | `readOnly` |
| `TicketService` | `findByUserEmail` | FR-SVC-015 | `readOnly` |
| `TicketService` | `findPaidTicketsByEvent` | FR-SVC-016 | `readOnly` |
| `TicketService` | `cancel` | FR-SVC-017 | escritura |
| `TicketService` | `markAsUsed` | FR-SVC-018 | escritura |

Son 5 servicios y 20 operaciones. Cada operación declara su propio `@Transactional` de Spring
(P2-D14): 7 escrituras y 13 lecturas con `readOnly = true` (SRV-004, §38).

## DTO y mappers

* **9 DTO**, todos `record` (NFR-003): 3 de entrada en `dto/request` y 6 de salida en
  `dto/response`, los que sugiere el PRD (§8). Sus campos reflejan las entidades sin agregar datos
  (P2-D1). `EventResponse.artists` es `List<ArtistResponse>`. `EventResponse` no incluye
  `streamingUrl`, porque §13 no lo pide (P2-04).
* `PurchaseTicketRequest` no incluye precio: el precio lo define el sistema (§22).
* Los servicios no validan la estructura de los DTO (P2-D18). En la Fase 2 los request DTO no
  llevaban Bean Validation; desde la Fase 3 (P3-03) la llevan y la aplican los controladores con
  `@Valid` (ver [Validación de entrada](#validación-de-entrada)).
* **5 mappers MapStruct** con `componentModel = "spring"` y `unmappedTargetPolicy = ERROR`
  (P2-05 D-E1): si un campo del DTO queda sin origen, la compilación falla. `EventMapper` usa
  `ArtistMapper` inyectado por constructor (P2-05 D-A1). `TicketMapper` aplana `user.email`,
  `event.eventCode` y `event.name` (§34).

## Excepciones y mensajes

| Excepción | Uso (PRD §35) | Código HTTP (PRD de Controladores §14; implementado en la Fase 3) |
|---|---|---|
| `ResourceNotFoundException` | El recurso no existe | 404 |
| `DuplicateResourceException` | Conflicto de unicidad | 409 |
| `BusinessRuleException` | El recurso existe, pero la operación no es válida | 409 |

Las tres extienden `RuntimeException`, así que provocan el rollback de la transacción. Los
mensajes están en inglés (P2-D17) e identifican el recurso o la regla (NFR-005), por ejemplo
`Event not found: CMF-2026`. Los textos exactos son decisiones del proyecto (V-2a, E-5a, F-6a,
U-1a, TK-6, TC-4); el PRD solo da ejemplos.

`TicketService.purchase` también puede lanzar `IllegalStateException` si no consigue un código de
ticket único (TK-5). Es un fallo técnico, no una regla de negocio.

## Reglas de negocio

| Operación | Reglas (PRD) | Comportamiento |
|---|---|---|
| `VenueService` | BR-VENUE-001..002 | Venue inexistente → `ResourceNotFoundException`; solo venues activos, ordenados por nombre |
| `ArtistService` | BR-ARTIST-001..002 | Artista inexistente → `ResourceNotFoundException`; solo artistas activos, ordenados por nombre artístico. `findByStageName` ignora mayúsculas (A-1a) y devuelve también artistas inactivos (A-4a) |
| `EventService.create` | BR-EVENT-001..006 | Código único; venue existente y activo; fecha futura (estricta, P2-D4); `minimumAge >= 0` (P2-D5); el evento nace `DRAFT` |
| `EventService.publish` | BR-EVENT-007..009 | Solo desde `DRAFT`; la fecha sigue siendo futura; el venue sigue activo |
| `EventService.addArtist` | BR-EVENT-010..011 | Sin artistas duplicados (P2-D5); no en eventos `CANCELLED` ni `FINISHED`; se admiten artistas inactivos (F-7a) |
| `EventService.findByArtist` | — | Solo eventos `PUBLISHED` (P2-D15), búsqueda exacta, lista vacía si no hay |
| `UserService.register` | BR-USER-001..005 | Username único; email único ignorando mayúsculas; el usuario nace activo; `User` y `UserProfile` en un único `save()` con cascade; fecha de nacimiento no futura («hoy» se acepta, U-2a) |
| `UserService.findBy*` | — | Devuelven también usuarios inactivos (U-4a) |
| `TicketService.purchase` | BR-TICKET-001..009 | Ver el flujo siguiente |
| `TicketService.cancel` | BR-TICKET-010..012 | Solo `PAID` → `CANCELLED`; la fecha del evento debe ser estrictamente posterior a ahora (P2-D20) |
| `TicketService.markAsUsed` | BR-TICKET-013..014 | Solo `PAID` → `USED`; sin restricciones de fecha (P2-D19) |

### Flujo de compra (PRD §24 y §54)

1. El usuario existe (BR-TICKET-001) y está activo (BR-TICKET-002).
2. El evento existe (BR-TICKET-003), está `PUBLISHED` (BR-TICKET-004) y su fecha es estrictamente
   futura (BR-TICKET-005).
3. Edad mínima (BR-TICKET-006): con `minimumAge = 0` no se exige perfil (TK-2); si no, el perfil
   es obligatorio (P2-D5) y la edad se calcula **en la fecha del evento** con `Period.between`.
4. Capacidad (BR-TICKET-007): los tickets `PAID` y `USED` ocupan capacidad y los `CANCELLED` no
   (TK-1, decisión del proyecto; el PRD habla de `paidTickets`).
5. Precio calculado por `TicketPricingStrategy` (§28).
6. Ticket `PAID` (§27) con código único y `purchaseDate = now(clock)`; `save()` explícito (P2-D10).
7. Si la compra completa exactamente la capacidad, el evento pasa a `SOLD_OUT` en la misma
   transacción (BR-TICKET-008; TK-3: conteo anterior + 1).
8. Se devuelve el DTO del ticket guardado.

* Si falla una validación de negocio (pasos 1 a 4) o no se consigue un código único, la excepción
  se lanza **antes** del primer `save()`: no se escribe nada.
* Si ocurre una excepción durante las escrituras (al guardar el ticket, al guardar el evento como
  `SOLD_OUT` o al mapear la respuesta), la transacción de `purchase` debe deshacer **todas** las
  escrituras ya hechas (§39), porque las excepciones de tiempo de ejecución provocan rollback en
  `@Transactional`. Este rollback no se demuestra en las pruebas unitarias (ver «Limitaciones
  conocidas»).

## Precios (decisión de diseño)

El PRD de Servicios **no define importes**. Solo exige `BigDecimal`, que el precio nunca sea
negativo (BR-TICKET-009) y que la estrategia esté encapsulada y probada (§28). §28 sugiere el
esquema «precio base, descuento y multiplicadores» como una opción posible.

Los importes son **DATO DE DISEÑO** aprobado en P2-12 (TP-1..TP-5), no requisitos del PRD, y no
reutilizan los precios de ejemplo del PRD de Persistencia (P2-D2):

| Tipo | Regla (TP-1) | Precio (TP-2) |
|---|---|---|
| `GENERAL` | Precio base | 100000.00 |
| `STUDENT` | Base × 0.50 (descuento) | 50000.00 |
| `VIP` | Base × 2 (multiplicador) | 200000.00 |
| `BACKSTAGE` | Base × 3 (multiplicador superior) | 300000.00 |

* Constantes `BigDecimal` construidas con `String`, sin `double` ni `float` (TP-3).
* Resultado con escala 2 y `RoundingMode.HALF_UP`, como la columna `NUMERIC(12,2)` (TP-4).
* `switch` exhaustivo sobre `TicketType`: un tipo nuevo sin precio no compila.
* BR-TICKET-009 se garantiza por diseño y por prueba, con respaldo de `ck_tickets_price` (TP-5).

## Código de ticket

* `TKT-` seguido de 6 caracteres hexadecimales en mayúsculas, generados con `SecureRandom`
  (P2-D3, TK-4). Ejemplo: `TKT-8F2A1C`.
* `TicketCodeGenerator` solo genera; `TicketService` comprueba la colisión con `findByTicketCode`
  y reintenta hasta 5 veces (TK-5). La restricción `uk_tickets_ticket_code` sigue siendo la
  autoridad final ante una colisión concurrente.

## Reloj

`ClockConfig` expone un bean `Clock` con la zona `America/Bogota` (P2-D4). Todas las comparaciones
de fecha usan `LocalDateTime.now(clock)` y son estrictas (`isAfter`): una fecha igual a «ahora» no
es futura. Las pruebas usan `Clock.fixed(...)` y no dependen del día de ejecución.

---

# Fase 3 — Controladores REST

## Endpoints

Los 20 endpoints del PRD de Controladores §8, uno por cada método público de servicio (§9). Todos
devuelven `ResponseEntity` con DTO, nunca entidades. Los cuerpos y las respuestas son JSON.

| Método | Ruta | Servicio | Éxito | Errores |
|---|---|---|---|---|
| GET | `/api/venues/{code}` | `VenueService.findByCode` | 200 `VenueResponse` | 404 |
| GET | `/api/venues/active` | `VenueService.findActiveVenues` | 200 `List<VenueResponse>` | — |
| POST | `/api/events` | `EventService.create` | **201** `EventResponse` | 400, 404 (venue), 409 |
| GET | `/api/events/{eventCode}` | `EventService.findByCode` | 200 `EventResponse` | 404 |
| GET | `/api/events/published` | `EventService.findPublishedEvents` | 200 `List<EventSummaryResponse>` | — |
| PATCH | `/api/events/{eventCode}/publish` | `EventService.publish` | 200 `EventResponse` | 404, 409 |
| POST | `/api/events/{eventCode}/artists/{artistId}` | `EventService.addArtist` | **200** `EventResponse` (§8.2: no es una creación) | 400 (`artistId` no numérico), 404, 409 |
| GET | `/api/events/by-artist?stageName=...` | `EventService.findByArtist` | 200 `List<EventSummaryResponse>` | 400 (sin `stageName`) |
| GET | `/api/artists/{id}` | `ArtistService.findById` | 200 `ArtistResponse` | 400 (`id` no numérico), 404 |
| GET | `/api/artists/by-stage-name?stageName=...` | `ArtistService.findByStageName` | 200 `ArtistResponse` | 400 (sin `stageName`), 404 |
| GET | `/api/artists/active` | `ArtistService.findActiveArtists` | 200 `List<ArtistResponse>` | — |
| POST | `/api/users` | `UserService.register` | **201** `UserResponse` | 400, 409 |
| GET | `/api/users/by-email?email=...` | `UserService.findByEmail` | 200 `UserResponse` | 400 (sin `email`), 404 |
| GET | `/api/users/by-username?username=...` | `UserService.findByUsername` | 200 `UserResponse` | 400 (sin `username`), 404 |
| POST | `/api/tickets` | `TicketService.purchase` | **201** `TicketResponse` | 400, 404 (usuario o evento), 409, 500 (TK-5) |
| GET | `/api/tickets/{ticketCode}` | `TicketService.findByCode` | 200 `TicketResponse` | 404 |
| GET | `/api/tickets/by-user?email=...` | `TicketService.findByUserEmail` | 200 `List<TicketResponse>` | 400 (sin `email`) |
| GET | `/api/events/{eventCode}/tickets/paid` | `TicketService.findPaidTicketsByEvent` | 200 `List<TicketResponse>` | — |
| PATCH | `/api/tickets/{ticketCode}/cancel` | `TicketService.cancel` | 200 `TicketResponse` | 404, 409 |
| PATCH | `/api/tickets/{ticketCode}/use` | `TicketService.markAsUsed` | 200 `TicketResponse` | 404, 409 |

La columna «Errores» recoge los errores específicos de cada endpoint: los de entrada (400) y los
de dominio que lanza su servicio (404 y 409). Además, cualquier endpoint puede responder 500 si
ocurre una excepción inesperada, y los errores de Spring MVC con estado 5xx pueden conservar su
estado original (por ejemplo, 503). En ambos casos `GlobalExceptionHandler` responde con
`ErrorResponse` y el mensaje genérico `Unexpected error` (ver
[Contrato de errores](#contrato-de-errores)).

Comportamientos del contrato:

* Los 201 no incluyen la cabecera `Location`, porque el PRD no la pide (P3-D8).
* Los `PATCH` y `POST /api/events/{eventCode}/artists/{artistId}` no declaran cuerpo; si se envía
  uno, se ignora (comportamiento de P3-08, confirmado para los tickets en TKC-5).
* `GET /api/events/{eventCode}/tickets/paid` está en `TicketController`, que usa el prefijo
  `/api` (P3-D2, TKC-1).
* Las listas vacías responden 200 con `[]`. `GET /api/tickets/by-user` y
  `GET /api/events/{eventCode}/tickets/paid` también responden `[]` si el usuario o el evento no
  existen (P2-13 TK-7).
* Los parámetros de ruta y de consulta no se validan con Bean Validation (P3-D9). Spring MVC sí
  los convierte al tipo declarado (por ejemplo, `id` y `artistId` a `Long`): un valor
  incompatible, o un parámetro obligatorio ausente, responde 400 antes de llegar al servicio. Los
  valores que sí se convierten llegan al servicio sin más cambios; ignorar mayúsculas en emails y
  nombres artísticos lo hace el servicio.
* Las rutas literales (`/active`, `/published`, `/by-artist`, `/by-stage-name`, `/by-user`)
  tienen prioridad sobre las rutas con variable.

## Validación de entrada

Los tres endpoints con cuerpo validan el request con `@Valid` antes de llamar al servicio
(§11, CTRL-005). Una petición inválida responde 400 y **no llega al servicio** (QT-CTRL-007).
Los mínimos son los del PRD (§11.1–§11.3); las longitudes máximas son las de las columnas de V1
(P3-D4); los mensajes están en inglés (P3-D5).

| DTO | Campo | Validación | Origen |
|---|---|---|---|
| `CreateEventRequest` | `eventCode`, `venueCode` | `@NotBlank`, máx. 30 | PRD / V1 |
| | `name` | `@NotBlank`, máx. 150 | PRD / V1 |
| | `description` | Opcional, máx. 500 | PRD («longitud máxima definida») / V1-4 |
| | `category`, `eventDate` | `@NotNull` | PRD |
| | `minimumAge` | `@NotNull`, `@Min(0)` | PRD |
| `RegisterUserRequest` | `username` | `@NotBlank`, máx. 50 | PRD / V1 |
| | `email` | `@NotBlank`, `@Email`, máx. 254 | PRD / V1 |
| | `firstName`, `lastName` | `@NotBlank`, máx. 100 | PRD / V1 |
| | `phone` / `city` | Opcionales, máx. 30 / 100 | V1 |
| | `birthDate` | `@NotNull` | PRD |
| `PurchaseTicketRequest` | `userEmail` | `@NotBlank`, `@Email`, máx. 254 | PRD / V1 |
| | `eventCode` | `@NotBlank`, máx. 30 | PRD / V1 |
| | `type` | `@NotNull` | PRD |

Las reglas de negocio no se validan en el controlador: que la fecha sea futura, la unicidad, que
los recursos existan, la edad, la capacidad y las transiciones de estado son del servicio
(§11.4).

## Contrato de errores

Todos los errores usan `ErrorResponse` (§13, NFR-CTRL-003):

```json
{
  "timestamp": "2026-11-15T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": { "eventCode": "Event code is required" }
}
```

* `timestamp`: hora local de America/Bogota del `Clock` inyectado, truncada a segundos (P3-D7).
* `error`: la frase estándar del estado HTTP.
* `details`: campo → mensaje en los errores de validación; si un campo tiene varios errores, sus
  mensajes se unen en orden alfabético con `; ` (P3-D6). En los demás errores es `{}`.

`GlobalExceptionHandler` (`@RestControllerAdvice`) es el único punto de traducción (§15). Extiende
`ResponseEntityExceptionHandler` para que también los errores propios de Spring MVC usen
`ErrorResponse`, con su estado HTTP real (P3-D3).

| Excepción o condición | HTTP | `message` | Origen |
|---|---|---|---|
| `MethodArgumentNotValidException` (Bean Validation) | 400 | `Validation failed`, con `details` | PRD §13, §14 |
| `HttpMessageNotReadableException` (JSON mal formado, enum o fecha ilegibles) | 400 | `Malformed JSON request` | PRD §14 / P3-D5 |
| `MissingServletRequestParameterException` | 400 | `Required parameter '<nombre>' is missing` | P3-D3 / P3-D5 |
| `TypeMismatchException` (por ejemplo, `id` no numérico) | 400 | `Invalid value for parameter '<nombre>'` | P3-D3 / P3-D5 |
| `ResourceNotFoundException` | 404 | El del servicio | PRD §14 |
| Ruta inexistente | 404 | `No endpoint found for <MÉTODO> <ruta>` | P3-D3 / P3-D5 |
| `HttpRequestMethodNotSupportedException` | 405, con `Allow` | `HTTP method '<MÉTODO>' is not supported for this endpoint` | P3-D3 / P3-D5 |
| `HttpMediaTypeNotSupportedException` | 415, con `Accept` | `Content type '<tipo/subtipo>' is not supported` | P3-D3 / P3-D5 |
| `DuplicateResourceException` | 409 | El del servicio | PRD §14 |
| `BusinessRuleException` | 409 | El del servicio | PRD §14 |
| Cualquier otra excepción, o un error MVC con estado 5xx | 500 (o el 5xx original) | `Unexpected error`, con `details` vacío | PRD §14 / P3-D10 |

Una respuesta 5xx nunca incluye el mensaje, la clase ni la traza de la excepción: solo se
registran en el log del servidor (NFR-CTRL-007). `DataIntegrityViolationException` e
`IllegalStateException` (TK-5) responden 500 (P3-D10).

---

# Fase 4 — Seguridad

Sigue el PRD de la Capa de Seguridad (páginas 86–111). La API es **sin sesión** (sin cookies ni
`JSESSIONID`): cada petición protegida lleva un JWT en la cabecera `Authorization: Bearer …`.

## Componentes

| Componente | Responsabilidad |
|---|---|
| `SecurityConfig` | Cadena de filtros sin sesión (CSRF, formLogin, httpBasic, logout y requestCache desactivados), reglas de URL, `PasswordEncoder` BCrypt de fuerza 10 y `AuthenticationManager` explícito |
| `MethodSecurityConfig` | Activa `@PreAuthorize` (`@EnableMethodSecurity`) |
| `JwtService` | Emite y valida el JWT: HS256, `sub` (id del usuario), `iss`, `iat`, `exp` y `jti` |
| `JwtAuthenticationFilter` | Lee una única cabecera Bearer, valida el token y carga la identidad actual del usuario |
| `CustomUserDetailsService` | Carga la identidad del token (por id) y la del login (por username o email), con roles y estado |
| `AuthService` | Registro atómico (usuario, credencial BCrypt y `ROLE_USER`), login y datos de `/me` |
| `AuthorizationService` (bean `authz`) | Visibilidad de borradores para STAFF/ADMIN y comprobaciones de propiedad por id persistente |
| `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler` | 401 y 403 de la cadena de filtros con `ErrorResponse` |
| `RequestIdFilter` | Request ID en el MDC y en cada línea de log |

Datos (migración V4): `user_credentials` (1:1 con `users`, `password_hash VARCHAR(100)`) y
`user_roles` (PK `user_id` + `role`, con `CHECK` USER, STAFF y ADMIN). V4 asigna `ROLE_USER` a los
usuarios existentes sin crearles credencial (P4-D11).

## Endpoints de autenticación

| Verbo | Ruta | Acceso | Request | Respuesta |
|---|---|---|---|---|
| POST | `/api/users` | Público | `RegisterUserRequest` + `password` (12 caracteres mínimo, 72 bytes UTF-8 máximo) | 201 `UserResponse` (sin contraseña ni hash, sin token) |
| POST | `/api/auth/login` | Público | `LoginRequest(identifier, password)`; `identifier` es el username exacto o el email sin distinguir mayúsculas | 200 `AuthResponse(accessToken, tokenType = "Bearer", expiresIn)`; `expiresIn` son 900 segundos por defecto y cambia según `PULSEPASS_JWT_ACCESS_TOKEN_TTL` |
| GET | `/api/auth/me` | Autenticado | — | 200 `AuthenticatedUserResponse(id, username, email, roles)`; `roles` sin el prefijo `ROLE_`, en orden alfabético |

Unas credenciales incorrectas, un usuario inexistente, inactivo o sin credencial reciben **el
mismo** 401 «Invalid credentials» (P4-D9). Una contraseña de más de 72 bytes en el login responde
400 sin intentar autenticar (P4-07-A).

## Matriz de autorización

Las 20 operaciones heredadas y las 2 de autenticación (22 en total), probadas para los cuatro
actores en `EndpointSecurityMatrixTest`. «Propiedad» indica una regla `@PreAuthorize` adicional en
el servicio, probada en `OwnershipSecurityIT`. Un anónimo denegado recibe 401; un usuario
autenticado sin permiso, 403.

| Requisito | Operación | Anónimo | USER | STAFF | ADMIN | Propiedad |
|---|---|---|---|---|---|---|
| FR-SEC-VEN-01 | `GET /api/venues/{code}` | ✅ | ✅ | ✅ | ✅ | — |
| FR-SEC-VEN-02 | `GET /api/venues/active` | ✅ | ✅ | ✅ | ✅ | — |
| FR-SEC-EVT-01 | `POST /api/events` | 401 | 403 | 403 | ✅ | — |
| FR-SEC-EVT-02 | `GET /api/events/{eventCode}` | ✅* | ✅* | ✅ | ✅ | *Solo `PUBLISHED`; otro estado responde 404 (P4-D2) |
| FR-SEC-EVT-03 | `GET /api/events/published` | ✅ | ✅ | ✅ | ✅ | — |
| FR-SEC-EVT-04 | `PATCH /api/events/{eventCode}/publish` | 401 | 403 | 403 | ✅ | — |
| FR-SEC-EVT-05 | `POST /api/events/{eventCode}/artists/{artistId}` | 401 | 403 | 403 | ✅ | — |
| FR-SEC-EVT-06 | `GET /api/events/by-artist` | ✅ | ✅ | ✅ | ✅ | Solo `PUBLISHED` para todos (P2-D15, P4-D4) |
| FR-SEC-ART-01..03 | `GET /api/artists/{id}`, `/by-stage-name`, `/active` | ✅ | ✅ | ✅ | ✅ | — |
| FR-SEC-USR-01 | `POST /api/users` | ✅ | ✅ | ✅ | ✅ | Rol inicial siempre USER |
| FR-SEC-USR-02 | `GET /api/users/by-email` | 401 | ✅ | ✅ | ✅ | Propio usuario o ADMIN |
| FR-SEC-USR-03 | `GET /api/users/by-username` | 401 | ✅ | ✅ | ✅ | Propio usuario o ADMIN |
| FR-SEC-TKT-01 | `POST /api/tickets` | 401 | ✅ | 403 | 403 | USER y `userEmail` propio |
| FR-SEC-TKT-02 | `GET /api/tickets/{ticketCode}` | 401 | ✅ | ✅ | ✅ | Propietario, STAFF o ADMIN |
| FR-SEC-TKT-03 | `GET /api/tickets/by-user` | 401 | ✅ | ✅ | ✅ | Email propio o ADMIN |
| FR-SEC-TKT-04 | `GET /api/events/{eventCode}/tickets/paid` | 401 | 403 | ✅ | ✅ | STAFF o ADMIN |
| FR-SEC-TKT-05 | `PATCH /api/tickets/{ticketCode}/cancel` | 401 | ✅ | ✅ | ✅ | Propietario o ADMIN (STAFF no cancela tickets ajenos) |
| FR-SEC-TKT-06 | `PATCH /api/tickets/{ticketCode}/use` | 401 | 403 | ✅ | ✅ | STAFF o ADMIN |
| FR-SEC-AUTH-002 | `POST /api/auth/login` | ✅ | ✅ | ✅ | ✅ | — |
| FR-SEC-AUTH-010 | `GET /api/auth/me` | 401 | ✅ | ✅ | ✅ | — |

Cualquier otra ruta exige autenticación (`anyRequest().authenticated()`, P4-D7). No hay jerarquía
de roles (P4-D8): ADMIN no compra tickets por tener ese rol.

## Propiedad de los recursos

* La comprueba el bean `authz` comparando **ids persistentes**: el id del usuario autenticado
  (claim `sub`) con el del usuario buscado o el propietario del ticket leído de la base de datos.
  Nunca se confía solo en el email del request (§11.1).
* Un ticket ajeno responde **403**; un ticket inexistente, **404**. Las búsquedas de usuario, el
  listado por email y la compra responden 403 aunque el usuario objetivo no exista (P4-D1).
* Las denegaciones de `@PreAuthorize` llegan a `GlobalExceptionHandler`, que responde 403 con
  `ErrorResponse` y registra `Access denied: METHOD ruta userId=N` (P4-D19).
* STAFF tiene alcance global en el MVP: consulta cualquier ticket y marca su uso (§11.1).

## Contrato del JWT

* HS256 con la clave de `PULSEPASS_JWT_SECRET_BASE64` (al menos 32 bytes); se rechaza cualquier
  otro algoritmo, incluido `none`.
* Claims obligatorios: `sub` (id positivo del usuario), `iss`, `iat`, `exp` y `jti`. Sin roles en
  el token: se leen de la base de datos en cada petición, igual que el estado `active`.
* Duración por defecto: 900 segundos (`PT15M`). Margen de reloj 0; un `iat` futuro se rechaza.
* El token solo se acepta en una única cabecera `Authorization: Bearer`; nunca en la query string.
* Un token inválido, caducado o manipulado responde 401 también en las rutas públicas (P4-D6).

Los 401 incluyen la cabecera `WWW-Authenticate: Bearer`. Mensajes (P4-D9): «Authentication is
required», «Invalid credentials» y «Access denied».

## Roles y usuarios de prueba

* **Registro público:** siempre `ROLE_USER`; un campo `role` en el JSON se ignora (TC-SEC-016).
* **STAFF y ADMIN** solo existen en las pruebas (P4-D12): las clases de `integration/` registran
  usuarios por la API y les cambian el rol en la base de datos. No hay usuarios privilegiados en
  las migraciones ni en `application.yml`.
* **Usuarios anteriores a la Fase 4** (P4-D11): tienen `ROLE_USER` pero no credencial, así que no
  pueden iniciar sesión hasta activarlos manualmente. No existe una contraseña por defecto.

### Activación manual de un usuario anterior a la Fase 4

1. Generar el hash BCrypt de fuerza 10 de una contraseña temporal (12 caracteres o más) con la
   misma clase que usa la aplicación. Los JAR los descarga Maven al ejecutar `./mvnw` por primera
   vez:

   ```bash
   CP="$HOME/.m2/repository/org/springframework/security/spring-security-crypto/7.1.2/spring-security-crypto-7.1.2.jar:$HOME/.m2/repository/commons-logging/commons-logging/1.3.6/commons-logging-1.3.6.jar"
   printf '%s\n' 'System.out.println(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(10).encode("CONTRASENA_TEMPORAL"));' '/exit' | jshell -q --class-path "$CP" -
   ```

   Imprime un hash `$2a$10$…` de 60 caracteres.
2. Insertar la credencial del usuario en la base de datos:

   ```sql
   INSERT INTO user_credentials (user_id, password_hash)
   VALUES ((SELECT id FROM users WHERE email = 'usuario@ejemplo.com'), '<hash del paso 1>');
   ```

3. Entregar la contraseña temporal al usuario por un canal seguro. El cambio de contraseña está
   fuera del alcance del PRD (§5.2).

## Variables de entorno

La plantilla está en [`.env.example`](.env.example); `.env` está ignorado por Git. Spring Boot no lee
`.env`: las variables se exportan antes de arrancar.

| Variable | Obligatoria | Por defecto | Uso |
|---|---|---|---|
| `PULSEPASS_JWT_SECRET_BASE64` | Sí | — | Clave HMAC en Base64, de al menos 32 bytes (`openssl rand -base64 32`). Sin ella la aplicación no arranca |
| `PULSEPASS_JWT_ISSUER` | No | `pulsepass-api` | Claim `iss` |
| `PULSEPASS_JWT_ACCESS_TOKEN_TTL` | No | `PT15M` | Duración del token en ISO-8601; positiva y de segundos enteros |

Las pruebas usan la clave ficticia de `src/test/resources/config/application.yml` (DATO DE PRUEBA)
y no necesitan variables.

## Comandos de verificación

Pruebas de la Fase 4 (con Docker en ejecución, desde `backend/`). Entre los dos comandos cubren las
348 pruebas que añadió la Fase 4. El primero ejecuta las unitarias y web, incluidas las clases
anteriores que recibieron 10 pruebas cada una (`EventServiceImplTest` y
`RegisterUserRequestValidationTest`): informa 335 pruebas, de las que 293 son de la Fase 4 y 42 son
las anteriores de esas dos clases (31 y 11).

```bash
./mvnw test -Dtest='Jwt*Test,RequestIdFilter*Test,SecurityExceptionHandlingTest,CustomUserDetailsServiceTest,Auth*Test,AuthorizationServiceImplTest,LoginRequestValidationTest,MaxUtf8BytesValidatorTest,EndpointSecurityMatrixTest,EventDraftVisibilityTest,EventServiceImplTest,RegisterUserRequestValidationTest'
```

El segundo ejecuta las de integración con PostgreSQL: 55 pruebas.

```bash
./mvnw test -Dtest='Security*IT,Auth*IT,OwnershipSecurityIT'
```

Arranque local con PostgreSQL de Testcontainers y la clave de pruebas (`TestPulsepassApplication`),
en `http://localhost:8080`:

```bash
./mvnw spring-boot:test-run
```

Con la aplicación en marcha, el flujo registro → login → `/me`:

```bash
curl -s -X POST http://localhost:8080/api/users -H 'Content-Type: application/json' \
  -d '{"username":"demo.andrea","email":"demo.andrea@pulsepass.test","firstName":"Andrea","lastName":"Demo","birthDate":"2001-06-15","password":"UnaFraseSeguraLarga2026!"}'
```

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"identifier":"demo.andrea","password":"UnaFraseSeguraLarga2026!"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
```

```bash
curl -s http://localhost:8080/api/auth/me -H "Authorization: Bearer $TOKEN"
```

Resultado comprobado (2026-11-18): 201, 200 con `"expiresIn":900`, y 200 con
`{"id":1,"username":"demo.andrea","email":"demo.andrea@pulsepass.test","roles":["USER"]}`. Sin token,
`/api/auth/me` responde 401. Los datos son ficticios y la base de datos se descarta al detener la
aplicación.

---

# Pruebas

## Cómo ejecutarlas

Con Docker en ejecución, desde la carpeta `backend`:

```bash
./mvnw clean test
```

Para ejecutar una sola clase:

```bash
./mvnw test -Dtest=EventSearchIT
```

Las pruebas unitarias de la Fase 2 no necesitan Docker:

```bash
./mvnw test -Dtest='*MapperTest,*ServiceImplTest,TicketPricingStrategyImplTest,TicketCodeGeneratorImplTest'
```

Las pruebas web y de validación de la Fase 3 tampoco necesitan Docker:

```bash
./mvnw test -Dtest='*ControllerTest,GlobalExceptionHandlerTest,*RequestValidationTest'
```

Las pruebas de la Fase 4 se pueden ejecutar por separado con los comandos de
[Comandos de verificación](#comandos-de-verificación).

Maven Surefire ejecuta tanto las clases `*Test` como las `*IT`, por lo que `./mvnw clean test`
incluye todas las pruebas de integración (QT-010). La configuración de Surefire no se modificó en
las Fases 2, 3 ni 4.

## Qué cubren

### Pruebas de integración (PostgreSQL con Testcontainers)

Las ocho clases del paquete `persistence` usan `@DataJpaTest` con
`@Import(TestcontainersConfiguration.class)`: Flyway construye el esquema sobre PostgreSQL 17 y
Hibernate lo valida antes de ejecutar las pruebas. `PulsepassApplicationTests` usa
`@SpringBootTest` con la misma configuración y comprueba que arranca la aplicación completa,
incluidos los servicios, los mappers, el bean `Clock` y, desde la Fase 3, la capa web
(controladores y `GlobalExceptionHandler`).

| Clase | Pruebas | Cubre |
|---|---|---|
| `FlywayMigrationIT` | 4 | QT-001, QT-002, NFR-002..NFR-005 y el contenido de V2 |
| `VenuePersistenceTest` | 6 | FR-VEN-001..003, AC-001, QT-009 |
| `EventRepositoryIT` | 9 | FR-EVT-001..006, FR-VEN-004, AC-002, AC-006, QT-003, QT-007 |
| `EventArtistIT` | 4 | FR-ART-001..004, AC-003, QT-005, QT-008 |
| `UserProfileIT` | 6 | FR-USR-001..004, AC-004, QT-004, QT-007 |
| `TicketRepositoryIT` | 12 | FR-TKT-001..008, AC-005, AC-008, QT-006..QT-009 |
| `EventSearchIT` | 4 | FR-SRC-001..004, AC-007, QT-008 |
| `ServiceRepositoryIT` (Fase 2) | 16 | Los 9 métodos del PRD de Servicios §32 (P2-D6..P2-D8) |
| `SecurityMigrationIT` (Fase 4) | 16 | V4: tablas, restricciones, `@MapsId`, roles y la migración de V3 a V4 con usuarios existentes (P4-D11) |
| `SecurityPrincipalQueryCountIT` (Fase 4) | 2 | Consultas fijas, sin N+1, al cargar la identidad (NFR-SEC-003) |
| `PulsepassApplicationTests` | 1 | Arranque completo de la aplicación |

`FlywayMigrationIT` comprueba desde la Fase 4 que se aplican V1 a V4 (QT-001, P4-D17). Las pruebas de
integración de seguridad con la aplicación completa están en [Pruebas de seguridad](#pruebas-de-seguridad-fase-4).

Los AC de esta tabla son los del PRD de Persistencia.

* Las violaciones de restricciones se comprueban con el SQLSTATE y el nombre real de la
  restricción que informa PostgreSQL.
* Las lecturas con JPA de datos guardados por la propia prueba van precedidas de `flush()` y
  `clear()`, para leerlos de PostgreSQL y no de la caché de JPA. Las lecturas con `JdbcTemplate`
  no usan esa caché y solo necesitan el `flush` previo.
* `PersistenceFixtures` solo construye objetos con los datos de referencia del PRD de Persistencia
  (§16); los valores que el PRD no define están marcados como `DATO DE PRUEBA`.

### Pruebas unitarias (Fase 2, sin Spring ni base de datos)

Siguen PRD §42: servicio real con repositorios y mappers simulados con Mockito
(`@ExtendWith(MockitoExtension.class)`), aserciones con AssertJ y estructura ARRANGE / ACT /
ASSERT. No usan `@SpringBootTest`, PostgreSQL ni Testcontainers (NFR-001). Los mappers se prueban
reales en sus propias clases (P2-D9), y en las pruebas de servicios se simulan (V-1a).

| Clase | Pruebas | Cubre |
|---|---|---|
| `VenueMapperTest`, `ArtistMapperTest`, `UserMapperTest`, `TicketMapperTest` | 3 cada una | Mapeo real de MapStruct (NFR-004, §34) |
| `EventMapperTest` | 5 | `toResponse` y `toSummary`, artistas incluidos |
| `VenueServiceImplTest` | 4 | FR-SVC-001..002, BR-VENUE-001..002 |
| `ArtistServiceImplTest` | 6 | FR-SVC-009, BR-ARTIST-001..002 |
| `EventServiceImplTest` | 41 | FR-SVC-003..008, BR-EVENT-001..011, TEST-EVENT-001..008, AC-001..003; desde la Fase 4, 10 más de la visibilidad de borradores (P4-04b) |
| `UserServiceImplTest` | 10 | FR-SVC-010..012, BR-USER-001..005, TEST-USER-001..004 |
| `TicketServiceImplTest` | 44 | FR-SVC-013..018, BR-TICKET-001..014, TEST-TICKET-001..012, AC-004..011 |
| `TicketPricingStrategyImplTest` | 11 | §28, BR-TICKET-009, TP-1..TP-5 |
| `TicketCodeGeneratorImplTest` | 21 | Formato `TKT-` + 6 hexadecimales (P2-D3); 20 de ellas son un `@RepeatedTest` |

En los caminos inválidos se verifica con `verify(..., never())` que no se llama a ningún `save()`
(DoD §50). `ServiceFixtures` contiene los datos del escenario del PRD de Servicios (§47) y es
independiente de `PersistenceFixtures` (P2-D12); los valores que §47 no define, incluido el
usuario Sofía de AC-008 (TK-8), están marcados como `DATO DE PRUEBA`.

### Pruebas web y de validación (Fase 3, sin base de datos)

Siguen PRD §21: la unidad bajo prueba es «petición HTTP → controlador → servicio simulado →
respuesta HTTP». Las clases de controlador usan `@WebMvcTest(XController.class)`, MockMvc y el
servicio como `@MockitoBean` (QT-CTRL-002); importan `FixedClockTestConfiguration`, que aporta un
`Clock` fijo para que el `timestamp` de los errores sea determinista. No usan PostgreSQL,
Testcontainers ni `@SpringBootTest` (§17, NFR-CTRL-008). Los JSON van escritos como texto
(P3-D12) y los datos salen de `ControllerFixtures`.

| Clase | Pruebas | Cubre |
|---|---|---|
| `CreateEventRequestValidationTest` | 9 | §11.1: obligatorios, `@Min(0)` y longitudes (P3-D4) |
| `RegisterUserRequestValidationTest` | 21 | §11.2: obligatorios, formato de email y longitudes; desde la Fase 4, 10 más de la contraseña (P4-06) |
| `PurchaseTicketRequestValidationTest` | 11 | §11.3: obligatorios, formato de email y longitudes |
| `GlobalExceptionHandlerTest` | 16 | §13–§16: cada fila de la tabla del contrato de errores, con un controlador de sondeo que solo existe en la prueba |
| `VenueControllerTest` | 4 | TEST-CTRL-VEN-001..003, AC-CTRL-001..002 |
| `ArtistControllerTest` | 8 | TEST-CTRL-ART-001..004 |
| `UserControllerTest` | 12 | TEST-CTRL-USR-001..005, AC-CTRL-007..008 |
| `EventControllerTest` | 18 | TEST-CTRL-EVT-001..009, AC-CTRL-003..006 |
| `TicketControllerTest` | 21 | TEST-CTRL-TKT-001..011, AC-CTRL-009..014 |

Cada prueba de controlador comprueba el estado HTTP, el Content-Type, los campos JSON relevantes
con `jsonPath`, los argumentos que recibe el servicio (`verify`) y, en las peticiones rechazadas
antes del servicio, que no se le llama (`verify(..., never())`) (QT-CTRL-003..011). Las 32 pruebas
mínimas de §22 están identificadas por su ID; las demás cubren límites, 404 adicionales y listas
vacías.

Desde la Fase 4, estas pruebas importan además `WebSecurityTestConfiguration` y usan la cadena de
seguridad real (nunca `addFilters = false`); la identidad se indica con `@WithSecurityPrincipal`.
Sus aserciones no cambiaron, salvo las del registro que exige la contraseña (P4-06).

### Pruebas de seguridad (Fase 4)

| Clase | Pruebas | Tipo | Cubre |
|---|---|---|---|
| `JwtServiceTest` | 35 | Unitaria | Emisión y validación: firma, issuer, `sub`, `exp`, `iat`, `jti`, algoritmo (T-SEC-007, T-SEC-008, TC-AUTH-011) |
| `JwtPropertiesTest` | 16 | Unitaria | Validación de la clave, el issuer y la duración |
| `JwtConfigTest` | 3 | Arranque | Sin clave, o con una de menos de 32 bytes, la aplicación no arranca (§12.2) |
| `JwtAuthenticationFilterTest` | 19 | Unitaria | Sin Bearer, Bearer válido e inválido, doble cabecera, autenticación ya establecida |
| `CustomUserDetailsServiceTest` | 14 | Unitaria | Usuario existente, inexistente, inactivo, sin credencial y roles (§17.1) |
| `RequestIdFilterTest`, `RequestIdFilterRegistrationTest` | 4 y 6 | Unitaria y web | Request ID en el MDC; filtro registrado una vez, antes de Spring Security; logs de 401 y 403 con request ID y sin query string ni `Authorization` (NFR-SEC-006) |
| `SecurityExceptionHandlingTest` | 6 | Web | 401 y 403 con `ErrorResponse` desde la cadena y desde Spring MVC (P4-D19) |
| `JwtAuthenticationWebMvcTest` | 11 | Web | Tokens reales: válido, manipulado, caducado, en query string y de un usuario desactivado (T-SEC-016) |
| `EndpointSecurityMatrixTest` | 96 | Web | Las 22 operaciones × 4 actores (§17.4) y casos de 401, 403, sesión y CSRF |
| `EventDraftVisibilityTest` | 21 | Web | Borradores solo visibles para STAFF y ADMIN (P4-D2, T-SEC-017) |
| `AuthControllerTest` | 7 | Web | Login y `/me` (T-SEC-005, T-SEC-006, T-SEC-019) |
| `LoginRequestValidationTest` | 8 | Unitaria | Validación del login y `toString` sin secretos |
| `MaxUtf8BytesValidatorTest` | 4 | Unitaria | Límite de 72 bytes UTF-8 |
| `AuthServiceImplTest` | 9 | Unitaria | Registro y login (§17.1: credenciales correctas, incorrectas, usuario inactivo, token solo tras autenticar) |
| `AuthorizationServiceImplTest` | 14 | Unitaria | Visibilidad y propiedad: propietario, tercero, inexistente, anónimo |
| `AuthRegistrationIT` | 4 | Integración | Registro atómico con rollback real, hash BCrypt, solo `ROLE_USER` (UC-SEC-01, TC-SEC-016) |
| `AuthLoginIT` | 7 | Integración | Login y `/me` con JWT real (UC-SEC-02, TC-AUTH-002..006 y 012) |
| `OwnershipSecurityIT` | 16 | Integración | Propiedad para USER, STAFF y ADMIN, distinguiendo `@PreAuthorize` de las reglas de URL (T-SEC-009..015) |
| `SecurityEndToEndIT` | 10 | Integración | Escenario de demostración de §23, pasos 1 a 10 (UC-SEC-01..10) |

Las clases de `integration/` usan `@SpringBootTest` con PostgreSQL real y tokens emitidos por el
login. Confirman sus transacciones, así que borran por id solo los datos que crean. STAFF y ADMIN
se crean cambiando el rol de un usuario registrado (P4-D12).

### Resultados registrados

* **Cierre de la Fase 1:** `./mvnw clean test` terminó con `Tests run: 46, Failures: 0, Errors: 0,
  Skipped: 0` y `BUILD SUCCESS`.
* **Cierre de la Fase 2** (2026-11-16, sobre el commit `a26799c`): `./mvnw clean test` terminó con
  `Tests run: 206, Failures: 0, Errors: 0, Skipped: 0` y `BUILD SUCCESS`. De ellas, 62 son de
  integración o arranque (16 nuevas, de `ServiceRepositoryIT`) y 144 unitarias.
* **Cierre de la Fase 3** (2026-11-16, sobre el commit `7e1f386`): `./mvnw clean test` terminó con
  `Tests run: 316, Failures: 0, Errors: 0, Skipped: 0` y `BUILD SUCCESS`. Las 110 nuevas son 31 de
  validación de los request DTO, 16 de `GlobalExceptionHandlerTest` y 63 de los controladores.
* **Cierre de la Fase 4** (2026-11-18, sobre el commit `2dd79b2` más este README, `.env.example` y
  la duración parametrizable de `application.yml`): `./mvnw clean test` terminó con
  `Tests run: 664, Failures: 0, Errors: 0, Skipped: 0` y `BUILD SUCCESS`. Las 348 nuevas son las de
  la tabla de [Pruebas de seguridad](#pruebas-de-seguridad-fase-4), las de `SecurityMigrationIT` y
  `SecurityPrincipalQueryCountIT`, y las añadidas a `EventServiceImplTest` y
  `RegisterUserRequestValidationTest`.

## Avisos esperados en la salida

La ejecución de `./mvnw clean test` muestra estos avisos. **Ninguno es un fallo:** todas las
pruebas pasan.

| Aviso | Origen | Explicación |
|---|---|---|
| `Could not transfer metadata /.meta/prefixes.txt ... HTTP Status: 401` (2 veces) | Maven | Repositorios remotos de Flyway que exigen autenticación para un archivo de metadatos opcional. Las dependencias se resuelven igual |
| `Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK.` | Mockito | Mockito carga su agente en tiempo de ejecución. Hoy funciona; una versión futura del JDK podría impedirlo, y entonces habría que declarar Mockito como agente en Surefire |
| `A Java agent has been loaded dynamically (... byte-buddy-agent-1.18.15.jar)` y `Dynamic loading of agents will be disallowed by default in a future release` | JVM | El mismo agente de Mockito, visto desde la JVM |
| `Sharing is only supported for boot loader classes because bootstrap classpath has been appended` | JVM | Consecuencia del agente anterior; solo afecta al uso compartido de clases de la JVM |
| `HHH000247: ErrorCode: 0, SQLState: 23505` / `23514` con `duplicate key value violates unique constraint ...` o `violates check constraint ...` (13 veces) | Hibernate | Las registran las pruebas de integración que violan restricciones **a propósito** para comprobar que PostgreSQL las rechaza (11 de la Fase 1 y 2 de `SecurityMigrationIT`) |
| `ERROR ... GlobalExceptionHandler : Unexpected error while processing the request`, con la traza (6 veces) | `GlobalExceptionHandler` | Lo registran a propósito las pruebas que provocan errores 5xx (4 en `GlobalExceptionHandlerTest`, 1 en `TicketControllerTest` y 1 en la prueba de atomicidad de `AuthRegistrationIT`). Es el log del servidor; la respuesta HTTP solo dice «Unexpected error» |
| `INFO ... Global AuthenticationManager configured with UserDetailsService bean with name customUserDetailsService` (15 veces) | Spring Security | Una vez por cada contexto de Spring que se arranca. Aparece aunque exista el `AuthenticationManager` explícito de `SecurityConfig`, que es el que usa el login |
| `WARN ... Exception encountered during context initialization` (2 veces) | Spring | Lo provoca a propósito `JwtConfigTest`, que comprueba que la aplicación no arranca sin clave o con una clave corta |
| `INFO ... Authentication required: METHOD ruta` (50 veces) | `c.p.s.RestAuthenticationEntryPoint` (42) y `c.p.exception.GlobalExceptionHandler` (8) | Los 401 que provocan las pruebas de seguridad: 42 en la cadena de filtros y 8 dentro de Spring MVC (5 logins fallidos por HTTP, en `AuthLoginIT` y `AuthControllerTest`, y 3 sondas de `SecurityExceptionHandlingTest`) |
| `WARN ... Access denied: METHOD ruta userId=N` (29 veces) | `c.p.security.RestAccessDeniedHandler` (17) y `c.p.exception.GlobalExceptionHandler` (12) | Los 403 que provocan las pruebas: 17 de las reglas de URL, en la cadena de filtros, y 12 dentro de Spring MVC (8 de `@PreAuthorize` en `OwnershipSecurityIT`, 3 en `SecurityEndToEndIT` y 1 de la sonda de `SecurityExceptionHandlingTest`) |
| `INFO ... Login succeeded: userId=N` y `INFO ... Login failed: reason=<tipo>` | `AuthServiceImpl` | Registro de los logins de las pruebas (NFR-SEC-006). Sin username/email utilizado para iniciar sesión, contraseña, hash ni token. Un login fallido deja además el INFO «Authentication required» del manejador |

Estos INFO y WARN no registran la query string, la cabecera `Authorization`, contraseñas ni tokens JWT.

Ninguna línea de la salida contiene las contraseñas de prueba ni un JWT; tampoco aparece
«Using generated security password», porque PulsePass no usa el usuario en memoria de Spring Boot
(P4-04-c).

---

# Trazabilidad

## Fase 1 — Persistencia

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

## Fase 2 — Servicios (FR-SVC → BR → TEST)

La matriz del PRD (§49) cubre FR-SVC-003, 006, 007, 010, 013, 017 y 018; el resto de las filas
completa la trazabilidad con las pruebas del proyecto.

| Requisito | Servicio | Reglas | Pruebas |
|---|---|---|---|
| FR-SVC-001..002 | `VenueService` | BR-VENUE-001..002 | `VenueServiceImplTest` |
| FR-SVC-003 | `EventService.create` | BR-EVENT-001..006 | TEST-EVENT-003..006 en `EventServiceImplTest` |
| FR-SVC-004 | `EventService.findByCode` | — | TEST-EVENT-001..002 |
| FR-SVC-005 | `EventService.findPublishedEvents` | — | `EventServiceImplTest` (E-4a) |
| FR-SVC-006 | `EventService.publish` | BR-EVENT-007..009 | TEST-EVENT-007..008 |
| FR-SVC-007 | `EventService.addArtist` | BR-EVENT-010..011 | `EventServiceImplTest` (la «prueba unitaria requerida» de §49) |
| FR-SVC-008 | `EventService.findByArtist` | — | `EventServiceImplTest` (P2-D15, F-3a) |
| FR-SVC-009 | `ArtistService` | BR-ARTIST-001..002 | `ArtistServiceImplTest` |
| FR-SVC-010 | `UserService.register` | BR-USER-001..005 | TEST-USER-001..004 en `UserServiceImplTest` |
| FR-SVC-011..012 | `UserService.findByEmail` / `findByUsername` | — | `UserServiceImplTest` |
| FR-SVC-013 | `TicketService.purchase` | BR-TICKET-001..009 | TEST-TICKET-001..008 en `TicketServiceImplTest`; `TicketPricingStrategyImplTest` |
| FR-SVC-014..016 | `TicketService.findByCode` / `findByUserEmail` / `findPaidTicketsByEvent` | — | `TicketServiceImplTest` |
| FR-SVC-017 | `TicketService.cancel` | BR-TICKET-010..012 | TEST-TICKET-009..010 |
| FR-SVC-018 | `TicketService.markAsUsed` | BR-TICKET-013..014 | TEST-TICKET-011..012 |

### Criterios de aceptación (PRD de Servicios §48)

Se verifican con pruebas unitarias sobre el escenario de §47 (VEN-SMR-01 con capacidad 3,
CMF-2026, sus tres artistas y los usuarios del escenario).

| AC | Criterio | Prueba |
|---|---|---|
| AC-001 | CMF-2026 se crea en `DRAFT` | `EventServiceImplTest` (TEST-EVENT-003) |
| AC-002 | Se publica cuando se cumplen las reglas | `EventServiceImplTest` (TEST-EVENT-007) |
| AC-003 | Se asocian los tres artistas | `EventServiceImplTest` |
| AC-004 | Andrea puede comprar | `TicketServiceImplTest` |
| AC-005 | Carlos puede comprar | `TicketServiceImplTest` |
| AC-006 | Laura no puede comprar por edad | `TicketServiceImplTest` (TEST-TICKET-006) |
| AC-007 | Miguel no puede comprar porque está inactivo | `TicketServiceImplTest` (TEST-TICKET-003) |
| AC-008 | Un tercer adulto compra el último ticket y el evento pasa a `SOLD_OUT` | `TicketServiceImplTest` (TEST-TICKET-008, Sofía) |
| AC-009 | Una cuarta compra produce `BusinessRuleException` | `TicketServiceImplTest` (evento `SOLD_OUT` y capacidad agotada) |
| AC-010 | Un ticket `PAID` pasa a `USED` | `TicketServiceImplTest` (TEST-TICKET-011) |
| AC-011 | Un ticket `USED` no puede cancelarse | `TicketServiceImplTest` (TEST-TICKET-010) |

## Fase 3 — Controladores REST (FR-CTRL → endpoint → TEST-CTRL / AC-CTRL)

Sigue la matriz del PRD de Controladores (§29). Las pruebas indicadas son las mínimas de §22; cada
clase incluye además las adicionales descritas en [Pruebas](#pruebas-web-y-de-validación-fase-3-sin-base-de-datos).

| Requisito | Endpoint | Servicio | Pruebas mínimas | AC |
|---|---|---|---|---|
| FR-CTRL-VEN-001 | `GET /api/venues/{code}` | `VenueService.findByCode` | TEST-CTRL-VEN-001/002 | AC-CTRL-001, 002 |
| FR-CTRL-VEN-002 | `GET /api/venues/active` | `VenueService.findActiveVenues` | TEST-CTRL-VEN-003 | — |
| FR-CTRL-EVT-001 | `POST /api/events` | `EventService.create` | TEST-CTRL-EVT-001/002 | AC-CTRL-003, 004 |
| FR-CTRL-EVT-002 | `GET /api/events/{eventCode}` | `EventService.findByCode` | TEST-CTRL-EVT-003/004 | — |
| FR-CTRL-EVT-003 | `GET /api/events/published` | `EventService.findPublishedEvents` | TEST-CTRL-EVT-005 | — |
| FR-CTRL-EVT-004 | `PATCH /api/events/{eventCode}/publish` | `EventService.publish` | TEST-CTRL-EVT-006/007 | AC-CTRL-005, 006 |
| FR-CTRL-EVT-005 | `POST /api/events/{eventCode}/artists/{artistId}` | `EventService.addArtist` | TEST-CTRL-EVT-008 | — |
| FR-CTRL-EVT-006 | `GET /api/events/by-artist` | `EventService.findByArtist` | TEST-CTRL-EVT-009 | — |
| FR-CTRL-ART-001 | `GET /api/artists/{id}` | `ArtistService.findById` | TEST-CTRL-ART-001/002 | — |
| FR-CTRL-ART-002 | `GET /api/artists/by-stage-name` | `ArtistService.findByStageName` | TEST-CTRL-ART-003 | — |
| FR-CTRL-ART-003 | `GET /api/artists/active` | `ArtistService.findActiveArtists` | TEST-CTRL-ART-004 | — |
| FR-CTRL-USR-001 | `POST /api/users` | `UserService.register` | TEST-CTRL-USR-001/002/003 | AC-CTRL-007, 008 |
| FR-CTRL-USR-002 | `GET /api/users/by-email` | `UserService.findByEmail` | TEST-CTRL-USR-004 | — |
| FR-CTRL-USR-003 | `GET /api/users/by-username` | `UserService.findByUsername` | TEST-CTRL-USR-005 | — |
| FR-CTRL-TKT-001 | `POST /api/tickets` | `TicketService.purchase` | TEST-CTRL-TKT-001..004 | AC-CTRL-009, 010 |
| FR-CTRL-TKT-002 | `GET /api/tickets/{ticketCode}` | `TicketService.findByCode` | TEST-CTRL-TKT-005 | — |
| FR-CTRL-TKT-003 | `GET /api/tickets/by-user` | `TicketService.findByUserEmail` | TEST-CTRL-TKT-006 | — |
| FR-CTRL-TKT-004 | `GET /api/events/{eventCode}/tickets/paid` | `TicketService.findPaidTicketsByEvent` | TEST-CTRL-TKT-007 | — |
| FR-CTRL-TKT-005 | `PATCH /api/tickets/{ticketCode}/cancel` | `TicketService.cancel` | TEST-CTRL-TKT-008/009 | AC-CTRL-011, 012 |
| FR-CTRL-TKT-006 | `PATCH /api/tickets/{ticketCode}/use` | `TicketService.markAsUsed` | TEST-CTRL-TKT-010/011 | AC-CTRL-013, 014 |

AC-CTRL-015 y AC-CTRL-016 son estructurales: ningún controlador depende de un repositorio ni
devuelve entidades JPA (ver el [DoD](#definition-of-done-de-la-fase-3), puntos 4 y 6).

## Fase 4 — Seguridad (FR-SEC / NFR-SEC → componente → pruebas)

Sigue la matriz del PRD de Seguridad (§18). E2E indica el paso de `SecurityEndToEndIT` (§23).

| Requisito | Componente | Pruebas |
|---|---|---|
| FR-SEC-AUTH-001 Registro | `AuthService`, `UserService`, V4 | `AuthRegistrationIT`, `AuthServiceImplTest`, `RegisterUserRequestValidationTest`; E2E 1 |
| FR-SEC-AUTH-002 Login | `AuthController`, `AuthenticationManager`, `JwtService` | `AuthControllerTest` (T-SEC-005), `AuthLoginIT` (TC-AUTH-002/003); E2E 2 |
| FR-SEC-AUTH-003 Contraseña incorrecta | `AuthenticationManager`, `GlobalExceptionHandler` | `AuthControllerTest` (T-SEC-006), `AuthLoginIT` (TC-AUTH-004/005) |
| FR-SEC-AUTH-004 Usuario inactivo | `CustomUserDetailsService`, `JwtAuthenticationFilter` | `AuthLoginIT`, `JwtAuthenticationWebMvcTest` (T-SEC-016), `CustomUserDetailsServiceTest`; E2E 10 |
| FR-SEC-AUTH-005 Emisión del JWT | `JwtService` | `JwtServiceTest` |
| FR-SEC-AUTH-006 Validación del JWT | `JwtService` | `JwtServiceTest` (T-SEC-007/008), `JwtAuthenticationWebMvcTest`; E2E 9 |
| FR-SEC-AUTH-007 Solo cabecera Bearer | `JwtAuthenticationFilter` | `JwtAuthenticationFilterTest`, `JwtAuthenticationWebMvcTest`, `AuthLoginIT` (TC-AUTH-012) |
| FR-SEC-AUTH-008 Sin sesión | `SecurityConfig` | `EndpointSecurityMatrixTest`, `JwtAuthenticationWebMvcTest` |
| FR-SEC-AUTH-009 Contexto desde el usuario persistido | `CustomUserDetailsService`, `JwtAuthenticationFilter` | `JwtAuthenticationFilterTest`, `CustomUserDetailsServiceTest`, `AuthLoginIT` (roles actuales en `/me`) |
| FR-SEC-AUTH-010 `/api/auth/me` | `AuthController`, `AuthService` | `AuthControllerTest`, `AuthLoginIT` (TC-AUTH-006) |
| FR-SEC-VEN-01/02, FR-SEC-ART-01..03 | `SecurityConfig` | `EndpointSecurityMatrixTest` |
| FR-SEC-EVT-01..06 | `SecurityConfig`, `AuthorizationService`, `EventService` | `EndpointSecurityMatrixTest` (T-SEC-001..004, 014), `EventDraftVisibilityTest` (T-SEC-017); E2E 3, 6 y 7 |
| FR-SEC-USR-01..03 | `AuthService`, `UserService`, `AuthorizationService` | `EndpointSecurityMatrixTest`, `OwnershipSecurityIT` (T-SEC-015), `AuthRegistrationIT` (TC-SEC-016) |
| FR-SEC-TKT-01..06 | `AuthorizationService`, `TicketService` | `EndpointSecurityMatrixTest`, `OwnershipSecurityIT` (T-SEC-009..013); E2E 4, 5 y 8 |
| SEC-009 / NFR-SEC-002 401 y 403 uniformes | `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`, `GlobalExceptionHandler` | `SecurityExceptionHandlingTest`, `EndpointSecurityMatrixTest`, `AuthControllerTest` (T-SEC-019) |
| NFR-SEC-001 Sin secretos expuestos | `toString` de los DTO y de `AccessToken`, logs | `LoginRequestValidationTest`, `RegisterUserRequestValidationTest`, `JwtServiceTest`, `RequestIdFilterRegistrationTest` |
| NFR-SEC-003 Sin N+1 | `CustomUserDetailsService` | `SecurityPrincipalQueryCountIT` |
| NFR-SEC-004 Registro atómico | `AuthService` | `AuthRegistrationIT` (rollback con los cuatro INSERT ejecutados) |
| NFR-SEC-005 / NFR-SEC-010 Compatibilidad y regresión | Todas | Pruebas de controladores de la Fase 3 con identidad (T-SEC-020), `EndpointSecurityMatrixTest` |
| NFR-SEC-006 Trazabilidad | `RequestIdFilter`, logs de 401, 403 y login | `RequestIdFilterTest`, `RequestIdFilterRegistrationTest` |
| NFR-SEC-008 Configuración (**parcial**) | `JwtProperties`, `application.yml` | Emisor, clave y duración parametrizables: `JwtPropertiesTest`, `JwtConfigTest`; la variable de la duración se comprobó a mano (con `PT5M`, `expiresIn` = 300). **La política CORS sigue pendiente** (aplazada; ver «Limitaciones conocidas») |
| NFR-SEC-009 Sin registro privilegiado | `AuthService` | `AuthRegistrationIT`, `AuthServiceImplTest` |

NFR-SEC-007 (HTTPS fuera de localhost) depende del despliegue y queda fuera de este repositorio
(ver «Limitaciones conocidas»).

---

# Definition of Done de la Fase 2

Revisión de los 27 puntos del PRD de Servicios §50 sobre el commit `a26799c`. La evidencia de cada
punto se puede comprobar en el código o reproducir con los comandos indicados.

| # | Punto del DoD | Estado | Evidencia |
|---|---|---|---|
| 1 | Existen interfaces Service | ✅ | `service/`: `VenueService`, `ArtistService`, `EventService`, `UserService`, `TicketService` |
| 2 | Existen implementaciones `@Service` | ✅ | Las 5 clases de `service/impl/*ServiceImpl.java` llevan `@Service` |
| 3 | Se utiliza constructor injection | ✅ | Dependencias `private final` asignadas en el constructor; no hay `@Autowired` en `src/main` |
| 4 | Los DTO utilizan `record` | ✅ | Los 9 archivos de `dto/request` y `dto/response` son `public record` |
| 5 | No se exponen entidades JPA | ✅ | Las 20 operaciones de las interfaces devuelven DTO; ninguna interfaz importa `com.pulsepass.domain` |
| 6 | MapStruct transforma Entity → DTO | ✅ | 5 interfaces `@Mapper(componentModel = "spring")` en `mapper/`; pruebas `*MapperTest` |
| 7 | Existen excepciones personalizadas | ✅ | `exception/`: las 3 de §35, todas `extends RuntimeException` |
| 8 | `Optional` se maneja de forma segura | ✅ | `orElseThrow` y `map`; ninguna llamada a `.get()` en `service/` |
| 9 | Las reglas están en Service | ✅ | Las BR-* están en `*ServiceImpl`; no hay `@Query`, `EntityManager` ni `JdbcTemplate` en `service/` |
| 10 | Las escrituras son transaccionales | ✅ | `@Transactional` en `create`, `publish`, `addArtist`, `register`, `purchase`, `cancel` y `markAsUsed` |
| 11 | Las lecturas utilizan `readOnly` cuando corresponde | ✅ | 13 lecturas con `@Transactional(readOnly = true)` |
| 12 | VenueService implementado | ✅ | `VenueServiceImpl`; `VenueServiceImplTest` (4) |
| 13 | ArtistService implementado | ✅ | `ArtistServiceImpl`; `ArtistServiceImplTest` (6) |
| 14 | EventService implementado | ✅ | `EventServiceImpl`; `EventServiceImplTest` (31) |
| 15 | UserService implementado | ✅ | `UserServiceImpl`; `UserServiceImplTest` (10) |
| 16 | TicketService implementado | ✅ | `TicketServiceImpl`, con los 6 métodos de §21; `TicketServiceImplTest` (44) |
| 17 | La compra valida el usuario | ✅ | BR-TICKET-001..002; TEST-TICKET-002..003 |
| 18 | La compra valida el evento | ✅ | BR-TICKET-003..005; TEST-TICKET-004..005, los cuatro estados distintos de `PUBLISHED` y la fecha pasada o igual a ahora |
| 19 | La compra valida la edad | ✅ | BR-TICKET-006 en la fecha del evento; TEST-TICKET-006 y los límites del cumpleaños |
| 20 | La compra valida la capacidad | ✅ | BR-TICKET-007; TEST-TICKET-007 y las pruebas de `PAID + USED` |
| 21 | `SOLD_OUT` se actualiza correctamente | ✅ | BR-TICKET-008; TEST-TICKET-008 (AC-008), con `InOrder`: el ticket se guarda antes que el evento |
| 22 | Los unit tests usan Mockito | ✅ | Las 5 clases `*ServiceImplTest` usan `@ExtendWith(MockitoExtension.class)` |
| 23 | Los unit tests usan AssertJ | ✅ | `assertThat` y `assertThatThrownBy` de `org.assertj` en todas las pruebas unitarias |
| 24 | No se levanta PostgreSQL en los unit tests | ✅ | Ninguna clase de `test/.../service` ni `test/.../mapper` usa `@DataJpaTest`, `@Testcontainers` ni `@Import` |
| 25 | No se utiliza `@SpringBootTest` en los unit tests de Service | ✅ | El único `@SpringBootTest` es `PulsepassApplicationTests` |
| 26 | Se utiliza `verify(..., never())` en los caminos inválidos | ✅ | `never()` en las 5 clases `*ServiceImplTest` |
| 27 | `mvn clean test` finaliza correctamente | ✅ | `./mvnw clean test` sobre `a26799c`: 206 pruebas, 0 fallos, 0 errores, `BUILD SUCCESS` |

Comandos para reproducir parte de la evidencia, desde la raíz del repositorio:

```bash
grep -rn '\.get()' backend/src/main/java/com/pulsepass/service
grep -rln '^@SpringBootTest\|^@DataJpaTest\|^@Testcontainers\|^@Import' backend/src/test/java/com/pulsepass/service backend/src/test/java/com/pulsepass/mapper
grep -c '^    @Transactional' backend/src/main/java/com/pulsepass/service/impl/*ServiceImpl.java
grep -c '^    @Transactional(readOnly = true)' backend/src/main/java/com/pulsepass/service/impl/*ServiceImpl.java
```

Los dos primeros no deben devolver nada.

---

# Definition of Done de la Fase 3

Revisión de los 27 puntos del PRD de Controladores §30. Los puntos 1 a 26 se verifican sobre el
commit `7e1f386`; el punto 27, sobre este README actualizado en P3-10, que todavía no tiene commit.

| # | Punto del DoD | Estado | Evidencia |
|---|---|---|---|
| 1 | Existen VenueController, EventController, ArtistController, UserController y TicketController | ✅ | `controller/` |
| 2 | Los cinco controladores dependen exclusivamente de servicios | ✅ | Cada uno tiene un único campo `private final`, que es su `*Service`, inyectado por constructor |
| 3 | Todos los controladores responden con `ResponseEntity` | ✅ | Los 20 métodos con `@GetMapping`, `@PostMapping` o `@PatchMapping` devuelven `ResponseEntity<…>` |
| 4 | Ningún controlador depende de un repositorio | ✅ | Ningún import de `com.pulsepass.repository` en `controller/` |
| 5 | Ningún controlador implementa reglas de negocio | ✅ | Solo delegan; sin `if` de dominio ni `try/catch` |
| 6 | Ningún endpoint devuelve entidades JPA | ✅ | Ningún import de entidades de `com.pulsepass.domain` en `controller/` |
| 7 | Se reutilizan los DTO de la capa de servicios | ✅ | Los 9 DTO de la Fase 2; `ErrorResponse` es el único nuevo (§13, §18) |
| 8 | Se cubren los 20 métodos públicos de los servicios | ✅ | Tabla de [Endpoints](#endpoints) (§9) |
| 9 | Los POST de creación devuelven 201 | ✅ | `create`, `register` y `purchase`. `addArtist` devuelve 200, como fija §8.2 |
| 10 | Los GET correctos devuelven 200 | ✅ | Pruebas de los cinco controladores |
| 11 | Los PATCH correctos devuelven 200 | ✅ | TEST-CTRL-EVT-006, TEST-CTRL-TKT-008 y TEST-CTRL-TKT-010 |
| 12 | Bean Validation devuelve 400 | ✅ | TEST-CTRL-EVT-002, TEST-CTRL-USR-002, TEST-CTRL-TKT-002 y `GlobalExceptionHandlerTest` |
| 13 | `ResourceNotFoundException` devuelve 404 | ✅ | TEST-CTRL-VEN-002, TEST-CTRL-EVT-004, TEST-CTRL-ART-002, TEST-CTRL-TKT-003 |
| 14 | `DuplicateResourceException` devuelve 409 | ✅ | TEST-CTRL-USR-003 y el email duplicado (AC-CTRL-008) |
| 15 | `BusinessRuleException` devuelve 409 | ✅ | TEST-CTRL-EVT-007, TEST-CTRL-TKT-004, TEST-CTRL-TKT-009, TEST-CTRL-TKT-011 |
| 16 | Un error inesperado devuelve 500 | ✅ | `GlobalExceptionHandlerTest` y la compra sin código único (`TicketControllerTest`) |
| 17 | Todos los errores utilizan `ErrorResponse` | ✅ | Todos los errores que gestiona `GlobalExceptionHandler` usan `ErrorResponse`, incluidos los de Spring MVC y los 5xx heredados (P3-D3); las pruebas comprueban las 5 claves en cada error. Los errores que ocurren fuera de Spring MVC quedan fuera (ver «Limitaciones conocidas») |
| 18 | Todos los handlers devuelven `ResponseEntity<ErrorResponse>` | ✅ con matiz | Los cuatro manejadores propios declaran `ResponseEntity<ErrorResponse>`. Los métodos heredados de `ResponseEntityExceptionHandler` tienen la firma `ResponseEntity<Object>` impuesta por Spring, pero su cuerpo es siempre un `ErrorResponse` |
| 19 | Existe `GlobalExceptionHandler` con `@RestControllerAdvice` | ✅ | `exception/GlobalExceptionHandler.java` |
| 20 | Las pruebas usan `@WebMvcTest` | ✅ | Las 5 clases `*ControllerTest` y `GlobalExceptionHandlerTest` |
| 21 | Los servicios se reemplazan con `@MockitoBean` | ✅ | Las 5 clases `*ControllerTest` |
| 22 | Las pruebas usan MockMvc | ✅ | Todas las pruebas web |
| 23 | Las pruebas validan el estado, el Content-Type y el JSON | ✅ | `status()`, `contentTypeCompatibleWith` y `jsonPath` en todas |
| 24 | Se usa `verify(...)` para comprobar las llamadas al servicio | ✅ | En todas las pruebas que llegan al servicio, con los argumentos exactos |
| 25 | Se usa `verify(..., never())` cuando un request inválido no debe llegar al servicio | ✅ | En todas las pruebas de 400 de los controladores |
| 26 | `mvn clean test` termina con `BUILD SUCCESS` | ✅ | `./mvnw clean test` sobre `7e1f386`: 316 pruebas, 0 fallos, 0 errores, `BUILD SUCCESS` |
| 27 | El README documenta los endpoints principales y cómo ejecutar las pruebas | ✅ | [Endpoints](#endpoints) y [Cómo ejecutarlas](#cómo-ejecutarlas) |

Comandos para reproducir parte de la evidencia, desde la raíz del repositorio (los dos primeros no
deben devolver nada):

```bash
grep -rn 'import com.pulsepass.repository\|import com.pulsepass.domain\.[A-Z]' backend/src/main/java/com/pulsepass/controller
grep -rn 'try {\|catch (' backend/src/main/java/com/pulsepass/controller
grep -c 'public ResponseEntity<' backend/src/main/java/com/pulsepass/controller/*.java
```

# Definition of Done de la Fase 4

Revisión de los 14 puntos del PRD de Seguridad §22. Los puntos 1 a 13 se verifican sobre el commit
`2dd79b2` más la duración parametrizable de `application.yml` (P4-11-B); el punto 14, sobre este
README.

| # | Punto del DoD | Estado | Evidencia |
|---|---|---|---|
| 1 | Se mantienen las 20 rutas heredadas y sus respuestas exitosas | ✅ | `EndpointSecurityMatrixTest`: las 20 rutas responden su código de éxito a los actores permitidos. Las pruebas de controladores de la Fase 3 conservan sus aserciones, ahora con identidad (T-SEC-020); solo el registro exige además la contraseña (§3.4) |
| 2 | El registro crea el usuario, la contraseña con hash y el rol USER de forma atómica | ✅ | `AuthRegistrationIT`: hash `$2a$10$` que cumple `matches`, roles exactamente `[USER]`, y rollback de usuario, perfil, credencial y rol después de que los cuatro INSERT llegan a PostgreSQL |
| 3 | Un login correcto emite un JWT válido y uno erróneo devuelve 401 | ✅ | `AuthLoginIT` (el `sub` es el id del usuario y la duración, 900 s; 401 idéntico para contraseña incorrecta e identificador inexistente) y `AuthControllerTest` |
| 4 | `JwtService` valida la firma, el issuer, `sub`, `exp` y el algoritmo | ✅ | `JwtServiceTest`: firma alterada y de otra clave, otro emisor, sin `sub` o no positivo, caducado (también en el instante exacto de `exp`), `alg=none` y HS512 rechazados |
| 5 | API sin sesión; Bearer obligatorio salvo las excepciones públicas explícitas | ✅ | `EndpointSecurityMatrixTest` (sin sesión ni cookies; ruta desconocida → 401) y `JwtAuthenticationWebMvcTest`; reglas de URL en `SecurityConfig` con `anyRequest().authenticated()` |
| 6 | USER, STAFF y ADMIN tienen reglas probadas | ✅ | `EndpointSecurityMatrixTest` (22 operaciones × 4 actores) y `OwnershipSecurityIT` (16 pruebas con JWT reales) |
| 7 | Ningún USER consulta, compra para o cancela tickets de terceros | ✅ | `OwnershipSecurityIT` (403 de `@PreAuthorize`, sin tickets nuevos y el ticket sigue `PAID`) y `SecurityEndToEndIT`, paso 5 |
| 8 | Ni eventos borrador ni datos privados quedan expuestos por GET público | ✅ | `EventDraftVisibilityTest` (un borrador responde el mismo 404 que un código inexistente), `findByArtist` solo `PUBLISHED` (P2-D15) y `SecurityEndToEndIT`, paso 3. Las búsquedas de usuarios y de tickets exigen autenticación y propiedad |
| 9 | `ErrorResponse` es consistente para 400, 401, 403, 404, 409 y 500 | ✅ | `GlobalExceptionHandlerTest` (400, 404, 409, 500), `SecurityExceptionHandlingTest` y `EndpointSecurityMatrixTest` (401 con `WWW-Authenticate: Bearer`, 403); las pruebas comprueban las 5 claves |
| 10 | No hay secretos en el código, contraseñas sin hash ni datos sensibles expuestos | ✅ | La clave solo llega por entorno (`JwtConfigTest`: sin ella no arranca); la de pruebas es ficticia y está en `src/test`; `.env.example` no contiene secretos reales: la clave está vacía y solo trae los valores por defecto del emisor (`pulsepass-api`) y de la duración (`PT15M`); BCrypt en `AuthRegistrationIT`; `toString` sin secretos en las pruebas de los DTO y de `AccessToken`; en la salida de la suite, 0 contraseñas de prueba y 0 JWT |
| 11 | Flyway se ejecuta con éxito sobre un esquema nuevo y sobre uno ya migrado | ✅ | `FlywayMigrationIT` (V1 a V4 desde una base vacía). `SecurityMigrationIT`, en un esquema aislado: migra hasta V3, inserta dos usuarios (uno activo y otro inactivo), ejecuta V4 y comprueba que se aplicaron V1 a V4, que cada usuario tiene exactamente el rol `USER` y que no hay ninguna credencial; otra prueba aplica V4 sobre una base nueva sin insertar roles ni credenciales |
| 12 | Pasan las pruebas unitarias, las de MockMvc con filtros y las de integración | ✅ | `./mvnw clean test`: 664 pruebas, 0 fallos, 0 errores, `BUILD SUCCESS` (2026-11-18). Ninguna prueba web usa `addFilters = false` |
| 13 | No se modifican reglas de negocio existentes sin requerimiento explícito | ✅ con matiz | El único servicio de negocio modificado es `EventServiceImpl.findByCode`, por el requisito explícito FR-SEC-EVT-02 (P4-D2). `TicketServiceImpl` y `UserServiceImpl` no cambiaron; BR-TICKET-004 sigue respondiendo 409 (P4-08-F) |
| 14 | El README incluye variables, roles de prueba, endpoints y comandos de verificación | ✅ | [Variables de entorno](#variables-de-entorno), [Roles y usuarios de prueba](#roles-y-usuarios-de-prueba), [Endpoints de autenticación](#endpoints-de-autenticación), [Matriz de autorización](#matriz-de-autorización) y [Comandos de verificación](#comandos-de-verificación) |

Comandos para reproducir parte de la evidencia, desde la raíz del repositorio. El primero no debe
devolver nada; el segundo muestra los servicios de negocio que cambiaron en la Fase 4:

```bash
grep -rnE 'secret-base64: *[A-Za-z0-9+/=]{20,}' backend/src/main/resources
```

```bash
git diff --stat 59faafc fase-4-seguridad -- backend/src/main/java/com/pulsepass/service/impl
```

---

# Decisiones clave

## Fase 1 — Persistencia

* **Configuración:** `ddl-auto: validate` (PRD §13) y `open-in-view: false`. No se configura
  ninguna conexión de base de datos (tampoco en las Fases 2, 3 y 4): las pruebas la reciben de
  Testcontainers. Sin secretos en el repositorio.
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

## Fase 2 — Servicios

La columna **Origen** indica si la decisión aplica un requisito explícito del PRD o la tomó el
proyecto para resolver algo que el PRD no define. «Proyecto (base §N)» significa que el PRD pide
el requisito y el proyecto decidió cómo cumplirlo.

| ID | Decisión | Origen |
|---|---|---|
| P2-D1 | DTO explícitos que reflejan cada entidad; `EventResponse.artists` es `List<ArtistResponse>` | Proyecto (base §8, §13, §23) |
| P2-D2 | El precio lo calcula un componente `TicketPricingStrategy`; no se reutilizan los precios del PRD de Persistencia. Importes en TP-2 | Proyecto (base §28) |
| P2-D3 | Código `TKT-` + 6 hexadecimales con `SecureRandom`, reintentando si hay colisión | Proyecto |
| P2-D4 | `Clock` inyectable con zona `America/Bogota`; comparaciones de fecha estrictas (`isAfter`) | Proyecto |
| P2-D5 | `BusinessRuleException` para `minimumAge < 0`, artista duplicado y usuario sin perfil | Proyecto (base §35) |
| P2-D6 | Agregar los 9 métodos de §32 sin modificar ni eliminar los existentes; se conserva `findByStageName` exacto | Proyecto (base §32) |
| P2-D7 | Tickets de un usuario por `purchaseDate DESC NULLS LAST`, con `@Query` y la firma del PRD | Proyecto (base §32) |
| P2-D8 | Nueva clase `ServiceRepositoryIT`; no se modifican las pruebas de la Fase 1 | Proyecto |
| P2-D9 | Pruebas de los mappers reales en clases propias | Proyecto |
| P2-D10 | `save()` explícito en cada escritura | Proyecto |
| P2-D11 | Pruebas mínimas para `VenueService` y `ArtistService` | Proyecto |
| P2-D12 | `ServiceFixtures` independiente, con los datos de §47 | Proyecto (base §47) |
| P2-D13 | MapStruct 1.6.3 | Proyecto (base NFR-004) |
| P2-D14 | `@Transactional` en cada método, `readOnly` en las lecturas | Proyecto (base SRV-004, §38) |
| P2-D15 | `findByArtist` devuelve solo eventos `PUBLISHED` (respaldado por el PRD de Postman §9.1, paso 7) | Proyecto |
| P2-D16 | Cancelar un ticket no modifica el evento, aunque esté `SOLD_OUT` (TC-1): §31 no define SOLD_OUT → PUBLISHED | Proyecto |
| P2-D17 | Mensajes de las excepciones en inglés | Proyecto |
| P2-D18 | Los servicios no validan `null` ni cadenas vacías; la validación estructural es Bean Validation en los controladores (PRD de Controladores CTRL-005, §11) | Proyecto (base PRD de Controladores) |
| P2-D19 | `markAsUsed` no tiene restricciones de fecha | Proyecto |
| P2-D20 | `cancel` exige `eventDate` estrictamente posterior a ahora; la igualdad se rechaza | Proyecto (base BR-TICKET-012) |
| TP-1 | Precio base, descuento y multiplicadores | Proyecto (sugerencia de §28) |
| TP-2 | GENERAL 100000.00, STUDENT 50000.00, VIP 200000.00, BACKSTAGE 300000.00 | Proyecto (DATO DE DISEÑO) |
| TP-3 | Importes como constantes en la implementación | Proyecto |
| TP-4 | Escala 2 y `RoundingMode.HALF_UP` | Proyecto |
| TP-5 | BR-TICKET-009 garantizado por diseño y por prueba, sin comprobación en tiempo de ejecución | Proyecto (base BR-TICKET-009) |
| TP-6 | Interfaz en `service`, implementación y prueba en `service.impl` | Proyecto (base §8) |
| TP-7 | La estrategia no valida `null` (P2-D18) | Proyecto |
| TP-8 | En `TicketServiceImplTest`, la estrategia es un mock | Proyecto (base §42) |
| TK-1 | Los tickets `PAID` y `USED` ocupan capacidad; los `CANCELLED` no | Proyecto (interpreta `paidTickets` de §26) |
| TK-2 | El perfil solo se exige si `minimumAge > 0` | Proyecto |
| TK-3 | `SOLD_OUT` cuando el conteo anterior + 1 es igual a la capacidad | Proyecto (base BR-TICKET-008) |
| TK-4 | `TicketCodeGenerator` como componente propio, simulado en las pruebas | Proyecto |
| TK-5 | Máximo 5 intentos; después, `IllegalStateException` | Proyecto |
| TK-6 | Mensajes de las excepciones de `purchase` y de las consultas de tickets | Proyecto |
| TK-7 | `findByUserEmail` y `findPaidTicketsByEvent` no validan el usuario ni el evento y devuelven una lista vacía | Proyecto |
| TK-8 | Sofía como tercer usuario adulto de AC-008 (DATO DE PRUEBA) | Proyecto (base AC-008) |
| TK-9 | `TicketService` en dos iteraciones: compra y consultas (P2-13), ciclo de vida (P2-14) | Proyecto |
| TK-10 | La estrategia de precios es un mock en las pruebas del servicio (confirma TP-8) | Proyecto |
| TC-1 | Igual que P2-D16 | Proyecto |
| TC-2 | En `cancel`, primero el estado y después la fecha, según la numeración del PRD | Proyecto |
| TC-3 | Sin validaciones del estado del evento en `cancel` ni en `markAsUsed` | Proyecto |
| TC-4 | Mensajes `Only PAID tickets can be cancelled: ` y `Only PAID tickets can be used: ` | Proyecto |
| TC-5 | `RESERVED` se rechaza en `cancel` y en `markAsUsed` («solo PAID») | Proyecto (base BR-TICKET-010, 013) |
| TC-6 | Helper privado `findTicketOrThrow` | Proyecto |
| TC-7 | Confirmación de P2-D19 y P2-D20 | Proyecto |

Las decisiones de cada iteración que no aparecen en la tabla (V-*, A-*, E-*, F-*, U-*, D-* y R-*)
se citan en el Javadoc del código y en las secciones anteriores.

### Desviación DEV-001: JUnit 6

Los PRD de Servicios, Controladores y Seguridad indican **JUnit 5**. El proyecto usa **JUnit 6.0.3
(Jupiter)**, la versión que gestiona Spring Boot 4.1.2.

* **Motivo:** el PRD exige Spring Boot 4.x, que incluye Spring Framework 7.0 (7.0.10 en este
  proyecto), y Spring Framework 7.0 establece JUnit 6 como requisito mínimo.
* **Impacto:** ninguno en la forma de escribir las pruebas. JUnit 6 conserva el modelo Jupiter y
  el paquete `org.junit.jupiter.api`. Las pruebas de la Fase 1 solo usan `@Test` y `@DisplayName`;
  las de la Fase 2 usan además `@ExtendWith` y `@BeforeEach`, y de `org.junit.jupiter.params`,
  `@ParameterizedTest`, `@EnumSource` y `@CsvSource`, junto con `@RepeatedTest`. Las de la Fase 3
  añaden `@NullAndEmptySource` y `@ValueSource`, y de Spring, `@WebMvcTest` y `@MockitoBean` (PRD
  de Controladores §17, que también indica JUnit 5).

## Fase 3 — Controladores REST

| ID | Decisión | Origen |
|---|---|---|
| P3-D1 | Añadir `spring-boot-starter-webmvc`, `spring-boot-starter-validation` y, en pruebas, `spring-boot-starter-webmvc-test`, con las versiones del BOM | Proyecto (base §17) |
| P3-02 | `spring.jpa.properties.hibernate.check_nullability: true`: con Hibernate Validator en el classpath, Hibernate desactiva su comprobación de nulos; se reactiva para conservar el rechazo antes del INSERT que verifican las pruebas de la Fase 1 | Proyecto |
| P3-D2 | `GET /api/events/{eventCode}/tickets/paid` va en `TicketController` | Proyecto (base §22.5 y §29) |
| P3-D3 | `GlobalExceptionHandler` extiende `ResponseEntityExceptionHandler`: también los errores de Spring MVC usan `ErrorResponse`, con su estado real (incluidos 405 y 415) | Proyecto (base NFR-CTRL-003) |
| P3-D4 | Las validaciones mínimas del PRD, más las longitudes máximas de las columnas de V1 | Proyecto (base §11) |
| P3-D5 | Mensajes en inglés con el patrón del ejemplo de §13; mensajes propios para JSON mal formado, parámetro ausente, valor incompatible, ruta inexistente, 405, 415 y 500 | Proyecto (base §13) |
| P3-D6 | Varios errores de un mismo campo se unen en orden alfabético con `; ` | Proyecto |
| P3-D7 | `timestamp` con el `Clock` inyectado: `LocalDateTime` (como §13) truncado a segundos, sin zona en el JSON | Proyecto (base §13) |
| P3-D8 | Sin cabecera `Location` en los 201 | Proyecto |
| P3-D9 | Los parámetros de consulta solo se exigen; no se validan | Proyecto |
| P3-D10 | `DataIntegrityViolationException` responde 500 | PRD §14 |
| P3-D11 | Solo pruebas `@WebMvcTest` y `GlobalExceptionHandlerTest`; sin pruebas de integración nuevas | Proyecto (base §17 y §21) |
| P3-D12 | `ControllerFixtures` con los DTO del escenario de §23; JSON escrito como texto en las pruebas | Proyecto (base §23) |
| P3-04 | Los errores de Spring MVC con estado 5xx también responden «Unexpected error», con `details` vacío y el estado original | Proyecto (base NFR-CTRL-007) |
| AR-1 | Solar Beat, Neon Waves y Caribbean Sound en `ControllerFixtures` | Proyecto (base §23.3) |
| AR-2 | 8 pruebas de `ArtistController`: las 4 mínimas, el 404 por nombre, el `id` no numérico, el parámetro ausente y la lista vacía | Proyecto |
| AR-3 | Sin prueba de `stageName` vacío: lo resuelve el servicio | Proyecto |
| AR-4 | Sin prueba específica de artistas inactivos: lo decide el servicio | Proyecto |
| US-1 | Datos de Andrea en `ControllerFixtures`; el request se comprueba con `eq(...)` sobre el record completo | Proyecto (base §23.4) |
| US-2 | 12 pruebas de `UserController`: las 5 mínimas, AC-CTRL-008 y 6 adicionales | Proyecto |
| US-3 | Los campos obligatorios ausentes se prueban con una sola petición `{}` | Proyecto |
| US-4 | Se mantiene el mensaje del servicio en el 409 por email duplicado | Proyecto (base AC-CTRL-008) |
| EV-1 | Fixtures de CMF-2026 con el estado y los artistas que pida cada prueba | Proyecto (base §23.2) |
| EV-2 | 18 pruebas de `EventController`: las 9 mínimas y 9 adicionales | Proyecto |
| EV-3 | TEST-CTRL-EVT-002 con `{}` y las 6 claves obligatorias | Proyecto (base AC-CTRL-004) |
| EV-4 | Los artistas de un evento se comparan sin orden, porque el servicio no lo garantiza | Proyecto (base P2-05 D-C1) |
| TKC-1 | `TicketController` usa el prefijo `/api` | Proyecto (base P3-D2) |
| TKC-2 | 21 pruebas de `TicketController`: las 11 mínimas y 10 adicionales | Proyecto |
| TKC-3 | TEST-CTRL-TKT-002 con `{}` y las 3 claves obligatorias | Proyecto |
| TKC-4 | El precio de las fixtures es el de VIP de TP-2 (DATO DE DISEÑO) | Proyecto |
| TKC-5 | Los `PATCH` no declaran cuerpo; si llega uno, se ignora | Proyecto |

El prefijo `TKC-` evita la colisión con las decisiones `TK-*` de P2-13 y `TC-*` de P2-14.

## Fase 4 — Seguridad

| ID | Decisión | Origen |
|---|---|---|
| P4-D1 | Un recurso ajeno responde 403 y un ticket inexistente, 404; las búsquedas de usuario, el listado por email y la compra responden 403 aunque el objetivo no exista | Proyecto (base §11.1, §13) |
| P4-D2 | Un evento no publicado pedido por un anónimo o un USER responde 404 con el mensaje de «no existe»; STAFF y ADMIN lo ven | PRD FR-SEC-EVT-02 |
| P4-D3 | Solo `PUBLISHED` es público | PRD §11 |
| P4-D4 | `GET /api/events/by-artist` sigue devolviendo solo `PUBLISHED` para todos (P2-D15) | Proyecto (base FR-SEC-EVT-06) |
| P4-D5 | La propiedad se comprueba con `@PreAuthorize` en las interfaces de `UserService` y `TicketService`, a través de `AuthorizationService` | Proyecto (base §7.1) |
| P4-D6 | Un Bearer inválido en una ruta pública responde 401; nunca autentica por accidente | PRD §14.3.5 |
| P4-D7 | `anyRequest().authenticated()`: lo no declarado exige autenticación | PRD SEC-001 |
| P4-D8 | Sin jerarquía de roles; la compra es solo para `ROLE_USER` | Proyecto (base §11) |
| P4-D9 | Mensajes «Authentication is required», «Invalid credentials» y «Access denied» | Proyecto (base §13) |
| P4-D10 | `password_hash VARCHAR(100)` y BCrypt de fuerza 10; sin `password_changed_at` | Proyecto (base §9.1) |
| P4-D11 | V4 da `ROLE_USER` a los usuarios existentes, sin credenciales; activación manual documentada | PRD §9.2 |
| P4-D12 | STAFF y ADMIN solo en las pruebas | PRD §9.2, NFR-SEC-009 |
| P4-D13 | El login busca primero por username exacto y después por email sin distinguir mayúsculas | Proyecto (base FR-SEC-AUTH-002) |
| P4-D14 | Contraseña: `@NotBlank`, al menos 12 caracteres y 72 bytes UTF-8 como máximo | PRD §9.3 |
| P4-D15 | `AuthenticatedUserResponse(id, username, email, roles)` | Proyecto (base FR-SEC-AUTH-010) |
| P4-D16 | `UserController.register` llama a `AuthService.register`, que crea usuario, credencial y `ROLE_USER` en una transacción | PRD §9.3 |
| P4-D17 | QT-001 se actualiza a V1–V4 | Proyecto |
| P4-D18 | Clave de pruebas en `src/test/resources/config/application.yml` y plantilla `.env.example` | Proyecto (base §14.2) |
| P4-D19 | `GlobalExceptionHandler` distingue 401 de 403 con `AuthenticationTrustResolver` | Proyecto (base §13) |
| P4-D20 | `spring-boot-starter-security-test` en lugar de `spring-security-test` ([DEV-002](#desviación-dev-002-spring-boot-starter-security-test)) | Desviación |
| P4-D21 | La propiedad se prueba de forma unitaria y de integración | Proyecto (base §17) |
| P4-03 | JWT HS256 con el registro de algoritmos limitado; `exp`, `iat`, `sub` positivo y `jti` obligatorios; margen de reloj 0; `iat` futuro rechazado; emisión truncada a segundos; sin roles en el token | Proyecto (base §12) |
| P4-04 | Sin CSRF, formLogin, httpBasic, logout ni requestCache; 401 con `WWW-Authenticate: Bearer`; `ErrorResponseFactory` como única fuente de los cuerpos de error; `RequestIdFilter` fuera de la cadena, con máxima prioridad; sin el usuario en memoria de Spring Boot | Proyecto (base §14.3) |
| P4-05 | Exactamente una cabecera Bearer bien formada; el filtro no es un bean y carga en cada petición el usuario, su estado y sus roles (2 consultas) | Proyecto (base §14.3, NFR-SEC-003) |
| P4-06 | `@MaxUtf8Bytes(72)` propio; `toString` de los request sin la contraseña; el hash se calcula después de las reglas de negocio; los campos JSON desconocidos (como `role`) se ignoran | Proyecto |
| P4-07 | `AuthenticationManager` explícito (`ProviderManager` + `DaoAuthenticationProvider` con los valores por defecto); el login sin mínimo de 12 caracteres y con máximo de 72 bytes (400); `roles` sin prefijo y ordenados; logs de login sin identificador; la matriz pasa a 22 operaciones | Proyecto |
| P4-08 | `@EnableMethodSecurity` en `MethodSecurityConfig`, separada para que `@WebMvcTest` no la cargue; bean `authz`; se reutilizan las consultas existentes de los repositorios; STAFF no cancela tickets ajenos; se mantiene el 409 de BR-TICKET-004 | Proyecto (base §11) |
| P4-10 | Escenario de §23 en el orden 1, 2, 6, 7, 3, 4, 5, 8, 9, 10; CMF-2026 con fecha 2030-12-12 (DATO DE PRUEBA) | Proyecto (base §23) |
| P4-11 | Duración del token parametrizable con `PULSEPASS_JWT_ACCESS_TOKEN_TTL`; con el emisor y la clave, cubre la parte de configuración del JWT de NFR-SEC-008. CORS sigue aplazado | PRD NFR-SEC-008 (parcial) |

Las decisiones de cada tarea con su letra (por ejemplo, P4-06-A) se citan en el Javadoc del código.

### Desviación DEV-002: `spring-boot-starter-security-test`

El PRD (§14.1) indica `spring-security-test`. El proyecto usa `spring-boot-starter-security-test`,
que lo incluye.

* **Motivo:** en Spring Boot 4, `@WebMvcTest` solo aplica la configuración de seguridad con el
  módulo `spring-boot-security-test`, que llega a través de ese starter. Con solo
  `spring-security-test`, las pruebas web no tendrían la cadena de filtros real.
* **Impacto:** ninguno en la forma de escribir las pruebas: las utilidades de
  `spring-security-test` (`SecurityMockMvcRequestPostProcessors`, `@WithSecurityContext`) siguen
  disponibles.

---

# Limitaciones conocidas

## Fase 1 — Persistencia

* **Comodines en `findRecommended`:** los caracteres `%` y `_` del texto de búsqueda no se
  escapan, porque el PRD no define ese caso.
* **Rechazos de Hibernate antes del INSERT:** las pruebas de relaciones obligatorias (`venue`,
  `user`, `event`) esperan que Hibernate detecte el valor null antes de enviar la sentencia. Desde
  la Fase 3, Hibernate Validator está en el classpath y Hibernate desactiva por defecto esa
  comprobación; `hibernate.check_nullability: true` en `application.yml` la mantiene activa
  (P3-02). Sin esa propiedad, el rechazo vendría de la restricción `NOT NULL` de PostgreSQL.
* **Código de pruebas:** los métodos auxiliares que verifican las restricciones de PostgreSQL
  están repetidos en varias clases de prueba; se mantienen así de forma intencional.

## Fase 2 — Servicios

* **Unicidad del email sin distinguir mayúsculas:** `UserService.register` la comprueba con
  `existsByEmailIgnoreCase` (BR-USER-002), pero `uk_users_email` en la base de datos **sí**
  distingue mayúsculas. Dos registros simultáneos con `ana@email.com` y `Ana@email.com` podrían
  pasar ambos la comprobación del servicio, y la base de datos aceptaría los dos. Si los dos usan
  exactamente el mismo email (o el mismo username), la restricción UNIQUE rechaza el segundo, pero
  el error llega como excepción de integridad de Spring y no como `DuplicateResourceException`.
* **Concurrencia en la compra (PRD §53):** la secuencia contar → validar → guardar no protege
  contra dos compras simultáneas, que podrían ver el mismo cupo libre y superar la capacidad. El
  PRD lo deja fuera del alcance; no se agregaron bloqueos.
* **Concurrencia en `cancel` y `markAsUsed`:** `Ticket` no tiene `@Version`, así que dos cambios
  de estado simultáneos sobre el mismo ticket podrían pisarse.
* **Código de ticket:** dos compras simultáneas podrían generar el mismo código y ambas superar la
  comprobación previa; en ese caso, `uk_tickets_ticket_code` rechaza la segunda.
* **Rollback:** las pruebas unitarias no levantan Spring y no pueden demostrar el rollback de
  `@Transactional`. Lo que sí demuestran es que no se llama a ningún `save()` en los caminos
  inválidos.
* **Precios:** son DATO DE DISEÑO (TP-2), no importes definidos por el PRD.
* **Tickets `RESERVED`:** ningún flujo los crea (§27 los deja para una versión futura). No ocupan
  capacidad (TK-1) y no se pueden cancelar ni usar (TC-5).
* **Datos de prueba:** `ServiceFixtures` y `PersistenceFixtures` difieren a propósito, porque cada
  una sigue su PRD (por ejemplo, la capacidad de VEN-SMR-01).
* **Sin validación estructural en los servicios:** los servicios no validan `null` ni cadenas
  vacías (P2-D18). Por HTTP esa validación la hace Bean Validation en los controladores desde la
  Fase 3; quien llame a un servicio directamente, sin pasar por un controlador, no la tiene.

## Fase 3 — Controladores REST

Implementado en esta fase, con estas limitaciones conocidas:

* **Firma de los métodos heredados del handler:** los métodos que `GlobalExceptionHandler` hereda
  de `ResponseEntityExceptionHandler` tienen la firma `ResponseEntity<Object>`, impuesta por
  Spring; su cuerpo es siempre un `ErrorResponse` (ver el punto 18 del DoD).
* **Errores fuera de Spring MVC:** los errores que ocurren antes de `DispatcherServlet` (por
  ejemplo, en un filtro de servlet) no pasan por `GlobalExceptionHandler` y usan el formato de
  `/error` de Spring Boot. Hoy no hay filtros propios. (Desde la Fase 4 hay filtros propios; los 401
  y 403 de la cadena de seguridad usan `ErrorResponse` a través de `RestAuthenticationEntryPoint` y
  `RestAccessDeniedHandler`.)
* **405 y 415:** se responden con su estado real, aunque no están en la lista de códigos de §16
  (P3-D3).
* **Códigos que coinciden con una ruta literal:** un venue con código `active`, un evento con
  código `published` o `by-artist`, o un ticket con código `by-user` no se podrían consultar por su
  ruta con variable. Los formatos reales (`VEN-…`, `CMF-…`, `TKT-…`) no chocan.
* **Campos JSON desconocidos:** se ignoran. Por ejemplo, un `"active": false` en el registro no
  tiene efecto, porque el record no tiene ese campo. (Desde la Fase 4, `password` sí es un campo
  del registro, y un `"role"` se ignora igual.)
* **Sin 404 en dos listas:** `GET /api/tickets/by-user` y `GET /api/events/{eventCode}/tickets/paid`
  responden `[]` si el usuario o el evento no existen (TK-7), así que un cliente no distingue «sin
  tickets» de «no existe».
* **Errores de integridad:** las carreras de concurrencia de la Fase 2 (email con distinto uso de
  mayúsculas, código de ticket repetido) llegan como `DataIntegrityViolationException` y
  responden 500 (P3-D10).
* **La aplicación no arranca sola:** no hay datasource configurado (decisión de la Fase 1: sin
  secretos en el repositorio). Las pruebas reciben la base de datos de Testcontainers; ejecutar la
  API manualmente, por ejemplo con Postman, requiere configurar la conexión, y corresponde a una
  fase posterior. (Desde la Fase 4 se puede arrancar en local con la base de datos de
  Testcontainers: ver [Comandos de verificación](#comandos-de-verificación).)

Queda para fases posteriores:

* **Seguridad (Fase 4):** en esta fase todos los endpoints son públicos (R-CTRL-003). No hay
  autenticación, roles ni control de propiedad: cualquiera puede consultar, cancelar o usar
  cualquier ticket, y `GET /api/events/{eventCode}` también muestra eventos en `DRAFT`.
  **Resuelto en la Fase 4** (ver [Fase 4 — Seguridad](#fase-4--seguridad)).
* **Fuera del alcance del PRD de Controladores (§27):** OpenAPI/Swagger, paginación, CORS avanzado,
  versionado de la API (`/api/v1`) y pruebas E2E.

## Fase 4 — Seguridad

* **STAFF con alcance global (§11.1):** el dominio no asigna personal a eventos, así que un STAFF
  puede consultar cualquier ticket y marcar el uso de cualquiera. En producción habría que
  limitarlo a sus eventos.
* **Sin revocación inmediata ni límite de intentos:** no hay lista de tokens revocados; un token
  firmado sigue siendo válido hasta que caduca (15 minutos por defecto). El estado y los roles se
  leen de la base de datos en cada petición, con dos efectos distintos:
  * **Desactivar un usuario** impide autenticar sus siguientes peticiones: el token deja de servir
    (401), aunque no haya caducado.
  * **Retirar un rol** no invalida el token: el usuario sigue autenticado, pero sus permisos
    efectivos se actualizan en la siguiente petición (por ejemplo, 403 donde antes tenía acceso).

  No hay bloqueo por intentos de login fallidos (§16.1).
* **CORS:** aplazado; no hay configuración de orígenes.
* **HTTPS (NFR-SEC-007):** depende del despliegue; la aplicación no fuerza HTTPS.
* **La compra revela borradores (P4-08-F):** comprar para un evento no publicado responde 409
  «Tickets can only be purchased for PUBLISHED events», como exigen BR-TICKET-004 y los PRD de
  Servicios, Controladores y E2E, mientras que un código inexistente responde 404. Un USER
  autenticado puede así saber que existe un evento no publicado (no ve sus datos).
* **403 frente a 404 en los tickets (P4-D1):** un ticket ajeno responde 403 y uno inexistente, 404,
  así que se puede saber si existe un código de ticket. Los códigos son aleatorios (P2-D3).
* **404 sin request ID en el despacho de errores:** un 404 generado durante el despacho interno de
  `/error` no lleva el request ID en el log.
* **Consultas extra del perfil:** `User.profile` es `EAGER` en el lado inverso de la relación 1:1,
  así que el login y las comprobaciones de propiedad por email o username hacen una consulta más
  para cargar el perfil (4 consultas por login; 4 en una búsqueda de usuario denegada).
* **Avisos repetidos de Spring Security:** el INFO «Global AuthenticationManager configured…»
  aparece una vez por contexto aunque el login use el `AuthenticationManager` explícito, y un login
  fallido deja dos líneas de log (ver [Avisos esperados](#avisos-esperados-en-la-salida)).
* **Cambio y recuperación de contraseña:** fuera del alcance del PRD (§5.2); la activación de los
  usuarios anteriores es manual.
* **Heredadas:** siguen las de las fases anteriores (concurrencia de la compra §53, `Ticket` sin
  `@Version`, email con distinto uso de mayúsculas).
