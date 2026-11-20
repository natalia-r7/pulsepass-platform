package com.pulsepass.integration;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.security.JwtProperties;
import com.pulsepass.security.JwtService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Escenario final de demostración de la Fase 4 de punta a punta (PRD Seguridad §23; UC-SEC-01..10;
 * §17.3; P4-10) sobre el contexto completo, PostgreSQL real (Testcontainers) y JWT reales.
 *
 * <p>Los pasos de §23 se ejecutan en el orden 1, 2, 6, 7, 3, 4, 5, 8, 9, 10 (P4-10-E1): el paso 3
 * necesita el evento ya publicado (paso 7), y Andrea debe intentar publicarlo (paso 6) mientras
 * sigue en DRAFT. Cada paso es una prueba ordenada que continúa el estado del anterior.
 *
 * <p>Datos (P4-10-E2, E3): venue VEN-SMR-01 y evento CMF-2026 con los valores del PRD §47, salvo la
 * fecha (DATO DE PRUEBA, 2030-12-12, para que la compra siga siendo futura). Andrea y Carlos son
 * USER registrados con {@code POST /api/users}; Laura es STAFF y {@code admin.prueba}, ADMIN: su rol
 * se cambia en la base de datos solo en pruebas (D12). El venue se inserta con {@code JdbcTemplate}
 * (no hay endpoint para crearlo) y el id de Solar Beat se lee de la base de datos (seed V2).
 *
 * <p>Al terminar se borran solo los datos creados aquí: tickets, asociaciones y evento CMF-2026,
 * el venue VEN-SMR-01 y los usuarios registrados, por sus ids.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityEndToEndIT {

    /** Contraseña ficticia del ejemplo del PRD Seguridad §10.2. */
    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

    private static final String VENUE_CODE = "VEN-SMR-01";   // PRD §47
    private static final String EVENT_CODE = "CMF-2026";     // PRD §47
    private static final String ANDREA_EMAIL = "andrea@email.com";   // PRD Servicios §47
    private static final String CARLOS_EMAIL = "carlos@email.com";   // PRD Servicios §47

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private Clock clock;

    private final List<Long> userIds = new ArrayList<>();

    private Long andreaId;
    private Long carlosId;
    private String tokenAndrea;
    private String tokenCarlos;
    private String tokenLaura;
    private String tokenAdmin;
    private String andreaTicket;
    private String carlosTicket;

    @BeforeAll
    void prepareContext() throws Exception {
        // Contexto heredado (§23): venue, Carlos, Laura (STAFF) y un ADMIN de prueba. Andrea se
        // registra en el paso 1.
        jdbcTemplate.update("INSERT INTO venues (code, name, city, address, capacity, active) "
                + "VALUES (?, 'Marina Convention Center', 'Santa Marta', NULL, 3, true)", VENUE_CODE);
        carlosId = register("carlos", CARLOS_EMAIL, "Carlos", "Pérez", "2005-06-15");
        Long lauraId = register("laura", "laura@email.com", "Laura", "Díaz", "2009-06-15");
        Long adminId = register("admin.prueba", "admin.prueba@pulsepass.test", "Admin", "Prueba", "1990-01-01"); // DATO DE PRUEBA
        replaceRole(lauraId, "STAFF");
        replaceRole(adminId, "ADMIN");
        tokenCarlos = login("carlos");
        tokenLaura = login("laura");
        tokenAdmin = login("admin.prueba");

        // ADMIN crea CMF-2026, que empieza en DRAFT (PRD Servicios §47).
        mockMvc.perform(as(tokenAdmin, post("/api/events").contentType(MediaType.APPLICATION_JSON).content("""
                        {"eventCode": "%s", "name": "Caribbean Music Fest 2026", "category": "MUSIC",
                         "eventDate": "2030-12-12T20:00:00", "minimumAge": 18, "venueCode": "%s"}"""
                        .formatted(EVENT_CODE, VENUE_CODE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @AfterAll
    void deleteCreatedRows() {
        jdbcTemplate.update("DELETE FROM tickets WHERE event_id IN (SELECT id FROM events WHERE event_code = ?)", EVENT_CODE);
        jdbcTemplate.update("DELETE FROM event_artists WHERE event_id IN (SELECT id FROM events WHERE event_code = ?)", EVENT_CODE);
        jdbcTemplate.update("DELETE FROM events WHERE event_code = ?", EVENT_CODE);
        jdbcTemplate.update("DELETE FROM venues WHERE code = ?", VENUE_CODE);
        for (Long id : userIds) {
            jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM user_credentials WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM user_profiles WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
        }
    }

    @Test
    @Order(1)
    @DisplayName("§23.1 / UC-SEC-01: Andrea se registra con contraseña → 201 sin hash ni contraseña en la respuesta")
    void step1AndreaRegisters() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("andrea", ANDREA_EMAIL, "Andrea", "Rojas", "2001-06-15")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(PASSWORD).doesNotContain("$2a$");
        andreaId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'andrea'", Long.class);
        userIds.add(andreaId);
        assertThat(jdbcTemplate.queryForList("SELECT role FROM user_roles WHERE user_id = ?", String.class, andreaId))
                .containsExactly("USER");
    }

    @Test
    @Order(2)
    @DisplayName("§23.2 / UC-SEC-02: Andrea inicia sesión → JWT con sub = su id y expiresIn = 900")
    void step2AndreaLogsIn() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("andrea")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn();
        tokenAndrea = field(result, "accessToken");

        assertThat(jwtService.extractUserId(tokenAndrea)).isEqualTo(andreaId);
    }

    @Test
    @Order(3)
    @DisplayName("§23.6 / UC-SEC-08: Andrea intenta publicar CMF-2026 → 403 y el evento sigue en DRAFT")
    void step6AndreaCannotPublish() throws Exception {
        MvcResult result = mockMvc.perform(as(tokenAndrea, patch("/api/events/" + EVENT_CODE + "/publish")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andReturn();

        // Lo bloquea la regla de URL, antes de Spring MVC: no se ejecuta el método de publicación.
        assertThat(result.getResolvedException()).isNull();
        assertThat(eventStatus()).isEqualTo("DRAFT");
    }

    @Test
    @Order(4)
    @DisplayName("§23.7: ADMIN publica CMF-2026 y asocia Solar Beat (id leído de la base de datos)")
    void step7AdminPublishesAndAssociatesSolarBeat() throws Exception {
        mockMvc.perform(as(tokenAdmin, patch("/api/events/" + EVENT_CODE + "/publish")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        Long solarBeatId = jdbcTemplate.queryForObject(
                "SELECT id FROM artists WHERE stage_name = 'Solar Beat'", Long.class);   // seed V2
        mockMvc.perform(as(tokenAdmin, post("/api/events/" + EVENT_CODE + "/artists/" + solarBeatId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artists[0].id").value(solarBeatId))
                .andExpect(jsonPath("$.artists[0].stageName").value("Solar Beat"));

        assertThat(eventStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    @Order(5)
    @DisplayName("§23.3 / UC-SEC-03: un anónimo consulta los eventos publicados sin token y encuentra CMF-2026")
    void step3AnonymousFindsPublishedEvent() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode events = jsonMapper.readTree(result.getResponse().getContentAsString());
        List<String> codes = new ArrayList<>();
        events.forEach(event -> codes.add(event.get("eventCode").asString()));
        assertThat(codes).contains(EVENT_CODE);
    }

    @Test
    @Order(6)
    @DisplayName("§23.4 / UC-SEC-04: Andrea compra su propia entrada → 201 PAID a su nombre")
    void step4AndreaBuysOwnTicket() throws Exception {
        MvcResult result = mockMvc.perform(as(tokenAndrea, post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson(ANDREA_EMAIL))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.userEmail").value(ANDREA_EMAIL))
                .andExpect(jsonPath("$.eventCode").value(EVENT_CODE))
                .andReturn();
        andreaTicket = field(result, "ticketCode");
    }

    @Test
    @Order(7)
    @DisplayName("§23.5 / UC-SEC-05 / UC-SEC-06: Andrea no compra para Carlos ni consulta o cancela su entrada (403)")
    void step5AndreaCannotTouchCarlosTicket() throws Exception {
        long ticketsBefore = ticketCount();

        // UC-SEC-05: compra con el email de Carlos → 403 y ningún ticket nuevo.
        deniedByPreAuthorize(as(tokenAndrea, post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                .content(purchaseJson(CARLOS_EMAIL))));
        assertThat(ticketCount()).isEqualTo(ticketsBefore);

        // Carlos compra su entrada.
        MvcResult result = mockMvc.perform(as(tokenCarlos, post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson(CARLOS_EMAIL))))
                .andExpect(status().isCreated())
                .andReturn();
        carlosTicket = field(result, "ticketCode");

        // UC-SEC-06: Andrea no la consulta ni la cancela; sigue PAID.
        deniedByPreAuthorize(as(tokenAndrea, get("/api/tickets/" + carlosTicket)));
        deniedByPreAuthorize(as(tokenAndrea, patch("/api/tickets/" + carlosTicket + "/cancel")));
        assertThat(ticketStatus(carlosTicket)).isEqualTo("PAID");
    }

    @Test
    @Order(8)
    @DisplayName("§23.8 / UC-SEC-07: un USER no marca el uso; Laura (STAFF) sí; el segundo uso → 409 por la regla heredada")
    void step8LauraMarksTicketAsUsedOnce() throws Exception {
        MvcResult userAttempt = mockMvc.perform(as(tokenAndrea, patch("/api/tickets/" + andreaTicket + "/use")))
                .andExpect(status().isForbidden())
                .andReturn();
        assertThat(userAttempt.getResolvedException()).isNull();   // regla de URL
        assertThat(ticketStatus(andreaTicket)).isEqualTo("PAID");

        mockMvc.perform(as(tokenLaura, patch("/api/tickets/" + andreaTicket + "/use")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));

        mockMvc.perform(as(tokenLaura, patch("/api/tickets/" + andreaTicket + "/use")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only PAID tickets can be used: " + andreaTicket));
        assertThat(ticketStatus(andreaTicket)).isEqualTo("USED");
    }

    @Test
    @Order(9)
    @DisplayName("§23.9 / UC-SEC-09: un JWT real con un carácter alterado y uno caducado → 401 uniforme")
    void step9TamperedAndExpiredTokensAreRejected() throws Exception {
        // El token original sigue siendo válido.
        mockMvc.perform(as(tokenAndrea, get("/api/auth/me"))).andExpect(status().isOk());

        // Manipulado: un carácter cambiado en la firma.
        expectUnauthorized(as(tamper(tokenAndrea), get("/api/auth/me")));

        // Caducado: emitido con la clave y el issuer reales, pero hace 1 hora (dura 15 minutos).
        String expired = new JwtService(jwtProperties, Clock.offset(clock, Duration.ofHours(-1)))
                .issueAccessToken(andreaId).token();
        expectUnauthorized(as(expired, get("/api/auth/me")));
    }

    @Test
    @Order(10)
    @DisplayName("§23.10 / UC-SEC-10: Carlos desactivado con su token aún vigente → la siguiente petición es 401")
    void step10DeactivatedUserIsRejected() throws Exception {
        mockMvc.perform(as(tokenCarlos, get("/api/auth/me"))).andExpect(status().isOk());

        jdbcTemplate.update("UPDATE users SET active = false WHERE id = ?", carlosId);

        expectUnauthorized(as(tokenCarlos, get("/api/auth/me")));
        expectUnauthorized(as(tokenCarlos, get("/api/tickets/" + carlosTicket)));
    }

    // ---------------------------------------------------------------- auxiliares

    /** 403 resuelto en Spring MVC: lo lanzó {@code @PreAuthorize}, no una regla de URL (P4-08). */
    private void deniedByPreAuthorize(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andReturn();
        assertThat(result.getResolvedException()).isInstanceOf(AccessDeniedException.class);
    }

    /** 401 uniforme del entry point (§13), sin llegar a Spring MVC. */
    private void expectUnauthorized(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andReturn();
        assertThat(result.getResolvedException()).isNull();
    }

    private static MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    /** Cambia un carácter en mitad de la firma (misma técnica que JwtAuthenticationWebMvcTest). */
    private static String tamper(String token) {
        int lastDot = token.lastIndexOf('.');
        char[] chars = token.toCharArray();
        int middle = lastDot + (token.length() - lastDot) / 2;
        chars[middle] = chars[middle] == 'A' ? 'B' : 'A';
        return new String(chars);
    }

    private Long register(String username, String email, String firstName, String lastName, String birthDate)
            throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(username, email, firstName, lastName, birthDate)))
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
                        .content(loginJson(username)))
                .andExpect(status().isOk())
                .andReturn();
        return field(result, "accessToken");
    }

    private String field(MvcResult result, String name) throws Exception {
        return jsonMapper.readTree(result.getResponse().getContentAsString()).get(name).asString();
    }

    private static String registerJson(String username, String email, String firstName, String lastName,
                                       String birthDate) {
        return """
                {"username": "%s", "email": "%s", "firstName": "%s", "lastName": "%s",
                 "birthDate": "%s", "password": "%s"}""".formatted(username, email, firstName, lastName, birthDate, PASSWORD);
    }

    private static String loginJson(String username) {
        return """
                {"identifier": "%s", "password": "%s"}""".formatted(username, PASSWORD);
    }

    private static String purchaseJson(String email) {
        return """
                {"userEmail": "%s", "eventCode": "%s", "type": "GENERAL"}""".formatted(email, EVENT_CODE);
    }

    private String eventStatus() {
        return jdbcTemplate.queryForObject("SELECT status FROM events WHERE event_code = ?", String.class, EVENT_CODE);
    }

    private long ticketCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tickets", Long.class);
        return count == null ? 0 : count;
    }

    private String ticketStatus(String ticketCode) {
        return jdbcTemplate.queryForObject("SELECT status FROM tickets WHERE ticket_code = ?", String.class, ticketCode);
    }
}
