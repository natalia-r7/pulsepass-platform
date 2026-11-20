package com.pulsepass.mapper;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.dto.response.TicketResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.pulsepass.service.ServiceFixtures.andrea;
import static com.pulsepass.service.ServiceFixtures.cmf2026;
import static com.pulsepass.service.ServiceFixtures.venSmr01;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias del {@link TicketMapper} real (P2-D9, PRD Servicios §23, §34).
 *
 * <p>Sin Spring, sin base de datos y sin mocks: el mapper se obtiene con
 * {@code Mappers.getMapper(...)}. Los IDs se asignan con {@code ReflectionTestUtils}
 * (P2-05 D-B).
 *
 * <p>El precio es un DATO DE PRUEBA y no un precio de negocio: la estrategia de precios
 * se decide en P2-12 (P2-D2).
 */
class TicketMapperTest {

    /** DATO DE PRUEBA: no es un precio de negocio (P2-D2). */
    private static final BigDecimal TEST_PRICE = new BigDecimal("100.00");

    private final TicketMapper ticketMapper = Mappers.getMapper(TicketMapper.class);

    private User user;

    private Event event;

    @BeforeEach
    void setUp() {
        user = andrea();
        event = cmf2026(venSmr01());
    }

    @Test
    @DisplayName("§34: Ticket se convierte en TicketResponse con user.email, event.eventCode y event.name")
    void mapsTicketAndNestedFields() {
        // ARRANGE
        Ticket ticket = new Ticket("TKT-0A1B2C", TicketType.GENERAL, TEST_PRICE,  // DATO DE PRUEBA: código
                TicketStatus.PAID, user, event);
        LocalDateTime purchaseDate = LocalDateTime.of(2026, 11, 20, 10, 30);    // DATO DE PRUEBA
        ticket.setPurchaseDate(purchaseDate);
        ReflectionTestUtils.setField(ticket, "id", 5000L);                       // DATO DE PRUEBA

        // ACT
        TicketResponse response = ticketMapper.toResponse(ticket);

        // ASSERT
        assertThat(response).isEqualTo(new TicketResponse(
                5000L, "TKT-0A1B2C", TicketType.GENERAL, TEST_PRICE, TicketStatus.PAID,
                purchaseDate, "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026"));
    }

    @Test
    @DisplayName("P1-D1: un ticket RESERVED sin fecha de compra produce purchaseDate = null")
    void keepsNullPurchaseDate() {
        // ARRANGE
        Ticket ticket = new Ticket("TKT-0A1B2D", TicketType.GENERAL, TEST_PRICE,  // DATO DE PRUEBA: código
                TicketStatus.RESERVED, user, event);

        // ACT
        TicketResponse response = ticketMapper.toResponse(ticket);

        // ASSERT
        assertThat(response.purchaseDate()).isNull();
        assertThat(response.status()).isEqualTo(TicketStatus.RESERVED);
    }

    @Test
    @DisplayName("Un Ticket null produce null")
    void returnsNullForNullTicket() {
        // ACT + ASSERT
        assertThat(ticketMapper.toResponse(null)).isNull();
    }
}
