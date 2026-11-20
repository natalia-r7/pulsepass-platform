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
 * <p>
 * Desde P4-06 el request lleva {@code password} (PRD Seguridad §3.4, §9.3; P4-D14, P4-06-B, D y E):
 * las pruebas anteriores usan una contraseña válida para que sigan comprobando solo sus campos.
 */
class RegisterUserRequestValidationTest {

    private static final LocalDate BIRTH_DATE = LocalDate.of(2001, 6, 15); // DATO DE PRUEBA

    /** Contraseña válida del ejemplo del PRD Seguridad §10.2. */
    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

    private static RegisterUserRequest valid() {
        return withPassword(PASSWORD);
    }

    private static RegisterUserRequest withPassword(String password) {
        return new RegisterUserRequest("andrea", "andrea@email.com", "Andrea", "Rojas",
                null, null, BIRTH_DATE, password);
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
                null, null, BIRTH_DATE, PASSWORD);

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
                "Andrea", "Rojas", null, null, null, PASSWORD);

        assertThat(violationsOf(request))
                .containsExactly(Map.entry("birthDate", Set.of("Birth date is required")));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"andrea", "andrea@", "@email.com", "andrea email.com"}) // DATO DE PRUEBA
    @DisplayName("§11.2: un email con formato inválido se rechaza (@Email)")
    void invalidEmailIsRejected(String email) {
        RegisterUserRequest request = new RegisterUserRequest("andrea", email, "Andrea", "Rojas",
                null, null, BIRTH_DATE, PASSWORD);

        assertThat(violationsOf(request))
                .containsExactly(Map.entry("email", Set.of("Email must be a valid email address")));
    }

    @Test
    @DisplayName("P3-D4: los textos con la longitud máxima de su columna se aceptan")
    void textFieldsAtMaxLengthAreAccepted() {
        // ARRANGE: DATO DE PRUEBA, longitudes de V1
        RegisterUserRequest request = new RegisterUserRequest(text('u', 50), emailOfLength(254),
                text('F', 100), text('L', 100), text('9', 30), text('C', 100), BIRTH_DATE, PASSWORD);

        // ACT + ASSERT
        assertThat(violationsOf(request)).isEmpty();
    }

    @Test
    @DisplayName("P3-D4: los textos que superan en 1 la longitud de su columna se rechazan")
    void textFieldsOverMaxLengthAreRejected() {
        // ARRANGE: DATO DE PRUEBA, longitud de V1 + 1
        RegisterUserRequest request = new RegisterUserRequest(text('u', 51), emailOfLength(255),
                text('F', 101), text('L', 101), text('9', 31), text('C', 101), BIRTH_DATE, PASSWORD);

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

    // ---------------------------------------------------------------- password (P4-06)

    @Test
    @DisplayName("Seguridad §3.4: password null se rechaza solo como obligatorio (@NotBlank)")
    void passwordRejectsNull() {
        assertThat(violationsOf(withPassword(null)))
                .containsExactly(Map.entry("password", Set.of("Password is required")));
    }

    @ParameterizedTest(name = "valor [{0}]")
    @ValueSource(strings = {"", "   "})
    @DisplayName("Seguridad §3.4: password vacío o solo con espacios se rechaza (@NotBlank y mínimo de 12)")
    void passwordRejectsEmptyAndBlank(String password) {
        // ACT
        Map<String, Set<String>> violations = violationsOf(withPassword(password));

        // ASSERT: además de vacío, tiene menos de 12 caracteres.
        assertThat(violations).containsOnlyKeys("password");
        assertThat(violations.get("password"))
                .containsExactlyInAnyOrder("Password is required", "Password must be at least 12 characters");
    }

    @Test
    @DisplayName("Seguridad §9.3: password de 11 caracteres se rechaza (mínimo 12)")
    void passwordShorterThan12CharactersIsRejected() {
        assertThat(violationsOf(withPassword(text('p', 11)))) // DATO DE PRUEBA
                .containsExactly(Map.entry("password", Set.of("Password must be at least 12 characters")));
    }

    @Test
    @DisplayName("Seguridad §9.3: password de exactamente 12 caracteres se acepta")
    void passwordOf12CharactersIsAccepted() {
        assertThat(violationsOf(withPassword(text('p', 12)))).isEmpty(); // DATO DE PRUEBA
    }

    @Test
    @DisplayName("Seguridad §9.3: password de exactamente 72 bytes ASCII se acepta")
    void passwordOf72AsciiBytesIsAccepted() {
        assertThat(violationsOf(withPassword(text('p', 72)))).isEmpty(); // DATO DE PRUEBA
    }

    @Test
    @DisplayName("Seguridad §9.3: password de 73 bytes ASCII se rechaza (máximo 72 bytes de BCrypt)")
    void passwordOf73AsciiBytesIsRejected() {
        assertThat(violationsOf(withPassword(text('p', 73)))) // DATO DE PRUEBA
                .containsExactly(Map.entry("password", Set.of("Password must not exceed 72 bytes")));
    }

    @Test
    @DisplayName("P4-D14: 36 «ñ» son 72 bytes UTF-8 y se aceptan")
    void passwordOf72MultibyteBytesIsAccepted() {
        assertThat(violationsOf(withPassword(text('ñ', 36)))).isEmpty(); // DATO DE PRUEBA
    }

    @Test
    @DisplayName("P4-D14: 37 «ñ» son solo 37 caracteres pero 74 bytes UTF-8, y se rechazan")
    void passwordOver72MultibyteBytesIsRejected() {
        assertThat(violationsOf(withPassword(text('ñ', 37)))) // DATO DE PRUEBA
                .containsExactly(Map.entry("password", Set.of("Password must not exceed 72 bytes")));
    }

    @Test
    @DisplayName("P4-06-G / NFR-SEC-001: toString no muestra la contraseña")
    void toStringHidesPassword() {
        // ACT
        String text = valid().toString();

        // ASSERT
        assertThat(text)
                .doesNotContain(PASSWORD)
                .contains("password=***")
                .contains("username=andrea", "email=andrea@email.com", "birthDate=2001-06-15");
    }
}
