package com.pulsepass.security;

import com.pulsepass.domain.enums.Role;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Identidad autenticada de una petición (PRD Seguridad §7.1, §14.5; P4-04-h).
 * <p>
 * Contiene solo lo que necesitan la autorización y los logs: el id persistente del usuario
 * ({@code users.id}, que es el {@code sub} del JWT), su username y sus roles actuales. No guarda
 * contraseñas, hashes ni tokens (NFR-SEC-001). La credencial que se usa durante el login se
 * decide en P4-05.
 *
 * @param userId   id persistente del usuario
 * @param username username del usuario
 * @param roles    roles actuales, sin el prefijo {@code ROLE_}
 */
public record SecurityPrincipal(Long userId, String username, Set<Role> roles) implements AuthenticatedPrincipal {

    public SecurityPrincipal {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(username, "username must not be null");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles must not be null"));
    }

    /** Authorities de Spring Security: {@code ROLE_} + nombre del rol (§6), en orden alfabético. */
    public List<GrantedAuthority> authorities() {
        return roles.stream()
                .sorted(Comparator.comparing(Role::name))
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
    }

    /** Nombre que devuelve {@code Authentication.getName()}: el username, nunca un secreto. */
    @Override
    public String getName() {
        return username;
    }
}
