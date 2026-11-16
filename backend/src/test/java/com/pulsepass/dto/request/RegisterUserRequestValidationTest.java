package com.pulsepass.dto.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static com.pulsepass.dto.request.RequestValidationSupport.emailOfLength;
import static com.pulsepass.dto.request.RequestValidationSupport.text;
import static com.pulsepass.dto.request.RequestValidationSupport.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de Bean Validation de {@link RegisterUserRequest} (PRD Controladores §11.2; P3-03,
 * P3-D4, P3-D5). Datos de Andrea (PRD §23.4); apellido y fecha de nacimiento son DATO DE PRUEBA
 * (los mismos que ServiceFixtures).
 */
class RegisterUserRequestValidationTest {

    private static final LocalDate BIRTH_DATE = LocalDate.of(2001, 6, 15); // DATO DE PRUEBA

    private static RegisterUserRequest valid() {
        return new RegisterUserRequest("andrea", "andrea@email.com", "Andrea", "Rojas",
                null, null, BIRTH_DATE);
    }

    @Test
    @DisplayName("§11.2: un request válido, sin phone ni city (opcionales), no tiene violaciones")
    void validRequestHasNoViolations() {
        assertThat(violationsOf(valid())).isEmpty();
    }

    @ParameterizedTest(name = "valor [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("§11.2: username, email, firstName y lastName rechazan null, vacío y solo espacios (@NotBlank)")
    void requiredTextFieldsRejectNullEmptyAndBlank(String value) {
        // ARRANGE
        RegisterUserRequest request = new RegisterUserRequest(value, value, value, value,
                null, null, BIRTH_DATE);

        // ACT
        Map<String, Set<String>> violations = violationsOf(request);

        // ASSERT: con solo espacios, el email además incumple @Email; por eso se usa contains.
        assertThat(violations).containsOnlyKeys("username", "email", "firstName", "lastName");
        assertThat(violations.get("username")).containsExactly("Username is required");
        assertThat(violations.get("email")).contains("Email is required");
        assertThat(violations.get("firstName")).containsExactly("First name is required");
        assertThat(violations.get("lastName")).containsExactly("Last name is required");
    }

    @Test
    @DisplayName("§11.2: birthDate rechaza null (@NotNull)")
    void birthDateRejectsNull() {
        RegisterUserRequest request = new RegisterUserRequest("andrea", "andrea@email.com",
                "Andrea", "Rojas", null, null, null);

        assertThat(violationsOf(request))
                .containsExactly(Map.entry("birthDate", Set.of("Birth date is required")));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"andrea", "andrea@", "@email.com", "andrea email.com"}) // DATO DE PRUEBA
    @DisplayName("§11.2: un email con formato inválido se rechaza (@Email)")
    void invalidEmailIsRejected(String email) {
        RegisterUserRequest request = new RegisterUserRequest("andrea", email, "Andrea", "Rojas",
                null, null, BIRTH_DATE);

        assertThat(violationsOf(request))
                .containsExactly(Map.entry("email", Set.of("Email must be a valid email address")));
    }

    @Test
    @DisplayName("P3-D4: los textos con la longitud máxima de su columna se aceptan")
    void textFieldsAtMaxLengthAreAccepted() {
        // ARRANGE: DATO DE PRUEBA, longitudes de V1
        RegisterUserRequest request = new RegisterUserRequest(text('u', 50), emailOfLength(254),
                text('F', 100), text('L', 100), text('9', 30), text('C', 100), BIRTH_DATE);

        // ACT + ASSERT
        assertThat(violationsOf(request)).isEmpty();
    }

    @Test
    @DisplayName("P3-D4: los textos que superan en 1 la longitud de su columna se rechazan")
    void textFieldsOverMaxLengthAreRejected() {
        // ARRANGE: DATO DE PRUEBA, longitud de V1 + 1
        RegisterUserRequest request = new RegisterUserRequest(text('u', 51), emailOfLength(255),
                text('F', 101), text('L', 101), text('9', 31), text('C', 101), BIRTH_DATE);

        // ACT
        Map<String, Set<String>> violations = violationsOf(request);

        // ASSERT
        assertThat(violations).containsOnlyKeys("username", "email", "firstName", "lastName", "phone", "city");
        assertThat(violations.get("username")).containsExactly("Username must not exceed 50 characters");
        assertThat(violations.get("email")).containsExactly("Email must not exceed 254 characters");
        assertThat(violations.get("firstName")).containsExactly("First name must not exceed 100 characters");
        assertThat(violations.get("lastName")).containsExactly("Last name must not exceed 100 characters");
        assertThat(violations.get("phone")).containsExactly("Phone must not exceed 30 characters");
        assertThat(violations.get("city")).containsExactly("City must not exceed 100 characters");
    }
}
