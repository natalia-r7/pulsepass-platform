package com.pulsepass.controller;

import com.pulsepass.FixedClockTestConfiguration;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.security.WebSecurityTestConfiguration;
import com.pulsepass.security.WithSecurityPrincipal;
import com.pulsepass.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de {@link AuthController} con la cadena de seguridad real (PRD Seguridad §10, §13, §17.2;
 * FR-SEC-AUTH-002, 003, 010; T-SEC-005, T-SEC-006, T-SEC-019; P4-07-A, B, C, G y H).
 * <p>
 * {@code AuthService} es un {@code @MockitoBean}: aquí se prueba el contrato HTTP; el login real con
 * PostgreSQL y JWT lo prueba {@code AuthLoginIT}. Los JSON van escritos como texto (P3-D12).
 */
@WebMvcTest(AuthController.class)
@Import({FixedClockTestConfiguration.class, WebSecurityTestConfiguration.class})
class AuthControllerTest {

    /** Login del ejemplo del PRD Seguridad §10.2. */
    private static final String LOGIN_JSON = """
            {"identifier": "andrea", "password": "UnaFraseSeguraLarga2026!"}""";

    private static final LoginRequest LOGIN = new LoginRequest("andrea", "UnaFraseSeguraLarga2026!");

    /** DATO DE PRUEBA: token ficticio; el formato real lo prueba AuthLoginIT. */
    private static final String TOKEN = "token.de.prueba";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    // ---------------------------------------------------------------- POST /api/auth/login

    @Test
    @DisplayName("T-SEC-005 / §10.2: login correcto como anónimo → 200 con accessToken, tokenType=Bearer y expiresIn=900")
    void loginReturns200WithBearerToken() throws Exception {
        // ARRANGE
        when(authService.login(eq(LOGIN))).thenReturn(AuthResponse.bearer(TOKEN, 900));

        // ACT + ASSERT
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$.accessToken").value(TOKEN))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));

        verify(authService).login(eq(LOGIN));
    }

    @Test
    @DisplayName("T-SEC-006 / T-SEC-019 / P4-D9: credenciales incorrectas → 401 genérico, sin stack trace ni secretos")
    void loginReturnsGeneric401WhenAuthenticationFails() throws Exception {
        // ARRANGE
        when(authService.login(eq(LOGIN))).thenThrow(new BadCredentialsException("Bad credentials"));

        // ACT
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON));

        // ASSERT
        expectError(result, HttpStatus.UNAUTHORIZED, "Invalid credentials")
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(content().string(not(containsString("UnaFraseSeguraLarga2026!"))))
                .andExpect(content().string(not(containsString("Bad credentials"))))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    @DisplayName("P4-07-A: identifier y password vacíos → 400 por campo y no se intenta autenticar")
    void loginReturns400WhenFieldsAreMissing() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content("{}"));

        expectError(result, HttpStatus.BAD_REQUEST, "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(2))
                .andExpect(jsonPath("$.details.identifier").value("Identifier is required"))
                .andExpect(jsonPath("$.details.password").value("Password is required"));

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("P4-07-A: contraseña de más de 72 bytes → 400 sin intentar autenticar")
    void loginReturns400WhenPasswordExceeds72Bytes() throws Exception {
        String json = """
                {"identifier": "andrea", "password": "%s"}""".formatted("p".repeat(73)); // DATO DE PRUEBA

        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(json));

        expectError(result, HttpStatus.BAD_REQUEST, "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details.password").value("Password must not exceed 72 bytes"));

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("P4-D6: un Bearer inválido en el login → 401 sin llegar al servicio")
    void loginWithInvalidBearerReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido") // DATO DE PRUEBA
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));

        verifyNoInteractions(authService);
    }

    // ---------------------------------------------------------------- GET /api/auth/me

    @Test
    @DisplayName("FR-SEC-AUTH-010 / P4-D15: /me autenticado → 200 con id, username, email y roles, sin secretos")
    @WithSecurityPrincipal(userId = 1L, username = "andrea", roles = Role.USER)
    void meReturnsAuthenticatedUser() throws Exception {
        // ARRANGE
        SecurityPrincipal principal = new SecurityPrincipal(1L, "andrea", Set.of(Role.USER));
        when(authService.currentUser(eq(principal))).thenReturn(
                new AuthenticatedUserResponse(1L, "andrea", "andrea@email.com", List.of("USER")));

        // ACT + ASSERT
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@email.com"))
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("USER"));

        // El principal llega tal cual desde el SecurityContext.
        verify(authService).currentUser(eq(principal));
    }

    @Test
    @DisplayName("FR-SEC-AUTH-010 / P4-07-H: /me sin autenticación → 401 sin llegar al servicio")
    void meRequiresAuthentication() throws Exception {
        expectError(mockMvc.perform(get("/api/auth/me")), HttpStatus.UNAUTHORIZED, "Authentication is required")
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));

        verifyNoInteractions(authService);
    }

    // ---------------------------------------------------------------- Auxiliares

    /** Contrato común de {@code ErrorResponse} (§13): estado, JSON, 5 claves y reloj fijo. */
    private static ResultActions expectError(ResultActions result, HttpStatus status, String message)
            throws Exception {
        return result
                .andExpect(status().is(status.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.timestamp").value(FixedClockTestConfiguration.EXPECTED_TIMESTAMP))
                .andExpect(jsonPath("$.status").value(status.value()))
                .andExpect(jsonPath("$.error").value(status.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(message));
    }
}
