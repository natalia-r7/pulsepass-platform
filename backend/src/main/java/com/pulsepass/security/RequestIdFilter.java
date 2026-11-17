package com.pulsepass.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Asigna un request ID a cada petición y lo guarda en el MDC del log (NFR-SEC-006, P4-04-j).
 * <p>
 * No es un {@code @Component}: se registra una sola vez con el {@code FilterRegistrationBean} de
 * {@link RequestIdFilterConfig}, con la máxima prioridad, y no forma parte de
 * {@code SecurityFilterChain}. Al ejecutarse antes que la cadena de seguridad, el ID está en el
 * MDC cuando escriben en el log los manejadores 401 y 403. No se añade a la respuesta.
 */
public class RequestIdFilter extends OncePerRequestFilter {

    /** Clave del MDC; {@code logging.pattern.correlation} la muestra en cada línea de log. */
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        MDC.put(MDC_KEY, UUID.randomUUID().toString());
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Siempre se limpia: el hilo del servidor atenderá después otras peticiones.
            MDC.remove(MDC_KEY);
        }
    }
}
