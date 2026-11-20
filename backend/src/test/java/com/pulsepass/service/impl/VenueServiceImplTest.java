package com.pulsepass.service.impl;

import com.pulsepass.domain.Venue;
import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.VenueMapper;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.pulsepass.service.ServiceFixtures.venSmr01;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias mínimas de {@link VenueServiceImpl} (P2-D11, PRD Servicios §10, §42, §46).
 *
 * <p>Servicio real; repositorio y mapper simulados con Mockito (§42, P2-07 V-1a). Sin Spring,
 * sin PostgreSQL y sin Testcontainers (NFR-001). El servicio se crea con su constructor, como
 * lo hará Spring (SRV-002).
 */
@ExtendWith(MockitoExtension.class)
class VenueServiceImplTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private VenueMapper venueMapper;

    private VenueServiceImpl venueService;

    @BeforeEach
    void setUp() {
        venueService = new VenueServiceImpl(venueRepository, venueMapper);
    }

    @Test
    @DisplayName("FR-SVC-001: findByCode devuelve el VenueResponse del venue encontrado")
    void findByCodeReturnsMappedVenue() {
        // ARRANGE
        Venue venue = venSmr01();
        VenueResponse expected = new VenueResponse(
                1L, "VEN-SMR-01", "Marina Convention Center", "Santa Marta", null, 3, true);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(venueMapper.toResponse(venue)).thenReturn(expected);

        // ACT
        VenueResponse response = venueService.findByCode("VEN-SMR-01");

        // ASSERT
        assertThat(response).isEqualTo(expected);
        verify(venueRepository).findByCode(eq("VEN-SMR-01"));
        verify(venueMapper).toResponse(venue);
    }

    @Test
    @DisplayName("BR-VENUE-001: findByCode lanza ResourceNotFoundException si el venue no existe")
    void findByCodeThrowsWhenVenueDoesNotExist() {
        // ARRANGE
        when(venueRepository.findByCode("VEN-XXX-00")).thenReturn(Optional.empty()); // DATO DE PRUEBA

        // ACT + ASSERT
        assertThatThrownBy(() -> venueService.findByCode("VEN-XXX-00"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Venue not found: VEN-XXX-00");
        verify(venueMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("FR-SVC-002 / BR-VENUE-002: findActiveVenues usa la consulta de activos y conserva su orden")
    void findActiveVenuesReturnsActiveVenuesInRepositoryOrder() {
        // ARRANGE
        Venue auditorio = new Venue("VEN-TST-AUD", "Auditorio Central", "Santa Marta", 100, true); // DATO DE PRUEBA
        Venue marina = venSmr01();
        VenueResponse auditorioResponse = new VenueResponse(
                2L, "VEN-TST-AUD", "Auditorio Central", "Santa Marta", null, 100, true);    // DATO DE PRUEBA
        VenueResponse marinaResponse = new VenueResponse(
                1L, "VEN-SMR-01", "Marina Convention Center", "Santa Marta", null, 3, true);
        when(venueRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(auditorio, marina));
        when(venueMapper.toResponse(auditorio)).thenReturn(auditorioResponse);
        when(venueMapper.toResponse(marina)).thenReturn(marinaResponse);

        // ACT
        List<VenueResponse> responses = venueService.findActiveVenues();

        // ASSERT
        assertThat(responses).containsExactly(auditorioResponse, marinaResponse);
        verify(venueRepository).findByActiveTrueOrderByNameAsc();
        verify(venueRepository, never()).findAll();
    }

    @Test
    @DisplayName("FR-SVC-002: findActiveVenues devuelve una lista vacía si no hay venues activos")
    void findActiveVenuesReturnsEmptyListWhenNoneActive() {
        // ARRANGE
        when(venueRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of());

        // ACT
        List<VenueResponse> responses = venueService.findActiveVenues();

        // ASSERT
        assertThat(responses).isEmpty();
        verify(venueMapper, never()).toResponse(any());
    }
}
