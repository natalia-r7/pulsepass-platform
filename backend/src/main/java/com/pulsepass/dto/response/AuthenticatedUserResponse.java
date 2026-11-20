package com.pulsepass.dto.response;

import java.util.List;

/**
 * Identidad mínima del usuario autenticado para {@code GET /api/auth/me} (PRD Seguridad §10.1;
 * FR-SEC-AUTH-010; P4-D15, P4-07-C). Sin contraseña, hash ni token.
 *
 * @param roles nombres de los roles sin el prefijo {@code ROLE_}, en orden alfabético
 *              (por ejemplo {@code ["USER"]})
 */
public record AuthenticatedUserResponse(Long id, String username, String email, List<String> roles) {
}
