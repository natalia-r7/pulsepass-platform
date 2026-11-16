package com.pulsepass.service;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;

import java.util.List;

/**
 * Contrato de tickets (PRD Servicios §21, FR-SVC-013..018).
 *
 * <p>Parte 1 (P2-13): compra y consultas. Parte 2 (P2-14): {@code cancel} y {@code markAsUsed}.
 * Devuelve DTO, nunca la entidad {@code Ticket} (SRV-001).
 */
public interface TicketService {

    /**
     * Compra un ticket (FR-SVC-013, BR-TICKET-001..009). El precio lo calcula el sistema, no el
     * cliente (§22).
     *
     * @throws com.pulsepass.exception.ResourceNotFoundException si el usuario o el evento no
     *         existen (BR-TICKET-001, BR-TICKET-003)
     * @throws com.pulsepass.exception.BusinessRuleException si el usuario está inactivo, el
     *         evento no está PUBLISHED o no es futuro, el usuario no cumple la edad mínima o no
     *         tiene perfil, o no queda capacidad (BR-TICKET-002, 004..007; P2-D5)
     * @throws IllegalStateException si no se consigue un código de ticket único (P2-13 TK-5)
     */
    TicketResponse purchase(PurchaseTicketRequest request);

    /**
     * Ticket por su código (FR-SVC-014).
     *
     * @throws com.pulsepass.exception.ResourceNotFoundException si no existe
     */
    TicketResponse findByCode(String ticketCode);

    /**
     * Tickets de un usuario, en todos los estados, del más reciente al más antiguo; lista vacía
     * si no hay (FR-SVC-015, P2-D7, P2-13 TK-7).
     */
    List<TicketResponse> findByUserEmail(String email);

    /** Solo los tickets PAID de un evento; lista vacía si no hay (FR-SVC-016, P2-13 TK-7). */
    List<TicketResponse> findPaidTicketsByEvent(String eventCode);

    /**
     * Cancela un ticket: PAID → CANCELLED (FR-SVC-017, BR-TICKET-010..012). No modifica el
     * estado del evento, aunque esté SOLD_OUT (P2-D16 / P2-14 TC-1).
     *
     * @throws com.pulsepass.exception.ResourceNotFoundException si el ticket no existe
     * @throws com.pulsepass.exception.BusinessRuleException si el ticket no está PAID, o si la
     *         fecha del evento no es estrictamente posterior a ahora (P2-D20)
     */
    TicketResponse cancel(String ticketCode);

    /**
     * Marca un ticket como usado: PAID → USED (FR-SVC-018, BR-TICKET-013..014). Sin
     * restricciones de fecha (P2-D19).
     *
     * @throws com.pulsepass.exception.ResourceNotFoundException si el ticket no existe
     * @throws com.pulsepass.exception.BusinessRuleException si el ticket no está PAID
     */
    TicketResponse markAsUsed(String ticketCode);
}
