package com.pulsepass.repository;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de {@link Event} (PRD Persistencia §14, FR-EVT-*, FR-VEN-004).
 * Incluye query methods y las consultas JPQL de búsqueda por artista, ciudad y
 * recomendados (FR-ART-004, FR-SRC-001..003).
 */
public interface EventRepository extends JpaRepository<Event, Long> {

    /** Evento por su código de negocio (§14, AC-002). */
    Optional<Event> findByEventCode(String eventCode);

    /** Indica si ya existe un evento con ese código (PRD Servicios §32, BR-EVENT-001, P2-D6). */
    boolean existsByEventCode(String eventCode);

    /** Eventos en un estado, en orden cronológico ascendente (FR-EVT-005, AC-006). */
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    /** Eventos de un venue, navegando Event -> Venue.code (FR-VEN-004). */
    List<Event> findByVenue_Code(String venueCode);

    /**
     * Eventos en los que participa un artista, por nombre artístico exacto, sin duplicados
     * (FR-ART-004, FR-SRC-001, AC-007, P1-D12).
     */
    @Query("""
            SELECT DISTINCT e
            FROM Event e
            JOIN e.artists a
            WHERE a.stageName = :stageName
            """)
    List<Event> findByArtistStageName(@Param("stageName") String stageName);

    /**
     * Eventos de una ciudad en los que participa un artista; coincidencias exactas y sin
     * ordenamiento (FR-SRC-002, P1-D10). Sin DISTINCT: stage_name es UNIQUE y la PK de
     * event_artists impide pares repetidos, por lo que no pueden aparecer duplicados (P1-10-c).
     */
    @Query("""
            SELECT e
            FROM Event e
            JOIN e.venue v
            JOIN e.artists a
            WHERE v.city = :city
              AND a.stageName = :stageName
            """)
    List<Event> findByVenueCityAndArtistStageName(@Param("city") String city,
                                                  @Param("stageName") String stageName);

    /**
     * Eventos recomendados (FR-SRC-003, P1-D10): PUBLISHED, posteriores a la fecha (estricto),
     * en la ciudad exacta, con un artista cuyo nombre contenga el texto ignorando mayúsculas,
     * sin duplicados y en orden ascendente por fecha. Los comodines % y _ del texto no se
     * escapan (P1-10-e).
     */
    @Query("""
            SELECT DISTINCT e
            FROM Event e
            JOIN e.venue v
            JOIN e.artists a
            WHERE e.status = com.pulsepass.domain.enums.EventStatus.PUBLISHED
              AND e.eventDate > :date
              AND v.city = :city
              AND LOWER(a.stageName) LIKE LOWER(CONCAT('%', :artistText, '%'))
            ORDER BY e.eventDate ASC
            """)
    List<Event> findRecommended(@Param("date") LocalDateTime date,
                                @Param("city") String city,
                                @Param("artistText") String artistText);
}
