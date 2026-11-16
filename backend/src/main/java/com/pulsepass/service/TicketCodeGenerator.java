package com.pulsepass.service;

/**
 * Genera códigos de ticket candidatos (P2-D3; P2-13 TK-4).
 *
 * <p>Solo genera el código: no comprueba si ya existe. La comprobación de colisiones y los
 * reintentos son responsabilidad de {@code TicketService} (TK-5). Al ser un componente
 * independiente, las pruebas del servicio pueden simularlo y obtener códigos predecibles.
 */
public interface TicketCodeGenerator {

    /**
     * Genera un código con el formato {@code TKT-XXXXXX}: el prefijo {@code TKT-} seguido de
     * seis caracteres hexadecimales en mayúsculas (ej. {@code TKT-8F2A1C}).
     */
    String generate();
}
