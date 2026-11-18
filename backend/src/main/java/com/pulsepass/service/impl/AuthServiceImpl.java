package com.pulsepass.service.impl;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.security.AccessToken;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.LoginUser;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link AuthService} (PRD Seguridad §9.3, §10; P4-D16, P4-06-A, H e I;
 * P4-07-E, F e I).
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
 *   <li>El registro no se registra en logs (P4-06-P).</li>
 *   <li>Login (P4-07-E): el {@link AuthenticationManager} explícito comprueba las credenciales; solo
 *       después se emite el token. Cualquier {@link AuthenticationException} se propaga y el handler
 *       responde el mismo 401 genérico (FR-SEC-AUTH-003, P4-D9). No se guarda nada en el
 *       {@code SecurityContext}: la API no tiene sesión.</li>
 *   <li>Logs del login (NFR-SEC-006, P4-07-I): solo el id del usuario o el tipo de fallo; nunca el
 *       identificador, la contraseña, el hash ni el token.</li>
 * </ul>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserService userService;

    private final UserRepository userRepository;

    private final UserCredentialRepository userCredentialRepository;

    private final UserRoleRepository userRoleRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    public AuthServiceImpl(UserService userService, UserRepository userRepository,
                           UserCredentialRepository userCredentialRepository,
                           UserRoleRepository userRoleRepository, PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
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

    /** {@inheritDoc} */
    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.identifier(), request.password()));
        } catch (AuthenticationException ex) {
            log.info("Login failed: reason={}", ex.getClass().getSimpleName());
            throw ex;
        }
        SecurityPrincipal principal = ((LoginUser) authentication.getPrincipal()).principal();
        AccessToken token = jwtService.issueAccessToken(principal.userId());
        log.info("Login succeeded: userId={}", principal.userId());
        return AuthResponse.bearer(token.token(), token.expiresInSeconds());
    }

    /**
     * {@inheritDoc} El username y el email salen de la base de datos (el usuario con el id del
     * principal); los roles, del principal, que el filtro JWT carga de nuevo en cada petición.
     */
    @Override
    @Transactional(readOnly = true)
    public AuthenticatedUserResponse currentUser(SecurityPrincipal principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new UsernameNotFoundException("User not available"));
        return new AuthenticatedUserResponse(principal.userId(), user.getUsername(), user.getEmail(),
                principal.roles().stream().map(Role::name).sorted().toList());
    }
}
