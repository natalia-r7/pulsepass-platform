package com.pulsepass.security;

import com.pulsepass.domain.enums.Role;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ejecuta una prueba con un {@link SecurityPrincipal} autenticado (P4-04-i).
 * <p>
 * Se usa en lugar de {@code @WithMockUser} porque la autorización de PulsePass necesita el
 * {@code userId} persistente del usuario, no solo su nombre. Puede ponerse en la clase (identidad
 * por defecto de todas sus pruebas) o en un método (la sustituye para esa prueba).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@WithSecurityContext(factory = WithSecurityPrincipalSecurityContextFactory.class)
public @interface WithSecurityPrincipal {

    /** DATO DE PRUEBA: id persistente del usuario autenticado. */
    long userId() default 1L;

    /** DATO DE PRUEBA: username del usuario autenticado. */
    String username() default "test.user";

    /** Roles del usuario autenticado, sin el prefijo {@code ROLE_}. */
    Role[] roles() default {Role.USER};
}
