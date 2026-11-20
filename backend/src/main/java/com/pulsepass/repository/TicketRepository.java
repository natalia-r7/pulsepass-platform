package com.pulsepass.repository;

import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de {@link Ticket} (PRD Persistencia §14, FR-TKT-*, FR-SRC-004).
 * Incluye query methods y la consulta JPQL de conteo por evento y estado (FR-TKT-008).
 */
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /** Tickets de un usuario, navegando Ticket -> User.email, ignorando mayúsculas (FR-TKT-006). */
    List<Ticket> findByUser_EmailIgnoreCase(String email);

    /** Tickets de un usuario filtrados por estado (FR-TKT-006). */
    List<Ticket> findByUser_EmailIgnoreCaseAndStatus(String email, TicketStatus status);

    /**
     * Tickets de un evento en un estado, navegando Ticket -> Event.eventCode (FR-TKT-007).
     * Query method con el estado como parámetro: se usa con PAID (P1-09-b).
     */
    List<Ticket> findByEvent_EventCodeAndStatus(String eventCode, TicketStatus status);

    /**
     * Tickets cuyo evento es posterior a una fecha (estricto), ordenados por la fecha
     * del evento ascendente (FR-SRC-004, P1-D9, P1-09-e).
     */
    List<Ticket> findByEvent_EventDateAfterOrderByEvent_EventDateAsc(LocalDateTime date);

    /**
     * Cuenta los tickets de un evento en un estado, mediante JPQL con COUNT (FR-TKT-008, AC-008).
     * Se usa con PAID; el estado es parámetro como en FR-TKT-007 (P1-10-b). El nombre sigue la
     * firma del PRD de Servicios; la consulta la define @Query, no el nombre.
     */
    @Query("""
            SELECT COUNT(t)
            FROM Ticket t
            WHERE t.event.eventCode = :eventCode
              AND t.status = :status
            """)
    long countByEventEventCodeAndStatus(@Param("eventCode") String eventCode,
                                        @Param("status") TicketStatus status);

    /** Ticket por su código (PRD Servicios §32, P2-D6). */
    Optional<Ticket> findByTicketCode(String ticketCode);

    /**
     * Tickets de un usuario, ignorando mayúsculas en el email, del más reciente al más antiguo
     * por fecha de compra (PRD Servicios §32, P2-D6, P2-D7).
     *
     * <p>Usa {@code @Query} porque en PostgreSQL {@code DESC} pone los NULL primero, y los
     * tickets sin fecha de compra (RESERVED, P1-D1) deben ir al final: {@code NULLS LAST}
     * (JPQL 3.2). No hay segundo criterio de orden (R-C1). El nombre sigue la firma del PRD; la
     * consulta la define {@code @Query}, no el nombre.
     */
    @Query("""
            SELECT t
            FROM Ticket t
            WHERE LOWER(t.user.email) = LOWER(:email)
            ORDER BY t.purchaseDate DESC NULLS LAST
            """)
    List<Ticket> findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(@Param("email") String email);
}
