package com.pulsepass.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias de {@link TicketCodeGeneratorImpl} (P2-D3; P2-13 TK-4).
 *
 * <p>Prueba pura, sin Spring ni Mockito. No depende de ningún valor aleatorio concreto: solo
 * comprueba el formato {@code TKT-} + 6 caracteres hexadecimales en mayúsculas.
 */
class TicketCodeGeneratorImplTest {

    /** Prefijo TKT- seguido de exactamente seis caracteres de 0-9 o A-F. */
    private static final String CODE_PATTERN = "^TKT-[0-9A-F]{6}$";

    private TicketCodeGeneratorImpl generator;

    @BeforeEach
    void setUp() {
        generator = new TicketCodeGeneratorImpl();
    }

    @Test
    @DisplayName("P2-D3: el código empieza con TKT- y le siguen exactamente seis caracteres")
    void generatedCodeHasPrefixAndSixCharacters() {
        // ACT
        String code = generator.generate();

        // ASSERT
        assertThat(code).startsWith("TKT-");
        assertThat(code.substring("TKT-".length())).hasSize(6);
    }

    @RepeatedTest(20)
    @DisplayName("P2-D3: cada código generado tiene el formato TKT- + 6 hexadecimales en mayúsculas")
    void everyGeneratedCodeMatchesFormat() {
        // ACT
        String code = generator.generate();

        // ASSERT
        assertThat(code).matches(CODE_PATTERN);
    }
}
