package com.pulsepass.service.impl;

import com.pulsepass.config.ClockConfig;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventCategory;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.pulsepass.service.ServiceFixtures.CMF_2026_EVENT_DATE;
import static com.pulsepass.service.ServiceFixtures.cmf2026;
import static com.pulsepass.service.ServiceFixtures.venSmr01;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link EventServiceImpl}, parte 1 (PRD Servicios §11, §14, §43;
 * TEST-EVENT-001..006; P2-09).
 *
 * <p>Servicio real; repositorios y mapper simulados con Mockito (§42, P2-07 V-1a). Sin Spring,
 * sin PostgreSQL y sin Testcontainers (NFR-001). El reloj es fijo ({@code Clock.fixed}), así
 * que el resultado no depende del día en que se ejecuten las pruebas (P2-D4).
 */
@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    /** DATO DE PRUEBA: «ahora» para todas las pruebas, anterior a CMF_2026_EVENT_DATE. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 15, 12, 0);

    private static final Clock FIXED_CLOCK = Clock.fixed(
            NOW.atZone(ClockConfig.APPLICATION_ZONE).toInstant(), ClockConfig.APPLICATION_ZONE);

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private EventMapper eventMapper;

    private EventServiceImpl eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventServiceImpl(eventRepository, venueRepository, eventMapper, FIXED_CLOCK);
    }

    // ---------------------------------------------------------------- findByCode

    @Test
    @DisplayName("TEST-EVENT-001 / FR-SVC-004: findByCode devuelve el EventResponse del evento encontrado")
    void findByCodeReturnsMappedEvent() {
        // ARRANGE
        Event event = cmf2026(venSmr01());
        EventResponse expected = eventResponse();
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(expected);

        // ACT
        EventResponse response = eventService.findByCode("CMF-2026");

        // ASSERT
        assertThat(response).isEqualTo(expected);
        verify(eventRepository).findByEventCode(eq("CMF-2026"));
    }

    @Test
    @DisplayName("TEST-EVENT-002: findByCode lanza ResourceNotFoundException si el evento no existe")
    void findByCodeThrowsWhenEventDoesNotExist() {
        // ARRANGE
        when(eventRepository.findByEventCode("XXX-0000")).thenReturn(Optional.empty()); // DATO DE PRUEBA

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.findByCode("XXX-0000"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found: XXX-0000");
        verify(eventMapper, never()).toResponse(any());
    }

    // ---------------------------------------------------------------- create

    @Test
    @DisplayName("TEST-EVENT-003 / BR-EVENT-005: crear un evento válido ejecuta save() con estado DRAFT")
    void createSavesValidEventAsDraft() {
        // ARRANGE
        Venue venue = venSmr01();
        EventResponse expected = eventResponse();
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(expected);

        // ACT
        EventResponse response = eventService.create(validRequest());

        // ASSERT
        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(saved.capture());
        Event event = saved.getValue();
        assertThat(event.getEventCode()).isEqualTo("CMF-2026");
        assertThat(event.getName()).isEqualTo("Caribbean Music Fest 2026");
        assertThat(event.getDescription()).isEqualTo("Descripción de prueba");
        assertThat(event.getCategory()).isEqualTo(EventCategory.MUSIC);
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(event.getEventDate()).isEqualTo(CMF_2026_EVENT_DATE);
        assertThat(event.getMinimumAge()).isEqualTo(18);
        assertThat(event.getVenue()).isSameAs(venue);
        assertThat(response).isEqualTo(expected);
        verify(eventMapper).toResponse(event);
    }

    @Test
    @DisplayName("BR-EVENT-001: un eventCode repetido lanza DuplicateResourceException y no guarda")
    void createThrowsWhenEventCodeAlreadyExists() {
        // ARRANGE
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(validRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Event already exists: CMF-2026");
        verify(venueRepository, never()).findByCode(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-EVENT-004 / BR-EVENT-002: un venue inexistente lanza ResourceNotFoundException y save() nunca se ejecuta")
    void createThrowsWhenVenueDoesNotExist() {
        // ARRANGE
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(validRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Venue not found: VEN-SMR-01");
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-EVENT-005 / BR-EVENT-003: un venue inactivo lanza BusinessRuleException y no guarda")
    void createThrowsWhenVenueIsInactive() {
        // ARRANGE
        Venue venue = venSmr01();
        venue.setActive(false); // DATO DE PRUEBA
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(validRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Venue is not active: VEN-SMR-01");
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-EVENT-006 / BR-EVENT-004: una fecha pasada lanza BusinessRuleException y no guarda")
    void createThrowsWhenEventDateIsInThePast() {
        // ARRANGE
        LocalDateTime pastDate = NOW.minusDays(1); // DATO DE PRUEBA
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venSmr01()));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(requestWith(pastDate, 18)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Event date must be in the future: " + pastDate);
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("BR-EVENT-004 / P2-D4: una fecha igual a «ahora» no es futura (comparación estricta) y no guarda")
    void createThrowsWhenEventDateEqualsNow() {
        // ARRANGE
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venSmr01()));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(requestWith(NOW, 18)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Event date must be in the future: " + NOW);
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("BR-EVENT-006 / P2-D5: una edad mínima negativa lanza BusinessRuleException y no guarda")
    void createThrowsWhenMinimumAgeIsNegative() {
        // ARRANGE
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venSmr01()));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(requestWith(CMF_2026_EVENT_DATE, -1))) // DATO DE PRUEBA
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Minimum age must be greater than or equal to 0: -1");
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("BR-EVENT-006: una edad mínima de 0 (sin restricción) se acepta y se guarda")
    void createAcceptsMinimumAgeZero() {
        // ARRANGE
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venSmr01()));

        // ACT
        eventService.create(requestWith(CMF_2026_EVENT_DATE, 0));

        // ASSERT
        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(saved.capture());
        assertThat(saved.getValue().getMinimumAge()).isZero();
    }

    // ---------------------------------------------------------------- findPublishedEvents

    @Test
    @DisplayName("FR-SVC-005 / E-4a: findPublishedEvents consulta solo PUBLISHED, usa toSummary y conserva el orden")
    void findPublishedEventsReturnsPublishedSummariesInRepositoryOrder() {
        // ARRANGE
        Venue venue = venSmr01();
        Event first = cmf2026(venue);
        first.setStatus(EventStatus.PUBLISHED);
        Event second = new Event("TST-2027", "Evento de Prueba 2027", EventCategory.CULTURE,       // DATO DE PRUEBA
                EventStatus.PUBLISHED, LocalDateTime.of(2027, 3, 1, 19, 0), 0, venue);         // DATO DE PRUEBA
        EventSummaryResponse firstSummary = new EventSummaryResponse(1000L, "CMF-2026",
                "Caribbean Music Fest 2026", EventCategory.MUSIC, EventStatus.PUBLISHED,
                CMF_2026_EVENT_DATE, "VEN-SMR-01", "Marina Convention Center");
        EventSummaryResponse secondSummary = new EventSummaryResponse(1001L, "TST-2027",
                "Evento de Prueba 2027", EventCategory.CULTURE, EventStatus.PUBLISHED,
                LocalDateTime.of(2027, 3, 1, 19, 0), "VEN-SMR-01", "Marina Convention Center");
        when(eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED))
                .thenReturn(List.of(first, second));
        when(eventMapper.toSummary(first)).thenReturn(firstSummary);
        when(eventMapper.toSummary(second)).thenReturn(secondSummary);

        // ACT
        List<EventSummaryResponse> responses = eventService.findPublishedEvents();

        // ASSERT
        assertThat(responses).containsExactly(firstSummary, secondSummary);
        verify(eventRepository).findByStatusOrderByEventDateAsc(eq(EventStatus.PUBLISHED));
        verify(eventMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("FR-SVC-005: findPublishedEvents devuelve una lista vacía si no hay eventos publicados")
    void findPublishedEventsReturnsEmptyListWhenNonePublished() {
        // ARRANGE
        when(eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)).thenReturn(List.of());

        // ACT
        List<EventSummaryResponse> responses = eventService.findPublishedEvents();

        // ASSERT
        assertThat(responses).isEmpty();
        verify(eventMapper, never()).toSummary(any());
    }

    // ---------------------------------------------------------------- Auxiliares

    /** Request válido para CMF-2026 (PRD §47) en VEN-SMR-01. */
    private static CreateEventRequest validRequest() {
        return requestWith(CMF_2026_EVENT_DATE, 18);
    }

    /** Request de CMF-2026 con la fecha y la edad mínima indicadas. */
    private static CreateEventRequest requestWith(LocalDateTime eventDate, Integer minimumAge) {
        return new CreateEventRequest(
                "CMF-2026",                   // PRD §47
                "Caribbean Music Fest 2026",  // PRD §47
                "Descripción de prueba",      // DATO DE PRUEBA
                EventCategory.MUSIC,          // PRD §47
                eventDate,
                minimumAge,
                "VEN-SMR-01");                // PRD §47
    }

    /** Respuesta que devuelve el mapper simulado; los valores no los calcula el servicio. */
    private static EventResponse eventResponse() {
        return new EventResponse(1000L, "CMF-2026", "Caribbean Music Fest 2026", // DATO DE PRUEBA: id
                "Descripción de prueba", EventCategory.MUSIC, EventStatus.DRAFT, CMF_2026_EVENT_DATE,
                18, "VEN-SMR-01", "Marina Convention Center", List.of());
    }
}
