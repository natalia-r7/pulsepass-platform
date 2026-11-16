package com.pulsepass.service.impl;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.TicketCodeGenerator;
import com.pulsepass.service.TicketPricingStrategy;
import com.pulsepass.service.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

/**
 * Implementación de {@link TicketService} (PRD Servicios §21–§31, §38, §39). Parte 1 en P2-13
 * ({@code purchase} y consultas); parte 2 en P2-14 ({@code cancel} y {@code markAsUsed}).
 *
 * <ul>
 *   <li>Dependencias {@code final} recibidas por constructor (SRV-002, NFR-002). El precio lo
 *       calcula {@link TicketPricingStrategy} (§28, P2-12) y el código lo genera
 *       {@link TicketCodeGenerator} (P2-D3, TK-4).</li>
 *   <li>{@code purchase}, {@code cancel} y {@code markAsUsed} son escrituras
 *       ({@code @Transactional}); las consultas son {@code readOnly} (SRV-004, §38, P2-D14).</li>
 *   <li>Solo se aplican reglas de negocio; la validación estructural pertenece a Bean
 *       Validation en la capa de Controladores (P2-D18).</li>
 *   <li>La conversión a DTO la hace {@link TicketMapper} (NFR-004).</li>
 * </ul>
 */
@Service
public class TicketServiceImpl implements TicketService {

    /** Máximo de códigos generados antes de rendirse (P2-13 TK-5). */
    private static final int MAX_CODE_ATTEMPTS = 5;

    private final TicketRepository ticketRepository;

    private final UserRepository userRepository;

    private final EventRepository eventRepository;

    private final TicketMapper ticketMapper;

    private final TicketPricingStrategy ticketPricingStrategy;

    private final TicketCodeGenerator ticketCodeGenerator;

    private final Clock clock;

    public TicketServiceImpl(TicketRepository ticketRepository, UserRepository userRepository,
                             EventRepository eventRepository, TicketMapper ticketMapper,
                             TicketPricingStrategy ticketPricingStrategy,
                             TicketCodeGenerator ticketCodeGenerator, Clock clock) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketMapper = ticketMapper;
        this.ticketPricingStrategy = ticketPricingStrategy;
        this.ticketCodeGenerator = ticketCodeGenerator;
        this.clock = clock;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sigue el orden de §24 y §54. Si una regla falla, se lanza la excepción antes de
     * cualquier {@code save()}, y {@code @Transactional} deshace la operación (§39). Mensajes
     * según P2-13 TK-6.
     *
     * <p><b>Limitación conocida (§53):</b> la secuencia contar → validar → guardar no protege
     * contra dos compras simultáneas, que podrían ver el mismo cupo libre. El PRD deja la
     * concurrencia fuera del alcance de esta versión; no se agregan bloqueos.
     */
    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {
        // BR-TICKET-001: el usuario debe existir (email sin distinguir mayúsculas).
        User user = userRepository.findByEmailIgnoreCase(request.userEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userEmail()));

