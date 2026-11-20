package com.pulsepass.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Seguridad por método (PRD Seguridad §11, §11.1, §14.3; P4-D5, P4-D21; P4-08-A).
 * <p>
 * Activa {@code @PreAuthorize} para las comprobaciones de propiedad declaradas en las interfaces
 * {@code UserService} y {@code TicketService}, que consultan al bean {@code authz}
 * ({@code AuthorizationService}). Una denegación lanza {@code AccessDeniedException}, que
 * {@code GlobalExceptionHandler} convierte en 403 (P4-D19).
 * <p>
 * Va en una clase propia, no en {@link SecurityConfig}: el escaneo de componentes la carga en la
 * aplicación y en {@code @SpringBootTest}, pero {@code @WebMvcTest} no, de modo que los servicios
 * simulados de las pruebas web no heredan las reglas de propiedad (riesgo de P4-D21). La
 * propiedad se prueba en integración ({@code OwnershipSecurityIT}).
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
