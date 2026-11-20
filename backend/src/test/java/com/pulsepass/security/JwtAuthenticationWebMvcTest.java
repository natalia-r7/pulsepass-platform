package com.pulsepass.security;

import com.pulsepass.FixedClockTestConfiguration;
import com.pulsepass.controller.EventController;
import com.pulsepass.controller.TicketController;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.service.EventService;
import com.pulsepass.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autenticación con JWT real a través de la cadena de seguridad real (PRD Seguridad §12, §14.3;
 * FR-SEC-AUTH-006..009; T-SEC-007, T-SEC-008, T-SEC-016; TC-AUTH-012; P4-D6; P4-05).
 * <p>
 * Los tokens los emite el {@link JwtService} real con la clave de pruebas y el {@code Clock} fijo.
 * Se simulan {@link CustomUserDetailsService} (qué usuario y roles hay en la base de datos) y los
 * servicios de los controladores. A diferencia de las demás pruebas web, aquí la identidad llega
 * por la cabecera {@code Authorization}, no por {@code @WithSecurityPrincipal}.
 */
@WebMvcTest(controllers = {EventController.class, TicketController.class})
@Import({FixedClockTestConfiguration.class, WebSecurityTestConfiguration.class})
class JwtAuthenticationWebMvcTest {

    private static final long USER_ID = 7L;    // DATO DE PRUEBA
    private static final long STAFF_ID = 8L;   // DATO DE PRUEBA

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private Clock clock;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private TicketService ticketService;

    @Test
    @DisplayName("FR-SEC-AUTH-009: un USER con token válido consulta un ticket (200)")
    void userWithValidTokenReachesProtectedRoute() throws Exception {
        givenUser(USER_ID, Role.USER);

        mockMvc.perform(get("/api/tickets/TKT-8F2A1C").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("T-SEC-003: un USER con token válido no puede crear eventos (403) y no se llama al servicio")
    void userWithValidTokenIsForbiddenOnAdminRoute() throws Exception {
        givenUser(USER_ID, Role.USER);

        mockMvc.perform(post("/api/events").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("T-SEC-013: un STAFF con token válido marca un ticket como usado (200)")
    void staffWithValidTokenMarksTicketAsUsed() throws Exception {
        givenUser(STAFF_ID, Role.STAFF);

        mockMvc.perform(patch("/api/tickets/TKT-8F2A1C/use").header(HttpHeaders.AUTHORIZATION, bearer(STAFF_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("T-SEC-008: un token manipulado responde 401 con ErrorResponse y no llega al servicio")
    void tamperedTokenIsUnauthorized() throws Exception {
        String tampered = tamper(jwtService.issueAccessToken(USER_ID).token());

        mockMvc.perform(get("/api/tickets/TKT-8F2A1C").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.timestamp").value(FixedClockTestConfiguration.EXPECTED_TIMESTAMP))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.details").value(anEmptyMap()));
        verifyNoInteractions(ticketService, userDetailsService);
    }

    @Test
    @DisplayName("T-SEC-007: un token caducado responde 401 y no llega al servicio")
    void expiredTokenIsUnauthorized() throws Exception {
        JwtService earlier = new JwtService(jwtProperties, Clock.offset(clock, Duration.ofHours(-1)));   // emitido hace 1 h (dura 15 min)
        String expired = earlier.issueAccessToken(USER_ID).token();

        mockMvc.perform(get("/api/tickets/TKT-8F2A1C").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
        verifyNoInteractions(ticketService, userDetailsService);
    }

    @Test
    @DisplayName("P4-D6: un Bearer inválido en una ruta pública responde 401 (nunca autentica por accidente)")
    void invalidBearerOnPublicRouteIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/events/published").header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("§14.3.5: sin cabecera, una ruta pública sigue respondiendo 200")
    void publicRouteWithoutHeaderStillWorks() throws Exception {
        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FR-SEC-AUTH-007 / TC-AUTH-012: un token en la query string no autentica (401 en una ruta protegida)")
    void tokenInQueryStringDoesNotAuthenticate() throws Exception {
        mockMvc.perform(get("/api/tickets/TKT-8F2A1C").queryParam("access_token", bearerToken(USER_ID)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(ticketService, userDetailsService);
    }

    @Test
    @DisplayName("T-SEC-016 / SEC-011: un usuario desactivado con un token todavía vigente recibe 401")
    void deactivatedUserWithValidTokenIsUnauthorized() throws Exception {
        when(userDetailsService.loadPrincipalById(USER_ID)).thenThrow(new DisabledException("prueba"));

        mockMvc.perform(get("/api/tickets/TKT-8F2A1C").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(ticketService);
    }

    @Test
    @DisplayName("FR-SEC-AUTH-008: una petición autenticada con JWT no crea sesión ni cookies")
    void authenticatedRequestDoesNotCreateSession() throws Exception {
        givenUser(USER_ID, Role.USER);

        var result = mockMvc.perform(get("/api/tickets/TKT-8F2A1C").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    @DisplayName("P4-05-f: el filtro JWT está una sola vez en la cadena y no es un bean (no se registra dos veces)")
    void jwtFilterIsRegisteredOnceInsideTheChain() {
        assertThat(securityFilterChain.getFilters())
                .filteredOn(JwtAuthenticationFilter.class::isInstance)
                .hasSize(1);
        assertThat(context.getBeansOfType(JwtAuthenticationFilter.class)).isEmpty();
    }

    // ------------------------------------------------------------------ ayudantes

    /** El usuario existe, está activo y tiene estos roles en la base de datos. */
    private void givenUser(long userId, Role role) {
        when(userDetailsService.loadPrincipalById(userId))
                .thenReturn(new SecurityPrincipal(userId, "usuario." + userId, Set.of(role)));   // DATO DE PRUEBA
    }

    private String bearerToken(long userId) {
        return jwtService.issueAccessToken(userId).token();
    }

    private String bearer(long userId) {
        return "Bearer " + bearerToken(userId);
    }

    /** Cambia un carácter en mitad de la firma. */
    private static String tamper(String token) {
        int lastDot = token.lastIndexOf('.');
        char[] chars = token.toCharArray();
        int middle = lastDot + (token.length() - lastDot) / 2;
        chars[middle] = chars[middle] == 'A' ? 'B' : 'A';
        return new String(chars);
    }
}
