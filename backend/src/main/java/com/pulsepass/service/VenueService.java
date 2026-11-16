package com.pulsepass.service;

import com.pulsepass.dto.response.VenueResponse;

import java.util.List;

/**
 * Contrato de consulta de venues (PRD Servicios §10.1, FR-SVC-001, FR-SVC-002).
 *
 * <p>Devuelve DTO, nunca la entidad {@code Venue} (SRV-001). La implementación está en
 * {@code service.impl} (SRV-003).
 */
public interface VenueService {

    /**
     * Venue por su código (FR-SVC-001).
     *
     * @throws com.pulsepass.exception.ResourceNotFoundException si no existe (BR-VENUE-001)
     */
    VenueResponse findByCode(String code);

    /** Venues con {@code active = true} (FR-SVC-002, BR-VENUE-002). */
    List<VenueResponse> findActiveVenues();
}
