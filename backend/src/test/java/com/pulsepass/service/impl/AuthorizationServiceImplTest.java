package com.pulsepass.service.impl;

import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.SecurityPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link AuthorizationServiceImpl} (PRD Seguridad §7.1, §11.1; P4-04b, P4-D1,
 * P4-D2; P4-08-B y C). Sin Spring: cada prueba rellena el {@link SecurityContextHolder} y se vacía
 * al terminar; los repositorios son simulados. Desde P4-08 el constructor recibe los repositorios
 * (solo cambia la preparación de las pruebas de {@code isStaffOrAdmin}).
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketRepository ticketRepository;

    private AuthorizationServiceImpl authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationServiceImpl(userRepository, ticketRepository);
    }

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

    // ------------------------------------------------------------------ propiedad (P4-08)

    /** DATO DE PRUEBA: ids persistentes de dos usuarios. */
    private static final long OWNER_ID = 10L;
    private static final long OTHER_ID = 20L;

    @Test
    @DisplayName("§11.1: el email del propio usuario, con otras mayúsculas, es suyo (compara ids)")
    void ownEmailIsCurrentUserEmail() {
        authenticateAs(OWNER_ID);
        User owner = userWithId(OWNER_ID);
        when(userRepository.findByEmailIgnoreCase("Andrea@Email.com")).thenReturn(Optional.of(owner));

        assertThat(authorizationService.isCurrentUserEmail("Andrea@Email.com")).isTrue();
    }

    @Test
    @DisplayName("§11.1 / TC-SEC-006: el email de otro usuario no es suyo")
    void otherUsersEmailIsNotCurrentUserEmail() {
        authenticateAs(OWNER_ID);
        User other = userWithId(OTHER_ID);
        when(userRepository.findByEmailIgnoreCase("carlos@email.com")).thenReturn(Optional.of(other));

        assertThat(authorizationService.isCurrentUserEmail("carlos@email.com")).isFalse();
    }

    @Test
    @DisplayName("P4-D1: un email inexistente no es suyo (la búsqueda responderá 403)")
    void unknownEmailIsNotCurrentUserEmail() {
        authenticateAs(OWNER_ID);
        when(userRepository.findByEmailIgnoreCase("nadie@email.com")).thenReturn(Optional.empty());

        assertThat(authorizationService.isCurrentUserEmail("nadie@email.com")).isFalse();
    }

    @Test
    @DisplayName("§11.1 / TC-SEC-007: el username propio es suyo y el de otro usuario no")
    void usernameComparesPersistedIds() {
        authenticateAs(OWNER_ID);
        User owner = userWithId(OWNER_ID);
        User other = userWithId(OTHER_ID);
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(owner));
        when(userRepository.findByUsername("carlos")).thenReturn(Optional.of(other));

        assertThat(authorizationService.isCurrentUsername("andrea")).isTrue();
        assertThat(authorizationService.isCurrentUsername("carlos")).isFalse();
    }

    @Test
    @DisplayName("§11.1 / T-SEC-009: el propietario accede a su ticket y un tercero no")
    void ticketAccessComparesOwnerId() {
        Ticket ticket = ticketOwnedBy(OWNER_ID);
        when(ticketRepository.findByTicketCode("TKT-A")).thenReturn(Optional.of(ticket));

        authenticateAs(OWNER_ID);
        assertThat(authorizationService.canAccessTicket("TKT-A")).isTrue();

        authenticateAs(OTHER_ID);
        assertThat(authorizationService.canAccessTicket("TKT-A")).isFalse();
    }

    @Test
    @DisplayName("P4-D1: un ticket inexistente se deja pasar para que el servicio responda 404")
    void unknownTicketIsLeftToTheService() {
        authenticateAs(OWNER_ID);
        when(ticketRepository.findByTicketCode("TKT-NADA")).thenReturn(Optional.empty());

        assertThat(authorizationService.canAccessTicket("TKT-NADA")).isTrue();
    }

    @Test
    @DisplayName("P4-D19: sin identidad, anónimo o sin SecurityPrincipal nunca es propietario y no consulta la base de datos")
    void withoutSecurityPrincipalNothingIsOwned() {
        SecurityContextHolder.clearContext();
        assertNotOwnerOfAnything();

        authenticate(new AnonymousAuthenticationToken(
                "clave-de-prueba", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertNotOwnerOfAnything();

        authenticate(authenticated("ROLE_USER")); // principal String, no SecurityPrincipal
        assertNotOwnerOfAnything();

        verifyNoInteractions(userRepository, ticketRepository);
    }

    // ------------------------------------------------------------------ ayudantes

    private void assertNotOwnerOfAnything() {
        assertThat(authorizationService.isCurrentUserEmail("andrea@email.com")).isFalse();
        assertThat(authorizationService.isCurrentUsername("andrea")).isFalse();
        assertThat(authorizationService.canAccessTicket("TKT-A")).isFalse();
    }

    /** Autentica un SecurityPrincipal USER con ese id, como lo hace el filtro JWT. */
    private static void authenticateAs(long userId) {
        SecurityPrincipal principal = new SecurityPrincipal(userId, "usuario.prueba", Set.of(Role.USER));
        authenticate(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities()));
    }

    /** DATO DE PRUEBA: usuario simulado con un id persistente. */
    private static User userWithId(long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    /** DATO DE PRUEBA: ticket simulado cuyo propietario tiene ese id. */
    private static Ticket ticketOwnedBy(long ownerId) {
        User owner = userWithId(ownerId);
        Ticket ticket = mock(Ticket.class);
        when(ticket.getUser()).thenReturn(owner); // owner ya está configurado: sin stubbing anidado
        return ticket;
    }

    /** DATO DE PRUEBA: autenticación ya validada con las authorities indicadas. */
    private static Authentication authenticated(String... authorities) {
        return UsernamePasswordAuthenticationToken.authenticated(
                "usuario.prueba", null, AuthorityUtils.createAuthorityList(authorities));
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
