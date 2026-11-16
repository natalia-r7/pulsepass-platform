package com.pulsepass.service;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;

import java.util.List;

/**
 * Contrato de eventos (PRD Servicios §11.1).
 *
 * <p>Contrato completo de §11.1. Parte 1 (P2-09): {@code create}, {@code findByCode} y
 * {@code findPublishedEvents}. Parte 2 (P2-10): {@code publish}, {@code addArtist} y
 * {@code findByArtist} (P2-09 E-1a, E-2a).
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

    /**
     * Publica un evento: DRAFT → PUBLISHED (FR-SVC-006, §15, BR-EVENT-007..009).
     *
     * @throws com.pulsepass.exception.ResourceNotFoundException si el evento no existe
     * @throws com.pulsepass.exception.BusinessRuleException si el evento no está en DRAFT
     *         (BR-EVENT-007), su fecha ya no es futura (BR-EVENT-008) o su venue está inactivo
     *         (BR-EVENT-009)
     */
    EventResponse publish(String eventCode);

    /**
     * Asocia un artista existente a un evento (FR-SVC-007, §16, BR-EVENT-010..011).
     *
     * @throws com.pulsepass.exception.ResourceNotFoundException si el evento o el artista no existen
     * @throws com.pulsepass.exception.BusinessRuleException si el evento está CANCELLED o FINISHED
     *         (BR-EVENT-011) o el artista ya está asociado (BR-EVENT-010)
     */
    EventResponse addArtist(String eventCode, Long artistId);

    /**
     * Eventos PUBLISHED en los que participa un artista, por nombre artístico exacto
     * (FR-SVC-008; P2-D15 / P2-10 F-1a, F-4a). Si no hay ninguno, devuelve una lista vacía
     * (F-3a).
     */
    List<EventSummaryResponse> findByArtist(String stageName);
}
