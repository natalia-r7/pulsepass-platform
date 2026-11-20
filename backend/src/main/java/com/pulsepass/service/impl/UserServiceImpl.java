package com.pulsepass.service.impl;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Implementación de {@link UserService} (PRD Servicios §18–§20).
 *
 * <ul>
 *   <li>Dependencias {@code final} recibidas por constructor (SRV-002, NFR-002). El
 *       {@link Clock} es el bean de {@code ClockConfig} (P2-D4).</li>
 *   <li>{@code register} es escritura ({@code @Transactional}); las consultas son
 *       {@code readOnly} (SRV-004, §38, P2-D14).</li>
 *   <li>Solo se aplican reglas de negocio; la validación estructural pertenece a Bean
 *       Validation en la capa de Controladores (P2-D18).</li>
 *   <li>La conversión a DTO la hace {@link UserMapper} (NFR-004).</li>
 * </ul>
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final Clock clock;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, Clock clock) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.clock = clock;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Orden de validación según la numeración del PRD (P2-11 U-6a). Si una regla falla, se
     * lanza la excepción y no se guarda nada. Mensajes según P2-D17 y P2-11 U-1a. El email se
     * guarda tal como llega (U-3a).
     */
    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        // BR-USER-001: username único (comparación exacta).
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists: " + request.username());
        }

        // BR-USER-002: email único ignorando mayúsculas (A11).
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Email already exists: " + request.email());
        }

        // BR-USER-005: la fecha de nacimiento no puede ser futura; «hoy» se acepta (U-2a).
        if (request.birthDate().isAfter(LocalDate.now(clock))) {
            throw new BusinessRuleException("Birth date cannot be in the future: " + request.birthDate());
        }

        // BR-USER-003: todo usuario nuevo inicia activo.
        User user = new User(request.username(), request.email(), true);

        UserProfile profile = new UserProfile(request.firstName(), request.lastName(), request.birthDate());
        profile.setPhone(request.phone());
        profile.setCity(request.city());

        // setProfile sincroniza los dos lados de la relación 1:1 (P1-D3).
        user.setProfile(profile);

        // BR-USER-004: un único save() dentro de la transacción; el perfil se persiste en
        // cascada (PERSIST) junto con el usuario (P1-D3, P2-D10).
        return userMapper.toResponse(userRepository.save(user));
    }

    /** {@inheritDoc} Mensaje según V-2a. */
    @Override
    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    /** {@inheritDoc} Mensaje según V-2a. */
    @Override
    @Transactional(readOnly = true)
    public UserResponse findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
