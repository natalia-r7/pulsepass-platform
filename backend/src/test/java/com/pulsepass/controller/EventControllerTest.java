package com.pulsepass.controller;

import com.pulsepass.FixedClockTestConfiguration;
import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.EventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static com.pulsepass.controller.ControllerFixtures.CARIBBEAN_SOUND;
import static com.pulsepass.controller.ControllerFixtures.CMF_2026_CODE;
import static com.pulsepass.controller.ControllerFixtures.CMF_2026_EVENT_DATE;
import static com.pulsepass.controller.ControllerFixtures.CMF_2026_NAME;
import static com.pulsepass.controller.ControllerFixtures.NEON_WAVES;
import static com.pulsepass.controller.ControllerFixtures.SOLAR_BEAT;
import static com.pulsepass.controller.ControllerFixtures.caribbeanSoundResponse;
import static com.pulsepass.controller.ControllerFixtures.cmf2026CreateRequest;
import static com.pulsepass.controller.ControllerFixtures.cmf2026Response;
import static com.pulsepass.controller.ControllerFixtures.cmf2026SummaryResponse;
import static com.pulsepass.controller.ControllerFixtures.neonWavesResponse;
import static com.pulsepass.controller.ControllerFixtures.solarBeatResponse;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de {@link EventController} (PRD Controladores §8.2, §11.1, §20, §21, §22.2;
 * TEST-CTRL-EVT-001..009, AC-CTRL-003..006; P3-08 EV-1..EV-4).
 * <p>
 * {@code @WebMvcTest} carga solo este controlador y {@code GlobalExceptionHandler}; el servicio
 * es un {@code @MockitoBean} (QT-CTRL-002). Sin base de datos ni Testcontainers (§21,
 * NFR-CTRL-008). {@link FixedClockTestConfiguration} aporta el {@code Clock} que necesita el
 * handler. Los JSON van escritos como texto (P3-D12). El orden de los artistas de un evento no
 * está garantizado (P2-05 D-C1): cuando hay varios, se comparan sin orden (EV-4).
 */
@WebMvcTest(EventController.class)
@Import(FixedClockTestConfiguration.class)
class EventControllerTest {

    /** JSON de la creación de CMF-2026; debe coincidir campo a campo con {@code cmf2026CreateRequest()}. */
    private static final String CMF_2026_CREATE_JSON = """
            {
              "eventCode": "CMF-2026",
              "name": "Caribbean Music Fest 2026",
              "category": "MUSIC",
              "eventDate": "2026-12-12T20:00:00",
              "minimumAge": 18,
              "venueCode": "VEN-SMR-01"
            }
            """;

    /** Fecha de CMF-2026 tal como aparece en el JSON de respuesta. */
    private static final String CMF_2026_EVENT_DATE_JSON = "2026-12-12T20:00:00";

    /** DATO DE PRUEBA: código de un evento que no existe. */
    private static final String UNKNOWN_EVENT_CODE = "XXX-0000";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    // ---------------------------------------------------------------- POST /api/events

