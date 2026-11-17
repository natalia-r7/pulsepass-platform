package com.pulsepass.security;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link CustomUserDetailsService} (PRD Seguridad §7.1, §17.1; FR-SEC-AUTH-004,
 * FR-SEC-AUTH-009; P4-D13; P4-05). Repositorios simulados, sin Spring.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    /** DATO DE PRUEBA: hash ficticio, no un hash real. */
    private static final String PASSWORD_HASH = "hash-de-prueba-para-login-00000000000000000000000000000000000";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new CustomUserDetailsService(userRepository, userCredentialRepository, userRoleRepository);
    }

    // ---------------------------------------------------------------- loadPrincipalById (filtro JWT)

    @Test
    @DisplayName("FR-SEC-AUTH-009: un usuario activo da su principal con los roles actuales de la base de datos")
    void loadPrincipalByIdReturnsActiveUserWithRoles() {
        User andrea = user(7L, "andrea", "andrea@email.com", true);
        when(userRepository.findById(7L)).thenReturn(Optional.of(andrea));
        when(userRoleRepository.findRolesByUserId(7L)).thenReturn(List.of(Role.STAFF, Role.USER));

        SecurityPrincipal principal = service.loadPrincipalById(7L);

        assertThat(principal.userId()).isEqualTo(7L);
        assertThat(principal.username()).isEqualTo("andrea");
        assertThat(principal.roles()).containsExactlyInAnyOrder(Role.STAFF, Role.USER);
    }

    @Test
    @DisplayName("§17.1: un usuario inexistente lanza UsernameNotFoundException")
    void loadPrincipalByIdRejectsUnknownUser() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadPrincipalById(404L))
                .isInstanceOf(UsernameNotFoundException.class);
        verifyNoInteractions(userRoleRepository);
    }

    @Test
    @DisplayName("FR-SEC-AUTH-004 / SEC-011: un usuario inactivo lanza DisabledException y no se cargan sus roles")
    void loadPrincipalByIdRejectsInactiveUser() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "andrea", "andrea@email.com", false)));

        assertThatThrownBy(() -> service.loadPrincipalById(7L))
                .isInstanceOf(DisabledException.class);
        verifyNoInteractions(userRoleRepository);
    }

    @Test
    @DisplayName("P4-05-e: un usuario sin roles queda autenticable, pero sin authorities (mínimo privilegio)")
    void loadPrincipalByIdAllowsUserWithoutRoles() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "andrea", "andrea@email.com", true)));
        when(userRoleRepository.findRolesByUserId(7L)).thenReturn(List.of());

        SecurityPrincipal principal = service.loadPrincipalById(7L);

        assertThat(principal.roles()).isEmpty();
        assertThat(principal.authorities()).isEmpty();
    }

    // ---------------------------------------------------------------- loadUserByUsername (login, P4-07)

    @Test
    @DisplayName("P4-D13: se encuentra por username exacto y devuelve hash, estado y authorities")
    void loadUserByUsernameFindsByUsername() {
        User andrea = user(7L, "andrea", "andrea@email.com", true);
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(andrea));
        when(userCredentialRepository.findById(7L)).thenReturn(Optional.of(credential(andrea)));
        when(userRoleRepository.findRolesByUserId(7L)).thenReturn(List.of(Role.USER));

        LoginUser loginUser = service.loadUserByUsername("andrea");

        assertThat(loginUser.getUsername()).isEqualTo("andrea");
        assertThat(loginUser.getPassword()).isEqualTo(PASSWORD_HASH);
        assertThat(loginUser.isEnabled()).isTrue();
        assertThat(loginUser.principal().userId()).isEqualTo(7L);
        assertThat(loginUser.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_USER");
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    @DisplayName("P4-D13: si no hay username, se busca por email sin distinguir mayúsculas")
    void loadUserByUsernameFallsBackToEmail() {
        User andrea = user(7L, "andrea", "andrea@email.com", true);
        when(userRepository.findByUsername("Andrea@Email.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("Andrea@Email.com")).thenReturn(Optional.of(andrea));
        when(userCredentialRepository.findById(7L)).thenReturn(Optional.of(credential(andrea)));
        when(userRoleRepository.findRolesByUserId(7L)).thenReturn(List.of(Role.USER));

        LoginUser loginUser = service.loadUserByUsername("Andrea@Email.com");

        assertThat(loginUser.principal().userId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("P4-D13: si el identificador es el username de uno y el email de otro, gana el username")
    void loadUserByUsernamePrefersUsernameOverEmail() {
        User owner = user(8L, "carlos@email.com", "otro@email.com", true);   // DATO DE PRUEBA: username con forma de email
        when(userRepository.findByUsername("carlos@email.com")).thenReturn(Optional.of(owner));
        when(userCredentialRepository.findById(8L)).thenReturn(Optional.of(credential(owner)));
        when(userRoleRepository.findRolesByUserId(8L)).thenReturn(List.of(Role.USER));

        LoginUser loginUser = service.loadUserByUsername("carlos@email.com");

        assertThat(loginUser.principal().userId()).isEqualTo(8L);
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    @DisplayName("FR-SEC-AUTH-003: un identificador inexistente lanza UsernameNotFoundException")
    void loadUserByUsernameRejectsUnknownIdentifier() {
        when(userRepository.findByUsername("nadie")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nadie"))
                .isInstanceOf(UsernameNotFoundException.class);
        verifyNoInteractions(userCredentialRepository);
    }

    @ParameterizedTest(name = "identificador «{0}»")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("FR-SEC-AUTH-003: un identificador vacío lanza UsernameNotFoundException sin consultar")
    void loadUserByUsernameRejectsBlankIdentifier(String identifier) {
        assertThatThrownBy(() -> service.loadUserByUsername(identifier))
                .isInstanceOf(UsernameNotFoundException.class);
        verifyNoInteractions(userRepository, userCredentialRepository, userRoleRepository);
    }

    @Test
    @DisplayName("P4-D11: un usuario sin credencial (anterior a la Fase 4) se trata como inexistente")
    void loadUserByUsernameRejectsUserWithoutCredential() {
        when(userRepository.findByUsername("legado")).thenReturn(Optional.of(user(9L, "legado", "legado@email.com", true)));
        when(userCredentialRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("legado"))
                .isInstanceOf(UsernameNotFoundException.class);
        verify(userRoleRepository, never()).findRolesByUserId(any());
    }

    @Test
    @DisplayName("FR-SEC-AUTH-004: un usuario inactivo se devuelve deshabilitado; lo rechazará el login")
    void loadUserByUsernameMarksInactiveUserAsDisabled() {
        User inactive = user(7L, "andrea", "andrea@email.com", false);
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(inactive));
        when(userCredentialRepository.findById(7L)).thenReturn(Optional.of(credential(inactive)));
        when(userRoleRepository.findRolesByUserId(7L)).thenReturn(List.of(Role.USER));

        assertThat(service.loadUserByUsername("andrea").isEnabled()).isFalse();
    }

    @Test
    @DisplayName("NFR-SEC-001: eraseCredentials borra el hash y toString nunca lo muestra")
    void loginUserNeverExposesTheHash() {
        User andrea = user(7L, "andrea", "andrea@email.com", true);
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(andrea));
        when(userCredentialRepository.findById(7L)).thenReturn(Optional.of(credential(andrea)));
        when(userRoleRepository.findRolesByUserId(7L)).thenReturn(List.of(Role.USER));
        LoginUser loginUser = service.loadUserByUsername("andrea");

        assertThat(loginUser.toString()).doesNotContain(PASSWORD_HASH);
        loginUser.eraseCredentials();
        assertThat(loginUser.getPassword()).isNull();
    }

    // ------------------------------------------------------------------ ayudantes

    /** DATO DE PRUEBA: usuario con id asignado (sin base de datos). */
    private static User user(Long id, String username, String email, boolean active) {
        User user = new User(username, email, active);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static UserCredential credential(User user) {
        return new UserCredential(user, PASSWORD_HASH);
    }
}
