package com.pulsepass.service;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;

/**
 * Contrato de autenticación y alta de credenciales (PRD Seguridad §7.1, §9.3, §14.5;
 * FR-SEC-AUTH-001).
 *
 * <p>Orquesta las operaciones que combinan la identidad de {@link UserService} con las
 * credenciales y los roles; no reemplaza a {@code UserService} (§7.1). El login se añadirá en
 * P4-07.
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
}
