package com.pulsepass.controller;

import com.pulsepass.FixedClockTestConfiguration;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.ArtistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.pulsepass.controller.ControllerFixtures.CARIBBEAN_SOUND;
import static com.pulsepass.controller.ControllerFixtures.NEON_WAVES;
import static com.pulsepass.controller.ControllerFixtures.SOLAR_BEAT;
import static com.pulsepass.controller.ControllerFixtures.caribbeanSoundResponse;
import static com.pulsepass.controller.ControllerFixtures.neonWavesResponse;
import static com.pulsepass.controller.ControllerFixtures.solarBeatResponse;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de {@link ArtistController} (PRD Controladores §8.3, §21, §22.3;
 * TEST-CTRL-ART-001..004; P3-06 AR-2).
 * <p>
 * {@code @WebMvcTest} carga solo este controlador y {@code GlobalExceptionHandler}; el servicio
 * es un {@code @MockitoBean} (QT-CTRL-002). Sin base de datos ni Testcontainers (§21,
 * NFR-CTRL-008). {@link FixedClockTestConfiguration} aporta el {@code Clock} que necesita el
 * handler.
 */
@WebMvcTest(ArtistController.class)
@Import(FixedClockTestConfiguration.class)
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService artistService;

    // ---------------------------------------------------------------- GET /api/artists/{id}

    @Test
    @DisplayName("TEST-CTRL-ART-001: GET /api/artists/1 → 200 y ArtistResponse")
    void findByIdReturns200WithArtist() throws Exception {
        // ARRANGE
        when(artistService.findById(1L)).thenReturn(solarBeatResponse());

        // ACT + ASSERT
        mockMvc.perform(get("/api/artists/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"))
                .andExpect(jsonPath("$.country").value(nullValue()))
                .andExpect(jsonPath("$.genre").value(nullValue()))
                .andExpect(jsonPath("$.active").value(true));

        verify(artistService).findById(eq(1L));
    }

    @Test
    @DisplayName("TEST-CTRL-ART-002: id inexistente → 404 con ErrorResponse")
    void findByIdReturns404WhenArtistDoesNotExist() throws Exception {
        // ARRANGE
        when(artistService.findById(99L)) // DATO DE PRUEBA
                .thenThrow(new ResourceNotFoundException("Artist not found: 99"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/artists/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.timestamp").value(FixedClockTestConfiguration.EXPECTED_TIMESTAMP))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Artist not found: 99"))
                .andExpect(jsonPath("$.details").isEmpty());

        verify(artistService).findById(eq(99L));
    }

    @Test
    @DisplayName("QT-CTRL-007: un id no numérico → 400 con ErrorResponse y no se llama al servicio")
    void findByIdReturns400WhenIdIsNotNumeric() throws Exception {
        // ACT + ASSERT
        mockMvc.perform(get("/api/artists/{id}", "abc")) // DATO DE PRUEBA
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.timestamp").value(FixedClockTestConfiguration.EXPECTED_TIMESTAMP))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"))
                .andExpect(jsonPath("$.details").isEmpty());

        verify(artistService, never()).findById(anyLong());
        verify(artistService, never()).findByStageName(anyString());
    }

    // ---------------------------------------------------------------- GET /api/artists/by-stage-name

    @Test
    @DisplayName("TEST-CTRL-ART-003: GET /api/artists/by-stage-name?stageName=Solar Beat → 200; el valor llega sin cambios")
    void findByStageNameReturns200WithArtist() throws Exception {
        // ARRANGE
        when(artistService.findByStageName(SOLAR_BEAT)).thenReturn(solarBeatResponse());

        // ACT + ASSERT
        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", SOLAR_BEAT))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"))
                .andExpect(jsonPath("$.country").value(nullValue()))
                .andExpect(jsonPath("$.genre").value(nullValue()))
                .andExpect(jsonPath("$.active").value(true));

        verify(artistService).findByStageName(eq(SOLAR_BEAT));
        verify(artistService, never()).findById(anyLong());
    }

    @Test
    @DisplayName("FR-CTRL-ART-002: nombre artístico inexistente → 404 con ErrorResponse")
    void findByStageNameReturns404WhenArtistDoesNotExist() throws Exception {
        // ARRANGE
        when(artistService.findByStageName("Unknown Artist")) // DATO DE PRUEBA
                .thenThrow(new ResourceNotFoundException("Artist not found: Unknown Artist"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Unknown Artist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.timestamp").value(FixedClockTestConfiguration.EXPECTED_TIMESTAMP))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Artist not found: Unknown Artist"))
                .andExpect(jsonPath("$.details").isEmpty());

        verify(artistService).findByStageName(eq("Unknown Artist"));
    }

    @Test
    @DisplayName("QT-CTRL-007: sin el parámetro stageName → 400 con ErrorResponse y no se llama al servicio")
    void findByStageNameReturns400WhenParameterIsMissing() throws Exception {
        // ACT + ASSERT
        mockMvc.perform(get("/api/artists/by-stage-name"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.timestamp").value(FixedClockTestConfiguration.EXPECTED_TIMESTAMP))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Required parameter 'stageName' is missing"))
                .andExpect(jsonPath("$.details").isEmpty());

        verify(artistService, never()).findById(anyLong());
        verify(artistService, never()).findByStageName(anyString());
    }

    // ---------------------------------------------------------------- GET /api/artists/active

    @Test
    @DisplayName("TEST-CTRL-ART-004: GET /api/artists/active → 200 con la lista en el orden del servicio")
    void findActiveArtistsReturns200WithList() throws Exception {
        // ARRANGE: orden alfabético, como lo devuelve el servicio (BR-ARTIST-002)
        when(artistService.findActiveArtists())
                .thenReturn(List.of(caribbeanSoundResponse(), neonWavesResponse(), solarBeatResponse()));

        // ACT + ASSERT
        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].stageName").value(CARIBBEAN_SOUND))
                .andExpect(jsonPath("$[1].stageName").value(NEON_WAVES))
                .andExpect(jsonPath("$[2].stageName").value(SOLAR_BEAT))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].active").value(true))
                .andExpect(jsonPath("$[2].active").value(true));

        verify(artistService).findActiveArtists();
        verify(artistService, never()).findById(anyLong());
        verify(artistService, never()).findByStageName(anyString());
    }

    @Test
    @DisplayName("FR-CTRL-ART-003: sin artistas activos → 200 con una lista vacía")
    void findActiveArtistsReturns200WithEmptyList() throws Exception {
        // ARRANGE
        when(artistService.findActiveArtists()).thenReturn(List.of());

        // ACT + ASSERT
        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(artistService).findActiveArtists();
    }
}
