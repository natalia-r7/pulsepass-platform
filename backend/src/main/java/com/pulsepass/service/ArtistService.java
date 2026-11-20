package com.pulsepass.service;

import com.pulsepass.dto.response.ArtistResponse;

import java.util.List;

/**
 * Contrato de consulta de artistas (PRD Servicios §17, FR-SVC-009).
 *
 * <p>Devuelve DTO, nunca la entidad {@code Artist} (SRV-001). La implementación está en
 * {@code service.impl} (SRV-003).
 */
public interface ArtistService {

    /**
     * Artista por su id, esté activo o no (P2-08 A-4a).
     *
     * @throws com.pulsepass.exception.ResourceNotFoundException si no existe (BR-ARTIST-001)
     */
    ArtistResponse findById(Long id);

    /**
     * Artista por nombre artístico, ignorando mayúsculas (P2-08 A-1a), esté activo o no
     * (P2-08 A-4a).
     *
     * @throws com.pulsepass.exception.ResourceNotFoundException si no existe (BR-ARTIST-001)
     */
    ArtistResponse findByStageName(String stageName);

    /** Solo los artistas activos (BR-ARTIST-002). */
    List<ArtistResponse> findActiveArtists();
}
