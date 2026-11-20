package com.pulsepass.mapper;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import static com.pulsepass.service.ServiceFixtures.CMF_2026_EVENT_DATE;
import static com.pulsepass.service.ServiceFixtures.caribbeanSound;
import static com.pulsepass.service.ServiceFixtures.cmf2026;
import static com.pulsepass.service.ServiceFixtures.neonWaves;
import static com.pulsepass.service.ServiceFixtures.solarBeat;
import static com.pulsepass.service.ServiceFixtures.venSmr01;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias del {@link EventMapper} real (P2-D9, PRD Servicios §13, §33, §37).
 *
 * <p>Sin Spring, sin base de datos y sin mocks. {@code EventMapperImpl} recibe su
 * {@link ArtistMapper} por constructor (P2-05 D-A), así que se crea a mano con el
 * {@code ArtistMapper} real. Los IDs se asignan con {@code ReflectionTestUtils} (D-B).
 */
class EventMapperTest {

    private EventMapper eventMapper;

    private Event event;

    @BeforeEach
    void setUp() {
        eventMapper = new EventMapperImpl(Mappers.getMapper(ArtistMapper.class));

        Venue venue = venSmr01();
        ReflectionTestUtils.setField(venue, "id", 1L);              // DATO DE PRUEBA
        event = cmf2026(venue);
        event.setDescription("Descripción de prueba");              // DATO DE PRUEBA
        ReflectionTestUtils.setField(event, "id", 1000L);           // DATO DE PRUEBA
    }

    @Test
    @DisplayName("§13 / §33: toResponse copia los campos del evento y venue.code / venue.name")
    void toResponseMapsEventAndVenueFields() {
        // ACT
        EventResponse response = eventMapper.toResponse(event);

        // ASSERT
        assertThat(response)
                .extracting(
                        EventResponse::id,
                        EventResponse::eventCode,
                        EventResponse::name,
                        EventResponse::description,
                        EventResponse::category,
                        EventResponse::status,
                        EventResponse::eventDate,
                        EventResponse::minimumAge,
                        EventResponse::venueCode,
                        EventResponse::venueName)
                .containsExactly(
                        1000L,
                        "CMF-2026",
                        "Caribbean Music Fest 2026",
                        "Descripción de prueba",
                        EventCategory.MUSIC,
                        EventStatus.DRAFT,
                        CMF_2026_EVENT_DATE,
                        18,
                        "VEN-SMR-01",
                        "Marina Convention Center");
    }

    @Test
    @DisplayName("P2-D1: los tres artistas de §47 se convierten en ArtistResponse (sin orden, D-C)")
    void toResponseMapsArtistsWithArtistMapper() {
        // ARRANGE
        event.addArtist(artistWithId(solarBeat(), 11L));
        event.addArtist(artistWithId(neonWaves(), 12L));
        event.addArtist(artistWithId(caribbeanSound(), 13L));

        // ACT
        EventResponse response = eventMapper.toResponse(event);

        // ASSERT
        assertThat(response.artists()).containsExactlyInAnyOrder(
                new ArtistResponse(11L, "Solar Beat", null, null, true),
                new ArtistResponse(12L, "Neon Waves", null, null, true),
                new ArtistResponse(13L, "Caribbean Sound", null, null, true));
    }

    @Test
    @DisplayName("Un evento sin artistas produce una lista vacía, no null")
    void toResponseMapsNoArtistsToEmptyList() {
        // ACT
        EventResponse response = eventMapper.toResponse(event);

        // ASSERT
        assertThat(response.artists()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("§37: toSummary copia los 8 campos del resumen")
    void toSummaryMapsSummaryFields() {
        // ACT
        EventSummaryResponse response = eventMapper.toSummary(event);

        // ASSERT
        assertThat(response).isEqualTo(new EventSummaryResponse(
                1000L, "CMF-2026", "Caribbean Music Fest 2026", EventCategory.MUSIC,
                EventStatus.DRAFT, CMF_2026_EVENT_DATE, "VEN-SMR-01", "Marina Convention Center"));
    }

    @Test
    @DisplayName("Un Event null produce null en toResponse y en toSummary")
    void returnsNullForNullEvent() {
        // ACT + ASSERT
        assertThat(eventMapper.toResponse(null)).isNull();
        assertThat(eventMapper.toSummary(null)).isNull();
    }

    /**
     * Asigna el ID antes de agregar el artista al Set del evento: equals/hashCode dependen
     * del ID (P1-D4).
     */
    private static Artist artistWithId(Artist artist, Long id) {
        ReflectionTestUtils.setField(artist, "id", id); // DATO DE PRUEBA
        return artist;
    }
}
