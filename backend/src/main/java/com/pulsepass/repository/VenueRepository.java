package com.pulsepass.repository;

import com.pulsepass.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acceso a datos de {@link Venue} (PRD Persistencia §14, FR-VEN-001).
 * findById se hereda de JpaRepository.
 */
public interface VenueRepository extends JpaRepository<Venue, Long> {

    /** Venue por su código de negocio (FR-VEN-001, UC-01, AC-001). */
    Optional<Venue> findByCode(String code);
}
