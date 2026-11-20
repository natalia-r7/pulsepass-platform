package com.pulsepass.service.impl;

import com.pulsepass.config.ClockConfig;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.pulsepass.service.ServiceFixtures.andrea;
import static com.pulsepass.service.ServiceFixtures.miguel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link UserServiceImpl} (PRD Servicios §18–§20, §44; TEST-USER-001..004;
 * P2-11).
 *
 * <p>Servicio real; repositorio y mapper simulados con Mockito (§42, P2-07 V-1a). Sin Spring,
 * sin PostgreSQL y sin Testcontainers (NFR-001). El reloj es fijo ({@code Clock.fixed}) para que
 * «hoy» sea siempre el mismo día (P2-D4). Los datos salen de {@code ServiceFixtures} (P2-D12).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    /** DATO DE PRUEBA: «ahora» para todas las pruebas (el mismo que en EventServiceImplTest). */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 15, 12, 0);

    private static final LocalDate TODAY = NOW.toLocalDate();

    private static final Clock FIXED_CLOCK = Clock.fixed(
            NOW.atZone(ClockConfig.APPLICATION_ZONE).toInstant(), ClockConfig.APPLICATION_ZONE);

    /**
     * Contraseña del request (PRD Seguridad §10.2), añadida en P4-06 solo para construir el record:
     * {@code UserServiceImpl} no la usa; el hash lo guarda {@code AuthServiceImpl}.
     */
    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userMapper, FIXED_CLOCK);
    }

    // ---------------------------------------------------------------- register

    @Test
    @DisplayName("TEST-USER-001 / BR-USER-003 / BR-USER-004: registrar un usuario válido lo crea activo, con su perfil asociado y un único save()")
    void registerCreatesActiveUserWithProfileInSingleSave() {
        // ARRANGE
        // Datos de Andrea (ServiceFixtures, §47) con phone y city no nulos, para comprobar que
        // setPhone y setCity se aplican. Valores reutilizados de UserMapperTest.
        User andrea = andrea();
        UserProfile andreaProfile = andrea.getProfile();
        RegisterUserRequest request = new RegisterUserRequest(andrea.getUsername(), andrea.getEmail(),
                andreaProfile.getFirstName(), andreaProfile.getLastName(),
                "3000000000",   // DATO DE PRUEBA (UserMapperTest)
                "Santa Marta",  // DATO DE PRUEBA (UserMapperTest)
                andreaProfile.getBirthDate(),
                PASSWORD);
        UserResponse expected = userResponseOf(andrea, 100L); // DATO DE PRUEBA: id
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(expected);

        // ACT
        UserResponse response = userService.register(request);

        // ASSERT
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(saved.capture());
        User user = saved.getValue();
        assertThat(user.getUsername()).isEqualTo(request.username());
        assertThat(user.getEmail()).isEqualTo(request.email());
        assertThat(user.getActive()).isTrue();

        UserProfile profile = user.getProfile();
        assertThat(profile).isNotNull();
        assertThat(profile.getFirstName()).isEqualTo(request.firstName());
        assertThat(profile.getLastName()).isEqualTo(request.lastName());
        assertThat(profile.getPhone()).isEqualTo("3000000000");
        assertThat(profile.getCity()).isEqualTo("Santa Marta");
        assertThat(profile.getBirthDate()).isEqualTo(request.birthDate());
        assertThat(profile.getUser()).isSameAs(user);

        assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("TEST-USER-002 / BR-USER-001: un username duplicado lanza DuplicateResourceException, no revisa el email y no guarda")
    void registerThrowsWhenUsernameAlreadyExists() {
        // ARRANGE
        when(userRepository.existsByUsername("andrea")).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.register(requestFrom(andrea())))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username already exists: andrea");
        verify(userRepository, never()).existsByEmailIgnoreCase(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-USER-003 / BR-USER-002: un email duplicado lanza DuplicateResourceException y no guarda")
    void registerThrowsWhenEmailAlreadyExists() {
        // ARRANGE
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.register(requestFrom(andrea())))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already exists: andrea@email.com");
        verify(userRepository).existsByEmailIgnoreCase(eq("andrea@email.com"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-USER-004 / BR-USER-005: una fecha de nacimiento futura lanza BusinessRuleException y no guarda")
    void registerThrowsWhenBirthDateIsInTheFuture() {
        // ARRANGE
        LocalDate tomorrow = TODAY.plusDays(1); // DATO DE PRUEBA
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.register(requestFrom(andrea(), tomorrow)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Birth date cannot be in the future: " + tomorrow);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("BR-USER-005 / U-2a: una fecha de nacimiento igual a hoy no es futura y se acepta")
    void registerAcceptsBirthDateEqualToToday() {
        // ARRANGE
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);

        // ACT
        userService.register(requestFrom(andrea(), TODAY)); // DATO DE PRUEBA: límite

        // ASSERT
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getProfile().getBirthDate()).isEqualTo(TODAY);
    }

    // ---------------------------------------------------------------- findByEmail

    @Test
    @DisplayName("FR-SVC-011: findByEmail busca ignorando mayúsculas y devuelve el UserResponse")
    void findByEmailSearchesIgnoringCase() {
        // ARRANGE
        User user = andrea();
        UserResponse expected = userResponseOf(user, 100L); // DATO DE PRUEBA: id
        when(userRepository.findByEmailIgnoreCase("ANDREA@EMAIL.COM")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        // ACT
        UserResponse response = userService.findByEmail("ANDREA@EMAIL.COM");

        // ASSERT
        assertThat(response).isEqualTo(expected);
        verify(userRepository).findByEmailIgnoreCase(eq("ANDREA@EMAIL.COM"));
    }

    @Test
    @DisplayName("FR-SVC-011: findByEmail lanza ResourceNotFoundException si el usuario no existe")
    void findByEmailThrowsWhenUserDoesNotExist() {
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase("nadie@email.com")).thenReturn(Optional.empty()); // DATO DE PRUEBA

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.findByEmail("nadie@email.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: nadie@email.com");
        verify(userMapper, never()).toResponse(any());
    }

    // ---------------------------------------------------------------- findByUsername

    @Test
    @DisplayName("FR-SVC-012: findByUsername devuelve el UserResponse del usuario encontrado")
    void findByUsernameReturnsMappedUser() {
        // ARRANGE
        User user = andrea();
        UserResponse expected = userResponseOf(user, 100L); // DATO DE PRUEBA: id
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        // ACT
        UserResponse response = userService.findByUsername("andrea");

        // ASSERT
        assertThat(response).isEqualTo(expected);
        verify(userRepository).findByUsername(eq("andrea"));
    }

    @Test
    @DisplayName("FR-SVC-012: findByUsername lanza ResourceNotFoundException si el usuario no existe")
    void findByUsernameThrowsWhenUserDoesNotExist() {
        // ARRANGE
        when(userRepository.findByUsername("inexistente")).thenReturn(Optional.empty()); // DATO DE PRUEBA

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.findByUsername("inexistente"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: inexistente");
        verify(userMapper, never()).toResponse(any());
    }

    // ---------------------------------------------------------------- Usuarios inactivos

    @Test
    @DisplayName("U-4a: findByEmail también devuelve un usuario inactivo (Miguel, §47), sin excepción")
    void findByEmailReturnsInactiveUser() {
        // ARRANGE
        User user = miguel();
        UserResponse expected = userResponseOf(user, 103L); // DATO DE PRUEBA: id
        when(userRepository.findByEmailIgnoreCase("miguel@email.com")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        // ACT
        UserResponse response = userService.findByEmail("miguel@email.com");

        // ASSERT
        assertThat(response).isEqualTo(expected);
        assertThat(response.active()).isFalse();
    }

    // ---------------------------------------------------------------- Auxiliares

    /** Request con los datos del usuario de ServiceFixtures (PRD §47 y DATO DE PRUEBA ya definidos). */
    private static RegisterUserRequest requestFrom(User user) {
        return requestFrom(user, user.getProfile().getBirthDate());
    }

    /** Request con los datos del usuario de ServiceFixtures y la fecha de nacimiento indicada. */
    private static RegisterUserRequest requestFrom(User user, LocalDate birthDate) {
        UserProfile profile = user.getProfile();
        return new RegisterUserRequest(user.getUsername(), user.getEmail(), profile.getFirstName(),
                profile.getLastName(), profile.getPhone(), profile.getCity(), birthDate, PASSWORD);
    }

    /** Respuesta que devuelve el mapper simulado; los valores no los calcula el servicio. */
    private static UserResponse userResponseOf(User user, Long id) {
        UserProfile profile = user.getProfile();
        return new UserResponse(id, user.getUsername(), user.getEmail(), user.getActive(),
                profile.getFirstName(), profile.getLastName(), profile.getPhone(), profile.getCity(),
                profile.getBirthDate());
    }
}
