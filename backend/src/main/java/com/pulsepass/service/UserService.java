package com.pulsepass.service;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Contrato de usuarios (PRD Servicios §18, FR-SVC-010..012).
 *
 * <p>Devuelve DTO, nunca las entidades {@code User} ni {@code UserProfile} (SRV-001). La
 * implementación está en {@code service.impl} (SRV-003). Sin contraseña ni roles: corresponden
 * a la Fase 4.
 *
 * <p>Propiedad (PRD Seguridad §11, §11.1; P4-D1, P4-D5, P4-08-D): las búsquedas solo las puede
 * hacer el propio usuario, comparando ids persistentes con el bean {@code authz}, o un ADMIN. Un
 * usuario ajeno o inexistente responde 403. {@code register} no tiene regla: es el registro
 * público (FR-SEC-USR-01).
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
     * @throws org.springframework.security.access.AccessDeniedException si no es el propio usuario
     *         ni un ADMIN (FR-SEC-USR-02)
     */
    @PreAuthorize("hasRole('ADMIN') or @authz.isCurrentUserEmail(#email)")
    UserResponse findByEmail(String email);

    /**
     * Usuario por username exacto, esté activo o no (FR-SVC-012, P2-11 U-4a).
     *
     * @throws com.pulsepass.exception.ResourceNotFoundException si no existe
     * @throws org.springframework.security.access.AccessDeniedException si no es el propio usuario
     *         ni un ADMIN (FR-SEC-USR-03)
     */
    @PreAuthorize("hasRole('ADMIN') or @authz.isCurrentUsername(#username)")
    UserResponse findByUsername(String username);
}
