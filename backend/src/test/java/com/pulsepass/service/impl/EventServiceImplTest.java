package com.pulsepass.service.impl;

import com.pulsepass.config.ClockConfig;
import com.pulsepass.domain.Artist;
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
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.pulsepass.service.ServiceFixtures.CMF_2026_EVENT_DATE;
import static com.pulsepass.service.ServiceFixtures.caribbeanSound;
import static com.pulsepass.service.ServiceFixtures.cmf2026;
import static com.pulsepass.service.ServiceFixtures.neonWaves;
import static com.pulsepass.service.ServiceFixtures.solarBeat;
import static com.pulsepass.service.ServiceFixtures.venSmr01;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link EventServiceImpl} (PRD Servicios §11, §14, §15, §16, §43).
 * Parte 1 (P2-09): TEST-EVENT-001..006. Parte 2 (P2-10): TEST-EVENT-007..008,
 * BR-EVENT-007..011 y FR-SVC-008.
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
    private ArtistRepository artistRepository;

    @Mock
    private EventMapper eventMapper;

    private EventServiceImpl eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventServiceImpl(
                eventRepository, venueRepository, artistRepository, eventMapper, FIXED_CLOCK);
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

    // ---------------------------------------------------------------- publish (P2-10)

    @Test
    @DisplayName("TEST-EVENT-007 / FR-SVC-006: publicar un evento DRAFT válido lo deja en PUBLISHED y lo guarda")
    void publishMovesDraftEventToPublished() {
        // ARRANGE
        Event event = cmf2026(venSmr01());
        EventResponse expected = eventResponse();
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(expected);

        // ACT
        EventResponse response = eventService.publish("CMF-2026");

        // ASSERT
        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("FR-SVC-006: publicar un evento inexistente lanza ResourceNotFoundException y no guarda")
    void publishThrowsWhenEventDoesNotExist() {
        // ARRANGE
        when(eventRepository.findByEventCode("XXX-0000")).thenReturn(Optional.empty()); // DATO DE PRUEBA

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.publish("XXX-0000"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found: XXX-0000");
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-EVENT-008 / BR-EVENT-007: publicar un evento CANCELLED lanza BusinessRuleException y no persiste")
    void publishThrowsWhenEventIsCancelled() {
        // ARRANGE
        Event event = cmf2026(venSmr01());
        event.setStatus(EventStatus.CANCELLED);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Only DRAFT events can be published: CMF-2026");
        verify(eventRepository, never()).save(any());
        assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELLED);
    }

    @ParameterizedTest
    @EnumSource(value = EventStatus.class, names = {"PUBLISHED", "SOLD_OUT", "FINISHED"})
    @DisplayName("BR-EVENT-007: no se publica un evento que no está en DRAFT; no guarda")
    void publishThrowsWhenEventIsNotDraft(EventStatus status) {
        // ARRANGE
        Event event = cmf2026(venSmr01());
        event.setStatus(status);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Only DRAFT events can be published: CMF-2026");
        verify(eventRepository, never()).save(any());
        assertThat(event.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("BR-EVENT-008 / P2-D4: no se publica un evento cuya fecha ya no es futura; no guarda")
    void publishThrowsWhenEventDateIsNoLongerInTheFuture() {
        // ARRANGE
        Event event = cmf2026(venSmr01());
        event.setEventDate(NOW); // DATO DE PRUEBA: igual a «ahora», no es futura (estricto)
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Event date must be in the future: " + NOW);
        verify(eventRepository, never()).save(any());
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    @DisplayName("BR-EVENT-009: no se publica un evento cuyo venue ya no está activo; no guarda")
    void publishThrowsWhenVenueIsNoLongerActive() {
        // ARRANGE
        Venue venue = venSmr01();
        Event event = cmf2026(venue);
        venue.setActive(false); // DATO DE PRUEBA
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Venue is not active: VEN-SMR-01");
        verify(eventRepository, never()).save(any());
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    // ---------------------------------------------------------------- addArtist (P2-10)

    @Test
    @DisplayName("FR-SVC-007: asociar un artista a un evento DRAFT lo agrega y guarda el evento")
    void addArtistAssociatesArtistAndSaves() {
        // ARRANGE
        Event event = cmf2026(venSmr01());
        Artist artist = artistWithId(solarBeat(), 11L);
        EventResponse expected = eventResponse();
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(11L)).thenReturn(Optional.of(artist));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(expected);

        // ACT
        EventResponse response = eventService.addArtist("CMF-2026", 11L);

        // ASSERT
        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(saved.capture());
        assertThat(saved.getValue().getArtists()).containsExactly(artist);
        assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("AC-003: se pueden asociar los tres artistas de §47 a CMF-2026")
    void addArtistAssociatesTheThreeArtistsOfTheMainScenario() {
        // ARRANGE
        Event event = cmf2026(venSmr01());
        Artist solar = artistWithId(solarBeat(), 11L);
        Artist neon = artistWithId(neonWaves(), 12L);
        Artist caribbean = artistWithId(caribbeanSound(), 13L);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(11L)).thenReturn(Optional.of(solar));
        when(artistRepository.findById(12L)).thenReturn(Optional.of(neon));
        when(artistRepository.findById(13L)).thenReturn(Optional.of(caribbean));

        // ACT
        eventService.addArtist("CMF-2026", 11L);
        eventService.addArtist("CMF-2026", 12L);
        eventService.addArtist("CMF-2026", 13L);

        // ASSERT
        verify(eventRepository, times(3)).save(event);
        assertThat(event.getArtists()).containsExactlyInAnyOrder(solar, neon, caribbean);
    }

    @Test
    @DisplayName("§16: si el evento no existe lanza ResourceNotFoundException, no busca el artista y no guarda")
    void addArtistThrowsWhenEventDoesNotExist() {
        // ARRANGE
        when(eventRepository.findByEventCode("XXX-0000")).thenReturn(Optional.empty()); // DATO DE PRUEBA

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist("XXX-0000", 11L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found: XXX-0000");
        verify(artistRepository, never()).findById(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("§16 / A-2a: si el artista no existe lanza ResourceNotFoundException y no guarda")
    void addArtistThrowsWhenArtistDoesNotExist() {
        // ARRANGE
        Event event = cmf2026(venSmr01());
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(99L)).thenReturn(Optional.empty()); // DATO DE PRUEBA

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist("CMF-2026", 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Artist not found: 99");
        verify(eventRepository, never()).save(any());
        assertThat(event.getArtists()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = EventStatus.class, names = {"CANCELLED", "FINISHED"})
    @DisplayName("BR-EVENT-011: no se agregan artistas a eventos CANCELLED o FINISHED; no guarda")
    void addArtistThrowsWhenEventIsCancelledOrFinished(EventStatus status) {
        // ARRANGE
        Event event = cmf2026(venSmr01());
        event.setStatus(status);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(11L)).thenReturn(Optional.of(artistWithId(solarBeat(), 11L)));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist("CMF-2026", 11L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Artists cannot be added to " + status + " events: CMF-2026");
        verify(eventRepository, never()).save(any());
        assertThat(event.getArtists()).isEmpty();
    }

    @Test
    @DisplayName("BR-EVENT-010 / P2-D5: un artista ya asociado (mismo id) lanza BusinessRuleException y no guarda")
    void addArtistThrowsWhenArtistIsAlreadyAssociated() {
        // ARRANGE
        Event event = cmf2026(venSmr01());
        event.addArtist(artistWithId(solarBeat(), 11L));
        // Otra instancia con el mismo id: la detección usa equals por id (P1-D4).
        Artist sameArtist = artistWithId(solarBeat(), 11L);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(11L)).thenReturn(Optional.of(sameArtist));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist("CMF-2026", 11L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Artist already associated with event CMF-2026: Solar Beat");
        verify(eventRepository, never()).save(any());
        assertThat(event.getArtists()).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(value = EventStatus.class, names = {"PUBLISHED", "SOLD_OUT"})
    @DisplayName("BR-EVENT-011: se permite asociar artistas a eventos PUBLISHED y SOLD_OUT")
    void addArtistIsAllowedForPublishedAndSoldOutEvents(EventStatus status) {
        // ARRANGE
        Event event = cmf2026(venSmr01());
        event.setStatus(status);
        Artist artist = artistWithId(solarBeat(), 11L);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(11L)).thenReturn(Optional.of(artist));

        // ACT
        eventService.addArtist("CMF-2026", 11L);

        // ASSERT
        verify(eventRepository).save(event);
        assertThat(event.getArtists()).containsExactly(artist);
    }

    // ---------------------------------------------------------------- findByArtist (P2-10)

    @Test
    @DisplayName("FR-SVC-008 / P2-D15: findByArtist devuelve solo los eventos PUBLISHED de la consulta exacta")
    void findByArtistReturnsOnlyPublishedEvents() {
        // ARRANGE
        Venue venue = venSmr01();
        Event draft = eventWithStatus("TST-DRF", EventStatus.DRAFT, venue);          // DATO DE PRUEBA
        Event published = eventWithStatus("TST-PUB", EventStatus.PUBLISHED, venue);  // DATO DE PRUEBA
        Event soldOut = eventWithStatus("TST-SLD", EventStatus.SOLD_OUT, venue);     // DATO DE PRUEBA
        Event cancelled = eventWithStatus("TST-CAN", EventStatus.CANCELLED, venue);  // DATO DE PRUEBA
        EventSummaryResponse publishedSummary = new EventSummaryResponse(2000L, "TST-PUB", // DATO DE PRUEBA: id
                "Evento de Prueba", EventCategory.MUSIC, EventStatus.PUBLISHED, CMF_2026_EVENT_DATE,
                "VEN-SMR-01", "Marina Convention Center");
        when(eventRepository.findByArtistStageName("Solar Beat"))
                .thenReturn(List.of(draft, published, soldOut, cancelled));
        when(eventMapper.toSummary(published)).thenReturn(publishedSummary);

        // ACT
        List<EventSummaryResponse> responses = eventService.findByArtist("Solar Beat");

        // ASSERT
        assertThat(responses).containsExactly(publishedSummary);
        verify(eventRepository).findByArtistStageName(eq("Solar Beat"));
        verify(eventMapper, times(1)).toSummary(any());
    }

    @Test
    @DisplayName("FR-SVC-008 / F-3a: un artista sin eventos (o inexistente) produce una lista vacía, sin excepción")
    void findByArtistReturnsEmptyListWhenNoEvents() {
        // ARRANGE
        when(eventRepository.findByArtistStageName("Artista Inexistente")).thenReturn(List.of()); // DATO DE PRUEBA

        // ACT
        List<EventSummaryResponse> responses = eventService.findByArtist("Artista Inexistente");

        // ASSERT
        assertThat(responses).isEmpty();
        verify(eventMapper, never()).toSummary(any());
    }

    // ---------------------------------------------------------------- Auxiliares

    /**
     * Asigna el id con ReflectionTestUtils (las entidades no tienen setId, P2-05 D-B1). El id
     * es necesario porque equals de Artist compara por id (P1-D4).
     */
    private static Artist artistWithId(Artist artist, Long id) {
        ReflectionTestUtils.setField(artist, "id", id); // DATO DE PRUEBA
        return artist;
    }

    /** Evento de prueba con el código y el estado indicados. */
    private static Event eventWithStatus(String eventCode, EventStatus status, Venue venue) {
        return new Event(eventCode, "Evento de Prueba", EventCategory.MUSIC, status, // DATO DE PRUEBA
                CMF_2026_EVENT_DATE, 18, venue);
    }

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
