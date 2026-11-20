package com.pulsepass.dto.request;

import com.pulsepass.dto.response.AuthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.Set;

import static com.pulsepass.dto.request.RequestValidationSupport.emailOfLength;
import static com.pulsepass.dto.request.RequestValidationSupport.text;
import static com.pulsepass.dto.request.RequestValidationSupport.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de Bean Validation de {@link LoginRequest} (PRD Seguridad §10.1, §10.2; P4-07-A) y de que
 * los {@code toString} del login no muestran secretos (NFR-SEC-001; P4-07-A y B).
 */
class LoginRequestValidationTest {

    /** Contraseña del ejemplo del PRD Seguridad §10.2. */
    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

    @Test
    @DisplayName("§10.2: el login del ejemplo del PRD no tiene violaciones")
    void validRequestHasNoViolations() {
        assertThat(violationsOf(new LoginRequest("andrea", PASSWORD))).isEmpty();
    }

    @ParameterizedTest(name = "valor [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("P4-07-A: identifier y password rechazan null, vacío y solo espacios (@NotBlank)")
    void requiredFieldsRejectNullEmptyAndBlank(String value) {
        assertThat(violationsOf(new LoginRequest(value, value))).containsExactly(
                Map.entry("identifier", Set.of("Identifier is required")),
                Map.entry("password", Set.of("Password is required")));
    }

    @Test
    @DisplayName("P4-07-A: el login no aplica el mínimo de 12 caracteres del registro")
    void shortPasswordIsAccepted() {
        assertThat(violationsOf(new LoginRequest("andrea", "corta"))).isEmpty(); // DATO DE PRUEBA
    }

    @Test
    @DisplayName("P4-07-A: un identifier de 254 caracteres se acepta y uno de 255 se rechaza")
    void identifierMaxLength() {
        assertThat(violationsOf(new LoginRequest(emailOfLength(254), PASSWORD))).isEmpty();
        assertThat(violationsOf(new LoginRequest(emailOfLength(255), PASSWORD))).containsExactly(
                Map.entry("identifier", Set.of("Identifier must not exceed 254 characters")));
    }

    @Test
    @DisplayName("P4-07-A: una contraseña de 72 bytes se acepta y una de 73 se rechaza")
    void passwordMaxBytes() {
        assertThat(violationsOf(new LoginRequest("andrea", text('p', 72)))).isEmpty(); // DATO DE PRUEBA
        assertThat(violationsOf(new LoginRequest("andrea", text('ñ', 37)))).containsExactly( // 74 bytes
                Map.entry("password", Set.of("Password must not exceed 72 bytes")));
    }

    @Test
    @DisplayName("NFR-SEC-001: los toString de LoginRequest y AuthResponse no muestran la contraseña ni el token")
    void toStringsHideSecrets() {
        assertThat(new LoginRequest("andrea", PASSWORD).toString())
                .doesNotContain(PASSWORD).contains("identifier=andrea", "password=***");
        assertThat(AuthResponse.bearer("token.de.prueba", 900).toString()) // DATO DE PRUEBA
                .doesNotContain("token.de.prueba").contains("accessToken=***", "tokenType=Bearer", "expiresIn=900");
    }
}