    @Test
    @DisplayName("TEST-CTRL-EVT-001 / AC-CTRL-003: creación válida → 201 con el evento en DRAFT y sin Location")
    void createReturns201WithEvent() throws Exception {
        // ARRANGE
        when(eventService.create(eq(cmf2026CreateRequest()))).thenReturn(cmf2026Response(EventStatus.DRAFT));

        // ACT + ASSERT
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CMF_2026_CREATE_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.length()").value(11))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.eventCode").value(CMF_2026_CODE))
                .andExpect(jsonPath("$.name").value(CMF_2026_NAME))
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.category").value("MUSIC"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.eventDate").value(CMF_2026_EVENT_DATE_JSON))
                .andExpect(jsonPath("$.minimumAge").value(18))
                .andExpect(jsonPath("$.venueCode").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.venueName").value("Marina Convention Center"))
                .andExpect(jsonPath("$.artists").isArray())
                .andExpect(jsonPath("$.artists.length()").value(0));

        // EV-1: el record llega igual campo a campo, incluida la fecha deserializada.
        verify(eventService).create(eq(cmf2026CreateRequest()));
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-002 / AC-CTRL-004 / EV-3: cuerpo vacío → 400 con exactamente los 6 campos obligatorios y sin llamar al servicio")
    void createReturns400WhenRequiredFieldsAreMissing() throws Exception {
        // ACT
        ResultActions result = mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));

        // ASSERT
        expectError(result, HttpStatus.BAD_REQUEST, "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(6))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.name").value("Name is required"))
                .andExpect(jsonPath("$.details.category").value("Category is required"))
                .andExpect(jsonPath("$.details.eventDate").value("Event date is required"))
                .andExpect(jsonPath("$.details.minimumAge").value("Minimum age is required"))
                .andExpect(jsonPath("$.details.venueCode").value("Venue code is required"));

        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("§11.1 / QT-CTRL-010: minimumAge = -1 → 400 y no se llama al servicio")
    void createReturns400WhenMinimumAgeIsNegative() throws Exception {
        // ARRANGE
        String json = CMF_2026_CREATE_JSON.replace("\"minimumAge\": 18", "\"minimumAge\": -1"); // DATO DE PRUEBA: límite

        // ACT
        ResultActions result = mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));

        // ASSERT
        expectError(result, HttpStatus.BAD_REQUEST, "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details.minimumAge").value("Minimum age must be greater than or equal to 0"));

        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("BR-EVENT-001: código de evento duplicado → 409 con ErrorResponse")
    void createReturns409WhenEventCodeIsDuplicated() throws Exception {
        // ARRANGE
        when(eventService.create(eq(cmf2026CreateRequest())))
                .thenThrow(new DuplicateResourceException("Event already exists: CMF-2026"));

        // ACT
        ResultActions result = mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CMF_2026_CREATE_JSON));

        // ASSERT
        expectError(result, HttpStatus.CONFLICT, "Event already exists: CMF-2026")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(eventService).create(eq(cmf2026CreateRequest()));
    }

    @Test
    @DisplayName("BR-EVENT-002: venue inexistente → 404 con ErrorResponse")
    void createReturns404WhenVenueDoesNotExist() throws Exception {
        // ARRANGE
        CreateEventRequest request = new CreateEventRequest(CMF_2026_CODE, CMF_2026_NAME, null,
                EventCategory.MUSIC, CMF_2026_EVENT_DATE, 18, "VEN-XXX-99"); // DATO DE PRUEBA: venue
        String json = CMF_2026_CREATE_JSON.replace("\"VEN-SMR-01\"", "\"VEN-XXX-99\"");
        when(eventService.create(eq(request)))
                .thenThrow(new ResourceNotFoundException("Venue not found: VEN-XXX-99"));

        // ACT
        ResultActions result = mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));

        // ASSERT
        expectError(result, HttpStatus.NOT_FOUND, "Venue not found: VEN-XXX-99")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(eventService).create(eq(request));
    }

    // ---------------------------------------------------------------- GET /api/events/{eventCode}

    @Test
    @DisplayName("TEST-CTRL-EVT-003: GET /api/events/CMF-2026 → 200 con el evento y sus artistas (sin orden)")
    void findByCodeReturns200WithEvent() throws Exception {
        // ARRANGE
        when(eventService.findByCode(CMF_2026_CODE)).thenReturn(cmf2026Response(EventStatus.PUBLISHED,
                solarBeatResponse(), neonWavesResponse(), caribbeanSoundResponse()));

        // ACT + ASSERT
        mockMvc.perform(get("/api/events/{eventCode}", CMF_2026_CODE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value(CMF_2026_CODE))
                .andExpect(jsonPath("$.name").value(CMF_2026_NAME))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.eventDate").value(CMF_2026_EVENT_DATE_JSON))
                .andExpect(jsonPath("$.minimumAge").value(18))
                .andExpect(jsonPath("$.venueCode").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.artists.length()").value(3))
                .andExpect(jsonPath("$.artists[*].stageName",
                        containsInAnyOrder(SOLAR_BEAT, NEON_WAVES, CARIBBEAN_SOUND)));

        verify(eventService).findByCode(eq(CMF_2026_CODE));
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-004: evento inexistente → 404 con ErrorResponse")
    void findByCodeReturns404WhenEventDoesNotExist() throws Exception {
        // ARRANGE
        when(eventService.findByCode(UNKNOWN_EVENT_CODE))
                .thenThrow(new ResourceNotFoundException("Event not found: " + UNKNOWN_EVENT_CODE));

        // ACT
        ResultActions result = mockMvc.perform(get("/api/events/{eventCode}", UNKNOWN_EVENT_CODE));

        // ASSERT
        expectError(result, HttpStatus.NOT_FOUND, "Event not found: XXX-0000")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(eventService).findByCode(eq(UNKNOWN_EVENT_CODE));
    }

    // ---------------------------------------------------------------- GET /api/events/published

    @Test
    @DisplayName("TEST-CTRL-EVT-005: GET /api/events/published → 200 con EventSummaryResponse; «published» no se toma como código")
    void findPublishedEventsReturns200WithList() throws Exception {
        // ARRANGE
        when(eventService.findPublishedEvents()).thenReturn(List.of(cmf2026SummaryResponse(EventStatus.PUBLISHED)));

        // ACT + ASSERT
        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].length()").value(8))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].eventCode").value(CMF_2026_CODE))
                .andExpect(jsonPath("$[0].name").value(CMF_2026_NAME))
                .andExpect(jsonPath("$[0].category").value("MUSIC"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$[0].eventDate").value(CMF_2026_EVENT_DATE_JSON))
                .andExpect(jsonPath("$[0].venueCode").value("VEN-SMR-01"))
                .andExpect(jsonPath("$[0].venueName").value("Marina Convention Center"));

        verify(eventService).findPublishedEvents();
        verify(eventService, never()).findByCode(anyString());
    }

    // ---------------------------------------------------------------- PATCH /api/events/{eventCode}/publish

    @Test
    @DisplayName("TEST-CTRL-EVT-006 / AC-CTRL-005: PATCH /api/events/CMF-2026/publish → 200 con el evento PUBLISHED")
    void publishReturns200WithPublishedEvent() throws Exception {
        // ARRANGE
        when(eventService.publish(CMF_2026_CODE)).thenReturn(cmf2026Response(EventStatus.PUBLISHED));

        // ACT + ASSERT
        mockMvc.perform(patch("/api/events/{eventCode}/publish", CMF_2026_CODE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value(CMF_2026_CODE))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).publish(eq(CMF_2026_CODE));
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-007 / AC-CTRL-006: publicar un evento que no está en DRAFT → 409 con ErrorResponse")
    void publishReturns409WhenEventIsNotDraft() throws Exception {
        // ARRANGE
        when(eventService.publish(CMF_2026_CODE))
                .thenThrow(new BusinessRuleException("Only DRAFT events can be published: CMF-2026"));

        // ACT
        ResultActions result = mockMvc.perform(patch("/api/events/{eventCode}/publish", CMF_2026_CODE));

        // ASSERT
        expectError(result, HttpStatus.CONFLICT, "Only DRAFT events can be published: CMF-2026")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(eventService).publish(eq(CMF_2026_CODE));
    }

    @Test
    @DisplayName("FR-CTRL-EVT-004: publicar un evento inexistente → 404 con ErrorResponse")
    void publishReturns404WhenEventDoesNotExist() throws Exception {
        // ARRANGE
        when(eventService.publish(UNKNOWN_EVENT_CODE))
                .thenThrow(new ResourceNotFoundException("Event not found: " + UNKNOWN_EVENT_CODE));

        // ACT
        ResultActions result = mockMvc.perform(patch("/api/events/{eventCode}/publish", UNKNOWN_EVENT_CODE));

        // ASSERT
        expectError(result, HttpStatus.NOT_FOUND, "Event not found: XXX-0000")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(eventService).publish(eq(UNKNOWN_EVENT_CODE));
    }

    // ---------------------------------------------------------------- POST /api/events/{eventCode}/artists/{artistId}

    @Test
    @DisplayName("TEST-CTRL-EVT-008: POST /api/events/CMF-2026/artists/1 → 200 (no 201) con el evento actualizado")
    void addArtistReturns200WithEvent() throws Exception {
        // ARRANGE
        when(eventService.addArtist(CMF_2026_CODE, 1L))
                .thenReturn(cmf2026Response(EventStatus.DRAFT, solarBeatResponse()));

        // ACT + ASSERT
        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", CMF_2026_CODE, 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value(CMF_2026_CODE))
                .andExpect(jsonPath("$.artists.length()").value(1))
                .andExpect(jsonPath("$.artists[0].id").value(1))
                .andExpect(jsonPath("$.artists[0].stageName").value(SOLAR_BEAT));

        verify(eventService).addArtist(eq(CMF_2026_CODE), eq(1L));
    }

    @Test
    @DisplayName("FR-CTRL-EVT-005: artista inexistente → 404 con ErrorResponse")
    void addArtistReturns404WhenArtistDoesNotExist() throws Exception {
        // ARRANGE
        when(eventService.addArtist(CMF_2026_CODE, 99L)) // DATO DE PRUEBA
                .thenThrow(new ResourceNotFoundException("Artist not found: 99"));

        // ACT
        ResultActions result = mockMvc.perform(
                post("/api/events/{eventCode}/artists/{artistId}", CMF_2026_CODE, 99L));

        // ASSERT
        expectError(result, HttpStatus.NOT_FOUND, "Artist not found: 99")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(eventService).addArtist(eq(CMF_2026_CODE), eq(99L));
    }

    @Test
    @DisplayName("BR-EVENT-010: artista ya asociado → 409 con ErrorResponse")
    void addArtistReturns409WhenArtistIsAlreadyAssociated() throws Exception {
        // ARRANGE
        when(eventService.addArtist(CMF_2026_CODE, 1L))
                .thenThrow(new BusinessRuleException("Artist already associated with event CMF-2026: Solar Beat"));

        // ACT
        ResultActions result = mockMvc.perform(
                post("/api/events/{eventCode}/artists/{artistId}", CMF_2026_CODE, 1L));

        // ASSERT
        expectError(result, HttpStatus.CONFLICT, "Artist already associated with event CMF-2026: Solar Beat")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(eventService).addArtist(eq(CMF_2026_CODE), eq(1L));
    }

    @Test
    @DisplayName("QT-CTRL-007: artistId no numérico → 400 y no se llama al servicio")
    void addArtistReturns400WhenArtistIdIsNotNumeric() throws Exception {
        // ACT
        ResultActions result = mockMvc.perform(
                post("/api/events/{eventCode}/artists/{artistId}", CMF_2026_CODE, "abc")); // DATO DE PRUEBA

        // ASSERT
        expectError(result, HttpStatus.BAD_REQUEST, "Invalid value for parameter 'artistId'")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(eventService, never()).addArtist(anyString(), anyLong());
    }

    // ---------------------------------------------------------------- GET /api/events/by-artist

    @Test
    @DisplayName("TEST-CTRL-EVT-009: GET /api/events/by-artist?stageName=Solar Beat → 200; el valor llega sin cambios")
    void findByArtistReturns200WithList() throws Exception {
        // ARRANGE
        when(eventService.findByArtist(SOLAR_BEAT)).thenReturn(List.of(cmf2026SummaryResponse(EventStatus.PUBLISHED)));

        // ACT + ASSERT
        mockMvc.perform(get("/api/events/by-artist").param("stageName", SOLAR_BEAT))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value(CMF_2026_CODE))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(eventService).findByArtist(eq(SOLAR_BEAT));
        verify(eventService, never()).findByCode(anyString());
    }

    @Test
    @DisplayName("QT-CTRL-007: sin el parámetro stageName → 400 y no se llama al servicio")
    void findByArtistReturns400WhenParameterIsMissing() throws Exception {
        // ACT
        ResultActions result = mockMvc.perform(get("/api/events/by-artist"));

        // ASSERT
        expectError(result, HttpStatus.BAD_REQUEST, "Required parameter 'stageName' is missing")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(eventService, never()).findByArtist(anyString());
        verify(eventService, never()).findByCode(anyString());
    }

    @Test
    @DisplayName("FR-CTRL-EVT-006 / F-3a: artista sin eventos publicados → 200 con una lista vacía")
    void findByArtistReturns200WithEmptyList() throws Exception {
        // ARRANGE
        when(eventService.findByArtist(NEON_WAVES)).thenReturn(List.of());

        // ACT + ASSERT
        mockMvc.perform(get("/api/events/by-artist").param("stageName", NEON_WAVES))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(eventService).findByArtist(eq(NEON_WAVES));
    }

    // ---------------------------------------------------------------- Auxiliares

    /**
     * Contrato común de {@code ErrorResponse} (§13): estado, JSON, exactamente 5 claves,
     * {@code timestamp} del reloj fijo, {@code status}, {@code error} y {@code message}.
     */
    private static ResultActions expectError(ResultActions result, HttpStatus status, String message)
            throws Exception {
        return result
                .andExpect(status().is(status.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.timestamp").value(FixedClockTestConfiguration.EXPECTED_TIMESTAMP))
                .andExpect(jsonPath("$.status").value(status.value()))
                .andExpect(jsonPath("$.error").value(status.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(message));
    }
}
