package com.pulsepass.service.impl;

import com.pulsepass.domain.User;
import com.pulsepass.domain.Ticket;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.AuthorizationService;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

/**
 * Implementación de {@link AuthorizationService} sobre el contexto de Spring Security
 * (PRD Seguridad §7.1, §11.1; P4-04b; P4-08-B y C).
 * <ul>
 *   <li>{@code isStaffOrAdmin} solo mira las authorities de la autenticación actual (P4-04b).</li>
 *   <li>Las comprobaciones de propiedad toman el id del {@link SecurityPrincipal} y lo comparan con
 *       el id persistente del recurso, leído con las consultas existentes de los repositorios
 *       (P4-08-C): el usuario por email o username y el ticket por código, cuyo usuario es
 *       {@code LAZY} y aporta su id sin otra consulta.</li>
 *   <li>Una autenticación ausente, no autenticada, anónima o sin {@code SecurityPrincipal} nunca
 *       cuenta como propietaria ni como STAFF o ADMIN (P4-D19).</li>
 *   <li>El nombre del bean, {@code authz}, es el que usan las expresiones {@code @PreAuthorize}.</li>
 * </ul>
 */
@Service("authz")
public class AuthorizationServiceImpl implements AuthorizationService {

    /** Authorities que pueden ver eventos en cualquier estado (P4-D2). */
    private static final Set<String> STAFF_OR_ADMIN = Set.of("ROLE_STAFF", "ROLE_ADMIN");

    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    private final UserRepository userRepository;

    private final TicketRepository ticketRepository;

    public AuthorizationServiceImpl(UserRepository userRepository, TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public boolean isStaffOrAdmin() {
        Authentication authentication = currentAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(STAFF_OR_ADMIN::contains);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCurrentUserEmail(String email) {
        Long currentUserId = currentUserId();
        if (currentUserId == null || email == null) {
            return false;
        }
        return isCurrentUser(userRepository.findByEmailIgnoreCase(email), currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCurrentUsername(String username) {
        Long currentUserId = currentUserId();
        if (currentUserId == null || username == null) {
            return false;
        }
        return isCurrentUser(userRepository.findByUsername(username), currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAccessTicket(String ticketCode) {
        Long currentUserId = currentUserId();
        if (currentUserId == null || ticketCode == null) {
            return false;
        }
        Optional<Ticket> ticket = ticketRepository.findByTicketCode(ticketCode);
        // Ticket inexistente: se deja pasar para que el servicio responda 404 (P4-D1).
        return ticket.isEmpty() || currentUserId.equals(ticket.get().getUser().getId());
    }

    // ------------------------------------------------------------------ ayudantes

    private static boolean isCurrentUser(Optional<User> user, Long currentUserId) {
        return user.map(User::getId).filter(currentUserId::equals).isPresent();
    }

    /** Autenticación actual si está validada y no es anónima; si no, {@code null}. */
    private Authentication currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || trustResolver.isAnonymous(authentication)) {
            return null;
        }
        return authentication;
    }

    /** Id persistente del usuario autenticado, o {@code null} si no hay un {@link SecurityPrincipal}. */
    private Long currentUserId() {
        Authentication authentication = currentAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityPrincipal principal) {
            return principal.userId();
        }
        return null;
    }
}
