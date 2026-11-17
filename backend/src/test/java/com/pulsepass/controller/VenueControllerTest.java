package com.pulsepass.controller;

import com.pulsepass.FixedClockTestConfiguration;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.security.WebSecurityTestConfiguration;
import com.pulsepass.service.VenueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.pulsepass.controller.ControllerFixtures.VEN_SMR_01_CODE;
import static com.pulsepass.controller.ControllerFixtures.venSmr01Response;
import static org.hamcrest.Matchers.nullValue;
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
 * Pruebas de {@link VenueController} (PRD Controladores §8.1, §20, §21, §22.1;
 * TEST-CTRL-VEN-001..003, AC-CTRL-001..002).
 * <p>
 * {@code @WebMvcTest} carga solo este controlador y {@code GlobalExceptionHandler}; el servicio
 * es un {@code @MockitoBean} (QT-CTRL-002). Sin base de datos ni Testcontainers (§21,
 * NFR-CTRL-008). {@link FixedClockTestConfiguration} aporta el {@code Clock} que necesita el
 * handler.
 */
@WebMvcTest(VenueController.class)
@Import({FixedClockTestConfiguration.class, WebSecurityTestConfiguration.class})
class VenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueService venueService;

    @Test
    @DisplayName("TEST-CTRL-VEN-001 / AC-CTRL-001: GET /api/venues/VEN-SMR-01 → 200 y VenueResponse")
    void findByCodeReturns200WithVenue() throws Exception {
        // ARRANGE
        when(venueService.findByCode(VEN_SMR_01_CODE)).thenReturn(venSmr01Response());

        // ACT + ASSERT
        mockMvc.perform(get("/api/venues/{code}", VEN_SMR_01_CODE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.name").value("Marina Convention Center"))
                .andExpect(jsonPath("$.city").value("Santa Marta"))
                .andExpect(jsonPath("$.address").value(nullValue()))
                .andExpect(jsonPath("$.capacity").value(3))
                .andExpect(jsonPath("$.active").value(true));

        verify(venueService).findByCode(eq(VEN_SMR_01_CODE));
    }

    @Test
    @DisplayName("TEST-CTRL-VEN-002 / AC-CTRL-002: venue inexistente → 404 con ErrorResponse")
    void findByCodeReturns404WhenVenueDoesNotExist() throws Exception {
        // ARRANGE
        when(venueService.findByCode("VEN-XXX-99")) // DATO DE PRUEBA
                .thenThrow(new ResourceNotFoundException("Venue not found: VEN-XXX-99"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/venues/{code}", "VEN-XXX-99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.timestamp").value(FixedClockTestConfiguration.EXPECTED_TIMESTAMP))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Venue not found: VEN-XXX-99"))
                .andExpect(jsonPath("$.details").isEmpty());

        verify(venueService).findByCode(eq("VEN-XXX-99"));
    }

    @Test
    @DisplayName("TEST-CTRL-VEN-003: GET /api/venues/active → 200 con la lista; no se interpreta «active» como código")
    void findActiveVenuesReturns200WithList() throws Exception {
        // ARRANGE
        when(venueService.findActiveVenues()).thenReturn(List.of(venSmr01Response()));

        // ACT + ASSERT
        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$[0].name").value("Marina Convention Center"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(venueService).findActiveVenues();
        verify(venueService, never()).findByCode(anyString());
    }

    @Test
    @DisplayName("FR-CTRL-VEN-002: sin venues activos → 200 con una lista vacía")
    void findActiveVenuesReturns200WithEmptyList() throws Exception {
        // ARRANGE
        when(venueService.findActiveVenues()).thenReturn(List.of());

        // ACT + ASSERT
        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(venueService).findActiveVenues();
    }
}
