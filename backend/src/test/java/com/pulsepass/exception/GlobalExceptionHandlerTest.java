package com.pulsepass.exception;

import com.pulsepass.FixedClockTestConfiguration;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.security.WebSecurityTestConfiguration;
import com.pulsepass.security.WithSecurityPrincipal;
import jakarta.validation.Valid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de {@link GlobalExceptionHandler} (PRD Controladores §13–§16, QT-CTRL-003/004/011;
 * P3-04, P3-D3, P3-D5..P3-D7, P3-D10).
 * <p>
 * Todavía no hay controladores reales (P3-05 en adelante). Por eso la prueba usa un controlador
 * de sondeo, {@link ProbeController}, que solo existe en las pruebas y provoca cada tipo de
 * error. Es una clase anidada de la prueba: el escaneo de componentes la ignora y solo se
 * registra aquí, con {@code @Import}. Con {@code controllers = ProbeController.class}, esta
 * prueba no cargará los controladores reales cuando existan.
 * <p>
 * {@code @WebMvcTest} levanta solo la capa web (Spring MVC, el handler y Jackson), sin
 * servicios, sin base de datos y sin Testcontainers (§21, P3-D11). Los JSON van escritos como
 * texto (P3-D12).
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.ProbeController.class)
@Import({GlobalExceptionHandlerTest.ProbeController.class, FixedClockTestConfiguration.class, WebSecurityTestConfiguration.class})
@WithSecurityPrincipal(roles = Role.USER)
class GlobalExceptionHandlerTest {

    /** CreateEventRequest válido salvo por {@code eventCode}, ausente. Datos de CMF-2026 (§23). */
    private static final String EVENT_WITHOUT_CODE = """
            {
              "name": "Caribbean Music Fest 2026",
              "category": "MUSIC",
              "eventDate": "2026-12-12T20:00:00",
              "minimumAge": 18,
              "venueCode": "VEN-SMR-01"
            }
            """;

