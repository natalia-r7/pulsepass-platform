package com.pulsepass.controller;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.service.TicketService;
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
 * Endpoints de tickets (PRD Controladores §7.5, §8.5; FR-CTRL-TKT-001..006).
 * <p>
 * Controlador delgado: solo traduce HTTP a llamadas de {@link TicketService} y devuelve sus DTO
 * (CTRL-001..004, CTRL-009).
 * <ul>
 *   <li>Solo la compra lleva cuerpo, validado con {@code @Valid} (§11.3, CTRL-005); un request
 *       inválido responde 400 y no llega al servicio. El precio no viaja en el request: lo
 *       calcula el servicio.</li>
 *   <li>Las reglas de compra, cancelación y uso (BR-TICKET-001..014) y el filtro de tickets
 *       PAID son del servicio; el handler traduce sus excepciones a 404, 409 o 500.</li>
 *   <li>Los parámetros llegan al servicio sin transformarse (P3-D9). Un usuario o un evento
 *       sin tickets producen una lista vacía con 200 (P2-13 TK-7).</li>
 *   <li>Los {@code PATCH} no declaran cuerpo: si llega uno, se ignora (P3-09 TKC-5).</li>
 * </ul>
 * El prefijo común es {@code /api} (TKC-1) porque {@code GET /api/events/{eventCode}/tickets/paid}
 * también pertenece a este controlador (P3-D2). El 201 no incluye {@code Location} (P3-D8). La
 * ruta literal {@code /tickets/by-user} tiene prioridad sobre {@code /tickets/{ticketCode}}.
 */
@RestController
@RequestMapping("/api")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /** {@code POST /api/tickets} → 201 con el ticket PAID (FR-CTRL-TKT-001). */
    @PostMapping("/tickets")
    public ResponseEntity<TicketResponse> purchase(@Valid @RequestBody PurchaseTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.purchase(request));
    }

    /** {@code GET /api/tickets/{ticketCode}} → 200 con el ticket (FR-CTRL-TKT-002). */
    @GetMapping("/tickets/{ticketCode}")
    public ResponseEntity<TicketResponse> findByCode(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.findByCode(ticketCode));
    }

    /** {@code GET /api/tickets/by-user?email=...} → 200 con los tickets del usuario (FR-CTRL-TKT-003). */
    @GetMapping("/tickets/by-user")
    public ResponseEntity<List<TicketResponse>> findByUserEmail(@RequestParam String email) {
        return ResponseEntity.ok(ticketService.findByUserEmail(email));
    }

    /** {@code GET /api/events/{eventCode}/tickets/paid} → 200 con los tickets PAID del evento (FR-CTRL-TKT-004). */
    @GetMapping("/events/{eventCode}/tickets/paid")
    public ResponseEntity<List<TicketResponse>> findPaidTicketsByEvent(@PathVariable String eventCode) {
        return ResponseEntity.ok(ticketService.findPaidTicketsByEvent(eventCode));
    }

    /** {@code PATCH /api/tickets/{ticketCode}/cancel} → 200 con el ticket CANCELLED (FR-CTRL-TKT-005). */
    @PatchMapping("/tickets/{ticketCode}/cancel")
    public ResponseEntity<TicketResponse> cancel(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.cancel(ticketCode));
    }

    /** {@code PATCH /api/tickets/{ticketCode}/use} → 200 con el ticket USED (FR-CTRL-TKT-006). */
    @PatchMapping("/tickets/{ticketCode}/use")
    public ResponseEntity<TicketResponse> markAsUsed(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.markAsUsed(ticketCode));
    }
}
