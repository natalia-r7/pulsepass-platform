package com.pulsepass.service;

import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.security.SecurityPrincipal;

/**
 * Contrato de autenticación y alta de credenciales (PRD Seguridad §7.1, §9.3, §10, §14.5;
 * FR-SEC-AUTH-001, FR-SEC-AUTH-002, FR-SEC-AUTH-010).
 *
 * <p>Orquesta las operaciones que combinan la identidad de {@link UserService} con las
 * credenciales, los roles y el token; no reemplaza a {@code UserService} (§7.1).
 */
public interface AuthService {

    /**
     * Registro público (FR-SEC-AUTH-001, UC-SEC-01, NFR-SEC-004, NFR-SEC-009; P4-D16): en una única
     * transacción crea el usuario y su perfil con {@link UserService#register}, guarda el hash
     * BCrypt de la contraseña y asigna {@code ROLE_USER}. Si cualquier paso falla, no se guarda
     * nada. El rol nunca lo elige el cliente. No emite token: el flujo es registro → login (§10.1).
     *
     * @return el usuario registrado, con el mismo contrato que antes (sin contraseña ni hash)
     * @throws com.pulsepass.exception.DuplicateResourceException si el username o el email ya
     *         existen (BR-USER-001..002)
     * @throws com.pulsepass.exception.BusinessRuleException si la fecha de nacimiento es futura
     *         (BR-USER-005)
     */
    UserResponse register(RegisterUserRequest request);

    /**
     * Login por username o email y contraseña (FR-SEC-AUTH-002..004, UC-SEC-02; P4-D13, P4-07-E). Solo
     * después de autenticar emite un JWT cuyo {@code sub} es el id del usuario.
     *
     * @return el token Bearer y su duración en segundos
     * @throws org.springframework.security.core.AuthenticationException si las credenciales no son
     *         válidas, el usuario no existe, está inactivo o no tiene credencial; el handler responde
     *         siempre el mismo 401 genérico (P4-D9)
     */
    AuthResponse login(LoginRequest request);

    /**
     * Datos mínimos del usuario autenticado (FR-SEC-AUTH-010; P4-D15, P4-07-C, P4-07-F): id,
     * username, email y roles actuales, sin secretos.
     */
    AuthenticatedUserResponse currentUser(SecurityPrincipal principal);
}
