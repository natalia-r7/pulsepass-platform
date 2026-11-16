package com.pulsepass.service.impl;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementación de {@link EventService}, parte 1 (PRD Servicios §11, §14; P2-09).
 *
 * <ul>
 *   <li>Dependencias {@code final} recibidas por constructor (SRV-002, NFR-002). El
 *       {@link Clock} es el bean de {@code ClockConfig} (P2-D4); en las pruebas se usa
 *       {@code Clock.fixed(...)}.</li>
 *   <li>{@code create} es escritura ({@code @Transactional}); las consultas son
 *       {@code readOnly} (SRV-004, §38, P2-D14).</li>
 *   <li>Solo se aplican reglas de negocio. La validación estructural (campos nulos o vacíos)
 *       pertenece a Bean Validation en la capa de Controladores (P2-D18, P2-09 E-3a).</li>
 *   <li>La conversión a DTO la hace {@link EventMapper} (NFR-004).</li>
 * </ul>
 */
@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    private final VenueRepository venueRepository;

    private final EventMapper eventMapper;

    private final Clock clock;

    public EventServiceImpl(EventRepository eventRepository, VenueRepository venueRepository,
                            EventMapper eventMapper, Clock clock) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.eventMapper = eventMapper;
        this.clock = clock;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Las reglas se validan en el orden del PRD (P2-09 E-6a). Si una falla, se lanza la
     * excepción y no se guarda nada. Mensajes según P2-D17, V-2a y P2-09 E-5a.
     */
    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {
        // BR-EVENT-001: código único.
        if (eventRepository.existsByEventCode(request.eventCode())) {
            throw new DuplicateResourceException("Event already exists: " + request.eventCode());
        }

        // BR-EVENT-002: el venue debe existir. Se usa el repositorio porque hace falta la entidad.
        Venue venue = venueRepository.findByCode(request.venueCode())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + request.venueCode()));

        // BR-EVENT-003: el venue debe estar activo.
        if (!Boolean.TRUE.equals(venue.getActive())) {
            throw new BusinessRuleException("Venue is not active: " + venue.getCode());
        }

        // BR-EVENT-004: fecha futura respecto al Clock inyectado; comparación estricta (P2-D4).
        if (!request.eventDate().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessRuleException("Event date must be in the future: " + request.eventDate());
        }

        // BR-EVENT-006: 0 significa sin restricción de edad (P2-D5).
        if (request.minimumAge() < 0) {
            throw new BusinessRuleException(
                    "Minimum age must be greater than or equal to 0: " + request.minimumAge());
        }

        // BR-EVENT-005: todo evento nuevo inicia en DRAFT; el request no controla el estado.
        Event event = new Event(request.eventCode(), request.name(), request.category(),
                EventStatus.DRAFT, request.eventDate(), request.minimumAge(), venue);
        event.setDescription(request.description());

        // P2-D10: save() explícito.
        return eventMapper.toResponse(eventRepository.save(event));
    }

    /** {@inheritDoc} Mensaje según §35 y V-2a. */
    @Override
    @Transactional(readOnly = true)
    public EventResponse findByCode(String eventCode) {
        return eventRepository.findByEventCode(eventCode)
                .map(eventMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));
    }

    /** {@inheritDoc} Conserva el orden por fecha que da el repositorio (§37). */
    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findPublishedEvents() {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }
}
