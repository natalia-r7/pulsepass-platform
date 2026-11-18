package com.pulsepass.controller;

import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de autenticación (PRD Seguridad §10.1, §14.5; FR-SEC-AUTH-002, FR-SEC-AUTH-010;
 * P4-07-G).
 * <p>
 * Controlador delgado: valida el cuerpo y delega en {@link AuthService}. Un login inválido por
 * Bean Validation responde 400 sin intentar autenticar (P4-07-A); unas credenciales incorrectas,
 * el 401 genérico del handler (P4-D9). {@code /me} exige autenticación (regla de URL, P4-07-H).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** {@code POST /api/auth/login} → 200 con el token Bearer (FR-SEC-AUTH-002). */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** {@code GET /api/auth/me} → 200 con la identidad del usuario autenticado (FR-SEC-AUTH-010). */
    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> me(@AuthenticationPrincipal SecurityPrincipal principal) {
        return ResponseEntity.ok(authService.currentUser(principal));
    }
}
