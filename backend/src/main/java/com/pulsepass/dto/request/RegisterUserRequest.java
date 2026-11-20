package com.pulsepass.dto.request;

import java.time.LocalDate;

import com.pulsepass.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un usuario y su perfil (PRD Servicios §19,
 * BR-USER-004).
 * <p>
 * {@code username} y {@code email} van a {@code User}; {@code password} va,
 * como hash BCrypt, a {@code UserCredential} (PRD Seguridad §3.4, §9.3); los
 * demás campos van a {@code UserProfile}.
 * <p>
 * Bean Validation (PRD Controladores §11.2; P3-03): campos obligatorios y
 * formato de email según el PRD; longitudes máximas según las columnas de V1
 * (P3-D4); mensajes en inglés (P3-D5). {@code phone} y {@code city} son
 * opcionales. La unicidad de username y email y que la fecha de nacimiento no
 * sea futura son reglas de negocio del servicio (§11.4).
 * <p>
 * Contraseña (PRD Seguridad §9.3; P4-D14, P4-06-B, D, E y F): obligatoria, de
 * al menos 12 caracteres y de 72 bytes UTF-8 como máximo, el límite de BCrypt.
 * {@code @Size} cuenta unidades UTF-16: un emoji cuenta como 2 caracteres
 * (P4-06-E). Va al final del record, como en el ejemplo del PRD E2E §12.1.
 * No hay campo de rol: el registro siempre asigna {@code USER} (NFR-SEC-009).
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
        LocalDate birthDate,

        @NotBlank(message = "Password is required")
        @Size(min = 12, message = "Password must be at least 12 characters")
        @MaxUtf8Bytes(value = 72, message = "Password must not exceed 72 bytes")
        String password
) {

    /**
     * Igual que el {@code toString} del record, pero con la contraseña oculta (P4-06-G, SEC-003,
     * NFR-SEC-001): así no llega a los logs aunque alguien registre el request.
     */
    @Override
    public String toString() {
        return "RegisterUserRequest[username=" + username
                + ", email=" + email
                + ", firstName=" + firstName
                + ", lastName=" + lastName
                + ", phone=" + phone
                + ", city=" + city
                + ", birthDate=" + birthDate
                + ", password=***]";
    }
}
