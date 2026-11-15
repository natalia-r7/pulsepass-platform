package com.pulsepass.repository;

import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Acceso a datos de {@link Ticket} (PRD Persistencia §14, FR-TKT-*, FR-SRC-004).
 * La consulta JPQL de conteo (FR-TKT-008) se agrega en P1-10.
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
}
