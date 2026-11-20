package com.pulsepass.security;

import com.pulsepass.exception.ErrorResponseFactory;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Seguridad real para las pruebas {@code @WebMvcTest} (P4-04-i, P4-05-h).
 * <p>
 * {@code @WebMvcTest} no carga las clases {@code @Configuration} ni los {@code @Component} de
 * seguridad, así que esta configuración los importa: la cadena de filtros (con el filtro JWT), los
 * manejadores 401 y 403, la fábrica de {@code ErrorResponse} (que también necesita
 * {@code GlobalExceptionHandler}), el registro de {@link RequestIdFilter} y {@link JwtService} con
 * su configuración (la clave de pruebas llega de {@code config/application.yml}). Los filtros se
 * ejecutan de verdad: nunca {@code addFilters = false} (§17.2). El {@code Clock} sigue llegando de
 * {@code FixedClockTestConfiguration}.
 * <p>
 * {@link CustomUserDetailsService} necesita los repositorios, que {@code @WebMvcTest} no carga: aquí
 * es un mock. Las pruebas que envían tokens reales lo sustituyen con {@code @MockitoBean} para
 * decidir qué usuario devuelve.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import({
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        ErrorResponseFactory.class,
        RequestIdFilterConfig.class,
        JwtConfig.class,
        JwtService.class
})
public class WebSecurityTestConfiguration {

    @Bean
    CustomUserDetailsService customUserDetailsService() {
        return Mockito.mock(CustomUserDetailsService.class);
    }
}
