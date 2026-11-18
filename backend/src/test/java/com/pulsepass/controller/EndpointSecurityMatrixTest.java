package com.pulsepass.controller;

import com.pulsepass.FixedClockTestConfiguration;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.security.WebSecurityTestConfiguration;
import com.pulsepass.service.ArtistService;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.EventService;
import com.pulsepass.service.TicketService;
import com.pulsepass.service.UserService;
import com.pulsepass.service.VenueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz de autorización por URL de las 20 operaciones (PRD Seguridad §11, §14.3, §17.4; P4-04-m).
 * <p>
 * Cada operación se ejecuta como anónimo, USER, STAFF y ADMIN con la cadena de seguridad real.
 * Comprueba el código que da la regla de URL y, cuando la petición se deniega, que no se llamó a
 * ningún servicio (T-SEC-003). Los servicios son {@code @MockitoBean}: aquí solo importa la
 * regla, no la respuesta de negocio, que ya prueban las clases de cada controlador.
 * <p>
 * La propiedad de los recursos (P4-08) y la visibilidad de los borradores (P4-04b) todavía no se
 * aplican: esas rutas aparecen aquí como «cualquier usuario autenticado».
 */
@WebMvcTest(controllers = {VenueController.class, ArtistController.class, EventController.class,
        UserController.class, TicketController.class})
@Import({FixedClockTestConfiguration.class, WebSecurityTestConfiguration.class})
class EndpointSecurityMatrixTest {

    /** DATO DE PRUEBA: cuerpos mínimos válidos para Bean Validation. */
    private static final String CREATE_EVENT_JSON = """
            {"eventCode": "MTX-2030", "name": "Evento de prueba", "category": "MUSIC",
             "eventDate": "2030-06-01T20:00:00", "minimumAge": 0, "venueCode": "VEN-SMR-01"}""";
    private static final String REGISTER_USER_JSON = """
            {"username": "matriz.usuario", "email": "matriz@pulsepass.test", "firstName": "Matriz",
             "lastName": "Prueba", "birthDate": "1995-01-15", "password": "ClaveDeMatriz2030!"}""";
    private static final String PURCHASE_TICKET_JSON = """
            {"userEmail": "matriz@pulsepass.test", "eventCode": "MTX-2030", "type": "GENERAL"}""";

    private static final Set<Actor> PUBLIC = EnumSet.allOf(Actor.class);
    private static final Set<Actor> AUTHENTICATED = EnumSet.of(Actor.USER, Actor.STAFF, Actor.ADMIN);
    private static final Set<Actor> STAFF_OR_ADMIN = EnumSet.of(Actor.STAFF, Actor.ADMIN);
    private static final Set<Actor> ADMIN_ONLY = EnumSet.of(Actor.ADMIN);
    private static final Set<Actor> USER_ONLY = EnumSet.of(Actor.USER);

    /** Las 20 operaciones con su regla de URL (§11) y su código de éxito (PRD Controladores). */
    private static final List<Endpoint> ENDPOINTS = List.of(
            new Endpoint("FR-SEC-VEN-01", HttpMethod.GET, "/api/venues/VEN-SMR-01", null, 200, PUBLIC),
            new Endpoint("FR-SEC-VEN-02", HttpMethod.GET, "/api/venues/active", null, 200, PUBLIC),
            new Endpoint("FR-SEC-EVT-01", HttpMethod.POST, "/api/events", CREATE_EVENT_JSON, 201, ADMIN_ONLY),
            new Endpoint("FR-SEC-EVT-02", HttpMethod.GET, "/api/events/CMF-2026", null, 200, PUBLIC),
            new Endpoint("FR-SEC-EVT-03", HttpMethod.GET, "/api/events/published", null, 200, PUBLIC),
            new Endpoint("FR-SEC-EVT-04", HttpMethod.PATCH, "/api/events/CMF-2026/publish", null, 200, ADMIN_ONLY),
            new Endpoint("FR-SEC-EVT-05", HttpMethod.POST, "/api/events/CMF-2026/artists/1", null, 200, ADMIN_ONLY),
            new Endpoint("FR-SEC-EVT-06", HttpMethod.GET, "/api/events/by-artist?stageName=SolarBeat", null, 200, PUBLIC),
            new Endpoint("FR-SEC-ART-01", HttpMethod.GET, "/api/artists/1", null, 200, PUBLIC),
            new Endpoint("FR-SEC-ART-02", HttpMethod.GET, "/api/artists/by-stage-name?stageName=SolarBeat", null, 200, PUBLIC),
            new Endpoint("FR-SEC-ART-03", HttpMethod.GET, "/api/artists/active", null, 200, PUBLIC),
            new Endpoint("FR-SEC-USR-01", HttpMethod.POST, "/api/users", REGISTER_USER_JSON, 201, PUBLIC),
            new Endpoint("FR-SEC-USR-02", HttpMethod.GET, "/api/users/by-email?email=andrea@email.com", null, 200, AUTHENTICATED),
            new Endpoint("FR-SEC-USR-03", HttpMethod.GET, "/api/users/by-username?username=andrea", null, 200, AUTHENTICATED),
            new Endpoint("FR-SEC-TKT-01", HttpMethod.POST, "/api/tickets", PURCHASE_TICKET_JSON, 201, USER_ONLY),
            new Endpoint("FR-SEC-TKT-02", HttpMethod.GET, "/api/tickets/TKT-8F2A1C", null, 200, AUTHENTICATED),
            new Endpoint("FR-SEC-TKT-03", HttpMethod.GET, "/api/tickets/by-user?email=andrea@email.com", null, 200, AUTHENTICATED),
            new Endpoint("FR-SEC-TKT-04", HttpMethod.GET, "/api/events/CMF-2026/tickets/paid", null, 200, STAFF_OR_ADMIN),
            new Endpoint("FR-SEC-TKT-05", HttpMethod.PATCH, "/api/tickets/TKT-8F2A1C/cancel", null, 200, AUTHENTICATED),
            new Endpoint("FR-SEC-TKT-06", HttpMethod.PATCH, "/api/tickets/TKT-8F2A1C/use", null, 200, STAFF_OR_ADMIN));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueService venueService;