        // BR-TICKET-002: un usuario inactivo no puede comprar.
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessRuleException("User is not active: " + user.getEmail());
        }

        // BR-TICKET-003: el evento debe existir.
        Event event = eventRepository.findByEventCode(request.eventCode())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.eventCode()));

        // BR-TICKET-004: solo se compra en eventos PUBLISHED.
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException(
                    "Tickets can only be purchased for PUBLISHED events: " + event.getEventCode());
        }

        // BR-TICKET-005: el evento no debe haber ocurrido; comparación estricta (P2-D4).
        LocalDateTime now = LocalDateTime.now(clock);
        if (!event.getEventDate().isAfter(now)) {
            throw new BusinessRuleException("Event date must be in the future: " + event.getEventDate());
        }

        // BR-TICKET-006: edad mínima, evaluada en la fecha del evento.
        validateMinimumAge(user, event);

        // BR-TICKET-007: los PAID y los USED ocupan capacidad; los CANCELLED no (TK-1).
        long occupied = countOccupiedTickets(event.getEventCode());
        int capacity = event.getVenue().getCapacity();
        if (occupied >= capacity) {
            throw new BusinessRuleException("Event has no available capacity: " + event.getEventCode());
        }

        // §28: el precio lo calcula la estrategia, nunca el cliente ni este servicio.
        BigDecimal price = ticketPricingStrategy.calculatePrice(request.type());

        // §27: toda compra válida genera un ticket PAID.
        Ticket ticket = new Ticket(generateUniqueTicketCode(), request.type(), price,
                TicketStatus.PAID, user, event);
        ticket.setPurchaseDate(now);

        // P2-D10: save() explícito.
        Ticket saved = ticketRepository.save(ticket);

        // BR-TICKET-008: si esta compra completa la capacidad, el evento pasa a SOLD_OUT en la
        // misma transacción (TK-3: cuenta anterior + 1).
        if (occupied + 1 == capacity) {
            event.setStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }

        return ticketMapper.toResponse(saved);
    }

    /** {@inheritDoc} Mensaje según V-2a / TK-6. */
    @Override
    @Transactional(readOnly = true)
    public TicketResponse findByCode(String ticketCode) {
        return ticketMapper.toResponse(findTicketOrThrow(ticketCode));
    }

    /** {@inheritDoc} Conserva el orden del repositorio (P2-D7). No valida el usuario (TK-7). */
    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(email)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    /** {@inheritDoc} No valida el evento (TK-7). */
    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        return ticketRepository.findByEvent_EventCodeAndStatus(eventCode, TicketStatus.PAID)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Orden: existencia, estado (BR-TICKET-010/011) y fecha (BR-TICKET-012), según la
     * numeración del PRD (P2-14 TC-2). RESERVED también se rechaza: solo PAID es cancelable
     * (TC-5). No se valida el estado del evento (TC-3) ni se modifica (TC-1 / P2-D16). Mensajes
     * según TC-4; el de fecha reutiliza el de {@code create} y {@code purchase} (E-5a).
     */
    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {
        Ticket ticket = findTicketOrThrow(ticketCode);

        // BR-TICKET-010/011: solo un ticket PAID puede cancelarse (no USED, CANCELLED ni RESERVED).
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException("Only PAID tickets can be cancelled: " + ticketCode);
        }

        // BR-TICKET-012 / P2-D20: el evento no debe haber ocurrido ni estar ocurriendo;
        // comparación estricta (P2-D4): la igualdad con ahora se rechaza.
        LocalDateTime eventDate = ticket.getEvent().getEventDate();
        if (!eventDate.isAfter(LocalDateTime.now(clock))) {
            throw new BusinessRuleException("Event date must be in the future: " + eventDate);
        }

        ticket.setStatus(TicketStatus.CANCELLED);

        // P2-D10: save() explícito.
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    /**
     * {@inheritDoc}
     *
     * <p>BR-TICKET-013 cubre BR-TICKET-014: un ticket CANCELLED no es PAID. También se rechazan
     * USED (segundo uso) y RESERVED (TC-5). Sin restricciones de fecha (P2-D19) ni del estado del
     * evento (TC-3). Mensaje según TC-4.
     */
    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {
        Ticket ticket = findTicketOrThrow(ticketCode);

        // BR-TICKET-013/014: solo un ticket PAID puede marcarse como usado.
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException("Only PAID tickets can be used: " + ticketCode);
        }

        ticket.setStatus(TicketStatus.USED);

        // P2-D10: save() explícito.
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    /** Busca el ticket o lanza {@code ResourceNotFoundException} (TK-6, TC-6). */
    private Ticket findTicketOrThrow(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));
    }

    /**
     * BR-TICKET-006. Con {@code minimumAge == 0} no hay restricción: no se exige perfil ni se
     * calcula la edad (TK-2). Con {@code minimumAge > 0} el perfil es obligatorio (P2-D5) y la
     * edad se calcula en la fecha del evento, no hoy.
     */
    private void validateMinimumAge(User user, Event event) {
        int minimumAge = event.getMinimumAge();
        if (minimumAge == 0) {
            return;
        }

        UserProfile profile = user.getProfile();
        if (profile == null) {
            throw new BusinessRuleException("User has no profile: " + user.getEmail());
        }

        int ageAtEvent = Period.between(profile.getBirthDate(), event.getEventDate().toLocalDate()).getYears();
        if (ageAtEvent < minimumAge) {
            throw new BusinessRuleException(
                    "User does not meet minimum age for event " + event.getEventCode() + ": " + user.getEmail());
        }
    }

    /** Tickets que ocupan capacidad: PAID + USED (TK-1). Usa el conteo JPQL existente. */
    private long countOccupiedTickets(String eventCode) {
        return ticketRepository.countByEventEventCodeAndStatus(eventCode, TicketStatus.PAID)
                + ticketRepository.countByEventEventCodeAndStatus(eventCode, TicketStatus.USED);
    }

    /**
     * Genera un código que no exista todavía (P2-D3). Hace como máximo {@link #MAX_CODE_ATTEMPTS}
     * intentos (TK-5). La restricción UNIQUE {@code uk_tickets_ticket_code} sigue siendo la
     * autoridad final ante una colisión concurrente.
     */
    private String generateUniqueTicketCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = ticketCodeGenerator.generate();
            if (ticketRepository.findByTicketCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique ticket code");
    }
}
