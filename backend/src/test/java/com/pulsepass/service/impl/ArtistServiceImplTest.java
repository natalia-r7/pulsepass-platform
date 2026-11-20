package com.pulsepass.service.impl;

import com.pulsepass.domain.Artist;
import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.ArtistMapper;
import com.pulsepass.repository.ArtistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.pulsepass.service.ServiceFixtures.caribbeanSound;
import static com.pulsepass.service.ServiceFixtures.neonWaves;
import static com.pulsepass.service.ServiceFixtures.solarBeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias mínimas de {@link ArtistServiceImpl} (P2-D11, PRD Servicios §17, §42, §46).
 *
 * <p>Servicio real; repositorio y mapper simulados con Mockito (§42, P2-07 V-1a). Sin Spring,
 * sin PostgreSQL y sin Testcontainers (NFR-001). El servicio se crea con su constructor, como
 * lo hará Spring (SRV-002).
 */
@ExtendWith(MockitoExtension.class)
class ArtistServiceImplTest {

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private ArtistMapper artistMapper;

    private ArtistServiceImpl artistService;

    @BeforeEach
    void setUp() {
        artistService = new ArtistServiceImpl(artistRepository, artistMapper);
    }

    @Test
    @DisplayName("FR-SVC-009: findById devuelve el ArtistResponse del artista encontrado")
    void findByIdReturnsMappedArtist() {
        // ARRANGE
        Artist artist = solarBeat();
        ArtistResponse expected = new ArtistResponse(11L, "Solar Beat", null, null, true); // DATO DE PRUEBA: id
        when(artistRepository.findById(11L)).thenReturn(Optional.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(expected);

        // ACT
        ArtistResponse response = artistService.findById(11L);

        // ASSERT
        assertThat(response).isEqualTo(expected);
        verify(artistRepository).findById(eq(11L));
        verify(artistMapper).toResponse(artist);
    }

    @Test
    @DisplayName("BR-ARTIST-001: findById lanza ResourceNotFoundException si el artista no existe")
    void findByIdThrowsWhenArtistDoesNotExist() {
        // ARRANGE
        when(artistRepository.findById(99L)).thenReturn(Optional.empty()); // DATO DE PRUEBA

        // ACT + ASSERT
        assertThatThrownBy(() -> artistService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Artist not found: 99");
        verify(artistMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("FR-SVC-009 / A-1a: findByStageName busca ignorando mayúsculas y no usa la búsqueda exacta")
    void findByStageNameSearchesIgnoringCase() {
        // ARRANGE
        Artist artist = solarBeat();
        ArtistResponse expected = new ArtistResponse(11L, "Solar Beat", null, null, true); // DATO DE PRUEBA: id
        when(artistRepository.findByStageNameIgnoreCase("solar beat")).thenReturn(Optional.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(expected);

        // ACT
        ArtistResponse response = artistService.findByStageName("solar beat");

        // ASSERT
        assertThat(response).isEqualTo(expected);
        verify(artistRepository).findByStageNameIgnoreCase(eq("solar beat"));
        verify(artistRepository, never()).findByStageName(any());
    }

    @Test
    @DisplayName("BR-ARTIST-001: findByStageName lanza ResourceNotFoundException si el artista no existe")
    void findByStageNameThrowsWhenArtistDoesNotExist() {
        // ARRANGE
        when(artistRepository.findByStageNameIgnoreCase("Artista Inexistente"))   // DATO DE PRUEBA
                .thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> artistService.findByStageName("Artista Inexistente"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Artist not found: Artista Inexistente");
        verify(artistMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("BR-ARTIST-002: findActiveArtists usa la consulta de activos y conserva su orden")
    void findActiveArtistsReturnsActiveArtistsInRepositoryOrder() {
        // ARRANGE
        Artist caribbean = caribbeanSound();
        Artist neon = neonWaves();
        Artist solar = solarBeat();
        // ids: DATO DE PRUEBA
        ArtistResponse caribbeanResponse = new ArtistResponse(13L, "Caribbean Sound", null, null, true);
        ArtistResponse neonResponse = new ArtistResponse(12L, "Neon Waves", null, null, true);
        ArtistResponse solarResponse = new ArtistResponse(11L, "Solar Beat", null, null, true);
        when(artistRepository.findByActiveTrueOrderByStageNameAsc()).thenReturn(List.of(caribbean, neon, solar));
        when(artistMapper.toResponse(caribbean)).thenReturn(caribbeanResponse);
        when(artistMapper.toResponse(neon)).thenReturn(neonResponse);
        when(artistMapper.toResponse(solar)).thenReturn(solarResponse);

        // ACT
        List<ArtistResponse> responses = artistService.findActiveArtists();

        // ASSERT
        assertThat(responses).containsExactly(caribbeanResponse, neonResponse, solarResponse);
        verify(artistRepository).findByActiveTrueOrderByStageNameAsc();
        verify(artistRepository, never()).findAll();
    }

    @Test
    @DisplayName("BR-ARTIST-002: findActiveArtists devuelve una lista vacía si no hay artistas activos")
    void findActiveArtistsReturnsEmptyListWhenNoneActive() {
        // ARRANGE
        when(artistRepository.findByActiveTrueOrderByStageNameAsc()).thenReturn(List.of());

        // ACT
        List<ArtistResponse> responses = artistService.findActiveArtists();

        // ASSERT
        assertThat(responses).isEmpty();
        verify(artistMapper, never()).toResponse(any());
    }
}
