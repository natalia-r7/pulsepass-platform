package com.pulsepass.service;

import com.pulsepass.domain.enums.TicketType;

import java.math.BigDecimal;

/**
 * Estrategia de precio de los tickets (PRD Servicios §28, §22; P2-D2; P2-12 TP-1..TP-8).
 *
 * <p>El precio lo define el sistema, nunca el cliente (§22). La estrategia está encapsulada
 * (§28): no conoce repositorios, entidades, DTO ni el reloj, y {@code TicketService} la recibirá
 * por constructor (P2-13), de modo que se puede sustituir sin tocar el servicio.
 */
public interface TicketPricingStrategy {

    /**
     * Calcula el precio de un ticket según su tipo.
     *
     * @param type tipo de ticket; no se valida {@code null} (P2-D18, P2-12 TP-7)
     * @return precio en {@link BigDecimal}, con escala 2 y nunca negativo (BR-TICKET-009)
     */
    BigDecimal calculatePrice(TicketType type);
}
