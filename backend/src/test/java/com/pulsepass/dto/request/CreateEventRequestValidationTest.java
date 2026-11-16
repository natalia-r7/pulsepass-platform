package com.pulsepass.dto.request;

import com.pulsepass.domain.enums.EventCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static com.pulsepass.dto.request.RequestValidationSupport.text;
import static com.pulsepass.dto.request.RequestValidationSupport.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de Bean Validation de {@link CreateEventRequest} (PRD Controladores §11.1; P3-03,
 * P3-D4, P3-D5). Datos de CMF-2026 (PRD §23.2); la fecha es DATO DE PRUEBA.
 */
class CreateEventRequestValidationTest {

    /** DATO DE PRUEBA: el PRD no da la fecha de CMF-2026 (la misma que ServiceFixtures). */
    private static final LocalDateTime EVENT_DATE = LocalDateTime.of(2026, 12, 12, 20, 0);

    private static CreateEventRequest valid() {
        return new CreateEventRequest("CMF-2026", "Caribbean Music Fest 2026", null,
                EventCategory.MUSIC, EVENT_DATE, 18, "VEN-SMR-01");
    }

    @Test
    @DisplayName("§11.1: un request válido, sin descripción (opcional), no tiene violaciones")
    void validRequestHasNoViolations() {
        assertThat(violationsOf(valid())).isEmpty();
    }

    @ParameterizedTest(name = "valor [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("§11.1: eventCode, name y venueCode rechazan null, vacío y solo espacios (@NotBlank)")
    void requiredTextFieldsRejectNullEmptyAndBlank(String value) {
        // ARRANGE
        CreateEventRequest request = new CreateEventRequest(value, value, null,
                EventCategory.MUSIC, EVENT_DATE, 18, value);

        // ACT
        Map<String, Set<String>> violations = violationsOf(request);

        // ASSERT
        assertThat(violations).containsOnlyKeys("eventCode", "name", "venueCode");
        assertThat(violations.get("eventCode")).containsExactly("Event code is required");
        assertThat(violations.get("name")).containsExactly("Name is required");
        assertThat(violations.get("venueCode")).containsExactly("Venue code is required");
    }

    @Test
    @DisplayName("§11.1: category, eventDate y minimumAge rechazan null (@NotNull)")
    void requiredNonTextFieldsRejectNull() {
        // ARRANGE
        CreateEventRequest request = new CreateEventRequest("CMF-2026", "Caribbean Music Fest 2026",
                null, null, null, null, "VEN-SMR-01");

        // ACT
        Map<String, Set<String>> violations = violationsOf(request);

        // ASSERT
        assertThat(violations).containsOnlyKeys("category", "eventDate", "minimumAge");
        assertThat(violations.get("category")).containsExactly("Category is required");
        assertThat(violations.get("eventDate")).containsExactly("Event date is required");
        assertThat(violations.get("minimumAge")).containsExactly("Minimum age is required");
    }

    @Test
    @DisplayName("§11.1: minimumAge = -1 se rechaza (@Min(0))")
    void minimumAgeBelowZeroIsRejected() {
        // ARRANGE
        CreateEventRequest request = new CreateEventRequest("CMF-2026", "Caribbean Music Fest 2026",
                null, EventCategory.MUSIC, EVENT_DATE, -1, "VEN-SMR-01"); // DATO DE PRUEBA: límite

        // ACT + ASSERT
        assertThat(violationsOf(request))
                .containsExactly(Map.entry("minimumAge",
                        Set.of("Minimum age must be greater than or equal to 0")));
    }

    @Test
    @DisplayName("§11.1: minimumAge = 0 (sin restricción de edad) se acepta")
    void minimumAgeZeroIsAccepted() {
        CreateEventRequest request = new CreateEventRequest("CMF-2026", "Caribbean Music Fest 2026",
                null, EventCategory.MUSIC, EVENT_DATE, 0, "VEN-SMR-01"); // DATO DE PRUEBA: límite

        assertThat(violationsOf(request)).isEmpty();
    }

    @Test
    @DisplayName("P3-D4: los textos con la longitud máxima de su columna se aceptan")
    void textFieldsAtMaxLengthAreAccepted() {
        // ARRANGE: DATO DE PRUEBA, longitudes de V1
        CreateEventRequest request = new CreateEventRequest(text('E', 30), text('N', 150),
                text('D', 500), EventCategory.MUSIC, EVENT_DATE, 18, text('V', 30));

        // ACT + ASSERT
        assertThat(violationsOf(request)).isEmpty();
    }

    @Test
    @DisplayName("P3-D4: los textos que superan en 1 la longitud de su columna se rechazan")
    void textFieldsOverMaxLengthAreRejected() {
        // ARRANGE: DATO DE PRUEBA, longitud de V1 + 1
        CreateEventRequest request = new CreateEventRequest(text('E', 31), text('N', 151),
                text('D', 501), EventCategory.MUSIC, EVENT_DATE, 18, text('V', 31));

        // ACT
        Map<String, Set<String>> violations = violationsOf(request);

        // ASSERT
        assertThat(violations).containsOnlyKeys("eventCode", "name", "description", "venueCode");
        assertThat(violations.get("eventCode")).containsExactly("Event code must not exceed 30 characters");
        assertThat(violations.get("name")).containsExactly("Name must not exceed 150 characters");
        assertThat(violations.get("description")).containsExactly("Description must not exceed 500 characters");
        assertThat(violations.get("venueCode")).containsExactly("Venue code must not exceed 30 characters");
    }
}
