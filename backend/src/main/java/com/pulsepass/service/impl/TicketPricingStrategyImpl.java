package com.pulsepass.service.impl;

import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.service.TicketPricingStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estrategia de precio base, descuento y multiplicadores (PRD Servicios §28; P2-12 TP-1).
 *
 * <p>Los importes son DATO DE DISEÑO aprobado en P2-D2 / P2-12 TP-2; el PRD no los define.
 * Viven como constantes en esta clase (TP-3), siempre en {@link BigDecimal} construido con
 * {@code String}, nunca con {@code double} ni {@code float} (§28, BR-007).
 *
 * <ul>
 *   <li>GENERAL → precio base: 100000.00</li>
 *   <li>STUDENT → descuento (base × 0.50): 50000.00</li>
 *   <li>VIP → multiplicador (base × 2): 200000.00</li>
 *   <li>BACKSTAGE → multiplicador superior (base × 3): 300000.00</li>
 * </ul>
 *
 * <p>BR-TICKET-009 (precio nunca negativo) se garantiza por diseño, con constantes positivas, y
 * se comprueba en las pruebas (TP-5). No se valida {@code null} (P2-D18, TP-7).
 */
@Component
public class TicketPricingStrategyImpl implements TicketPricingStrategy {

    /** Escala de la columna {@code price NUMERIC(12,2)} (TP-4). */
    private static final int PRICE_SCALE = 2;

    /** DATO DE DISEÑO (P2-D2 / TP-2): precio base de GENERAL. */
    private static final BigDecimal BASE_PRICE = new BigDecimal("100000.00");

    /** DATO DE DISEÑO (TP-2): STUDENT paga el 50 % del precio base. */
    private static final BigDecimal STUDENT_DISCOUNT_FACTOR = new BigDecimal("0.50");

    /** DATO DE DISEÑO (TP-2): VIP paga el doble del precio base. */
    private static final BigDecimal VIP_MULTIPLIER = new BigDecimal("2");

    /** DATO DE DISEÑO (TP-2): BACKSTAGE paga el triple del precio base (multiplicador superior). */
    private static final BigDecimal BACKSTAGE_MULTIPLIER = new BigDecimal("3");

    /**
     * {@inheritDoc}
     *
     * <p>El {@code switch} es exhaustivo sobre {@link TicketType}: si se agrega un tipo nuevo sin
     * precio, el código no compila. El resultado se ajusta a escala 2 con {@code HALF_UP} (TP-4).
     */
    @Override
    public BigDecimal calculatePrice(TicketType type) {
        BigDecimal price = switch (type) {
            case GENERAL -> BASE_PRICE;
            case STUDENT -> BASE_PRICE.multiply(STUDENT_DISCOUNT_FACTOR);
            case VIP -> BASE_PRICE.multiply(VIP_MULTIPLIER);
            case BACKSTAGE -> BASE_PRICE.multiply(BACKSTAGE_MULTIPLIER);
        };
        return price.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }
}
