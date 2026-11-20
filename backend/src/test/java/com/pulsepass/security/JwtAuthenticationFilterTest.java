package com.pulsepass.security;

import com.pulsepass.config.ClockConfig;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.exception.ErrorResponseFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link JwtAuthenticationFilter} (PRD Seguridad §14.3, §17.1; FR-SEC-AUTH-006..009;
 * P4-05). Sin Spring: {@link JwtService} real con una clave de prueba y un {@link Clock} fijo,
 * {@link CustomUserDetailsService} simulado y el {@link RestAuthenticationEntryPoint} real, para
 * comprobar el 401 completo. La cadena es un {@link MockFilterChain}: si recibe la petición, el
 * filtro la dejó seguir.
 */
@ExtendWith(OutputCaptureExtension.class)
class JwtAuthenticationFilterTest {

    /** DATO DE PRUEBA: clave ficticia y «ahora» fijo. */
    private static final String KEY = "jwt-filter-unit-test-key-32-byte";
    private static final Instant NOW = Instant.parse("2026-11-15T17:00:00Z");
    private static final Long USER_ID = 7L;

    private CustomUserDetailsService userDetailsService;
    private JwtService jwtService;
    private JwtAuthenticationFilter filter;
    private MockFilterChain chain;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userDetailsService = mock(CustomUserDetailsService.class);
        jwtService = jwtService(NOW);
        filter = filterAt(NOW);
        chain = new MockFilterChain();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("§14.3.5: sin cabecera Authorization la petición continúa y el contexto queda vacío")
    void requestWithoutHeaderContinuesUnauthenticated() throws Exception {
        filter.doFilter(new MockHttpServletRequest("GET", "/api/tickets/TKT-1"), response, chain);

        assertContinued();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadPrincipalById(anyLong());
    }

    @Test
    @DisplayName("FR-SEC-AUTH-009: un Bearer válido autentica con el principal cargado desde la base de datos")
    void validBearerAuthenticatesWithPersistedPrincipal() throws Exception {
        SecurityPrincipal principal = principal(Role.USER);
        when(userDetailsService.loadPrincipalById(USER_ID)).thenReturn(principal);

        filter.doFilter(bearerRequest(validToken()), response, chain);

        assertContinued();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isEqualTo(principal);
        assertThat(authentication.getCredentials()).isNull();
    }

    @Test
    @DisplayName("§12.1 / SEC-011: los roles salen de la base de datos, no del token (el token no lleva roles)")
    void rolesComeFromDatabase() throws Exception {
        when(userDetailsService.loadPrincipalById(USER_ID)).thenReturn(principal(Role.STAFF, Role.ADMIN));

        filter.doFilter(bearerRequest(validToken()), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN", "ROLE_STAFF");
    }

    @Test
    @DisplayName("T-SEC-008: un token manipulado responde 401 y la petición no continúa")
    void tamperedTokenIsRejected() throws Exception {
        filter.doFilter(bearerRequest(tamper(validToken())), response, chain);

        assertUnauthorizedAndStopped();
        verify(userDetailsService, never()).loadPrincipalById(anyLong());
    }

    @Test
    @DisplayName("T-SEC-007: un token caducado responde 401 y la petición no continúa")
    void expiredTokenIsRejected() throws Exception {
        String token = validToken();
        JwtAuthenticationFilter laterFilter = filterAt(NOW.plus(Duration.ofMinutes(16)));

        laterFilter.doFilter(bearerRequest(token), response, chain);

        assertUnauthorizedAndStopped();
    }

    @ParameterizedTest(name = "Authorization «{0}»")
    @ValueSource(strings = {"Bearer", "Bearer ", "Bearer a b", "Basic dXN1YXJpbzpjbGF2ZQ==", "Token abc"})
    @DisplayName("P4-05-c: una cabecera Authorization que no es un Bearer bien formado responde 401")
    void malformedAuthorizationHeaderIsRejected(String header) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tickets/TKT-1");
        request.addHeader(HttpHeaders.AUTHORIZATION, header);

        filter.doFilter(request, response, chain);

        assertUnauthorizedAndStopped();
    }

