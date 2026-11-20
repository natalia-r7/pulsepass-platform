package com.pulsepass.service;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * Contrato de tickets (PRD Servicios §21, FR-SVC-013..018).
 *
 * <p>Parte 1 (P2-13): compra y consultas. Parte 2 (P2-14): {@code cancel} y {@code markAsUsed}.
 * Devuelve DTO, nunca la entidad {@code Ticket} (SRV-001).
 *
 * <p>Autorización (PRD Seguridad §11, §11.1; P4-D1, P4-D5, P4-D8, P4-08-D): cada operación declara
 * su regla con {@code @PreAuthorize}; la propiedad la comprueba el bean {@code authz} comparando
 * ids persistentes. Solo un USER compra, y solo para sí mismo. El propietario consulta y cancela
 * sus tickets; STAFF y ADMIN consultan cualquiera; ADMIN, además, cancela cualquiera; STAFF y
 * ADMIN listan los PAID de un evento y marcan el uso (STAFF global en el MVP, §11.1). Un ticket
 * ajeno responde 403; uno inexistente, 404. Las reglas de negocio no cambian (BR-TICKET-004 sigue
 * respondiendo 409, P4-08-F).
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
     * @throws org.springframework.security.access.AccessDeniedException si quien compra no es un
     *         USER o el email no es el suyo (FR-SEC-TKT-01)
     */
    @PreAuthorize("hasRole('USER') and @authz.isCurrentUserEmail(#request.userEmail())")
    TicketResponse purchase(PurchaseTicketRequest request);

    /**
     * Ticket por su código (FR-SVC-014).
     *
     * @throws com.pulsepass.exception.ResourceNotFoundException si no existe
     * @throws org.springframework.security.access.AccessDeniedException si no es el propietario
     *         ni STAFF o ADMIN (FR-SEC-TKT-02)
     */
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or @authz.canAccessTicket(#ticketCode)")
    TicketResponse findByCode(String ticketCode);

    /**
     * Tickets de un usuario, en todos los estados, del más reciente al más antiguo; lista vacía
     * si no hay (FR-SVC-015, P2-D7, P2-13 TK-7).
     *
     * @throws org.springframework.security.access.AccessDeniedException si el email no es el del
     *         usuario autenticado y no es ADMIN (FR-SEC-TKT-03)
     */
    @PreAuthorize("hasRole('ADMIN') or @authz.isCurrentUserEmail(#email)")
    List<TicketResponse> findByUserEmail(String email);

    /**
     * Solo los tickets PAID de un evento; lista vacía si no hay (FR-SVC-016, P2-13 TK-7). Solo
     * STAFF o ADMIN (FR-SEC-TKT-04), igual que la regla de URL.
     */
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    List<TicketResponse> findPaidTicketsByEvent(String eventCode);

    /**
     * Cancela un ticket: PAID → CANCELLED (FR-SVC-017, BR-TICKET-010..012). No modifica el
     * estado del evento, aunque esté SOLD_OUT (P2-D16 / P2-14 TC-1).
     *
     * @throws com.pulsepass.exception.ResourceNotFoundException si el ticket no existe
     * @throws com.pulsepass.exception.BusinessRuleException si el ticket no está PAID, o si la
     *         fecha del evento no es estrictamente posterior a ahora (P2-D20)
     * @throws org.springframework.security.access.AccessDeniedException si no es el propietario
     *         ni un ADMIN; STAFF no cancela tickets ajenos (FR-SEC-TKT-05)
     */
    @PreAuthorize("hasRole('ADMIN') or @authz.canAccessTicket(#ticketCode)")
    TicketResponse cancel(String ticketCode);

    /**
     * Marca un ticket como usado: PAID → USED (FR-SVC-018, BR-TICKET-013..014). Sin
     * restricciones de fecha (P2-D19).
     *
     * @throws com.pulsepass.exception.ResourceNotFoundException si el ticket no existe
     * @throws com.pulsepass.exception.BusinessRuleException si el ticket no está PAID
     * @throws org.springframework.security.access.AccessDeniedException si no es STAFF ni ADMIN
     *         (FR-SEC-TKT-06), igual que la regla de URL
     */
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    TicketResponse markAsUsed(String ticketCode);
}
