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
}
