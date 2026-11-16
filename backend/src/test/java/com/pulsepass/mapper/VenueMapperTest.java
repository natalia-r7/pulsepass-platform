package com.pulsepass.mapper;

import com.pulsepass.domain.Venue;
import com.pulsepass.dto.response.VenueResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import static com.pulsepass.service.ServiceFixtures.venSmr01;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias del {@link VenueMapper} real (P2-D9, NFR-004).
 *
 * <p>Sin Spring, sin base de datos y sin mocks: el mapper se obtiene con
 * {@code Mappers.getMapper(...)}. Los IDs se asignan con {@code ReflectionTestUtils}
 * (P2-05 D-B), porque las entidades no tienen {@code setId}.
 */
class VenueMapperTest {

    private final VenueMapper venueMapper = Mappers.getMapper(VenueMapper.class);

    @Test
    @DisplayName("NFR-004: Venue se convierte en VenueResponse con sus 7 campos")
    void mapsAllFields() {
        // ARRANGE
        Venue venue = venSmr01();
        venue.setAddress("Calle de Prueba 123");         // DATO DE PRUEBA
        ReflectionTestUtils.setField(venue, "id", 1L);   // DATO DE PRUEBA

        // ACT
        VenueResponse response = venueMapper.toResponse(venue);

        // ASSERT
        assertThat(response).isEqualTo(new VenueResponse(
                1L, "VEN-SMR-01", "Marina Convention Center", "Santa Marta",
                "Calle de Prueba 123", 3, true));
    }

    @Test
    @DisplayName("V1-3: un venue sin dirección produce address = null")
    void keepsNullAddress() {
        // ARRANGE
        Venue venue = venSmr01();

        // ACT
        VenueResponse response = venueMapper.toResponse(venue);

        // ASSERT
        assertThat(response.address()).isNull();
        assertThat(response.code()).isEqualTo("VEN-SMR-01");
    }

    @Test
    @DisplayName("Un Venue null produce null")
    void returnsNullForNullVenue() {
        // ACT + ASSERT
        assertThat(venueMapper.toResponse(null)).isNull();
    }
}
