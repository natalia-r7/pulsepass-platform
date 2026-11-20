package com.pulsepass.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias de la validación de {@link JwtProperties} (PRD Seguridad §12.2, §14.2;
 * P4-03-b, P4-03-f2). Cada rechazo es una {@link IllegalArgumentException} que, dentro de Spring,
 * impide arrancar la aplicación (lo comprueba {@code JwtConfigTest}).
 */
class JwtPropertiesTest {

    private static final String ISSUER = "pulsepass-api";
    private static final Duration TTL = Duration.ofMinutes(15);

    /** DATO DE PRUEBA: claves ficticias escritas como texto, sin valor real. */
    private static final String SECRET_32 = base64("jwt-properties-test-key-32-bytes");
    private static final String SECRET_31 = base64("jwt-properties-test-key-31-byte");
    private static final String SECRET_64 = base64("jwt-properties-test-key-32-bytes" + "jwt-properties-test-key-64-bytes");

    @Test
    @DisplayName("§14.2: acepta una configuración válida y conserva sus valores")
    void acceptsValidConfiguration() {
        JwtProperties properties = new JwtProperties(ISSUER, SECRET_64, TTL);

        assertThat(properties.issuer()).isEqualTo(ISSUER);
        assertThat(properties.secretBase64()).isEqualTo(SECRET_64);
        assertThat(properties.accessTokenTtl()).isEqualTo(TTL);
    }

    @Test
    @DisplayName("§12.2: acepta una clave de exactamente 32 bytes")
    void acceptsSecretOfExactly32Bytes() {
        assertThat(new JwtProperties(ISSUER, SECRET_32, TTL).secretBase64()).isEqualTo(SECRET_32);
    }

    @Test
    @DisplayName("§12.2: rechaza una clave de 31 bytes")
    void rejectsSecretShorterThan32Bytes() {
        assertThatThrownBy(() -> new JwtProperties(ISSUER, SECRET_31, TTL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pulsepass.security.jwt.secret-base64 must decode to at least 32 bytes");
    }

    @ParameterizedTest(name = "clave «{0}»")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("§12.2: rechaza una clave nula, vacía o en blanco")
    void rejectsMissingSecret(String secret) {
        assertThatThrownBy(() -> new JwtProperties(ISSUER, secret, TTL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pulsepass.security.jwt.secret-base64 must be provided");
    }

    @Test
    @DisplayName("§12.2: rechaza una clave que no es Base64 válido")
    void rejectsInvalidBase64() {
        assertThatThrownBy(() -> new JwtProperties(ISSUER, "esto no es base64!!", TTL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pulsepass.security.jwt.secret-base64 must be valid standard Base64")
                .hasNoCause();
    }

    @Test
    @DisplayName("P4-03-b: rechaza la variante Base64 URL (con - y _)")
    void rejectsUrlSafeBase64() {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) 0xFF);                                   // DATO DE PRUEBA
        String urlSafe = Base64.getUrlEncoder().encodeToString(bytes);     // "____…"
        assertThat(urlSafe).contains("_");

        assertThatThrownBy(() -> new JwtProperties(ISSUER, urlSafe, TTL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pulsepass.security.jwt.secret-base64 must be valid standard Base64");
    }

    @ParameterizedTest(name = "issuer «{0}»")
    @NullAndEmptySource
    @DisplayName("§12.1: rechaza un issuer nulo o vacío")
    void rejectsBlankIssuer(String issuer) {
        assertThatThrownBy(() -> new JwtProperties(issuer, SECRET_32, TTL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pulsepass.security.jwt.issuer must not be blank");
    }

    @ParameterizedTest(name = "duración {0}")
    @MethodSource("nonPositiveTtls")
    @DisplayName("§12.2: rechaza una duración nula, cero o negativa")
    void rejectsNonPositiveTtl(Duration ttl) {
        assertThatThrownBy(() -> new JwtProperties(ISSUER, SECRET_32, ttl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pulsepass.security.jwt.access-token-ttl must be positive");
    }

    @ParameterizedTest(name = "duración {0}")
    @MethodSource("fractionalTtls")
    @DisplayName("P4-03-f2: rechaza una duración con fracciones de segundo")
    void rejectsFractionalTtl(Duration ttl) {
        assertThatThrownBy(() -> new JwtProperties(ISSUER, SECRET_32, ttl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pulsepass.security.jwt.access-token-ttl must be a whole number of seconds");
    }

    @Test
    @DisplayName("NFR-SEC-001: ni toString ni los mensajes de error muestran la clave")
    void neverExposesTheSecret() {
        JwtProperties properties = new JwtProperties(ISSUER, SECRET_32, TTL);

        assertThat(properties.toString()).doesNotContain(SECRET_32).contains("****");
        assertThatThrownBy(() -> new JwtProperties(ISSUER, SECRET_31, TTL))
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain(SECRET_31));
    }

    static Stream<Duration> nonPositiveTtls() {
        return Stream.of(null, Duration.ZERO, Duration.ofSeconds(-1));
    }

    static Stream<Duration> fractionalTtls() {
        return Stream.of(Duration.parse("PT0.5S"), Duration.parse("PT15M0.5S"));
    }

    private static String base64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }
}