    @MockitoBean
    private ArtistService artistService;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TicketService ticketService;

    @ParameterizedTest(name = "{0} {1} como {2}")
    @MethodSource("matrix")
    @DisplayName("§11 / §17.4: cada operación aplica su regla de URL por actor; si se deniega, no llama a ningún servicio")
    void enforcesUrlRuleForEachActor(String requirement, String operation, Actor actor, Endpoint endpoint)
            throws Exception {
        // ARRANGE
        int expected = endpoint.allowed().contains(actor)
                ? endpoint.successStatus()
                : (actor == Actor.ANONYMOUS ? 401 : 403);

        // ACT
        MvcResult result = mockMvc.perform(as(actor, endpoint.toRequest())).andReturn();

        // ASSERT
        assertThat(result.getResponse().getStatus()).as(requirement + " " + operation + " como " + actor)
                .isEqualTo(expected);
        if (expected == 401 || expected == 403) {
            verifyNoInteractions(venueService, artistService, eventService, authService, userService, ticketService);
        }
    }

    @Test
    @DisplayName("§14.3.3: GET /api/venues/active es pública (prueba HTTP explícita)")
    void activeVenuesArePublic() throws Exception {
        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SEC-001 / P4-D7: una ruta desconocida exige autenticación (401 para un anónimo)")
    void unknownRouteRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/no-existe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("P4-D7 / P3-D3: para un usuario autenticado, una ruta desconocida sigue respondiendo 404")
    void unknownRouteIsNotFoundForAuthenticatedUser() throws Exception {
        mockMvc.perform(as(Actor.USER, get("/api/no-existe")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No endpoint found for GET /api/no-existe"));
    }

    @Test
    @DisplayName("§13 / P4-04-e / P4-04-f: el 401 es un ErrorResponse con WWW-Authenticate: Bearer")
    void unauthorizedResponseUsesErrorResponse() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(CREATE_EVENT_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").value(FixedClockTestConfiguration.EXPECTED_TIMESTAMP))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.details").value(anEmptyMap()));
    }

    @Test
    @DisplayName("§13 / P4-04-e: el 403 es un ErrorResponse con un mensaje genérico")
    void forbiddenResponseUsesErrorResponse() throws Exception {
        mockMvc.perform(as(Actor.USER, post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_EVENT_JSON)))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").value(FixedClockTestConfiguration.EXPECTED_TIMESTAMP))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.details").value(anEmptyMap()));
    }

    @Test
    @DisplayName("FR-SEC-AUTH-008: la API no crea sesión ni envía cookies")
    void doesNotCreateSession() throws Exception {
        MvcResult result = mockMvc.perform(as(Actor.USER, get("/api/tickets/TKT-8F2A1C")))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    @DisplayName("§14.3.6: con CSRF desactivado, un POST público sin token CSRF no se rechaza")
    void publicPostDoesNotRequireCsrfToken() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(REGISTER_USER_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("P4-04-b: no existe POST /logout (sin redirección: 401 para un anónimo)")
    void logoutEndpointIsDisabled() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }

    // ------------------------------------------------------------------ ayudantes

    static Stream<Arguments> matrix() {
        return ENDPOINTS.stream().flatMap(endpoint -> Stream.of(Actor.values())
                .map(actor -> Arguments.of(endpoint.requirement(), endpoint.label(), actor, endpoint)));
    }

    /** Añade la identidad del actor; el anónimo va sin autenticación. */
    private static MockHttpServletRequestBuilder as(Actor actor, MockHttpServletRequestBuilder builder) {
        if (actor.role() == null) {
            return builder;
        }
        SecurityPrincipal principal = new SecurityPrincipal(
                actor.userId(), actor.name().toLowerCase() + ".prueba", Set.of(actor.role()));   // DATO DE PRUEBA
        builder.with(authentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities())));
        return builder;
    }

    /** Actores de la matriz (§4): visitante anónimo y los tres roles. */
    enum Actor {
        ANONYMOUS(null, 0L),
        USER(Role.USER, 1L),
        STAFF(Role.STAFF, 2L),
        ADMIN(Role.ADMIN, 3L);

        private final Role role;
        private final long userId;   // DATO DE PRUEBA

        Actor(Role role, long userId) {
            this.role = role;
            this.userId = userId;
        }

        Role role() {
            return role;
        }

        long userId() {
            return userId;
        }
    }

    /** Una operación de la API con su regla de URL y su código de éxito. */
    record Endpoint(String requirement, HttpMethod method, String uri, String body, int successStatus,
                    Set<Actor> allowed) {

        String label() {
            return method + " " + uri;
        }

        MockHttpServletRequestBuilder toRequest() {
            MockHttpServletRequestBuilder builder = request(method, uri);
            return body == null ? builder : builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
    }
}
