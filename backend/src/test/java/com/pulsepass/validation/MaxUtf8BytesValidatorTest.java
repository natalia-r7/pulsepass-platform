package com.pulsepass.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de {@link MaxUtf8BytesValidator} (PRD Seguridad §9.3; P4-D14, P4-06-B). Sin Spring: el
 * validador se crea a mano con una anotación real de límite 72. Los textos son DATO DE PRUEBA.
 */
class MaxUtf8BytesValidatorTest {

    /** Portador de la anotación: así se obtiene una instancia real con {@code value = 72}. */
    private static final class Holder {
        @MaxUtf8Bytes(value = 72, message = "Password must not exceed 72 bytes")
        String password;
    }

    private MaxUtf8BytesValidator validator;

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        MaxUtf8Bytes constraint = Holder.class.getDeclaredField("password").getAnnotation(MaxUtf8Bytes.class);
        validator = new MaxUtf8BytesValidator();
        validator.initialize(constraint);
    }

    @Test
    @DisplayName("P4-06-B: null es válido; la obligatoriedad es de @NotBlank")
    void nullIsValid() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    @DisplayName("P4-06-B: 72 bytes ASCII son válidos y 73 no")
    void asciiBoundary() {
        assertThat(validator.isValid("a".repeat(72), null)).isTrue();
        assertThat(validator.isValid("a".repeat(73), null)).isFalse();
    }

    @Test
    @DisplayName("P4-D14: cuenta bytes, no caracteres: «€» ocupa 3 bytes (24 = 72 válido, 25 = 75 no)")
    void threeByteCharactersAreCountedAsBytes() {
        assertThat(validator.isValid("€".repeat(24), null)).isTrue();
        assertThat(validator.isValid("€".repeat(25), null)).isFalse();
    }

    @Test
    @DisplayName("P4-D14: un emoji ocupa 4 bytes (18 = 72 válido, 19 = 76 no)")
    void fourByteCharactersAreCountedAsBytes() {
        assertThat(validator.isValid("🎵".repeat(18), null)).isTrue();
        assertThat(validator.isValid("🎵".repeat(19), null)).isFalse();
    }
}
