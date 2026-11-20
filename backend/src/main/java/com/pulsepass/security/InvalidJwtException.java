package com.pulsepass.security;

/**
 * Token de acceso no válido por cualquier motivo: firma, algoritmo, emisor, caducidad, claims
 * ausentes o formato (FR-SEC-AUTH-006, P4-03-d).
 * <p>
 * El mensaje es siempre el mismo y nunca incluye el token ni el motivo concreto (SEC-009,
 * NFR-SEC-001). La causa original, si la hay, se conserva solo para depurar.
 */
public class InvalidJwtException extends RuntimeException {

    static final String MESSAGE = "Invalid access token";

    public InvalidJwtException() {
        super(MESSAGE);
    }

    public InvalidJwtException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