    @Test
    @DisplayName("§17.1 / P4-05-c: dos cabeceras Authorization responden 401")
    void duplicateAuthorizationHeadersAreRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tickets/TKT-1");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + validToken());
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + validToken());

        filter.doFilter(request, response, chain);

        assertUnauthorizedAndStopped();
    }

    @Test
    @DisplayName("FR-SEC-AUTH-007: un token en la query string se ignora; la petición sigue sin autenticar")
    void tokenInQueryStringIsIgnored() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tickets/TKT-1");
        request.setParameter("access_token", validToken());

        filter.doFilter(request, response, chain);

        assertContinued();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadPrincipalById(anyLong());
    }

    @Test
    @DisplayName("P4-05-e: si el usuario del token ya no existe, responde 401")
    void unknownUserIsRejected() throws Exception {
        when(userDetailsService.loadPrincipalById(USER_ID)).thenThrow(new UsernameNotFoundException("prueba"));

        filter.doFilter(bearerRequest(validToken()), response, chain);

        assertUnauthorizedAndStopped();
    }

    @Test
    @DisplayName("T-SEC-016 / SEC-011: un usuario desactivado con un token todavía vigente responde 401")
    void inactiveUserIsRejected() throws Exception {
        when(userDetailsService.loadPrincipalById(USER_ID)).thenThrow(new DisabledException("prueba"));

        filter.doFilter(bearerRequest(validToken()), response, chain);

        assertUnauthorizedAndStopped();
    }

    @Test
    @DisplayName("§17.1 / P4-05-d: una autenticación ya válida (autenticada y no anónima) no se sobrescribe")
    void existingValidAuthenticationIsNotOverwritten() throws Exception {
        SecurityPrincipal existing = new SecurityPrincipal(99L, "ya.autenticado", Set.of(Role.ADMIN));   // DATO DE PRUEBA
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(existing, null, existing.authorities()));

        filter.doFilter(bearerRequest("token-cualquiera"), response, chain);

        assertContinued();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(existing);
        verify(userDetailsService, never()).loadPrincipalById(anyLong());
    }

    @Test
    @DisplayName("P4-05-d (condición 1): una autenticación anónima no cuenta como válida; el Bearer se procesa")
    void anonymousAuthenticationDoesNotSkipTheFilter() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "clave-de-prueba", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        SecurityPrincipal principal = principal(Role.USER);
        when(userDetailsService.loadPrincipalById(USER_ID)).thenReturn(principal);

        filter.doFilter(bearerRequest(validToken()), response, chain);

        assertContinued();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(principal);
    }

    @Test
    @DisplayName("P4-05-d (condición 1): una autenticación no validada no cuenta; un Bearer inválido responde 401")
    void unauthenticatedAuthenticationDoesNotSkipTheFilter() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("sin.validar", null));

        filter.doFilter(bearerRequest("token-invalido"), response, chain);

        assertUnauthorizedAndStopped();
    }

    @Test
    @DisplayName("P4-05-c / RFC 6750: el esquema Bearer no distingue mayúsculas")
    void lowercaseBearerSchemeIsAccepted() throws Exception {
        when(userDetailsService.loadPrincipalById(USER_ID)).thenReturn(principal(Role.USER));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tickets/TKT-1");
        request.addHeader(HttpHeaders.AUTHORIZATION, "bearer " + validToken());

        filter.doFilter(request, response, chain);

        assertContinued();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    @DisplayName("§12.2 / NFR-SEC-001: el token nunca aparece en el log, ni válido ni rechazado")
    void tokenIsNeverLogged(CapturedOutput output) throws Exception {
        String valid = validToken();
        String tampered = tamper(validToken());
        when(userDetailsService.loadPrincipalById(USER_ID)).thenReturn(principal(Role.USER));

        filter.doFilter(bearerRequest(valid), new MockHttpServletResponse(), new MockFilterChain());
        SecurityContextHolder.clearContext();
        filter.doFilter(bearerRequest(tampered), response, chain);

        assertThat(output.getOut()).contains("Authentication required: GET /api/tickets/TKT-1");
        assertThat(output.getOut()).doesNotContain(valid).doesNotContain(tampered);
    }

    // ------------------------------------------------------------------ ayudantes

    private JwtService jwtService(Instant now) {
        JwtProperties properties = new JwtProperties("pulsepass-api",
                Base64.getEncoder().encodeToString(KEY.getBytes(StandardCharsets.UTF_8)), Duration.ofMinutes(15));
        return new JwtService(properties, Clock.fixed(now, ClockConfig.APPLICATION_ZONE));
    }

    private JwtAuthenticationFilter filterAt(Instant now) {
        Clock clock = Clock.fixed(now, ClockConfig.APPLICATION_ZONE);
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(
                new ErrorResponseFactory(clock, JsonMapper.builder().build()));
        return new JwtAuthenticationFilter(jwtService(now), userDetailsService, entryPoint);
    }

    private String validToken() {
        return jwtService.issueAccessToken(USER_ID).token();
    }

    private static MockHttpServletRequest bearerRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tickets/TKT-1");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }

    /** Cambia un carácter en mitad de la firma. */
    private static String tamper(String token) {
        int lastDot = token.lastIndexOf('.');
        char[] chars = token.toCharArray();
        int middle = lastDot + (token.length() - lastDot) / 2;
        chars[middle] = chars[middle] == 'A' ? 'B' : 'A';
        return new String(chars);
    }

    /** DATO DE PRUEBA: identidad del usuario {@link #USER_ID} con los roles indicados. */
    private static SecurityPrincipal principal(Role... roles) {
        return new SecurityPrincipal(USER_ID, "usuario.prueba", Set.of(roles));
    }

    private void assertContinued() {
        assertThat(chain.getRequest()).as("la petición debe continuar por la cadena").isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private void assertUnauthorizedAndStopped() throws Exception {
        assertThat(chain.getRequest()).as("la petición no debe continuar").isNull();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(response.getContentAsString()).contains("\"message\":\"Authentication is required\"");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
