package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de usuarios (PRD Controladores §7.4, §8.4; FR-CTRL-USR-001..003).
 * <p>
 * Controlador delgado: solo traduce HTTP a llamadas de {@link UserService} y devuelve sus DTO
 * (CTRL-001..004, CTRL-009).
 * <ul>
 *   <li>El registro valida el cuerpo con {@code @Valid} antes de llamar al servicio (§11.2,
 *       CTRL-005); un request inválido responde 400 y no llega al servicio.</li>
 *   <li>Username y email duplicados (BR-USER-001..002) y la fecha de nacimiento futura
 *       (BR-USER-005) son reglas del servicio; el handler las traduce a 409.</li>
 *   <li>Los parámetros de consulta llegan al servicio sin transformarse (P3-D9): ignorar
 *       mayúsculas en el email es regla del servicio. Un usuario inexistente responde 404; un
 *       parámetro ausente, 400 (P3-D3).</li>
 * </ul>
 * El 201 no incluye la cabecera {@code Location} (P3-D8).
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** {@code POST /api/users} → 201 con el usuario registrado (FR-CTRL-USR-001). */
    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    /** {@code GET /api/users/by-email?email=...} → 200 con el usuario (FR-CTRL-USR-002). */
    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> findByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.findByEmail(email));
    }

    /** {@code GET /api/users/by-username?username=...} → 200 con el usuario (FR-CTRL-USR-003). */
    @GetMapping("/by-username")
    public ResponseEntity<UserResponse> findByUsername(@RequestParam String username) {
        return ResponseEntity.ok(userService.findByUsername(username));
    }
}
