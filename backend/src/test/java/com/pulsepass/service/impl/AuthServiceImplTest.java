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
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.security.AccessToken;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.LoginUser;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.pulsepass.controller.ControllerFixtures.ANDREA_PASSWORD;
import static com.pulsepass.controller.ControllerFixtures.andreaRegisterRequest;
import static com.pulsepass.controller.ControllerFixtures.andreaResponse;
import static com.pulsepass.service.ServiceFixtures.andrea;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link AuthServiceImpl} (PRD Seguridad §9.3, §10, §17.1;
 * FR-SEC-AUTH-001..004, FR-SEC-AUTH-010, NFR-SEC-009; P4-D16, P4-06-A, H e I; P4-07-E y F).
 *
 * <p>Servicio real; {@code UserService}, repositorios, {@code PasswordEncoder},
 * {@code AuthenticationManager} y {@code JwtService} simulados con Mockito. Sin Spring ni base de
 * datos: la atomicidad real la prueba {@code AuthRegistrationIT} y el login real, {@code AuthLoginIT}.
 * Los DTO de Andrea salen de {@code ControllerFixtures} y la entidad, de {@code ServiceFixtures}.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    /** DATO DE PRUEBA: hash ficticio que devuelve el encoder simulado. */
    private static final String PASSWORD_HASH = "$2a$10$hash-de-prueba-ficticio-000000000000000000000000000000";

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userService, userRepository, userCredentialRepository,
                userRoleRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    @DisplayName("FR-SEC-AUTH-001: registra el usuario, guarda el hash BCrypt y asigna solo ROLE_USER, en ese orden")
    void registerCreatesUserCredentialAndUserRole() {
        // ARRANGE
        RegisterUserRequest request = andreaRegisterRequest();
        UserResponse registered = andreaResponse(); // id = 1
        User user = andrea();
        when(userService.register(request)).thenReturn(registered);
        when(userRepository.getReferenceById(registered.id())).thenReturn(user);
        when(passwordEncoder.encode(ANDREA_PASSWORD)).thenReturn(PASSWORD_HASH);

        // ACT
        UserResponse result = authService.register(request);

        // ASSERT: devuelve el mismo UserResponse, sin cambios de contrato (NFR-SEC-005).
        assertThat(result).isSameAs(registered);

        // Las reglas de negocio van antes que el hash (P4-06-H), y el rol al final.
        InOrder order = inOrder(userService, passwordEncoder, userCredentialRepository, userRoleRepository);
        order.verify(userService).register(request);
        order.verify(passwordEncoder).encode(ANDREA_PASSWORD);
        order.verify(userCredentialRepository).save(any(UserCredential.class));
        order.verify(userRoleRepository).save(any(UserRole.class));

        // La credencial apunta al usuario gestionado y guarda el hash, no la contraseña (SEC-003).
        ArgumentCaptor<UserCredential> credential = ArgumentCaptor.forClass(UserCredential.class);
        verify(userCredentialRepository).save(credential.capture());
        assertThat(credential.getValue().getUser()).isSameAs(user);
        assertThat(credential.getValue().getPasswordHash()).isEqualTo(PASSWORD_HASH);

        // Un único rol y siempre USER (NFR-SEC-009).
        ArgumentCaptor<UserRole> role = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(role.capture());
        assertThat(role.getValue().getUserId()).isEqualTo(registered.id());
        assertThat(role.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("BR-USER-001/002: un duplicado se propaga sin calcular el hash ni guardar credencial ni rol")
    void registerDoesNotHashOrSaveWhenUserIsDuplicated() {
        // ARRANGE
        RegisterUserRequest request = andreaRegisterRequest();
        when(userService.register(request))
                .thenThrow(new DuplicateResourceException("Username already exists: andrea"));

        // ACT + ASSERT
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username already exists: andrea");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).getReferenceById(anyLong());
        verify(userCredentialRepository, never()).save(any());
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    @DisplayName("BR-USER-005: una fecha futura se propaga sin calcular el hash ni guardar credencial ni rol")
    void registerDoesNotHashOrSaveWhenBirthDateIsInTheFuture() {
        // ARRANGE
        RegisterUserRequest request = andreaRegisterRequest();
        when(userService.register(request))
                .thenThrow(new BusinessRuleException("Birth date cannot be in the future: 2030-01-01"));

        // ACT + ASSERT
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).getReferenceById(anyLong());
        verify(userCredentialRepository, never()).save(any());
        verify(userRoleRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- login (P4-07)

    /** DATO DE PRUEBA: token ficticio que devuelve el JwtService simulado. */
    private static final String TOKEN = "token.de.prueba";

    /** DATO DE PRUEBA: principal de Andrea con id 1. */
    private static final SecurityPrincipal ANDREA = new SecurityPrincipal(1L, "andrea", Set.of(Role.USER));

    @Test
    @DisplayName("FR-SEC-AUTH-002 / UC-SEC-02: credenciales correctas → token Bearer emitido para el id del usuario autenticado")
    void loginIssuesTokenForAuthenticatedUser() {
        // ARRANGE
        LoginRequest request = new LoginRequest("andrea", ANDREA_PASSWORD);
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
                new LoginUser(ANDREA, null, true), null, ANDREA.authorities());
        when(authenticationManager.authenticate(any())).thenReturn(authenticated);
        when(jwtService.issueAccessToken(1L)).thenReturn(new AccessToken(TOKEN, 900));

        // ACT
        AuthResponse response = authService.login(request);

        // ASSERT
        assertThat(response).isEqualTo(new AuthResponse(TOKEN, "Bearer", 900));

        // Se autentica con el identificador y la contraseña recibidos, como token no autenticado.
        ArgumentCaptor<Authentication> attempt = ArgumentCaptor.forClass(Authentication.class);
        InOrder order = inOrder(authenticationManager, jwtService);
        order.verify(authenticationManager).authenticate(attempt.capture());
        order.verify(jwtService).issueAccessToken(1L);
        assertThat(attempt.getValue().getPrincipal()).isEqualTo("andrea");
        assertThat(attempt.getValue().getCredentials()).isEqualTo(ANDREA_PASSWORD);
        assertThat(attempt.getValue().isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("FR-SEC-AUTH-003: contraseña incorrecta → se propaga la excepción y no se emite token")
    void loginWithBadCredentialsDoesNotIssueToken() {
        assertLoginFailsWithoutToken(new BadCredentialsException("Bad credentials"));
    }

    @Test
    @DisplayName("FR-SEC-AUTH-004: usuario inactivo → se propaga la excepción y no se emite token")
    void loginWithDisabledUserDoesNotIssueToken() {
        assertLoginFailsWithoutToken(new DisabledException("User not available"));
    }

    @Test
    @DisplayName("FR-SEC-AUTH-003 / P4-D11: usuario inexistente o sin credencial → se propaga la excepción y no se emite token")
    void loginWithUnknownUserDoesNotIssueToken() {
        assertLoginFailsWithoutToken(new UsernameNotFoundException("User not available"));
    }

    @Test
    @DisplayName("FR-SEC-AUTH-010 / P4-D15: /me devuelve id, username y email persistidos y los roles sin prefijo, ordenados")
    void currentUserReturnsPersistedDataAndSortedRoles() {
        // ARRANGE: DATO DE PRUEBA, un usuario con dos roles.
        SecurityPrincipal principal = new SecurityPrincipal(1L, "andrea", Set.of(Role.USER, Role.ADMIN));
        User andrea = andrea();
        when(userRepository.findById(1L)).thenReturn(Optional.of(andrea));

        // ACT
        AuthenticatedUserResponse response = authService.currentUser(principal);

        // ASSERT
        assertThat(response).isEqualTo(new AuthenticatedUserResponse(
                1L, "andrea", "andrea@email.com", List.of("ADMIN", "USER")));
    }

    @Test
    @DisplayName("FR-SEC-AUTH-009: si el usuario ya no existe, /me no devuelve datos (excepción de autenticación)")
    void currentUserFailsWhenUserNoLongerExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.currentUser(ANDREA))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private void assertLoginFailsWithoutToken(AuthenticationException failure) {
        // ARRANGE
        when(authenticationManager.authenticate(any())).thenThrow(failure);

        // ACT + ASSERT
        assertThatThrownBy(() -> authService.login(new LoginRequest("andrea", ANDREA_PASSWORD)))
                .isSameAs(failure);

        verify(jwtService, never()).issueAccessToken(any());
    }
}
