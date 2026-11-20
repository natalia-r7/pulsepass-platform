package com.pulsepass.service.impl;

import com.pulsepass.domain.Artist;
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
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.AuthorizationService;
import com.pulsepass.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementación de {@link EventService} (PRD Servicios §11, §14, §15, §16). Parte 1 en P2-09
 * ({@code create}, {@code findByCode}, {@code findPublishedEvents}); parte 2 en P2-10
 * ({@code publish}, {@code addArtist}, {@code findByArtist}).
 *
 * <ul>
 *   <li>Dependencias {@code final} recibidas por constructor (SRV-002, NFR-002). El
 *       {@link Clock} es el bean de {@code ClockConfig} (P2-D4); en las pruebas se usa
 *       {@code Clock.fixed(...)}.</li>
 *   <li>{@code create}, {@code publish} y {@code addArtist} son escrituras
 *       ({@code @Transactional}); las consultas son {@code readOnly} (SRV-004, §38, P2-D14).</li>
 *   <li>Solo se aplican reglas de negocio. La validación estructural (campos nulos o vacíos)
 *       pertenece a Bean Validation en la capa de Controladores (P2-D18, P2-09 E-3a).</li>
 *   <li>La conversión a DTO la hace {@link EventMapper} (NFR-004).</li>
 * </ul>
 */
@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    private final VenueRepository venueRepository;

    /** Necesario para {@code addArtist} (P2-10). */
    private final ArtistRepository artistRepository;

    private final EventMapper eventMapper;

    private final Clock clock;

    /** Decide si la petición actual puede ver eventos no publicados (P4-04b, P4-D2). */
    private final AuthorizationService authorizationService;

    public EventServiceImpl(EventRepository eventRepository, VenueRepository venueRepository,
                            ArtistRepository artistRepository, EventMapper eventMapper, Clock clock,
                            AuthorizationService authorizationService) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.eventMapper = eventMapper;
        this.clock = clock;
        this.authorizationService = authorizationService;
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

    /**
     * {@inheritDoc} Mensaje según §35 y V-2a.
     *
     * <p>Visibilidad (PRD Seguridad FR-SEC-EVT-02; P4-D2, P4-D3): solo un evento PUBLISHED es
     * público. Un evento en cualquier otro estado solo lo ven STAFF y ADMIN; para el resto responde
     * exactamente igual que un evento inexistente, para no revelar que existe. La identidad solo se
     * consulta si el evento no está publicado, y el evento oculto nunca se convierte en DTO.
     */
    @Override
    @Transactional(readOnly = true)
    public EventResponse findByCode(String eventCode) {
        Event event = findEventOrThrow(eventCode);
        if (event.getStatus() != EventStatus.PUBLISHED && !authorizationService.isStaffOrAdmin()) {
            throw eventNotFound(eventCode);
        }
        return eventMapper.toResponse(event);
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

    /**
     * {@inheritDoc}
     *
     * <p>Orden de validación según la numeración del PRD (P2-10 F-5a). Si una regla falla, se
     * lanza la excepción y no se guarda nada. Mensajes según P2-10 F-6a; BR-EVENT-008 y 009
     * reutilizan los de {@code create} (P2-09 E-5a).
     */
    @Override
    @Transactional
    public EventResponse publish(String eventCode) {
        Event event = findEventOrThrow(eventCode);

        // BR-EVENT-007: solo se publica desde DRAFT.
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT events can be published: " + eventCode);
        }

        // BR-EVENT-008: la fecha debe seguir siendo futura; comparación estricta (P2-D4).
        if (!event.getEventDate().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessRuleException("Event date must be in the future: " + event.getEventDate());
        }

        // BR-EVENT-009: el venue debe continuar activo.
        if (!Boolean.TRUE.equals(event.getVenue().getActive())) {
            throw new BusinessRuleException("Venue is not active: " + event.getVenue().getCode());
        }

        event.setStatus(EventStatus.PUBLISHED);

        // P2-D10: save() explícito.
        return eventMapper.toResponse(eventRepository.save(event));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sigue los pasos de §16 (P2-10 F-5a): buscar evento, buscar artista, validar el estado
     * del evento (BR-EVENT-011), evitar duplicados (BR-EVENT-010), asociar, guardar y devolver.
     * No se rechazan artistas inactivos (P2-10 F-7a). Se permite en DRAFT, PUBLISHED y SOLD_OUT.
     */
    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {
        Event event = findEventOrThrow(eventCode);

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + artistId));

        // BR-EVENT-011: no se agregan artistas a eventos CANCELLED o FINISHED.
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleException(
                    "Artists cannot be added to " + event.getStatus() + " events: " + eventCode);
        }

        // BR-EVENT-010: contains usa equals por id de Artist (P1-D4).
        if (event.getArtists().contains(artist)) {
            throw new BusinessRuleException(
                    "Artist already associated with event " + eventCode + ": " + artist.getStageName());
        }

        event.addArtist(artist);

        // P2-D10: save() explícito.
        return eventMapper.toResponse(eventRepository.save(event));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reutiliza la consulta exacta {@code findByArtistStageName} (P1-D12, P2-10 F-2a, F-4a)
     * y filtra aquí los PUBLISHED (P2-D15 / F-1a). No añade orden. Un artista sin eventos, o
     * inexistente, produce una lista vacía (F-3a).
     */
    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findByArtist(String stageName) {
        return eventRepository.findByArtistStageName(stageName)
                .stream()
                .filter(event -> event.getStatus() == EventStatus.PUBLISHED)
                .map(eventMapper::toSummary)
                .toList();
    }

    /**
     * Busca el evento o lanza {@code ResourceNotFoundException} (V-2a). Lo usan findByCode, publish y
     * addArtist.
     */
    private Event findEventOrThrow(String eventCode) {
        return eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> eventNotFound(eventCode));
    }

    /**
     * Única fuente del 404 de un evento (V-2a). La usan tanto un evento inexistente como uno oculto,
     * para que la respuesta sea idéntica y no revele que el evento existe (P4-D2).
     */
    private static ResourceNotFoundException eventNotFound(String eventCode) {
        return new ResourceNotFoundException("Event not found: " + eventCode);
    }
}
