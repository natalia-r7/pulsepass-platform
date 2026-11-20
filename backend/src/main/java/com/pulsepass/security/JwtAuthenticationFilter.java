package com.pulsepass.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Autentica la petición con el JWT de la cabecera {@code Authorization: Bearer <token>}
 * (PRD Seguridad §7.1, §12.3, §14.3; FR-SEC-AUTH-006..009; P4-05).
 * <ol>
 *   <li>Si el contexto ya tiene una autenticación válida (autenticada y no anónima), no hace nada
 *       (P4-05-d, §17.1). Sin sesión, en producción el contexto siempre llega vacío.</li>
 *   <li>Sin cabecera {@code Authorization}, deja seguir la petición: la regla de URL decide si es
 *       pública (§14.3.5). El token en la query string nunca se lee (FR-SEC-AUTH-007).</li>
 *   <li>Con cabecera, exige exactamente una, con el esquema Bearer y un token sin espacios
 *       (P4-05-c); valida el token con {@link JwtService} y carga el usuario actual con
 *       {@link CustomUserDetailsService} (P4-05-e). Los roles salen de la base de datos.</li>
 *   <li>Cualquier fallo responde 401 a través de {@link RestAuthenticationEntryPoint}, con el mismo
 *       {@code ErrorResponse}, cabecera y log que el resto de 401, y corta la petición: un Bearer
 *       inválido nunca autentica, ni siquiera en una ruta pública (P4-D6, P4-05-g).</li>
 * </ol>
 * No es un bean: lo crea {@link SecurityConfig} y solo existe dentro de la cadena de seguridad, así
 * que se ejecuta una sola vez (P4-05-f). No escribe logs y nunca registra el token (§12.2).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_SCHEME = "Bearer";

    private final JwtService jwtService;

    private final CustomUserDetailsService userDetailsService;

    private final AuthenticationEntryPoint authenticationEntryPoint;

    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    private final SecurityContextHolderStrategy contextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService,
                                   AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isAlreadyAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }
        List<String> authorizationHeaders = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));
        if (authorizationHeaders.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String token = bearerToken(authorizationHeaders);
            Long userId = jwtService.extractUserId(token);
            SecurityPrincipal principal = userDetailsService.loadPrincipalById(userId);
            authenticate(principal);
        } catch (InvalidJwtException | AuthenticationException ex) {
            contextHolderStrategy.clearContext();
            authenticationEntryPoint.commence(request, response, asAuthenticationException(ex));
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** P4-05-d: solo cuenta una autenticación ya validada y no anónima; un objeto cualquiera no basta. */
    private boolean isAlreadyAuthenticated() {
        Authentication current = contextHolderStrategy.getContext().getAuthentication();
        return current != null && current.isAuthenticated() && !trustResolver.isAnonymous(current);
    }

    /**
     * P4-05-c: exactamente una cabecera, con el esquema Bearer (sin distinguir mayúsculas, RFC 6750),
     * un espacio y un token no vacío y sin espacios. Cualquier otra forma, incluido Basic, se rechaza.
     */
    private static String bearerToken(List<String> authorizationHeaders) {
        if (authorizationHeaders.size() != 1) {
            throw new BadCredentialsException("Invalid authorization header");
        }
        String value = authorizationHeaders.get(0);
        int separator = value.indexOf(' ');
        if (separator < 0 || !BEARER_SCHEME.equalsIgnoreCase(value.substring(0, separator))) {
            throw new BadCredentialsException("Invalid authorization header");
        }
        String token = value.substring(separator + 1);
        if (token.isEmpty() || token.chars().anyMatch(Character::isWhitespace)) {
            throw new BadCredentialsException("Invalid authorization header");
        }
        return token;
    }

    /** Contexto nuevo con la identidad actual; no se guarda en ninguna sesión (FR-SEC-AUTH-008). */
    private void authenticate(SecurityPrincipal principal) {
        SecurityContext context = contextHolderStrategy.createEmptyContext();
        context.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities()));
        contextHolderStrategy.setContext(context);
    }

    private static AuthenticationException asAuthenticationException(RuntimeException ex) {
        return ex instanceof AuthenticationException authenticationException
                ? authenticationException
                : new BadCredentialsException("Invalid access token", ex);
    }
}
