package com.pulsepass.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias de {@link AuthorizationServiceImpl} (PRD Seguridad §7.1; P4-04b, P4-D2).
 * Sin Spring: cada prueba rellena el {@link SecurityContextHolder} y se vacía al terminar.
 */
class AuthorizationServiceImplTest {

    private final AuthorizationServiceImpl authorizationService = new AuthorizationServiceImpl();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("P4-D2: sin ninguna autenticación no es STAFF ni ADMIN")
    void withoutAuthenticationIsNotStaffOrAdmin() {
        SecurityContextHolder.clearContext();

        assertThat(authorizationService.isStaffOrAdmin()).isFalse();
    }

    @Test
    @DisplayName("P4-D2 / P4-D19: un visitante anónimo no es STAFF ni ADMIN")
    void anonymousIsNotStaffOrAdmin() {
        authenticate(new AnonymousAuthenticationToken(
                "clave-de-prueba", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(authorizationService.isStaffOrAdmin()).isFalse();
    }

    @Test
    @DisplayName("P4-D2: un USER no es STAFF ni ADMIN")
    void userIsNotStaffOrAdmin() {
        authenticate(authenticated("ROLE_USER"));

        assertThat(authorizationService.isStaffOrAdmin()).isFalse();
    }

    @Test
    @DisplayName("P4-D2: un STAFF autenticado es STAFF o ADMIN")
    void staffIsStaffOrAdmin() {
        authenticate(authenticated("ROLE_STAFF"));

        assertThat(authorizationService.isStaffOrAdmin()).isTrue();
    }

    @Test
    @DisplayName("P4-D2: un ADMIN autenticado es STAFF o ADMIN")
    void adminIsStaffOrAdmin() {
        authenticate(authenticated("ROLE_ADMIN"));

        assertThat(authorizationService.isStaffOrAdmin()).isTrue();
    }

    @Test
    @DisplayName("P4-D8: una identidad con USER y ADMIN cuenta como ADMIN")
    void userWithAdminRoleIsStaffOrAdmin() {
        authenticate(authenticated("ROLE_USER", "ROLE_ADMIN"));

        assertThat(authorizationService.isStaffOrAdmin()).isTrue();
    }

    @Test
    @DisplayName("P4-D2: una autenticación no validada nunca cuenta como ADMIN, aunque diga tener el rol")
    void unauthenticatedTokenIsNotStaffOrAdmin() {
        Authentication unauthenticated = new UsernamePasswordAuthenticationToken("sin.validar", null);
        assertThat(unauthenticated.isAuthenticated()).isFalse();
        authenticate(unauthenticated);

        assertThat(authorizationService.isStaffOrAdmin()).isFalse();
    }

    // ------------------------------------------------------------------ ayudantes

    /** DATO DE PRUEBA: autenticación ya validada con las authorities indicadas. */
    private static Authentication authenticated(String... authorities) {
        return UsernamePasswordAuthenticationToken.authenticated(
                "usuario.prueba", null, AuthorityUtils.createAuthorityList(authorities));
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
