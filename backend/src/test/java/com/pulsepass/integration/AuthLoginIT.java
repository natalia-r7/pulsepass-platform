package com.pulsepass.integration;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración del login y de {@code GET /api/auth/me} sobre PostgreSQL real y JWT real (PRD Seguridad
 * §10, §15 UC-SEC-02, §17.3; FR-SEC-AUTH-002..004, FR-SEC-AUTH-010; PRD E2E TC-AUTH-002..006 y 012;
 * P4-D9, P4-D11, P4-D13, P4-D15; P4-07).
 *
 * <p>Cada prueba registra sus usuarios con {@code POST /api/users} (el flujo real de P4-06), inicia
 * sesión con {@code POST /api/auth/login} y usa el token emitido. Las transacciones se confirman de
 * verdad: {@link #deleteCreatedRows()} borra los ids creados con {@code JdbcTemplate}, en orden de
 * claves foráneas (P4-06-M). Contraseñas y datos ficticios (§17.3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthLoginIT {

    /** Contraseña ficticia del ejemplo del PRD Seguridad §10.2. */
    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JsonMapper jsonMapper;

    /** Ids de usuario creados por la prueba en curso, para borrarlos al terminar. */
    private final List<Long> createdUserIds = new ArrayList<>();

    @AfterEach
    void deleteCreatedRows() {
        for (Long id : createdUserIds) {
            jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM user_credentials WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM user_profiles WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
        }
        createdUserIds.clear();
    }

    @Test
    @DisplayName("TC-AUTH-002 / UC-SEC-02: login por username → 200; JWT con sub = id del usuario y 900 s de vida")
    void loginByUsernameIssuesTokenForTheUser() throws Exception {
        // ARRANGE
        Long userId = register("login.andrea", "login.andrea@pulsepass.test");

        // ACT
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("login.andrea", PASSWORD)))
                // ASSERT: contrato de §10.2.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn();
        String token = jsonMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asString();

        // ASSERT: el servidor valida el token y su sub es el id persistente (UC-SEC-02).
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);

        // ASSERT: claims obligatorios y caducidad de 15 minutos (lectura no autoritativa del payload).
        JsonNode claims = payloadOf(token);
        assertThat(claims.has("iss") && claims.has("jti")).isTrue();
        assertThat(claims.get("exp").asLong() - claims.get("iat").asLong()).isEqualTo(900);
    }

    @Test
    @DisplayName("TC-AUTH-003 / P4-D13: login por email con otras mayúsculas → 200 y el mismo usuario")
    void loginByEmailIgnoringCaseIdentifiesTheSameUser() throws Exception {
        Long userId = register("login.email", "login.email@pulsepass.test");

        String token = login("LOGIN.Email@PulsePass.TEST", PASSWORD);

        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("TC-AUTH-004 / TC-AUTH-005 / P4-D9: contraseña incorrecta e identificador inexistente → el mismo 401 genérico")
    void wrongPasswordAndUnknownIdentifierReturnTheSame401() throws Exception {
        // ARRANGE
        register("login.carlos", "login.carlos@pulsepass.test");

        // ACT
        JsonNode wrongPassword = failedLogin("login.carlos", "OtraClaveIncorrecta2026!"); // DATO DE PRUEBA
        JsonNode unknownUser = failedLogin("nadie.registrado", PASSWORD);                 // DATO DE PRUEBA

        // ASSERT: mismo cuerpo salvo el timestamp; no revela si el usuario existe.
        assertThat(wrongPassword.get("message").asString()).isEqualTo("Invalid credentials");
        ((ObjectNode) wrongPassword).remove("timestamp");
        ((ObjectNode) unknownUser).remove("timestamp");
        assertThat(wrongPassword).isEqualTo(unknownUser);
    }

    @Test
    @DisplayName("FR-SEC-AUTH-004: un usuario inactivo con la contraseña correcta → 401 genérico")
    void inactiveUserCannotLogIn() throws Exception {
        Long userId = register("login.inactivo", "login.inactivo@pulsepass.test");
        jdbcTemplate.update("UPDATE users SET active = false WHERE id = ?", userId);

        assertThat(failedLogin("login.inactivo", PASSWORD).get("message").asString()).isEqualTo("Invalid credentials");
    }

    @Test
    @DisplayName("P4-D11: un usuario sin credencial (anterior a la Fase 4) → 401 genérico")
    void userWithoutCredentialCannotLogIn() throws Exception {
        Long userId = register("login.legado", "login.legado@pulsepass.test");
        jdbcTemplate.update("DELETE FROM user_credentials WHERE user_id = ?", userId);

        assertThat(failedLogin("login.legado", PASSWORD).get("message").asString()).isEqualTo("Invalid credentials");
    }

    @Test
    @DisplayName("TC-AUTH-006 / FR-SEC-AUTH-010 / P4-D15: /me con el token real → 200 con la identidad y los roles actuales")
    void meReturnsIdentityAndCurrentRoles() throws Exception {
        // ARRANGE
        Long userId = register("login.me", "login.me@pulsepass.test");
        String token = login("login.me", PASSWORD);

        // ACT + ASSERT: datos persistidos y solo USER, sin secretos.
        MvcResult result = mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value("login.me"))
                .andExpect(jsonPath("$.email").value("login.me@pulsepass.test"))
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(PASSWORD).doesNotContain("$2a$");

        // ACT + ASSERT: el mismo token refleja un rol añadido después (los roles no van en el JWT).
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role) VALUES (?, 'ADMIN')", userId); // DATO DE PRUEBA
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.length()").value(2))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.roles[1]").value("USER"));
    }

    @Test
    @DisplayName("TC-AUTH-012 / FR-SEC-AUTH-007: el token en la query string no autentica → 401")
    void tokenInQueryStringIsRejected() throws Exception {
        register("login.query", "login.query@pulsepass.test");
        String token = login("login.query", PASSWORD);

        mockMvc.perform(get("/api/auth/me").param("access_token", token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    // ---------------------------------------------------------------- Auxiliares

    /** Registra un usuario con {@code POST /api/users} y anota su id para borrarlo al terminar. */
    private Long register(String username, String email) throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "email": "%s", "firstName": "Andrea", "lastName": "Login",
                                 "birthDate": "2001-06-15", "password": "%s"}""".formatted(username, email, PASSWORD)))
                .andExpect(status().isCreated());
        Long id = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
        createdUserIds.add(id);
        return id;
    }

    /** Login correcto; devuelve el token. */
    private String login(String identifier, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(identifier, password)))
                .andExpect(status().isOk())
                .andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asString();
    }

    /** Login fallido: exige 401 y devuelve el cuerpo. */
    private JsonNode failedLogin(String identifier, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(identifier, password)))
                .andExpect(status().isUnauthorized())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(password).doesNotContain(identifier);
        return jsonMapper.readTree(body);
    }

    private static String loginJson(String identifier, String password) {
        return """
                {"identifier": "%s", "password": "%s"}""".formatted(identifier, password);
    }

    /** Payload del JWT, decodificado sin validar: la validación real la hace {@code JwtService}. */
    private JsonNode payloadOf(String token) {
        String payload = token.split("\\.")[1];
        return jsonMapper.readTree(new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8));
    }
}
