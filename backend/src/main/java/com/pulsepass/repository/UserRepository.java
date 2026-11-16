package com.pulsepass.repository;

import com.pulsepass.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acceso a datos de {@link User} (PRD Persistencia §14, FR-USR-001).
 * findById se hereda de JpaRepository. El perfil se persiste en cascada
 * desde User, por lo que no existe UserProfileRepository (P1-D3).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Usuario por email, ignorando mayúsculas (§14, A11). */
    Optional<User> findByEmailIgnoreCase(String email);

    /** Usuario por username exacto (PRD Servicios §32, P2-D6). */
    Optional<User> findByUsername(String username);

    /** Indica si ya existe un usuario con ese email, ignorando mayúsculas (§32, BR-USER-002, A11). */
    boolean existsByEmailIgnoreCase(String email);

    /** Indica si ya existe un usuario con ese username exacto (§32, BR-USER-001, P2-D6). */
    boolean existsByUsername(String username);
}
