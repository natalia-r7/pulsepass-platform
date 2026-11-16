package com.pulsepass.dto.request;

import com.pulsepass.domain.enums.TicketType;
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
 * Pruebas de Bean Validation de {@link PurchaseTicketRequest} (PRD Controladores §11.3; P3-03,
 * P3-D4, P3-D5). Datos de Andrea y CMF-2026 (PRD §23).
 */
class PurchaseTicketRequestValidationTest {

    @Test
    @DisplayName("§11.3: un request válido no tiene violaciones")
    void validRequestHasNoViolations() {
        PurchaseTicketRequest request = new PurchaseTicketRequest("andrea@email.com", "CMF-2026", TicketType.VIP);

        assertThat(violationsOf(request)).isEmpty();
    }

    @ParameterizedTest(name = "valor [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("§11.3: userEmail y eventCode rechazan null, vacío y solo espacios (@NotBlank)")
    void requiredTextFieldsRejectNullEmptyAndBlank(String value) {
        // ARRANGE
        PurchaseTicketRequest request = new PurchaseTicketRequest(value, value, TicketType.VIP);

        // ACT
        Map<String, Set<String>> violations = violationsOf(request);

        // ASSERT: con solo espacios, el email además incumple @Email; por eso se usa contains.
        assertThat(violations).containsOnlyKeys("userEmail", "eventCode");
        assertThat(violations.get("userEmail")).contains("User email is required");
        assertThat(violations.get("eventCode")).containsExactly("Event code is required");
    }

    @Test
    @DisplayName("§11.3: type rechaza null (@NotNull)")
    void typeRejectsNull() {
        PurchaseTicketRequest request = new PurchaseTicketRequest("andrea@email.com", "CMF-2026", null);

        assertThat(violationsOf(request))
                .containsExactly(Map.entry("type", Set.of("Ticket type is required")));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"andrea", "andrea@", "@email.com", "andrea email.com"}) // DATO DE PRUEBA
    @DisplayName("§11.3: un userEmail con formato inválido se rechaza (@Email)")
    void invalidUserEmailIsRejected(String email) {
        PurchaseTicketRequest request = new PurchaseTicketRequest(email, "CMF-2026", TicketType.VIP);

        assertThat(violationsOf(request))
                .containsExactly(Map.entry("userEmail", Set.of("User email must be a valid email address")));
    }

    @Test
    @DisplayName("P3-D4: los textos con la longitud máxima de su columna se aceptan")
    void textFieldsAtMaxLengthAreAccepted() {
        // DATO DE PRUEBA: longitudes de V1 (users.email y events.event_code)
        PurchaseTicketRequest request = new PurchaseTicketRequest(emailOfLength(254), text('E', 30), TicketType.VIP);

        assertThat(violationsOf(request)).isEmpty();
    }

    @Test
    @DisplayName("P3-D4: los textos que superan en 1 la longitud de su columna se rechazan")
    void textFieldsOverMaxLengthAreRejected() {
        // ARRANGE: DATO DE PRUEBA, longitud de V1 + 1
        PurchaseTicketRequest request = new PurchaseTicketRequest(emailOfLength(255), text('E', 31), TicketType.VIP);

        // ACT
        Map<String, Set<String>> violations = violationsOf(request);

        // ASSERT
        assertThat(violations).containsOnlyKeys("userEmail", "eventCode");
        assertThat(violations.get("userEmail")).containsExactly("User email must not exceed 254 characters");
        assertThat(violations.get("eventCode")).containsExactly("Event code must not exceed 30 characters");
    }
}
