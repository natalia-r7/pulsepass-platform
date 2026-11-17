package com.pulsepass.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Set;

/**
 * Crea el contexto de seguridad de {@link WithSecurityPrincipal}: una autenticación ya validada
 * cuyo principal es un {@link SecurityPrincipal}, sin credenciales (P4-04-i).
 */
public class WithSecurityPrincipalSecurityContextFactory implements WithSecurityContextFactory<WithSecurityPrincipal> {

    @Override
    public SecurityContext createSecurityContext(WithSecurityPrincipal annotation) {
        SecurityPrincipal principal = new SecurityPrincipal(
                annotation.userId(), annotation.username(), Set.of(annotation.roles()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities()));
        return context;
    }
}
