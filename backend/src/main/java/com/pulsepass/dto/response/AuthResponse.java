package com.pulsepass.dto.response;

/**
 * Respuesta de un login correcto (PRD Seguridad §10.1, §10.2; FR-SEC-AUTH-002; P4-07-B).
 *
 * @param accessToken JWT firmado
 * @param tokenType   siempre {@value #BEARER}
 * @param expiresIn   segundos de validez del token (900 por defecto)
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn) {

    /** Tipo de token del encabezado {@code Authorization} (§10.2). */
    public static final String BEARER = "Bearer";

    /** Respuesta Bearer con el token y su duración. */
    public static AuthResponse bearer(String accessToken, long expiresIn) {
        return new AuthResponse(accessToken, BEARER, expiresIn);
    }

    /** Sin el token (§12.2, NFR-SEC-001). */
    @Override
    public String toString() {
        return "AuthResponse[accessToken=***, tokenType=" + tokenType + ", expiresIn=" + expiresIn + "]";
    }
}
