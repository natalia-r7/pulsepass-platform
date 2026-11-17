package com.pulsepass.service.impl;

import com.pulsepass.service.AuthorizationService;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Implementación de {@link AuthorizationService} sobre el contexto de Spring Security
 * (PRD Seguridad §7.1; P4-04b).
 * <p>
 * Solo mira las authorities de la autenticación actual, no el tipo de principal, así que funciona
 * igual con la identidad que construya el filtro JWT (P4-05). Una autenticación ausente, no
 * autenticada o anónima nunca cuenta como STAFF ni ADMIN (P4-D19).
 */
@Service
public class AuthorizationServiceImpl implements AuthorizationService {

    /** Authorities que pueden ver eventos en cualquier estado (P4-D2). */
    private static final Set<String> STAFF_OR_ADMIN = Set.of("ROLE_STAFF", "ROLE_ADMIN");

    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    @Override
    public boolean isStaffOrAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || trustResolver.isAnonymous(authentication)) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(STAFF_OR_ADMIN::contains);
    }
}
