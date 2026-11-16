package com.pulsepass.service;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;

import java.util.List;

/**
 * Contrato de eventos (PRD Servicios §11.1).
 *
 * <p>Parte 1 (P2-09): {@code create}, {@code findByCode} y {@code findPublishedEvents}.
 * {@code publish}, {@code addArtist} y {@code findByArtist} se añaden en P2-10, con lo que el
 * contrato quedará completo como en §11.1 (P2-09 E-1a, E-2a).
 *
 * <p>Devuelve DTO, nunca la entidad {@code Event} (SRV-001). La implementación está en
 * {@code service.impl} (SRV-003).
 */
public interface EventService {

    /**
     * Crea un evento en estado DRAFT (FR-SVC-003, BR-EVENT-001..006).
     *
     * @throws com.pulsepass.exception.DuplicateResourceException si ya existe el eventCode (BR-EVENT-001)
     * @throws com.pulsepass.exception.ResourceNotFoundException si el venue no existe (BR-EVENT-002)
     * @throws com.pulsepass.exception.BusinessRuleException si el venue está inactivo (BR-EVENT-003),
     *         la fecha no es futura (BR-EVENT-004) o la edad mínima es negativa (BR-EVENT-006)
     */
    EventResponse create(CreateEventRequest request);

    /**
     * Evento por su código (FR-SVC-004).
     *
     * @throws com.pulsepass.exception.ResourceNotFoundException si no existe
     */
    EventResponse findByCode(String eventCode);

    /** Eventos PUBLISHED en orden ascendente por fecha (FR-SVC-005, P2-09 E-4a). */
    List<EventSummaryResponse> findPublishedEvents();
}
