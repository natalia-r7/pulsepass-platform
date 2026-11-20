package com.pulsepass.security;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registro único de {@link RequestIdFilter} (P4-04-j).
 * <p>
 * El filtro solo se registra aquí: no es un bean de tipo {@code Filter} (que Spring Boot
 * registraría por su cuenta) ni se añade a {@code SecurityFilterChain}, así que existe un único
 * registro. Con {@link Ordered#HIGHEST_PRECEDENCE} se ejecuta antes que la cadena de Spring
 * Security (orden -100), y el MDC ya tiene el request ID cuando se producen los 401 y 403.
 */
@Configuration
public class RequestIdFilterConfig {

    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistration() {
        FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>(new RequestIdFilter());
        registration.setName("requestIdFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        return registration;
    }
}
