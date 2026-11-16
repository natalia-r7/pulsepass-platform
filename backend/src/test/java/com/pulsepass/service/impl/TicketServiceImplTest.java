package com.pulsepass.service.impl;

import com.pulsepass.config.ClockConfig;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.pulsepass.service.ServiceFixtures.CMF_2026_EVENT_DATE;
import static com.pulsepass.service.ServiceFixtures.andrea;
import static com.pulsepass.service.ServiceFixtures.carlos;
import static com.pulsepass.service.ServiceFixtures.cmf2026;
import static com.pulsepass.service.ServiceFixtures.laura;
import static com.pulsepass.service.ServiceFixtures.miguel;
import static com.pulsepass.service.ServiceFixtures.sofia;
import static com.pulsepass.service.ServiceFixtures.venSmr01;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link TicketServiceImpl}, parte 1 (PRD Servicios §21–§28, §45, §48;
 * TEST-TICKET-001..008; AC-004..009; P2-13).
 *
 * <p>Servicio real; repositorios, mapper, estrategia de precios y generador de códigos simulados
 * con Mockito (§42, V-1a, TP-8, TK-4). Sin Spring, sin PostgreSQL y sin Testcontainers
 * (NFR-001). El reloj es fijo (P2-D4). Los datos salen de {@code ServiceFixtures} (P2-D12).
 *
 * <p>VEN-SMR-01 tiene capacidad 3 (§47): Andrea y Carlos compran, Sofía se lleva el último
 * ticket y el evento pasa a SOLD_OUT (AC-004, AC-005, AC-008).
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    /** DATO DE PRUEBA: «ahora» para todas las pruebas, anterior a CMF_2026_EVENT_DATE. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 15, 12, 0);

    private static final Clock FIXED_CLOCK = Clock.fixed(
            NOW.atZone(ClockConfig.APPLICATION_ZONE).toInstant(), ClockConfig.APPLICATION_ZONE);

    private static final String EVENT_CODE = "CMF-2026"; // PRD §47

    /** DATO DE PRUEBA: precio que devuelve la estrategia simulada (TP-8); no es un importe real. */
    private static final BigDecimal TEST_PRICE = new BigDecimal("100000.00");

    /** DATO DE PRUEBA: códigos que devuelve el generador simulado. */
    private static final String CODE = "TKT-8F2A1C";

    private static final String OTHER_CODE = "TKT-91B07D";

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TicketPricingStrategy ticketPricingStrategy;

    @Mock
    private TicketCodeGenerator ticketCodeGenerator;

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(ticketRepository, userRepository, eventRepository,
                ticketMapper, ticketPricingStrategy, ticketCodeGenerator, FIXED_CLOCK);
    }

    // ---------------------------------------------------------------- purchase: compras válidas

    @Test
    @DisplayName("TEST-TICKET-001 / AC-004: Andrea compra un VIP; el ticket nace PAID con código, precio, fecha, usuario y evento")
    void andreaPurchaseCreatesPaidTicket() {
        // ARRANGE
        User andrea = andrea();
        Event event = publishedEvent();
        TicketResponse expected = ticketResponseOf(CODE, TicketType.VIP, andrea, event);
        stubValidPurchase(andrea, event, TicketType.VIP, 0L, 0L);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(expected);

        // ACT
        TicketResponse response = ticketService.purchase(requestFor(andrea, TicketType.VIP));

        // ASSERT
        Ticket ticket = captureSavedTicket();
        assertThat(ticket.getTicketCode()).isEqualTo(CODE);
        assertThat(ticket.getType()).isEqualTo(TicketType.VIP);
        assertThat(ticket.getPrice()).isEqualByComparingTo(TEST_PRICE);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(ticket.getPurchaseDate()).isEqualTo(NOW);
        assertThat(ticket.getUser()).isSameAs(andrea);
        assertThat(ticket.getEvent()).isSameAs(event);
        assertThat(response).isEqualTo(expected);

        verify(ticketPricingStrategy).calculatePrice(eq(TicketType.VIP));
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC-005: Carlos compra un GENERAL con un ticket ya vendido; el evento sigue PUBLISHED")
    void carlosPurchaseKeepsEventPublished() {
        // ARRANGE
        User carlos = carlos();
        Event event = publishedEvent();
        stubValidPurchase(carlos, event, TicketType.GENERAL, 1L, 0L); // el de Andrea
        when(ticketMapper.toResponse(any(Ticket.class)))
                .thenReturn(ticketResponseOf(CODE, TicketType.GENERAL, carlos, event));

        // ACT
        ticketService.purchase(requestFor(carlos, TicketType.GENERAL));

        // ASSERT
        Ticket ticket = captureSavedTicket();
        assertThat(ticket.getUser()).isSameAs(carlos);
        assertThat(ticket.getType()).isEqualTo(TicketType.GENERAL);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-TICKET-008 / AC-008: Sofía compra el último ticket; se guarda y el evento pasa a SOLD_OUT, en el orden del PRD")
    void sofiaBuysLastTicketAndEventBecomesSoldOut() {
        // ARRANGE
        User sofia = sofia();
        Event event = publishedEvent();
        stubValidPurchase(sofia, event, TicketType.GENERAL, 2L, 0L); // Andrea y Carlos
        when(ticketMapper.toResponse(any(Ticket.class)))
                .thenReturn(ticketResponseOf(CODE, TicketType.GENERAL, sofia, event));

        // ACT
        ticketService.purchase(requestFor(sofia, TicketType.GENERAL));

        // ASSERT
        assertThat(captureSavedTicket().getUser()).isSameAs(sofia);
        assertThat(event.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
        verify(eventRepository).save(event);

        // Orden de §24: usuario → evento → capacidad → precio → código → guardar → SOLD_OUT.
        InOrder order = inOrder(userRepository, eventRepository, ticketRepository,
                ticketPricingStrategy, ticketCodeGenerator, ticketMapper);
        order.verify(userRepository).findByEmailIgnoreCase("sofia@email.com");
        order.verify(eventRepository).findByEventCode(EVENT_CODE);
        order.verify(ticketRepository).countByEventEventCodeAndStatus(EVENT_CODE, TicketStatus.PAID);
        order.verify(ticketPricingStrategy).calculatePrice(TicketType.GENERAL);
        order.verify(ticketCodeGenerator).generate();
        order.verify(ticketRepository).save(any(Ticket.class));
        order.verify(eventRepository).save(event);
        order.verify(ticketMapper).toResponse(any(Ticket.class));
    }

    // ---------------------------------------------------------------- purchase: usuario

    @Test
    @DisplayName("TEST-TICKET-002 / BR-TICKET-001: un usuario inexistente lanza ResourceNotFoundException y no se busca el evento")
    void purchaseThrowsWhenUserDoesNotExist() {
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase("nadie@email.com")).thenReturn(Optional.empty()); // DATO DE PRUEBA

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(
                new PurchaseTicketRequest("nadie@email.com", EVENT_CODE, TicketType.GENERAL)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: nadie@email.com");
        verify(eventRepository, never()).findByEventCode(any());
        verifyNothingSaved();
    }

    @Test
    @DisplayName("TEST-TICKET-003 / BR-TICKET-002 / AC-007: Miguel está inactivo y no puede comprar")
    void purchaseThrowsWhenUserIsInactive() {
        // ARRANGE
        User miguel = miguel();
        when(userRepository.findByEmailIgnoreCase("miguel@email.com")).thenReturn(Optional.of(miguel));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(requestFor(miguel, TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("User is not active: miguel@email.com");
        verify(eventRepository, never()).findByEventCode(any());
        verifyNothingSaved();
    }

    // ---------------------------------------------------------------- purchase: evento

    @Test
    @DisplayName("BR-TICKET-003: un evento inexistente lanza ResourceNotFoundException")
    void purchaseThrowsWhenEventDoesNotExist() {
        // ARRANGE
        User andrea = andrea();
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(andrea));
        when(eventRepository.findByEventCode("NO-EXISTE")).thenReturn(Optional.empty()); // DATO DE PRUEBA

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(
                new PurchaseTicketRequest("andrea@email.com", "NO-EXISTE", TicketType.GENERAL)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found: NO-EXISTE");
        verifyNothingSaved();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = EventStatus.class, names = "PUBLISHED", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("TEST-TICKET-004 / 005 / AC-009 / BR-TICKET-004: no se compra en eventos DRAFT, SOLD_OUT, CANCELLED ni FINISHED")
    void purchaseThrowsWhenEventIsNotPublished(EventStatus status) {
        // ARRANGE
        User andrea = andrea();
        Event event = publishedEvent();
        event.setStatus(status);
        stubUserAndEvent(andrea, event);

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(requestFor(andrea, TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Tickets can only be purchased for PUBLISHED events: CMF-2026");
        verify(ticketRepository, never()).countByEventEventCodeAndStatus(any(), any());
        verifyNothingSaved();
    }

    @Test
    @DisplayName("BR-TICKET-005: no se compra para un evento que ya ocurrió")
    void purchaseThrowsWhenEventDateIsInThePast() {
        // ARRANGE
        User andrea = andrea();
        Event event = publishedEvent();
        LocalDateTime yesterday = NOW.minusDays(1); // DATO DE PRUEBA
        event.setEventDate(yesterday);
        stubUserAndEvent(andrea, event);

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(requestFor(andrea, TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Event date must be in the future: " + yesterday);
        verifyNothingSaved();
    }

    @Test
    @DisplayName("BR-TICKET-005 / P2-D4: una fecha exactamente igual a ahora no es futura")
    void purchaseThrowsWhenEventDateEqualsNow() {
        // ARRANGE
        User andrea = andrea();
        Event event = publishedEvent();
        event.setEventDate(NOW); // DATO DE PRUEBA: límite
        stubUserAndEvent(andrea, event);

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(requestFor(andrea, TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Event date must be in the future: " + NOW);
        verifyNothingSaved();
    }

    // ---------------------------------------------------------------- purchase: edad

    @Test
    @DisplayName("TEST-TICKET-006 / BR-TICKET-006 / AC-006: Laura tiene 17 años en la fecha del evento y no puede comprar")
    void purchaseThrowsWhenUserIsUnderMinimumAge() {
        // ARRANGE
        User laura = laura();
        stubUserAndEvent(laura, publishedEvent());

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(requestFor(laura, TicketType.STUDENT)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("User does not meet minimum age for event CMF-2026: laura@email.com");
        verify(ticketRepository, never()).countByEventEventCodeAndStatus(any(), any());
        verifyNothingSaved();
    }

    @Test
    @DisplayName("P2-D5 / TK-2: con edad mínima, un usuario sin perfil no puede comprar")
    void purchaseThrowsWhenUserHasNoProfileAndAgeIsRequired() {
        // ARRANGE
        User withoutProfile = new User("sinperfil", "sinperfil@email.com", true); // DATO DE PRUEBA
        stubUserAndEvent(withoutProfile, publishedEvent());

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(requestFor(withoutProfile, TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("User has no profile: sinperfil@email.com");
        verifyNothingSaved();
    }

    @Test
    @DisplayName("BR-TICKET-006: quien cumple exactamente 18 años el día del evento puede comprar")
    void purchaseAllowsUserWhoTurnsMinimumAgeOnEventDate() {
        // ARRANGE
        User andrea = andrea();
        andrea.getProfile().setBirthDate(CMF_2026_EVENT_DATE.toLocalDate().minusYears(18)); // DATO DE PRUEBA: límite
        Event event = publishedEvent();
        stubValidPurchase(andrea, event, TicketType.GENERAL, 0L, 0L);

        // ACT
        ticketService.purchase(requestFor(andrea, TicketType.GENERAL));

        // ASSERT
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    @DisplayName("BR-TICKET-006: quien cumple 18 años un día después del evento no puede comprar")
    void purchaseThrowsWhenUserTurnsMinimumAgeTheDayAfterEvent() {
        // ARRANGE
        User andrea = andrea();
        andrea.getProfile().setBirthDate(CMF_2026_EVENT_DATE.toLocalDate().minusYears(18).plusDays(1)); // DATO DE PRUEBA
        stubUserAndEvent(andrea, publishedEvent());

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(requestFor(andrea, TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("User does not meet minimum age for event CMF-2026: andrea@email.com");
        verifyNothingSaved();
    }

    @Test
    @DisplayName("TK-2: con minimumAge = 0 no se exige perfil ni se calcula la edad")
    void purchaseDoesNotRequireProfileWhenMinimumAgeIsZero() {
        // ARRANGE
        User withoutProfile = new User("sinperfil", "sinperfil@email.com", true); // DATO DE PRUEBA
        Event event = publishedEvent();
        event.setMinimumAge(0); // DATO DE PRUEBA
        stubValidPurchase(withoutProfile, event, TicketType.GENERAL, 0L, 0L);

        // ACT
        ticketService.purchase(requestFor(withoutProfile, TicketType.GENERAL));

        // ASSERT
        assertThat(captureSavedTicket().getUser()).isSameAs(withoutProfile);
    }

    // ---------------------------------------------------------------- purchase: capacidad

    @Test
    @DisplayName("TEST-TICKET-007 / BR-TICKET-007: sin capacidad no se compra, ni se calcula precio ni código")
    void purchaseThrowsWhenEventHasNoCapacity() {
        // ARRANGE
        User andrea = andrea();
        stubUserAndEvent(andrea, publishedEvent());
        stubOccupied(3L, 0L); // capacidad 3 completa

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(requestFor(andrea, TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Event has no available capacity: CMF-2026");
        verify(ticketPricingStrategy, never()).calculatePrice(any());
        verify(ticketCodeGenerator, never()).generate();
        verifyNothingSaved();
    }

    @Test
    @DisplayName("TK-1: los tickets USED ocupan capacidad (1 PAID + 2 USED = 3 → sin capacidad)")
    void usedTicketsOccupyCapacity() {
        // ARRANGE
        User andrea = andrea();
        stubUserAndEvent(andrea, publishedEvent());
        stubOccupied(1L, 2L);

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(requestFor(andrea, TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Event has no available capacity: CMF-2026");
        verifyNothingSaved();
    }

    @Test
    @DisplayName("TK-1 / BR-TICKET-008: PAID + USED + esta compra = capacidad → SOLD_OUT")
    void usedTicketsCountTowardsSoldOut() {
        // ARRANGE
        User sofia = sofia();
        Event event = publishedEvent();
        stubValidPurchase(sofia, event, TicketType.GENERAL, 1L, 1L);

        // ACT
        ticketService.purchase(requestFor(sofia, TicketType.GENERAL));

        // ASSERT
        assertThat(event.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("TK-1: los tickets CANCELLED no se cuentan; con 1 PAID todavía hay capacidad")
    void cancelledTicketsDoNotOccupyCapacity() {
        // ARRANGE
        User carlos = carlos();
        Event event = publishedEvent();
        stubValidPurchase(carlos, event, TicketType.GENERAL, 1L, 0L);

        // ACT
        ticketService.purchase(requestFor(carlos, TicketType.GENERAL));

        // ASSERT
        verify(ticketRepository).save(any(Ticket.class));
        verify(ticketRepository, never()).countByEventEventCodeAndStatus(any(), eq(TicketStatus.CANCELLED));
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    // ---------------------------------------------------------------- purchase: código

    @Test
    @DisplayName("P2-D3 / TK-4: si el código ya existe, se genera otro y se usa el disponible")
    void purchaseRetriesWhenTicketCodeCollides() {
        // ARRANGE
        User andrea = andrea();
        Event event = publishedEvent();
        stubUserAndEvent(andrea, event);
        stubOccupied(0L, 0L);
        when(ticketPricingStrategy.calculatePrice(TicketType.GENERAL)).thenReturn(TEST_PRICE);
        when(ticketCodeGenerator.generate()).thenReturn(CODE, OTHER_CODE);
        when(ticketRepository.findByTicketCode(CODE)).thenReturn(Optional.of(existingTicket(CODE, event)));
        when(ticketRepository.findByTicketCode(OTHER_CODE)).thenReturn(Optional.empty());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ACT
        ticketService.purchase(requestFor(andrea, TicketType.GENERAL));

        // ASSERT
        assertThat(captureSavedTicket().getTicketCode()).isEqualTo(OTHER_CODE);
        verify(ticketCodeGenerator, times(2)).generate();
    }

    @Test
    @DisplayName("TK-5: tras cinco colisiones seguidas se lanza IllegalStateException y no se guarda nada")
    void purchaseThrowsAfterFiveCodeCollisions() {
        // ARRANGE
        User andrea = andrea();
        Event event = publishedEvent();
        stubUserAndEvent(andrea, event);
        stubOccupied(0L, 0L);
        when(ticketPricingStrategy.calculatePrice(TicketType.GENERAL)).thenReturn(TEST_PRICE);
        when(ticketCodeGenerator.generate()).thenReturn(CODE);
        when(ticketRepository.findByTicketCode(CODE)).thenReturn(Optional.of(existingTicket(CODE, event)));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(requestFor(andrea, TicketType.GENERAL)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not generate a unique ticket code");
        verify(ticketCodeGenerator, times(5)).generate();
        verifyNothingSaved();
    }

    // ---------------------------------------------------------------- findByCode

    @Test
    @DisplayName("FR-SVC-014: findByCode devuelve el TicketResponse del ticket encontrado")
    void findByCodeReturnsMappedTicket() {
        // ARRANGE
        Ticket ticket = existingTicket(CODE, publishedEvent());
        TicketResponse expected = ticketResponseOf(CODE, TicketType.VIP, ticket.getUser(), ticket.getEvent());
        when(ticketRepository.findByTicketCode(CODE)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toResponse(ticket)).thenReturn(expected);

        // ACT
        TicketResponse response = ticketService.findByCode(CODE);

        // ASSERT
        assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("FR-SVC-014: findByCode lanza ResourceNotFoundException si el ticket no existe")
    void findByCodeThrowsWhenTicketDoesNotExist() {
        // ARRANGE
        when(ticketRepository.findByTicketCode("TKT-000000")).thenReturn(Optional.empty()); // DATO DE PRUEBA

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.findByCode("TKT-000000"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ticket not found: TKT-000000");
        verify(ticketMapper, never()).toResponse(any());
    }

    // ---------------------------------------------------------------- findByUserEmail

    @Test
    @DisplayName("FR-SVC-015 / P2-D7: findByUserEmail mapea los tickets conservando el orden del repositorio")
    void findByUserEmailMapsTicketsInRepositoryOrder() {
        // ARRANGE
        Event event = publishedEvent();
        Ticket newer = existingTicket(CODE, event);
        Ticket older = existingTicket(OTHER_CODE, event);
        TicketResponse newerResponse = ticketResponseOf(CODE, TicketType.VIP, newer.getUser(), event);
        TicketResponse olderResponse = ticketResponseOf(OTHER_CODE, TicketType.VIP, older.getUser(), event);
        when(ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc("ANDREA@EMAIL.COM"))
                .thenReturn(List.of(newer, older));
        when(ticketMapper.toResponse(newer)).thenReturn(newerResponse);
        when(ticketMapper.toResponse(older)).thenReturn(olderResponse);

        // ACT
        List<TicketResponse> responses = ticketService.findByUserEmail("ANDREA@EMAIL.COM");

        // ASSERT
        assertThat(responses).containsExactly(newerResponse, olderResponse);
    }

    @Test
    @DisplayName("TK-7: findByUserEmail devuelve una lista vacía si no hay tickets, sin consultar el usuario")
    void findByUserEmailReturnsEmptyList() {
        // ARRANGE
        when(ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc("nadie@email.com"))
                .thenReturn(List.of()); // DATO DE PRUEBA

        // ACT
        List<TicketResponse> responses = ticketService.findByUserEmail("nadie@email.com");

        // ASSERT
        assertThat(responses).isEmpty();
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    // ---------------------------------------------------------------- findPaidTicketsByEvent

    @Test
    @DisplayName("FR-SVC-016: findPaidTicketsByEvent consulta solo los tickets PAID del evento")
    void findPaidTicketsByEventReturnsOnlyPaidTickets() {
        // ARRANGE
        Ticket ticket = existingTicket(CODE, publishedEvent());
        TicketResponse expected = ticketResponseOf(CODE, TicketType.VIP, ticket.getUser(), ticket.getEvent());
        when(ticketRepository.findByEvent_EventCodeAndStatus(EVENT_CODE, TicketStatus.PAID))
                .thenReturn(List.of(ticket));
        when(ticketMapper.toResponse(ticket)).thenReturn(expected);

        // ACT
        List<TicketResponse> responses = ticketService.findPaidTicketsByEvent(EVENT_CODE);

        // ASSERT
        assertThat(responses).containsExactly(expected);
        verify(ticketRepository).findByEvent_EventCodeAndStatus(eq(EVENT_CODE), eq(TicketStatus.PAID));
    }

    @Test
    @DisplayName("TK-7: findPaidTicketsByEvent devuelve una lista vacía si no hay tickets, sin consultar el evento")
    void findPaidTicketsByEventReturnsEmptyList() {
        // ARRANGE
        when(ticketRepository.findByEvent_EventCodeAndStatus("NO-EXISTE", TicketStatus.PAID))
                .thenReturn(List.of()); // DATO DE PRUEBA

        // ACT
        List<TicketResponse> responses = ticketService.findPaidTicketsByEvent("NO-EXISTE");

        // ASSERT
        assertThat(responses).isEmpty();
        verify(eventRepository, never()).findByEventCode(anyString());
    }

    // ---------------------------------------------------------------- Auxiliares

    /** CMF-2026 en VEN-SMR-01 (capacidad 3), ya publicado (AC-002). */
    private static Event publishedEvent() {
        Event event = cmf2026(venSmr01());
        event.setStatus(EventStatus.PUBLISHED);
        return event;
    }

    private static PurchaseTicketRequest requestFor(User user, TicketType type) {
        return new PurchaseTicketRequest(user.getEmail(), EVENT_CODE, type);
    }

    private void stubUserAndEvent(User user, Event event) {
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
    }

    private void stubOccupied(long paid, long used) {
        when(ticketRepository.countByEventEventCodeAndStatus(EVENT_CODE, TicketStatus.PAID)).thenReturn(paid);
        when(ticketRepository.countByEventEventCodeAndStatus(EVENT_CODE, TicketStatus.USED)).thenReturn(used);
    }

    /** Prepara una compra que supera todas las reglas; save() devuelve el mismo ticket. */
    private void stubValidPurchase(User user, Event event, TicketType type, long paid, long used) {
        stubUserAndEvent(user, event);
        stubOccupied(paid, used);
        when(ticketPricingStrategy.calculatePrice(type)).thenReturn(TEST_PRICE);
        when(ticketCodeGenerator.generate()).thenReturn(CODE);
        when(ticketRepository.findByTicketCode(CODE)).thenReturn(Optional.empty());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Ticket captureSavedTicket() {
        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        return captor.getValue();
    }

    /** §50: en los caminos inválidos no se guarda ni el ticket ni el evento. */
    private void verifyNothingSaved() {
        verify(ticketRepository, never()).save(any());
        verify(eventRepository, never()).save(any());
    }

    /** Ticket ya existente de Andrea (DATO DE PRUEBA). */
    private static Ticket existingTicket(String code, Event event) {
        Ticket ticket = new Ticket(code, TicketType.VIP, TEST_PRICE, TicketStatus.PAID, andrea(), event);
        ticket.setPurchaseDate(NOW);
        return ticket;
    }

    /** Respuesta que devuelve el mapper simulado; los valores no los calcula el servicio. */
    private static TicketResponse ticketResponseOf(String code, TicketType type, User user, Event event) {
        return new TicketResponse(1L, code, type, TEST_PRICE, TicketStatus.PAID, NOW, // DATO DE PRUEBA: id
                user.getEmail(), event.getEventCode(), event.getName());
    }
}
