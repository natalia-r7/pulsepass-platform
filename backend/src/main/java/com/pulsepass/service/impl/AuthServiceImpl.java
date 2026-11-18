package com.pulsepass.service.impl;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link AuthService} (PRD Seguridad §9.3; P4-D16, P4-06-A, H e I).
 *
 * <ul>
 *   <li>Atomicidad (NFR-SEC-004): {@code register} es {@code @Transactional} y
 *       {@link UserService#register} se une a esta misma transacción (propagación
 *       {@code REQUIRED}). Una excepción en cualquier paso deshace el usuario, el perfil, la
 *       credencial y el rol.</li>
 *   <li>El {@code User} para {@code @MapsId} se obtiene con {@code getReferenceById}: ya está en el
 *       contexto de persistencia de esta transacción, así que no hay consulta extra
 *       (P4-06-A).</li>
 *   <li>El hash se calcula después de las reglas de negocio: un duplicado o una fecha futura falla
 *       antes de gastar el coste de BCrypt (P4-06-H).</li>
 *   <li>El rol es siempre {@link Role#USER}, fijo en el código: el cliente no puede elegirlo
 *       (NFR-SEC-009, SEC-005, TC-SEC-016; P4-06-I).</li>
 *   <li>Sin logs: el registro no es un evento que NFR-SEC-006 pida registrar (P4-06-P).</li>
 * </ul>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;

    private final UserRepository userRepository;

    private final UserCredentialRepository userCredentialRepository;

    private final UserRoleRepository userRoleRepository;

    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserService userService, UserRepository userRepository,
                           UserCredentialRepository userCredentialRepository,
                           UserRoleRepository userRoleRepository, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        // 1. Usuario y perfil, con las reglas BR-USER de la Fase 2 (§9.3).
        UserResponse registered = userService.register(request);

        // 2. El usuario recién guardado, gestionado en esta transacción (P4-06-A).
        User user = userRepository.getReferenceById(registered.id());

        // 3. Credencial con el hash BCrypt; nunca la contraseña en claro (SEC-003).
        userCredentialRepository.save(new UserCredential(user, passwordEncoder.encode(request.password())));

        // 4. Rol inicial, siempre USER (NFR-SEC-009).
        userRoleRepository.save(new UserRole(registered.id(), Role.USER));

        return registered;
    }
}
