package com.pulsepass.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;

/**
 * Vista de un ticket que devuelven los servicios (PRD Servicios §23, §34).
 * <p>
 * No expone {@code User} ni {@code Event}: del usuario solo da
 * {@code userEmail}, y del evento, {@code eventCode} y {@code eventName}.
 * {@code purchaseDate} puede ser {@code null} si el ticket está en
 * {@code RESERVED} (P1-D1).
 */
public record TicketResponse(
        Long id,
        String ticketCode,
        TicketType type,
        BigDecimal price,
        TicketStatus status,
        LocalDateTime purchaseDate,
        String userEmail,
        String eventCode,
        String eventName
) {
}