    /**
     * RegisterUserRequest con el email solo con espacios: incumple @NotBlank y @Email a la vez.
     * Datos de Andrea (§23); apellido y fecha son DATO DE PRUEBA. La contraseña (válida, del PRD
     * Seguridad §10.2) se añadió en P4-06 para que el email siga siendo el único campo inválido.
     */
    private static final String USER_WITH_BLANK_EMAIL = """
            {
              "username": "andrea",
              "email": "   ",
              "firstName": "Andrea",
              "lastName": "Rojas",
              "birthDate": "2001-06-15",
              "password": "UnaFraseSeguraLarga2026!"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    // ---------------------------------------------------------------- 400: Bean Validation

    @Test
    @DisplayName("§13/§14: un campo inválido → 400, «Validation failed» y el campo en details")
    void validationErrorOnOneFieldReturns400WithDetails() throws Exception {
        ResultActions result = mockMvc.perform(post("/probe/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(EVENT_WITHOUT_CODE));

        assertErrorEnvelope(result, HttpStatus.BAD_REQUEST, "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"));
    }

    @Test
    @DisplayName("P3-D6: varios errores en un mismo campo se unen en orden alfabético con «; »")
    void severalErrorsOnSameFieldAreJoinedAlphabetically() throws Exception {
        ResultActions result = mockMvc.perform(post("/probe/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(USER_WITH_BLANK_EMAIL));

        assertErrorEnvelope(result, HttpStatus.BAD_REQUEST, "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details.email")
                        .value("Email is required; Email must be a valid email address"));
    }

    // ---------------------------------------------------------------- 400: request mal formado

    @Test
    @DisplayName("§14: JSON mal formado → 400 «Malformed JSON request»")
    void malformedJsonReturns400() throws Exception {
        ResultActions result = mockMvc.perform(post("/probe/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventCode\": "));

        assertErrorEnvelope(result, HttpStatus.BAD_REQUEST, "Malformed JSON request")
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    @DisplayName("§14: un valor de enum desconocido hace el JSON ilegible → 400 «Malformed JSON request»")
    void unknownEnumValueReturns400() throws Exception {
        String unknownCategory = EVENT_WITHOUT_CODE.replace("\"MUSIC\"", "\"OPERA\""); // DATO DE PRUEBA

        ResultActions result = mockMvc.perform(post("/probe/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(unknownCategory));

        assertErrorEnvelope(result, HttpStatus.BAD_REQUEST, "Malformed JSON request");
    }

    @Test
    @DisplayName("P3-D3: parámetro obligatorio ausente → 400 con el nombre del parámetro")
    void missingRequestParameterReturns400() throws Exception {
        ResultActions result = mockMvc.perform(get("/probe/users/by-email"));

        assertErrorEnvelope(result, HttpStatus.BAD_REQUEST, "Required parameter 'email' is missing")
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    @DisplayName("P3-D3: valor incompatible con el tipo del parámetro → 400 con el nombre del parámetro")
    void typeMismatchReturns400() throws Exception {
        ResultActions result = mockMvc.perform(get("/probe/artists/abc")); // DATO DE PRUEBA

        assertErrorEnvelope(result, HttpStatus.BAD_REQUEST, "Invalid value for parameter 'id'")
                .andExpect(jsonPath("$.details").isEmpty());
    }

    // ---------------------------------------------------------------- 404 / 409 del dominio

    @Test
    @DisplayName("§14: ResourceNotFoundException → 404 con el mensaje del servicio")
    void resourceNotFoundReturns404() throws Exception {
        ResultActions result = mockMvc.perform(get("/probe/not-found"));

        assertErrorEnvelope(result, HttpStatus.NOT_FOUND, "Event not found: CMF-2026")
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    @DisplayName("§14: DuplicateResourceException → 409 con el mensaje del servicio")
    void duplicateResourceReturns409() throws Exception {
        ResultActions result = mockMvc.perform(get("/probe/duplicate"));

        assertErrorEnvelope(result, HttpStatus.CONFLICT, "Email already exists: andrea@email.com");
    }

    @Test
    @DisplayName("§14: BusinessRuleException → 409 con el mensaje del servicio")
    void businessRuleReturns409() throws Exception {
        ResultActions result = mockMvc.perform(get("/probe/business-rule"));

        assertErrorEnvelope(result, HttpStatus.CONFLICT, "Event has no available capacity: CMF-2026");
    }

    // ---------------------------------------------------------------- 404 / 405 / 415 de Spring MVC

    @Test
    @DisplayName("P3-D3: ruta inexistente → 404 con ErrorResponse")
    void unknownRouteReturns404() throws Exception {
        ResultActions result = mockMvc.perform(get("/probe/does-not-exist")); // DATO DE PRUEBA

        assertErrorEnvelope(result, HttpStatus.NOT_FOUND, "No endpoint found for GET /probe/does-not-exist");
    }

    @Test
    @DisplayName("P3-D3: método HTTP no permitido → 405 (se conserva) con la cabecera Allow")
    void methodNotAllowedReturns405() throws Exception {
        ResultActions result = mockMvc.perform(delete("/probe/not-found"));

        assertErrorEnvelope(result, HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method 'DELETE' is not supported for this endpoint")
                .andExpect(header().string(HttpHeaders.ALLOW, "GET"));
    }

    @Test
    @DisplayName("P3-D3: Content-Type no admitido → 415 (se conserva) con la cabecera Accept")
    void unsupportedMediaTypeReturns415() throws Exception {
        ResultActions result = mockMvc.perform(post("/probe/events")
                .contentType(MediaType.TEXT_PLAIN)
                .content("eventCode=CMF-2026"));

        // Spring pone en Accept los tipos que el endpoint sí admite; JSON debe estar entre ellos.
        assertErrorEnvelope(result, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type 'text/plain' is not supported")
                .andExpect(header().exists(HttpHeaders.ACCEPT))
                .andExpect(header().string(HttpHeaders.ACCEPT, containsString(MediaType.APPLICATION_JSON_VALUE)));
    }

    // ---------------------------------------------------------------- 500

    @Test
    @DisplayName("§14 / NFR-CTRL-007: una excepción inesperada → 500 genérico, sin mensaje ni traza internos")
    void unexpectedExceptionReturnsGeneric500() throws Exception {
        ResultActions result = mockMvc.perform(get("/probe/unexpected"));

        assertErrorEnvelope(result, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error")
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(content().string(not(containsString(ProbeController.INTERNAL_DETAIL))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "/probe/missing-path-variable, 500, MissingPathVariableException, Required path variable",
            "/probe/async-timeout,         503, AsyncRequestTimeoutException, timeout"
    })
    @DisplayName("NFR-CTRL-007: un error MVC heredado con estado 5xx conserva el estado, pero su mensaje es genérico")
    void inheritedMvc5xxErrorKeepsStatusWithGenericMessage(String path, int status, String exceptionName,
                                                           String springDetail) throws Exception {
        ResultActions result = mockMvc.perform(get(path));

        assertErrorEnvelope(result, HttpStatus.valueOf(status), "Unexpected error")
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(content().string(not(containsString(exceptionName))))
                .andExpect(content().string(not(containsString(springDetail))))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    @DisplayName("P3-D10: DataIntegrityViolationException → 500 genérico, sin detalles de la base de datos")
    void dataIntegrityViolationReturnsGeneric500() throws Exception {
        ResultActions result = mockMvc.perform(get("/probe/data-integrity"));

        assertErrorEnvelope(result, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error")
                .andExpect(content().string(not(containsString("uk_users_email"))));
    }

    // ---------------------------------------------------------------- Auxiliares

    /**
     * Comprueba el contrato común de §13: estado, Content-Type JSON, {@code status}, {@code error}
     * (frase estándar), {@code message} y el {@code timestamp} del reloj fijo truncado a segundos
     * (P3-D7). Las cinco claves, y solo ellas.
     */
    private static ResultActions assertErrorEnvelope(ResultActions result, HttpStatus status, String message)
            throws Exception {
        return result
                .andExpect(status().is(status.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.timestamp").value(FixedClockTestConfiguration.EXPECTED_TIMESTAMP))
                .andExpect(jsonPath("$.status").value(status.value()))
                .andExpect(jsonPath("$.error").value(status.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.details").isMap());
    }

    /** Controlador que solo existe en esta prueba para provocar cada tipo de error. */
    @RestController
    @RequestMapping("/probe")
    static class ProbeController {

        /** DATO DE PRUEBA: texto interno que nunca debe aparecer en una respuesta 500. */
        static final String INTERNAL_DETAIL = "jdbc:postgresql://db:5432/pulsepass password=secret";

        @PostMapping("/events")
        void createEvent(@Valid @RequestBody CreateEventRequest request) {
            // Si la validación pasa, no hace nada: la prueba solo usa casos inválidos.
        }

        @PostMapping("/users")
        void registerUser(@Valid @RequestBody RegisterUserRequest request) {
            // Igual que createEvent.
        }

        @GetMapping("/users/by-email")
        String findUserByEmail(@RequestParam String email) {
            return email;
        }

        @GetMapping("/artists/{id}")
        Long findArtist(@PathVariable Long id) {
            return id;
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Event not found: CMF-2026");
        }

        @GetMapping("/duplicate")
        void duplicate() {
            throw new DuplicateResourceException("Email already exists: andrea@email.com");
        }

        @GetMapping("/business-rule")
        void businessRule() {
            throw new BusinessRuleException("Event has no available capacity: CMF-2026");
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException(INTERNAL_DETAIL);
        }

        /**
         * Declara una variable de ruta que la plantilla no contiene: Spring lanza
         * {@code MissingPathVariableException}, que {@code ResponseEntityExceptionHandler}
         * traduce a 500.
         */
        @GetMapping("/missing-path-variable")
        String missingPathVariable(@PathVariable String code) {
            return code;
        }

        /** {@code AsyncRequestTimeoutException} es una excepción MVC heredada que da 503. */
        @GetMapping("/async-timeout")
        void asyncTimeout() {
            throw new AsyncRequestTimeoutException();
        }

        @GetMapping("/data-integrity")
        void dataIntegrity() {
            throw new DataIntegrityViolationException(
                    "duplicate key value violates unique constraint \"uk_users_email\"");
        }
    }
}
