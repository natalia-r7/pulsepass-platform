package com.pulsepass.integration;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.security.MethodSecurityConfig;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.TicketService;
import com.pulsepass.service.UserService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Propiedad de los recursos con {@code @PreAuthorize} sobre el contexto completo, PostgreSQL real y
 * JWT reales (PRD Seguridad §11, §11.1, §17.3, §17.4; T-SEC-009..013, T-SEC-015; PRD E2E
 * TC-SEC-006..012, TC-SEC-014; P4-D1, P4-D5, P4-D8, P4-D19; P4-08).
 *
 * <p>Actores (D12): A y B son USER registrados con {@code POST /api/users}; a STAFF y ADMIN se les
 * cambia el rol en la base de datos (solo en pruebas). Todos inician sesión con el login real. El
 * venue se inserta con {@code JdbcTemplate} (no hay endpoint para crearlo) y ADMIN crea y publica
 * el evento con la API.
 *
 * <p>Cómo se distingue una denegación de {@code @PreAuthorize} de una de URL: las reglas de URL se
 * aplican en la cadena de filtros, antes de Spring MVC, así que el {@code MvcResult} no tiene
 * excepción resuelta. Una denegación del servicio lanza {@code AccessDeniedException} dentro de
 * Spring MVC y la resuelve {@code GlobalExceptionHandler} (P4-D19). {@link #deniedByPreAuthorize}
 * exige esa excepción resuelta; {@link #deniedByUrlRule}, que no la haya. Para las reglas que
 * coinciden con la de URL, {@link #runAs} llama directamente al bean del servicio, sin HTTP.
 *
 * <p>Los datos se crean una vez para toda la clase y se borran al final por sus ids.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OwnershipSecurityIT {

    /** Contraseña ficticia del ejemplo del PRD Seguridad §10.2. */
    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

    // DATO DE PRUEBA: actores, venue y eventos exclusivos de esta clase.
    private static final String A_EMAIL = "owner.a@pulsepass.test";
    private static final String B_EMAIL = "owner.b@pulsepass.test";
    private static final String STAFF_EMAIL = "owner.staff@pulsepass.test";
    private static final String ADMIN_EMAIL = "owner.admin@pulsepass.test";
    private static final String VENUE_CODE = "VEN-OWN-01";
    private static final String EVENT_CODE = "OWN-2030";
    private static final String DRAFT_CODE = "OWN-DRAFT";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private UserService userService;

    private final List<Long> userIds = new ArrayList<>();

    private String tokenA;
    private String tokenB;
    private String tokenStaff;
    private String tokenAdmin;

    private Long aId;
    private Long staffId;
    private Long adminId;

    @BeforeAll
    void createActorsAndEvent() throws Exception {
        // Registro público sin autenticación (condición 5 / FR-SEC-USR-01).
        aId = register("owner.a", A_EMAIL);
        register("owner.b", B_EMAIL);
        staffId = register("owner.staff", STAFF_EMAIL);
        adminId = register("owner.admin", ADMIN_EMAIL);
        replaceRole(staffId, "STAFF");
        replaceRole(adminId, "ADMIN");

        tokenA = login("owner.a");
        tokenB = login("owner.b");
        tokenStaff = login("owner.staff");
        tokenAdmin = login("owner.admin");

        jdbcTemplate.update("INSERT INTO venues (code, name, city, address, capacity, active) "
                + "VALUES (?, 'Venue de propiedad', 'Santa Marta', NULL, 100, true)", VENUE_CODE);
        createEvent(EVENT_CODE);
        mockMvc.perform(as(tokenAdmin, patch("/api/events/" + EVENT_CODE + "/publish")))
                .andExpect(status().isOk());
        createEvent(DRAFT_CODE);
    }

    @AfterAll
    void deleteCreatedRows() {
        jdbcTemplate.update("DELETE FROM tickets WHERE event_id IN (SELECT id FROM events WHERE event_code IN (?, ?))",
                EVENT_CODE, DRAFT_CODE);
        jdbcTemplate.update("DELETE FROM events WHERE event_code IN (?, ?)", EVENT_CODE, DRAFT_CODE);
        jdbcTemplate.update("DELETE FROM venues WHERE code = ?", VENUE_CODE);
        for (Long id : userIds) {
            jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM user_credentials WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM user_profiles WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
        }
    }

    // ---------------------------------------------------------------- activación (P4-08-A)

    @Test
    @DisplayName("P4-08-A: en el contexto completo MethodSecurityConfig está cargada y los servicios son proxies con seguridad")
    void methodSecurityIsActiveInFullContext() {
        assertThat(context.getBeansOfType(MethodSecurityConfig.class)).hasSize(1);
        assertThat(AopUtils.isAopProxy(ticketService)).isTrue();
        assertThat(AopUtils.isAopProxy(userService)).isTrue();
    }

    // ---------------------------------------------------------------- usuarios (FR-SEC-USR-02/03)

    @Test
    @DisplayName("T-SEC-015 / TC-SEC-006 / TC-SEC-007: B no puede buscar a A por email ni por username (403 de @PreAuthorize)")
    void userCannotLookUpAnotherUser() throws Exception {
        deniedByPreAuthorize(as(tokenB, get("/api/users/by-email").param("email", A_EMAIL)));
        deniedByPreAuthorize(as(tokenB, get("/api/users/by-username").param("username", "owner.a")));
    }

    @Test
    @DisplayName("§11.1: A se busca a sí mismo (email con otras mayúsculas) y ADMIN busca a A → 200")
    void ownerAndAdminCanLookUpUser() throws Exception {
        mockMvc.perform(as(tokenA, get("/api/users/by-email").param("email", "OWNER.A@PulsePass.TEST")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("owner.a"));
        mockMvc.perform(as(tokenA, get("/api/users/by-username").param("username", "owner.a")))
                .andExpect(status().isOk());
        mockMvc.perform(as(tokenAdmin, get("/api/users/by-email").param("email", A_EMAIL)))
                .andExpect(status().isOk());
        mockMvc.perform(as(tokenAdmin, get("/api/users/by-username").param("username", "owner.a")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("P4-D1: un USER que busca un email inexistente recibe 403, no 404")
    void unknownEmailLookupIsForbiddenForUser() throws Exception {
        deniedByPreAuthorize(as(tokenA, get("/api/users/by-email").param("email", "nadie@pulsepass.test")));
    }

    // ---------------------------------------------------------------- consulta de tickets

    @Test
    @DisplayName("T-SEC-009 / TC-SEC-009: B no lee el ticket de A (403); A, STAFF y ADMIN sí; uno inexistente → 404")
    void ticketReadIsLimitedToOwnerStaffAndAdmin() throws Exception {
        String code = purchase(tokenA, A_EMAIL);

        deniedByPreAuthorize(as(tokenB, get("/api/tickets/" + code)));
        mockMvc.perform(as(tokenA, get("/api/tickets/" + code))).andExpect(status().isOk());
        mockMvc.perform(as(tokenStaff, get("/api/tickets/" + code))).andExpect(status().isOk());
        mockMvc.perform(as(tokenAdmin, get("/api/tickets/" + code))).andExpect(status().isOk());
        mockMvc.perform(as(tokenA, get("/api/tickets/TKT-NO-EXISTE")))   // DATO DE PRUEBA
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-SEC-010: B no lista los tickets de A (403); A y ADMIN sí")
    void ticketListByEmailIsLimitedToOwnerAndAdmin() throws Exception {
        purchase(tokenA, A_EMAIL);

        deniedByPreAuthorize(as(tokenB, get("/api/tickets/by-user").param("email", A_EMAIL)));
        mockMvc.perform(as(tokenA, get("/api/tickets/by-user").param("email", A_EMAIL)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].userEmail").value(A_EMAIL));
        mockMvc.perform(as(tokenAdmin, get("/api/tickets/by-user").param("email", A_EMAIL)))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- compra (FR-SEC-TKT-01)

    @Test
    @DisplayName("T-SEC-010 / TC-SEC-008: A compra con el email de B → 403 de @PreAuthorize y 0 tickets nuevos")
    void userCannotPurchaseForAnotherUser() throws Exception {
        long before = ticketCount();

        deniedByPreAuthorize(as(tokenA, post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                .content(purchaseJson(B_EMAIL, EVENT_CODE))));

        assertThat(ticketCount()).isEqualTo(before);
    }

    @Test
    @DisplayName("§11.1: A compra para sí con su email en otras mayúsculas → 201 (se comparan ids)")
    void userCanPurchaseWithOwnEmailInAnyCase() throws Exception {
        mockMvc.perform(as(tokenA, post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson("Owner.A@PULSEPASS.test", EVENT_CODE))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("P4-D8: ADMIN no compra solo por tener ese rol (403) y no se crea ningún ticket")
    void adminCannotPurchase() throws Exception {
        long before = ticketCount();

        // Lo bloquea ya la regla de URL (hasRole USER) y, detrás, la de @PreAuthorize.
        deniedByUrlRule(as(tokenAdmin, post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                .content(purchaseJson(ADMIN_EMAIL, EVENT_CODE))));

        assertThat(ticketCount()).isEqualTo(before);
    }

    @Test
    @DisplayName("P4-08-F: comprar para un evento no publicado sigue respondiendo 409 (BR-TICKET-004)")
    void purchaseOnDraftKeeps409() throws Exception {
        mockMvc.perform(as(tokenA, post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson(A_EMAIL, DRAFT_CODE))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Tickets can only be purchased for PUBLISHED events: " + DRAFT_CODE));
    }

    // ---------------------------------------------------------------- cancelación (FR-SEC-TKT-05)

    @Test
    @DisplayName("TC-SEC-011: B y STAFF no cancelan el ticket de A (403) y el ticket sigue PAID")
    void otherUserAndStaffCannotCancel() throws Exception {
        String code = purchase(tokenA, A_EMAIL);

        deniedByPreAuthorize(as(tokenB, patch("/api/tickets/" + code + "/cancel")));
        deniedByPreAuthorize(as(tokenStaff, patch("/api/tickets/" + code + "/cancel")));

        assertThat(ticketStatus(code)).isEqualTo("PAID");
    }

    @Test
    @DisplayName("T-SEC-011: A cancela su ticket y ADMIN cancela un ticket de A → 200 CANCELLED")
    void ownerAndAdminCanCancel() throws Exception {
        String own = purchase(tokenA, A_EMAIL);
        String byAdmin = purchase(tokenA, A_EMAIL);

        mockMvc.perform(as(tokenA, patch("/api/tickets/" + own + "/cancel")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(as(tokenAdmin, patch("/api/tickets/" + byAdmin + "/cancel")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // ---------------------------------------------------------------- uso (FR-SEC-TKT-06)

    @Test
    @DisplayName("T-SEC-012 / TC-SEC-012 / T-SEC-013 / TC-SEC-014: un USER no marca el uso (403); STAFF sí (200 USED)")
    void onlyStaffOrAdminMarkTicketAsUsed() throws Exception {
        String code = purchase(tokenA, A_EMAIL);

        deniedByUrlRule(as(tokenA, patch("/api/tickets/" + code + "/use")));
        assertThat(ticketStatus(code)).isEqualTo("PAID");

        mockMvc.perform(as(tokenStaff, patch("/api/tickets/" + code + "/use")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("USED"));
    }

    @Test
    @DisplayName("FR-SEC-TKT-06: ADMIN también marca un ticket como usado → 200 USED")
    void adminCanMarkTicketAsUsed() throws Exception {
        String code = purchase(tokenA, A_EMAIL);

        mockMvc.perform(as(tokenAdmin, patch("/api/tickets/" + code + "/use")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("USED"));
        assertThat(ticketStatus(code)).isEqualTo("USED");
    }

    // ---------------------------------------------------------------- tickets PAID de un evento (FR-SEC-TKT-04)

    @Test
    @DisplayName("FR-SEC-TKT-04 / TC-SEC-013: STAFF y ADMIN consultan los tickets PAID del evento por HTTP → 200 con el ticket")
    void staffAndAdminListPaidTicketsByHttp() throws Exception {
        String code = purchase(tokenA, A_EMAIL);

        for (String token : List.of(tokenStaff, tokenAdmin)) {
            MvcResult result = mockMvc.perform(as(token, get("/api/events/" + EVENT_CODE + "/tickets/paid")))
                    .andExpect(status().isOk())
                    .andReturn();
            assertThat(result.getResponse().getContentAsString()).contains(code);
        }
    }

    @Test
    @DisplayName("FR-SEC-TKT-04 / P4-08-D: @PreAuthorize del servicio permite a STAFF y ADMIN y rechaza a un USER, sin pasar por HTTP")
    void paidTicketsRuleIsEnforcedByTheService() throws Exception {
        String code = purchase(tokenA, A_EMAIL);

        // Llamada directa al bean (proxy) del servicio: aquí no hay reglas de URL.
        runAs(new SecurityPrincipal(staffId, "owner.staff", Set.of(Role.STAFF)), () ->
                assertThat(ticketService.findPaidTicketsByEvent(EVENT_CODE))
                        .extracting(TicketResponse::ticketCode).contains(code));
        runAs(new SecurityPrincipal(adminId, "owner.admin", Set.of(Role.ADMIN)), () ->
                assertThat(ticketService.findPaidTicketsByEvent(EVENT_CODE))
                        .extracting(TicketResponse::ticketCode).contains(code));
        runAs(new SecurityPrincipal(aId, "owner.a", Set.of(Role.USER)), () ->
                assertThatThrownBy(() -> ticketService.findPaidTicketsByEvent(EVENT_CODE))
                        .isInstanceOf(AccessDeniedException.class));
    }

    // ---------------------------------------------------------------- auxiliares

    /** 403 «Access denied» resuelto en Spring MVC: lo lanzó {@code @PreAuthorize}, no una regla de URL. */
    private void deniedByPreAuthorize(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andReturn();
        assertThat(result.getResolvedException()).isInstanceOf(AccessDeniedException.class);
    }

    /** 403 de la cadena de filtros: la regla de URL lo bloquea antes de Spring MVC. */
    private void deniedByUrlRule(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andReturn();
        assertThat(result.getResolvedException()).isNull();
    }

    /** Ejecuta el bloque con ese principal en el SecurityContext, como lo deja el filtro JWT, y lo limpia. */
    private static void runAs(SecurityPrincipal principal, Runnable block) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities()));
        try {
            block.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private Long register(String username, String email) throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "%s", "email": "%s", "firstName": "Prueba", "lastName": "Propiedad",
                         "birthDate": "1990-01-01", "password": "%s"}""".formatted(username, email, PASSWORD)))
                .andExpect(status().isCreated());
        Long id = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
        userIds.add(id);
        return id;
    }

    /** D12: STAFF y ADMIN solo en pruebas; se quita USER para que el rol sea exactamente ese. */
    private void replaceRole(Long userId, String role) {
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", userId, role);
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier": "%s", "password": "%s"}""".formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asString();
    }

    private void createEvent(String eventCode) throws Exception {
        mockMvc.perform(as(tokenAdmin, post("/api/events").contentType(MediaType.APPLICATION_JSON).content("""
                        {"eventCode": "%s", "name": "Evento de propiedad", "category": "MUSIC",
                         "eventDate": "2030-06-01T20:00:00", "minimumAge": 0, "venueCode": "%s"}"""
                        .formatted(eventCode, VENUE_CODE))))
                .andExpect(status().isCreated());
    }

    /** A compra un ticket GENERAL para el evento publicado; devuelve su código. */
    private String purchase(String token, String email) throws Exception {
        MvcResult result = mockMvc.perform(as(token, post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson(email, EVENT_CODE))))
                .andExpect(status().isCreated())
                .andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString()).get("ticketCode").asString();
    }

    private static String purchaseJson(String email, String eventCode) {
        return """
                {"userEmail": "%s", "eventCode": "%s", "type": "GENERAL"}""".formatted(email, eventCode);
    }

    private long ticketCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tickets", Long.class);
        return count == null ? 0 : count;
    }

    private String ticketStatus(String ticketCode) {
        return jdbcTemplate.queryForObject("SELECT status FROM tickets WHERE ticket_code = ?", String.class, ticketCode);
    }
}
