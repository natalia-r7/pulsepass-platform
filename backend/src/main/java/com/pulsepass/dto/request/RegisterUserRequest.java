package com.pulsepass.dto.request;

import java.time.LocalDate;

/**
 * Datos para registrar un usuario y su perfil (PRD Servicios §19,
 * BR-USER-004).
 * <p>
 * {@code username} y {@code email} van a {@code User}; los demás campos van a
 * {@code UserProfile}. No incluye contraseña: corresponde a la Fase 4
 * (Seguridad).
 * <p>
 * Sin Bean Validation: las validaciones de entrada corresponden a la Fase 3.
 */
public record RegisterUserRequest(
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        String city,
        LocalDate birthDate
) {
}
