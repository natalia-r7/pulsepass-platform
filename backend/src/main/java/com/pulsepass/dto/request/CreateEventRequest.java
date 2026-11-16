package com.pulsepass.dto.request;

import java.time.LocalDateTime;

import com.pulsepass.domain.enums.EventCategory;

/**
 * Datos para crear un evento (PRD Servicios §12).
 * <p>
 * No incluye {@code status}: todo evento nuevo inicia en {@code DRAFT} y el
 * request no controla ese valor (BR-EVENT-005). El venue se indica por su
 * código, no como entidad (SRV-001).
 * <p>
 * Sin Bean Validation: las validaciones de entrada corresponden a la Fase 3.
 */
public record CreateEventRequest(
        String eventCode,
        String name,
        String description,
        EventCategory category,
        LocalDateTime eventDate,
        Integer minimumAge,
        String venueCode
) {
}
