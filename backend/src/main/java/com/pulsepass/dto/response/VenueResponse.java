package com.pulsepass.dto.response;

/**
 * Vista de un venue que devuelven los servicios (PRD Servicios §8, §33;
 * P2-D1: refleja los campos de la entidad {@code Venue}).
 */
public record VenueResponse(
        Long id,
        String code,
        String name,
        String city,
        String address,
        Integer capacity,
        Boolean active
) {
}
