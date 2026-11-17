package com.pulsepass.controller;

import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.service.VenueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de venues (PRD Controladores §7.1, §8.1; FR-CTRL-VEN-001..002).
 * <p>
 * Controlador delgado: solo traduce HTTP a llamadas de {@link VenueService} y devuelve sus DTO
 * (CTRL-001..004, CTRL-009). No accede a repositorios ni contiene reglas de negocio. Los errores
 * los traduce {@code GlobalExceptionHandler}: un venue inexistente llega como
 * {@code ResourceNotFoundException} y responde 404 (BR-VENUE-001).
 * <p>
 * Spring da prioridad a la ruta literal {@code /active} sobre la variable {@code /{code}}, así
 * que {@code GET /api/venues/active} siempre llama a {@code findActiveVenues()}.
 */
@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    /** {@code GET /api/venues/{code}} → 200 con el venue (FR-CTRL-VEN-001). */
    @GetMapping("/{code}")
    public ResponseEntity<VenueResponse> findByCode(@PathVariable String code) {
        return ResponseEntity.ok(venueService.findByCode(code));
    }

    /** {@code GET /api/venues/active} → 200 con los venues activos (FR-CTRL-VEN-002). */
    @GetMapping("/active")
    public ResponseEntity<List<VenueResponse>> findActiveVenues() {
        return ResponseEntity.ok(venueService.findActiveVenues());
    }
}
