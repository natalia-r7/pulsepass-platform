# PulsePass — Backend · Fases 1 (Persistencia) y 2 (Servicios)

Backend de PulsePass, plataforma de gestión y venta de entradas para eventos. Se implementa por
fases, cada una según su PRD dentro de `docs/proyecto Pulsepass.pdf`:

* **Fase 1 — Persistencia** (PRD de Persistencia, páginas 1–24): modelo relacional, migraciones
  Flyway, entidades JPA, repositorios Spring Data y pruebas de integración contra PostgreSQL real.
* **Fase 2 — Servicios** (PRD de la Capa de Servicios): contratos `Service`, DTO, mappers
  MapStruct, excepciones de negocio, reglas de negocio, transacciones y pruebas unitarias con
  Mockito.

Siguen fuera de alcance: la API REST (Fase 3, Controladores), la seguridad (Fase 4) y el frontend.

## Estado de las fases

| Fase | PRD | Estado | Cierre |
|---|---|---|---|
| 1 — Persistencia | Persistencia | Cerrada | Commit `35b3ac6`, etiqueta `fase-1-persistencia` |
| 2 — Servicios | Capa de Servicios | En cierre (P2-15) | Suite completa verde sobre `a26799c`; revisión del [DoD](#definition-of-done-de-la-fase-2) en este documento |

## Tecnologías

| Tecnología | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.2 |
| Maven (mediante Maven Wrapper `./mvnw`) | 3.10.0 |
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

## Requisitos para ejecutar

* Java 21.
* Docker en ejecución: las pruebas de integración levantan PostgreSQL 17 con Testcontainers.
* No hace falta instalar Maven ni PostgreSQL.

## Estructura

```
backend/
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/            Maven Wrapper
└── src/
    ├── main/java/com/pulsepass/
    │   ├── PulsepassApplication.java
    │   ├── config/                  ClockConfig: bean Clock con zona America/Bogota (Fase 2)
    │   ├── domain/                  Entidades JPA
    │   │   └── enums/               EventCategory, EventStatus, TicketType, TicketStatus
    │   ├── dto/                     DTO como record (Fase 2)
    │   │   ├── request/             CreateEventRequest, RegisterUserRequest, PurchaseTicketRequest
    │   │   └── response/            VenueResponse, ArtistResponse, UserResponse, EventResponse,
    │   │                            EventSummaryResponse, TicketResponse
    │   ├── exception/               ResourceNotFoundException, DuplicateResourceException,
    │   │                            BusinessRuleException (Fase 2)
    │   ├── mapper/                  Mappers MapStruct (Fase 2)
    │   ├── repository/              Repositorios Spring Data JPA
    │   └── service/                 Interfaces de servicio, TicketPricingStrategy y
    │       │                        TicketCodeGenerator (Fase 2)
    │       └── impl/                Implementaciones (Fase 2)
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/            V1, V2 y V3 (Flyway)
    └── test/java/com/pulsepass/
        ├── PulsepassApplicationTests.java
        ├── TestcontainersConfiguration.java
        ├── persistence/             Pruebas de integración y PersistenceFixtures
        ├── mapper/                  Pruebas de los mappers reales (Fase 2)
        └── service/                 ServiceFixtures (Fase 2)
            └── impl/                Pruebas unitarias de servicios y componentes (Fase 2)
```

## Arquitectura

```
Service (interfaz)  ──►  ServiceImpl (@Service)
                          ├── Repository (Spring Data JPA)      acceso a datos, sin reglas
                          ├── Mapper (MapStruct)                Entity → DTO
                          ├── Clock (ClockConfig)               "ahora" inyectable
                          ├── TicketPricingStrategy             precio (solo TicketService)
                          └── TicketCodeGenerator               código de ticket (solo TicketService)
```

* Los servicios reciben y devuelven DTO; nunca exponen entidades JPA (SRV-001).
* Cada servicio tiene interfaz e implementación en `service.impl` (SRV-003), con dependencias
  `final` inyectadas por constructor (SRV-002, NFR-002).
* Las reglas de negocio viven en los servicios; los repositorios solo acceden a datos (SRV-005,
  NFR-006). No hay SQL en los servicios.
* `open-in-view: false`: el mapeo a DTO ocurre dentro de la transacción del servicio, donde las
  relaciones `LAZY` todavía se pueden cargar.

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

La Fase 2 no agregó ni modificó migraciones. La migración V4 (credenciales y roles) pertenece a la
Fase 4 (Seguridad).

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
* Los DTO no llevan Bean Validation: la validación estructural corresponde a la Fase 3 (P2-D18).
* **5 mappers MapStruct** con `componentModel = "spring"` y `unmappedTargetPolicy = ERROR`
  (P2-05 D-E1): si un campo del DTO queda sin origen, la compilación falla. `EventMapper` usa
  `ArtistMapper` inyectado por constructor (P2-05 D-A1). `TicketMapper` aplana `user.email`,
  `event.eventCode` y `event.name` (§34).

## Excepciones y mensajes

| Excepción | Uso (PRD §35) | Código HTTP previsto (PRD de Controladores §13, Fase 3) |
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

Maven Surefire ejecuta tanto las clases `*Test` como las `*IT`, por lo que `./mvnw clean test`
incluye todas las pruebas de integración (QT-010). La configuración de Surefire no se modificó en
la Fase 2.

## Qué cubren

### Pruebas de integración (PostgreSQL con Testcontainers)

Las ocho clases del paquete `persistence` usan `@DataJpaTest` con
`@Import(TestcontainersConfiguration.class)`: Flyway construye el esquema sobre PostgreSQL 17 y
Hibernate lo valida antes de ejecutar las pruebas. `PulsepassApplicationTests` usa
`@SpringBootTest` con la misma configuración y comprueba que arranca la aplicación completa,
incluidos los servicios, los mappers y el bean `Clock`.

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
| `PulsepassApplicationTests` | 1 | Arranque completo de la aplicación |

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
| `EventServiceImplTest` | 31 | FR-SVC-003..008, BR-EVENT-001..011, TEST-EVENT-001..008, AC-001..003 |
| `UserServiceImplTest` | 10 | FR-SVC-010..012, BR-USER-001..005, TEST-USER-001..004 |
| `TicketServiceImplTest` | 44 | FR-SVC-013..018, BR-TICKET-001..014, TEST-TICKET-001..012, AC-004..011 |
| `TicketPricingStrategyImplTest` | 11 | §28, BR-TICKET-009, TP-1..TP-5 |
| `TicketCodeGeneratorImplTest` | 21 | Formato `TKT-` + 6 hexadecimales (P2-D3); 20 de ellas son un `@RepeatedTest` |

En los caminos inválidos se verifica con `verify(..., never())` que no se llama a ningún `save()`
(DoD §50). `ServiceFixtures` contiene los datos del escenario del PRD de Servicios (§47) y es
independiente de `PersistenceFixtures` (P2-D12); los valores que §47 no define, incluido el
usuario Sofía de AC-008 (TK-8), están marcados como `DATO DE PRUEBA`.

### Resultados registrados

* **Cierre de la Fase 1:** `./mvnw clean test` terminó con `Tests run: 46, Failures: 0, Errors: 0,
  Skipped: 0` y `BUILD SUCCESS`.
* **Cierre de la Fase 2** (2026-11-16, sobre el commit `a26799c`): `./mvnw clean test` terminó con
  `Tests run: 206, Failures: 0, Errors: 0, Skipped: 0` y `BUILD SUCCESS`. De ellas, 62 son de
  integración o arranque (16 nuevas, de `ServiceRepositoryIT`) y 144 unitarias.

## Avisos esperados en la salida

La ejecución de `./mvnw clean test` muestra estos avisos. **Ninguno es un fallo:** todas las
pruebas pasan.

| Aviso | Origen | Explicación |
|---|---|---|
| `Could not transfer metadata /.meta/prefixes.txt ... HTTP Status: 401` (2 veces) | Maven | Repositorios remotos de Flyway que exigen autenticación para un archivo de metadatos opcional. Las dependencias se resuelven igual |
| `Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK.` | Mockito | Mockito carga su agente en tiempo de ejecución. Hoy funciona; una versión futura del JDK podría impedirlo, y entonces habría que declarar Mockito como agente en Surefire |
| `A Java agent has been loaded dynamically (... byte-buddy-agent-1.18.15.jar)` y `Dynamic loading of agents will be disallowed by default in a future release` | JVM | El mismo agente de Mockito, visto desde la JVM |
| `Sharing is only supported for boot loader classes because bootstrap classpath has been appended` | JVM | Consecuencia del agente anterior; solo afecta al uso compartido de clases de la JVM |
| `HHH000247: ErrorCode: 0, SQLState: 23505` / `23514` con `duplicate key value violates unique constraint ...` o `violates check constraint ...` (11 veces) | Hibernate | Las registran las pruebas de integración que violan restricciones **a propósito** para comprobar que PostgreSQL las rechaza |

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

# Decisiones clave

## Fase 1 — Persistencia

* **Configuración:** `ddl-auto: validate` (PRD §13) y `open-in-view: false`. No se configura
  ninguna conexión de base de datos (tampoco en la Fase 2): las pruebas la reciben de
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
  `@ParameterizedTest`, `@EnumSource` y `@CsvSource`, junto con `@RepeatedTest`.

---

# Limitaciones conocidas

## Fase 1 — Persistencia

* **Comodines en `findRecommended`:** los caracteres `%` y `_` del texto de búsqueda no se
  escapan, porque el PRD no define ese caso.
* **Rechazos de Hibernate antes del INSERT:** las pruebas de relaciones obligatorias (`venue`,
  `user`, `event`) esperan que Hibernate detecte el valor null antes de enviar la sentencia, lo
  que ocurre mientras el proyecto no incluya Bean Validation (sigue sin incluirlo en la Fase 2).
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
* **Sin validación estructural:** los servicios no validan `null` ni cadenas vacías (P2-D18); esa
  validación llegará con Bean Validation en la Fase 3.
