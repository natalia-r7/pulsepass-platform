package com.pulsepass.security;

import com.pulsepass.FixedClockTestConfiguration;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.dto.response.ErrorResponse;
import com.pulsepass.exception.ErrorResponseFactory;
import com.pulsepass.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Errores de seguridad que llegan a Spring MVC (PRD Seguridad §13; P4-D19, P4-04-g).
 * <p>
 * Un controlador de prueba lanza las excepciones que, más adelante, lanzarán la seguridad a nivel
 * de método (P4-08) y el login (P4-07). Sin los manejadores nuevos de
 * {@link GlobalExceptionHandler} acabarían en el 500 genérico. El caso anónimo se prueba llamando
 * directamente al manejador, porque la cadena de seguridad no deja llegar a un anónimo hasta un
 * controlador protegido.
 */
@WebMvcTest(controllers = SecurityExceptionHandlingTest.SecurityProbeController.class)
@Import({SecurityExceptionHandlingTest.SecurityProbeController.class, FixedClockTestConfiguration.class,
        WebSecurityTestConfiguration.class})
@ExtendWith(OutputCaptureExtension.class)
class SecurityExceptionHandlingTest {

    /** DATO DE PRUEBA: valores que nunca deben aparecer en el log. */
    private static final String PRIVATE_EMAIL = "privado@pulsepass.test";
    private static final String AUTHORIZATION_VALUE = "Bearer token-de-prueba";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Clock clock;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithSecurityPrincipal(roles = Role.USER)
    @DisplayName("P4-D19 / P4-04-j: una AccessDeniedException con una identidad autenticada responde 403 y se registra en WARN con método, ruta y userId")
    void accessDeniedForAuthenticatedUserIs403(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/probe-security/access-denied")
                        .queryParam("email", PRIVATE_EMAIL)
                        .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_VALUE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.timestamp").value(FixedClockTestConfiguration.EXPECTED_TIMESTAMP))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.details").value(anEmptyMap()));

        assertThat(output.getOut())
                .containsPattern(" WARN .*Access denied: GET /probe-security/access-denied userId=1");
        assertNoSensitiveData(output);
    }

    @Test
    @DisplayName("P4-D19 / P4-04-j: una AccessDeniedException con una identidad anónima responde 401 y se registra en INFO")
    void accessDeniedForAnonymousIs401(CapturedOutput output) {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "clave-de-prueba", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        ResponseEntity<ErrorResponse> response =
                handler().handleAccessDenied(new AccessDeniedException("denied"), probeRequest());

        assertUnauthorized(response, "Authentication is required");
        assertThat(output.getOut())
                .containsPattern(" INFO .*Authentication required: GET /probe-security/access-denied");
        assertNoSensitiveData(output);
    }

    @Test
    @DisplayName("P4-D19 / P4-04-j: una AccessDeniedException sin ninguna identidad responde 401 y se registra en INFO")
    void accessDeniedWithoutAuthenticationIs401(CapturedOutput output) {
        SecurityContextHolder.clearContext();

        ResponseEntity<ErrorResponse> response =
                handler().handleAccessDenied(new AccessDeniedException("denied"), probeRequest());

        assertUnauthorized(response, "Authentication is required");
        assertThat(output.getOut())
                .containsPattern(" INFO .*Authentication required: GET /probe-security/access-denied");
        assertNoSensitiveData(output);
    }

    @Test
    @WithSecurityPrincipal(roles = Role.USER)
    @DisplayName("FR-SEC-AUTH-003 / P4-04-j: una AuthenticationException dentro de MVC responde 401 «Invalid credentials» y se registra en INFO")
    void authenticationFailureIs401(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/probe-security/bad-credentials")
                        .queryParam("email", PRIVATE_EMAIL)
                        .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_VALUE))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andExpect(jsonPath("$.details").value(anEmptyMap()));

        assertThat(output.getOut())
                .containsPattern(" INFO .*Authentication required: GET /probe-security/bad-credentials");
        assertNoSensitiveData(output);
    }

    @Test
    @DisplayName("§13: si la respuesta ya se envió, el punto de entrada 401 no la modifica (ni cabecera ni cuerpo)")
    void entryPointDoesNotTouchCommittedResponse() throws Exception {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(
                new ErrorResponseFactory(clock, JsonMapper.builder().build()));
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCommitted(true);

        entryPoint.commence(probeRequest(), response, new InsufficientAuthenticationException("probe"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    @DisplayName("P4-04-c: Spring Boot no crea el usuario en memoria (ningún UserDetailsService)")
    void noInMemoryUserIsCreated() {
        assertThat(context.getBeansOfType(UserDetailsService.class)).isEmpty();
    }

    private GlobalExceptionHandler handler() {
        return new GlobalExceptionHandler(new ErrorResponseFactory(clock, JsonMapper.builder().build()));
    }

    /** Petición con query string y cabecera Authorization, que no deben llegar al log. */
    private static MockHttpServletRequest probeRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/probe-security/access-denied");
        request.setQueryString("email=" + PRIVATE_EMAIL);
        request.addParameter("email", PRIVATE_EMAIL);
        request.addHeader(HttpHeaders.AUTHORIZATION, AUTHORIZATION_VALUE);
        return request;
    }

    /** NFR-SEC-001 / NFR-SEC-006: el log no contiene la query string ni la cabecera Authorization. */
    private static void assertNoSensitiveData(CapturedOutput output) {
        assertThat(output.getOut())
                .doesNotContain(PRIVATE_EMAIL)
                .doesNotContain(AUTHORIZATION_VALUE);
    }

    private static void assertUnauthorized(ResponseEntity<ErrorResponse> response, String message) {
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().error()).isEqualTo("Unauthorized");
        assertThat(response.getBody().message()).isEqualTo(message);
        assertThat(response.getBody().details()).isEmpty();
    }

    /** Controlador que solo existe en esta prueba y lanza las excepciones de seguridad. */
    @RestController
    @RequestMapping("/probe-security")
    static class SecurityProbeController {

        @GetMapping("/access-denied")
        String accessDenied() {
            throw new AccessDeniedException("probe");
        }

        @GetMapping("/bad-credentials")
        String badCredentials() {
            throw new BadCredentialsException("probe");
        }
    }
}
