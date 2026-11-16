package com.pulsepass.mapper;

import com.pulsepass.domain.Artist;
import com.pulsepass.dto.response.ArtistResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import static com.pulsepass.service.ServiceFixtures.solarBeat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias del {@link ArtistMapper} real (P2-D9, NFR-004).
 *
 * <p>Sin Spring, sin base de datos y sin mocks: el mapper se obtiene con
 * {@code Mappers.getMapper(...)}. Los IDs se asignan con {@code ReflectionTestUtils}
 * (P2-05 D-B).
 */
class ArtistMapperTest {

    private final ArtistMapper artistMapper = Mappers.getMapper(ArtistMapper.class);

    @Test
    @DisplayName("NFR-004: Artist se convierte en ArtistResponse con sus 5 campos")
    void mapsAllFields() {
        // ARRANGE
        Artist artist = solarBeat();
        artist.setCountry("Colombia");                    // DATO DE PRUEBA
        artist.setGenre("Electrónica");                   // DATO DE PRUEBA
        ReflectionTestUtils.setField(artist, "id", 10L);  // DATO DE PRUEBA

        // ACT
        ArtistResponse response = artistMapper.toResponse(artist);

        // ASSERT
        assertThat(response).isEqualTo(
                new ArtistResponse(10L, "Solar Beat", "Colombia", "Electrónica", true));
    }

    @Test
    @DisplayName("A8: un artista sin country ni genre produce ambos campos en null")
    void keepsNullCountryAndGenre() {
        // ARRANGE
        Artist artist = solarBeat();

        // ACT
        ArtistResponse response = artistMapper.toResponse(artist);

        // ASSERT
        assertThat(response.country()).isNull();
        assertThat(response.genre()).isNull();
        assertThat(response.stageName()).isEqualTo("Solar Beat");
    }

    @Test
    @DisplayName("Un Artist null produce null")
    void returnsNullForNullArtist() {
        // ACT + ASSERT
        assertThat(artistMapper.toResponse(null)).isNull();
    }
}
