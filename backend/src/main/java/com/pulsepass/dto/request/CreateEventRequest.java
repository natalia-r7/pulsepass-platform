package com.pulsepass.dto.request;

import java.time.LocalDateTime;

import com.pulsepass.domain.enums.EventCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un evento (PRD Servicios §12).
 * <p>
 * No incluye {@code status}: todo evento nuevo inicia en {@code DRAFT} y el
 * request no controla ese valor (BR-EVENT-005). El venue se indica por su
 * código, no como entidad (SRV-001).
 * <p>
 * Bean Validation (PRD Controladores §11.1; P3-03): campos obligatorios y
 * {@code minimumAge >= 0} según el PRD; longitudes máximas según las columnas de
 * V1 (P3-D4); mensajes en inglés (P3-D5). Que la fecha sea futura, que el venue
 * exista y esté activo, y que el código no se repita son reglas de negocio del
 * servicio (§11.4).
 */
public record CreateEventRequest(
        @NotBlank(message = "Event code is required")
        @Size(max = 30, message = "Event code must not exceed 30 characters")
        String eventCode,

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "Category is required")
        EventCategory category,

        @NotNull(message = "Event date is required")
        LocalDateTime eventDate,

        @NotNull(message = "Minimum age is required")
        @Min(value = 0, message = "Minimum age must be greater than or equal to 0")
        Integer minimumAge,

        @NotBlank(message = "Venue code is required")
        @Size(max = 30, message = "Venue code must not exceed 30 characters")
        String venueCode
) {
}
