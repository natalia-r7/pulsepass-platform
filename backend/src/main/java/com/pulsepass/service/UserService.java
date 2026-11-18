package com.pulsepass.service;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;

/**
 * Contrato de usuarios (PRD Servicios §18, FR-SVC-010..012).
 *
 * <p>Devuelve DTO, nunca las entidades {@code User} ni {@code UserProfile} (SRV-001). La
 * implementación está en {@code service.impl} (SRV-003). Sin contraseña ni roles: corresponden
 * a la Fase 4.
 */
public interface UserService {

    /**
     * Registra un usuario activo con su perfil (FR-SVC-010, BR-USER-001..005).
     *
     * <p>Desde la Fase 4, el registro público pasa por
     * {@link com.pulsepass.service.AuthService#register}, que llama a este método dentro de su
     * transacción y añade la credencial y {@code ROLE_USER} (PRD Seguridad §9.3; P4-06-N). Llamarlo
     * directamente crearía un usuario sin credencial, que no podría iniciar sesión.
     *
     * @throws com.pulsepass.exception.DuplicateResourceException si el username (BR-USER-001) o
     *         el email, ignorando mayúsculas (BR-USER-002), ya existen
     * @throws com.pulsepass.exception.BusinessRuleException si la fecha de nacimiento es futura
     *         (BR-USER-005)
     */
    UserResponse register(RegisterUserRequest request);

    /**
     * Usuario por email, ignorando mayúsculas, esté activo o no (FR-SVC-011, P2-11 U-4a).
     *
     * @throws com.pulsepass.exception.ResourceNotFoundException si no existe
     */
    UserResponse findByEmail(String email);

    /**
     * Usuario por username exacto, esté activo o no (FR-SVC-012, P2-11 U-4a).
     *
     * @throws com.pulsepass.exception.ResourceNotFoundException si no existe
     */
    UserResponse findByUsername(String username);
}
