package com.pulsepass.dto.response;

import java.time.LocalDate;

/**
 * Vista de un usuario que devuelven los servicios (PRD Servicios §8, §33;
 * P2-D1).
 * <p>
 * Une en un solo objeto plano los datos de {@code User} ({@code id},
 * {@code username}, {@code email}, {@code active}) y de su
 * {@code UserProfile} (el resto), sin exponer ninguna de las dos entidades.
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        Boolean active,
        String firstName,
        String lastName,
        String phone,
        String city,
        LocalDate birthDate
) {
}
