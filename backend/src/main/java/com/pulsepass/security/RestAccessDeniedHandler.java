package com.pulsepass.security;

import com.pulsepass.exception.ErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 403 cuando una identidad válida no tiene el rol que exige la ruta (PRD Seguridad §7, §13;
 * SEC-009).
 * <p>
 * Igual que {@link RestAuthenticationEntryPoint}, escribe el {@code ErrorResponse} común con
 * {@link ErrorResponseFactory} y un mensaje genérico (P4-D9). Registra la denegación con el
 * request ID del MDC y el id del usuario, sin la query string ni la cabecera
 * {@code Authorization} (NFR-SEC-006, P4-04-j).
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestAccessDeniedHandler.class);

    private final ErrorResponseFactory errorResponseFactory;

    public RestAccessDeniedHandler(ErrorResponseFactory errorResponseFactory) {
        this.errorResponseFactory = errorResponseFactory;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Access denied: {} {} userId={}", request.getMethod(), request.getRequestURI(), currentUserId());
        errorResponseFactory.write(response, HttpStatus.FORBIDDEN, ErrorResponseFactory.ACCESS_DENIED);
    }

    private static String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof SecurityPrincipal principal
                ? String.valueOf(principal.userId())
                : "unknown";
    }
}
