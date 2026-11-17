package com.pulsepass.controller;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de eventos (PRD Controladores §7.2, §8.2; FR-CTRL-EVT-001..006).
 * <p>
 * Controlador delgado: solo traduce HTTP a llamadas de {@link EventService} y devuelve sus DTO
 * (CTRL-001..004, CTRL-009).
 * <ul>
 *   <li>Solo la creación lleva cuerpo, validado con {@code @Valid} (§11.1, CTRL-005); un
 *       request inválido responde 400 y no llega al servicio.</li>
 *   <li>Las reglas de creación, publicación y asociación (BR-EVENT-001..011) y el filtro de
 *       eventos publicados de {@code findByArtist} (P2-D15) son del servicio; el handler
 *       traduce sus excepciones a 404 o 409.</li>
 *   <li>Los parámetros llegan al servicio sin transformarse (P3-D9). Un {@code artistId} no
 *       numérico o la falta de {@code stageName} responden 400 (P3-D3).</li>
 * </ul>
 * El 201 no incluye la cabecera {@code Location} (P3-D8). Asociar un artista responde 200, como
 * fija el PRD (§8.2). {@code GET /api/events/{eventCode}/tickets/paid} no está aquí: pertenece a
 * {@code TicketController} (P3-D2). Las rutas literales {@code /published} y
 * {@code /by-artist} tienen prioridad sobre {@code /{eventCode}}.
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /** {@code POST /api/events} → 201 con el evento creado en DRAFT (FR-CTRL-EVT-001). */
    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request));
    }

    /** {@code GET /api/events/{eventCode}} → 200 con el evento (FR-CTRL-EVT-002). */
    @GetMapping("/{eventCode}")
    public ResponseEntity<EventResponse> findByCode(@PathVariable String eventCode) {
        return ResponseEntity.ok(eventService.findByCode(eventCode));
    }

    /** {@code GET /api/events/published} → 200 con los eventos publicados (FR-CTRL-EVT-003). */
    @GetMapping("/published")
    public ResponseEntity<List<EventSummaryResponse>> findPublishedEvents() {
        return ResponseEntity.ok(eventService.findPublishedEvents());
    }

    /** {@code PATCH /api/events/{eventCode}/publish} → 200 con el evento publicado (FR-CTRL-EVT-004). */
    @PatchMapping("/{eventCode}/publish")
    public ResponseEntity<EventResponse> publish(@PathVariable String eventCode) {
        return ResponseEntity.ok(eventService.publish(eventCode));
    }

    /**
     * {@code POST /api/events/{eventCode}/artists/{artistId}} → 200 con el evento actualizado
     * (FR-CTRL-EVT-005).
     */
    @PostMapping("/{eventCode}/artists/{artistId}")
    public ResponseEntity<EventResponse> addArtist(@PathVariable String eventCode, @PathVariable Long artistId) {
        return ResponseEntity.ok(eventService.addArtist(eventCode, artistId));
    }

    /** {@code GET /api/events/by-artist?stageName=...} → 200 con los eventos del artista (FR-CTRL-EVT-006). */
    @GetMapping("/by-artist")
    public ResponseEntity<List<EventSummaryResponse>> findByArtist(@RequestParam String stageName) {
        return ResponseEntity.ok(eventService.findByArtist(stageName));
    }
}
