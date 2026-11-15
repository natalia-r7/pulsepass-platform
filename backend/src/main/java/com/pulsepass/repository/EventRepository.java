package com.pulsepass.repository;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de {@link Event} (PRD Persistencia §14, FR-EVT-*, FR-VEN-004).
 * Las consultas JPQL (FR-ART-004, FR-SRC-001..003) se agregan en P1-10.
 */
public interface EventRepository extends JpaRepository<Event, Long> {

    /** Evento por su código de negocio (§14, AC-002). */
    Optional<Event> findByEventCode(String eventCode);

    /** Eventos en un estado, en orden cronológico ascendente (FR-EVT-005, AC-006). */
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    /** Eventos de un venue, navegando Event -> Venue.code (FR-VEN-004). */
    List<Event> findByVenue_Code(String venueCode);
}
