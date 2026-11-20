package com.pulsepass.service.impl;

import com.pulsepass.service.TicketCodeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Generador de códigos de ticket con {@link SecureRandom} (P2-D3; P2-13 TK-4).
 *
 * <p>Toma 3 bytes aleatorios (24 bits) y los convierte en 6 caracteres hexadecimales en
 * mayúsculas: hay 16^6 = 16.777.216 códigos posibles. {@code SecureRandom} produce valores
 * difíciles de predecir, de modo que un código no permite adivinar el siguiente.
 */
@Component
public class TicketCodeGeneratorImpl implements TicketCodeGenerator {

    private static final String PREFIX = "TKT-";

    /** 3 bytes = 6 caracteres hexadecimales. */
    private static final int RANDOM_BYTES = 3;

    /** Formato hexadecimal en mayúsculas, sin separadores (java.util.HexFormat, Java 17+). */
    private static final HexFormat HEX = HexFormat.of().withUpperCase();

    /** SecureRandom es seguro para usarse desde varios hilos. */
    private final SecureRandom secureRandom = new SecureRandom();

    /** {@inheritDoc} */
    @Override
    public String generate() {
        byte[] bytes = new byte[RANDOM_BYTES];
        secureRandom.nextBytes(bytes);
        return PREFIX + HEX.formatHex(bytes);
    }
}
