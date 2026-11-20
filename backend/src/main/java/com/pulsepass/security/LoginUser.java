package com.pulsepass.security;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Objects;

/**
 * Usuario que se autentica con usuario o correo y contraseña (PRD Seguridad §7.1, §9; P4-05-b).
 * <p>
 * Lo devuelve {@link CustomUserDetailsService#loadUserByUsername(String)} para el login (P4-07).
 * Une la identidad ({@link SecurityPrincipal}, que nunca guarda contraseñas, P4-04-h) con el hash
 * BCrypt que hace falta para comprobar la contraseña. Tras autenticar, Spring Security llama a
 * {@link #eraseCredentials()} y el hash deja de estar en memoria. {@link #toString()} nunca lo
 * muestra (NFR-SEC-001).
 */
public final class LoginUser implements UserDetails, CredentialsContainer {

    private final SecurityPrincipal principal;

    private String passwordHash;

    private final boolean enabled;

    /**
     * @param principal    identidad del usuario, con sus roles actuales
     * @param passwordHash hash BCrypt de su contraseña
     * @param enabled      {@code users.active}: un usuario inactivo no puede iniciar sesión (FR-SEC-AUTH-004)
     */
    public LoginUser(SecurityPrincipal principal, String passwordHash, boolean enabled) {
        this.principal = Objects.requireNonNull(principal, "principal must not be null");
        this.passwordHash = passwordHash;
        this.enabled = enabled;
    }

    /** Identidad que se usará para emitir el token tras un login correcto (P4-07). */
    public SecurityPrincipal principal() {
        return principal;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return principal.authorities();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return principal.username();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /** Borra el hash después de autenticar (Spring Security lo llama automáticamente). */
    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }

    /** Sin el hash (NFR-SEC-001). */
    @Override
    public String toString() {
        return "LoginUser[userId=" + principal.userId() + ", username=" + principal.username()
                + ", enabled=" + enabled + ", password=****]";
    }
}
