package com.pulsepass.security;

import com.pulsepass.exception.ErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 401 cuando una ruta protegida se pide sin identidad válida (PRD Seguridad §7, §13; SEC-009).
 * <p>
 * Lo invoca la cadena de seguridad, antes de {@code DispatcherServlet}, así que
 * {@code GlobalExceptionHandler} no lo ve: escribe el mismo {@code ErrorResponse} con
 * {@link ErrorResponseFactory} (P4-04-d). El mensaje es siempre el mismo y no indica el motivo
 * (P4-D9). Añade {@code WWW-Authenticate: Bearer} (RFC 6750, P4-04-f).
 * <p>
 * Registra el intento en el log con el request ID del MDC, sin la query string ni la cabecera
 * {@code Authorization} (NFR-SEC-006, P4-04-j).
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);

    private final ErrorResponseFactory errorResponseFactory;

    public RestAuthenticationEntryPoint(ErrorResponseFactory errorResponseFactory) {
        this.errorResponseFactory = errorResponseFactory;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.info("Authentication required: {} {}", request.getMethod(), request.getRequestURI());
        if (response.isCommitted()) {
            // La respuesta ya se envió: no se modifica (ni cabecera ni cuerpo), para emitirla una sola vez (§13).
            return;
        }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        errorResponseFactory.write(response, HttpStatus.UNAUTHORIZED, ErrorResponseFactory.AUTHENTICATION_REQUIRED);
    }
}
