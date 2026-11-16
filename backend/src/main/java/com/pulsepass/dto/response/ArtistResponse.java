package com.pulsepass.dto.response;

/**
 * Vista de un artista que devuelven los servicios (PRD Servicios §8, §33;
 * P2-D1: refleja los campos de la entidad {@code Artist}).
 */
public record ArtistResponse(
        Long id,
        String stageName,
        String country,
        String genre,
        Boolean active
) {
}
