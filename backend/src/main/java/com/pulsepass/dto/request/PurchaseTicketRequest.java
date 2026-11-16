package com.pulsepass.dto.request;

import com.pulsepass.domain.enums.TicketType;

/**
 * Datos para comprar un ticket (PRD Servicios §22).
 * <p>
 * No incluye {@code price}: el precio no se confía al cliente y lo calcula el
 * sistema (§22; la estrategia de precios se decide en P2-12, P2-D2).
 * <p>
 * Sin Bean Validation: las validaciones de entrada corresponden a la Fase 3.
 */
public record PurchaseTicketRequest(
        String userEmail,
        String eventCode,
        TicketType type
) {
}
