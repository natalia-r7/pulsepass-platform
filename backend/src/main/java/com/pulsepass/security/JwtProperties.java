package com.pulsepass.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Base64;

/**
 * Configuración del JWT de acceso (PRD Seguridad §12.2, §14.2; NFR-SEC-008), enlazada desde
 * {@code pulsepass.security.jwt} en {@code application.yml}.
 * <p>
 * Se valida al crearse (P4-03-b): si algo no es correcto, lanza {@link IllegalArgumentException}
 * y la aplicación no arranca (§12.2). Los mensajes nunca incluyen el valor del secreto, y
 * {@link #toString()} lo oculta (NFR-SEC-001).
 *
 * @param issuer         emisor esperado en el claim {@code iss}
 * @param secretBase64   clave HMAC en Base64 estándar; decodificada debe tener al menos 32 bytes
 * @param accessTokenTtl duración del token: positiva y en segundos enteros (P4-03-f2)
 */
@ConfigurationProperties(prefix = "pulsepass.security.jwt")
public record JwtProperties(String issuer, String secretBase64, Duration accessTokenTtl) {

    /** Longitud mínima de la clave HS256: 256 bits (§12.2). */
    public static final int MIN_SECRET_BYTES = 32;

    private static final String PREFIX = "pulsepass.security.jwt.";

    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException(PREFIX + "issuer must not be blank");
        }
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalArgumentException(PREFIX + "secret-base64 must be provided");
        }
        if (decodedLength(secretBase64) < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    PREFIX + "secret-base64 must decode to at least " + MIN_SECRET_BYTES + " bytes");
        }
        if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
            throw new IllegalArgumentException(PREFIX + "access-token-ttl must be positive");
        }
        if (accessTokenTtl.getNano() != 0) {
            throw new IllegalArgumentException(PREFIX + "access-token-ttl must be a whole number of seconds");
        }
    }

    /**
     * Longitud de la clave decodificada con el decodificador Base64 estándar, que rechaza la
     * variante URL ({@code -} y {@code _}) y cualquier otro carácter no válido.
     */
    private static int decodedLength(String secretBase64) {
        try {
            return Base64.getDecoder().decode(secretBase64).length;
        } catch (IllegalArgumentException ex) {
            // Sin la causa: su mensaje podría contener fragmentos del secreto.
            throw new IllegalArgumentException(PREFIX + "secret-base64 must be valid standard Base64");
        }
    }

    /** Oculta el secreto: el toString automático de un record lo mostraría (NFR-SEC-001). */
    @Override
    public String toString() {
        return "JwtProperties[issuer=" + issuer + ", secretBase64=****, accessTokenTtl=" + accessTokenTtl + "]";
    }
}
