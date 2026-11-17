package com.pulsepass.security;

import com.pulsepass.exception.ErrorResponseFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Seguridad real para las pruebas {@code @WebMvcTest} (P4-04-i).
 * <p>
 * {@code @WebMvcTest} no carga las clases {@code @Configuration} ni los {@code @Component} de
 * seguridad, así que esta configuración los importa: la cadena de filtros, los manejadores 401 y
 * 403, la fábrica de {@code ErrorResponse} (que también necesita {@code GlobalExceptionHandler})
 * y el registro de {@link RequestIdFilter}. Los filtros se ejecutan de verdad: nunca
 * {@code addFilters = false} (§17.2). El {@code Clock} sigue llegando de
 * {@code FixedClockTestConfiguration}.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import({
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        ErrorResponseFactory.class,
        RequestIdFilterConfig.class
})
public class WebSecurityTestConfiguration {
}
