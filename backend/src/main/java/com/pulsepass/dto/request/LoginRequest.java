package com.pulsepass.dto.request;

import com.pulsepass.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credenciales de login (PRD Seguridad §10.1, §10.2; FR-SEC-AUTH-002; P4-07-A).
 * <p>
 * {@code identifier} es el username o el email (P4-D13); su máximo es el de la columna del email
 * (254). La contraseña no aplica el mínimo de 12 del registro, que solo revelaría la política; sí
 * el máximo de 72 bytes de BCrypt, para responder 400 sin intentar autenticar (P4-07-A). Mensajes en
 * inglés (P4-D9).
 */
public record LoginRequest(
        @NotBlank(message = "Identifier is required")
        @Size(max = 254, message = "Identifier must not exceed 254 characters")
        String identifier,

        @NotBlank(message = "Password is required")
        @MaxUtf8Bytes(value = 72, message = "Password must not exceed 72 bytes")
        String password
) {

    /** Sin la contraseña (SEC-003, NFR-SEC-001). */
    @Override
    public String toString() {
        return "LoginRequest[identifier=" + identifier + ", password=***]";
    }
}
