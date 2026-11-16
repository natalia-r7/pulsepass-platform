package com.pulsepass.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un usuario y su perfil (PRD Servicios §19,
 * BR-USER-004).
 * <p>
 * {@code username} y {@code email} van a {@code User}; los demás campos van a
 * {@code UserProfile}. No incluye contraseña: corresponde a la Fase 4
 * (Seguridad).
 * <p>
 * Bean Validation (PRD Controladores §11.2; P3-03): campos obligatorios y
 * formato de email según el PRD; longitudes máximas según las columnas de V1
 * (P3-D4); mensajes en inglés (P3-D5). {@code phone} y {@code city} son
 * opcionales. La unicidad de username y email y que la fecha de nacimiento no
 * sea futura son reglas de negocio del servicio (§11.4).
 */
public record RegisterUserRequest(
        @NotBlank(message = "Username is required")
        @Size(max = 50, message = "Username must not exceed 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @NotNull(message = "Birth date is required")
        LocalDate birthDate
) {
}
