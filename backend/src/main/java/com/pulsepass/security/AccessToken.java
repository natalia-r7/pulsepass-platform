package com.pulsepass.security;

/**
 * Token de acceso emitido por {@link JwtService} (P4-03-d).
 *
 * @param token            JWT firmado (JWS compacto)
 * @param expiresInSeconds duración del token en segundos; coincide con {@code exp - iat} (P4-03-f2)
 *                         y es el {@code expiresIn} de la respuesta de login (§10.2)
 */
public record AccessToken(String token, long expiresInSeconds) {

    /** Oculta el token: el toString automático de un record lo mostraría (§12.2, NFR-SEC-001). */
    @Override
    public String toString() {
        return "AccessToken[token=****, expiresInSeconds=" + expiresInSeconds + "]";
    }
}
