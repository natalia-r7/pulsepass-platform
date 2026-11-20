package com.pulsepass.service.impl;

import com.pulsepass.domain.enums.TicketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias de {@link TicketPricingStrategyImpl} (PRD Servicios §28, BR-TICKET-009;
 * P2-D2; P2-12 TP-1..TP-5).
 *
 * <p>Prueba pura: la estrategia no tiene dependencias, así que no se usan Spring, Mockito,
 * PostgreSQL ni Testcontainers (NFR-001). Los importes esperados son DATO DE DISEÑO aprobado en
 * P2-12 TP-2; el PRD no los define.
 */
class TicketPricingStrategyImplTest {

    private TicketPricingStrategyImpl pricingStrategy;

    @BeforeEach
    void setUp() {
        pricingStrategy = new TicketPricingStrategyImpl();
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "GENERAL,   100000.00",  // DATO DE DISEÑO (TP-2): precio base
            "STUDENT,   50000.00",   // DATO DE DISEÑO (TP-2): base × 0.50
            "VIP,       200000.00",  // DATO DE DISEÑO (TP-2): base × 2
            "BACKSTAGE, 300000.00"   // DATO DE DISEÑO (TP-2): base × 3
    })
    @DisplayName("TP-2: cada tipo de ticket tiene su precio exacto")
    void calculatePriceReturnsExactPriceForEachType(TicketType type, BigDecimal expectedPrice) {
        // ACT
        BigDecimal price = pricingStrategy.calculatePrice(type);

        // ASSERT
        assertThat(price).isEqualByComparingTo(expectedPrice);
    }

    @Test
    @DisplayName("§28: STUDENT tiene descuento, así que cuesta menos que GENERAL")
    void studentPriceIsLowerThanGeneral() {
        // ACT
        BigDecimal student = pricingStrategy.calculatePrice(TicketType.STUDENT);
        BigDecimal general = pricingStrategy.calculatePrice(TicketType.GENERAL);

        // ASSERT
        assertThat(student).isLessThan(general);
    }

    @Test
    @DisplayName("§28: VIP aplica un multiplicador, así que cuesta más que GENERAL")
    void vipPriceIsHigherThanGeneral() {
        // ACT
        BigDecimal vip = pricingStrategy.calculatePrice(TicketType.VIP);
        BigDecimal general = pricingStrategy.calculatePrice(TicketType.GENERAL);

        // ASSERT
        assertThat(vip).isGreaterThan(general);
    }

    @Test
    @DisplayName("§28: BACKSTAGE aplica un multiplicador superior, así que cuesta más que VIP")
    void backstagePriceIsHigherThanVip() {
        // ACT
        BigDecimal backstage = pricingStrategy.calculatePrice(TicketType.BACKSTAGE);
        BigDecimal vip = pricingStrategy.calculatePrice(TicketType.VIP);

        // ASSERT
        assertThat(backstage).isGreaterThan(vip);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TicketType.class)
    @DisplayName("BR-TICKET-009 / TP-4: ningún tipo produce un precio negativo y todos tienen escala 2")
    void calculatePriceIsNeverNegativeAndHasScaleTwo(TicketType type) {
        // ACT
        BigDecimal price = pricingStrategy.calculatePrice(type);

        // ASSERT
        assertThat(price).isNotNegative();
        assertThat(price.scale()).isEqualTo(2);
    }
}
