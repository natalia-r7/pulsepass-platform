package com.pulsepass.dto.request;

import com.pulsepass.domain.enums.TicketType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para comprar un ticket (PRD Servicios §22).
 * <p>
 * No incluye {@code price}: el precio no se confía al cliente y lo calcula el
 * sistema (§22; la estrategia de precios se decide en P2-12, P2-D2).
 * <p>
 * Bean Validation (PRD Controladores §11.3; P3-03): campos obligatorios y
 * formato de email según el PRD; longitudes máximas según las columnas de V1
 * ({@code users.email} y {@code events.event_code}, P3-D4); mensajes en inglés
 * (P3-D5). Que el usuario y el evento existan, el estado, la edad y la capacidad
 * son reglas de negocio del servicio (§11.4).
 */
public record PurchaseTicketRequest(
        @NotBlank(message = "User email is required")
        @Email(message = "User email must be a valid email address")
        @Size(max = 254, message = "User email must not exceed 254 characters")
        String userEmail,

        @NotBlank(message = "Event code is required")
        @Size(max = 30, message = "Event code must not exceed 30 characters")
        String eventCode,

        @NotNull(message = "Ticket type is required")
        TicketType type
) {
}
