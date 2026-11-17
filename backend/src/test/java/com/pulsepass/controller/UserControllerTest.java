package com.pulsepass.controller;

import com.pulsepass.FixedClockTestConfiguration;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.security.WebSecurityTestConfiguration;
import com.pulsepass.security.WithSecurityPrincipal;
import com.pulsepass.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static com.pulsepass.controller.ControllerFixtures.ANDREA_EMAIL;
import static com.pulsepass.controller.ControllerFixtures.ANDREA_USERNAME;
import static com.pulsepass.controller.ControllerFixtures.andreaRegisterRequest;
import static com.pulsepass.controller.ControllerFixtures.andreaResponse;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de {@link UserController} (PRD Controladores §8.4, §11.2, §20, §21, §22.4;
 * TEST-CTRL-USR-001..005, AC-CTRL-007..008; P3-07 US-1..US-4).
 * <p>
 * {@code @WebMvcTest} carga solo este controlador y {@code GlobalExceptionHandler}; el servicio
 * es un {@code @MockitoBean} (QT-CTRL-002). Sin base de datos ni Testcontainers (§21,
 * NFR-CTRL-008). {@link FixedClockTestConfiguration} aporta el {@code Clock} que necesita el
 * handler. Los JSON van escritos como texto (P3-D12).
 */
@WebMvcTest(UserController.class)
@Import({FixedClockTestConfiguration.class, WebSecurityTestConfiguration.class})
class UserControllerTest {

    /** JSON del registro de Andrea; debe coincidir campo a campo con {@code andreaRegisterRequest()}. */
    private static final String ANDREA_REGISTER_JSON = """
            {
              "username": "andrea",
              "email": "andrea@email.com",
              "firstName": "Andrea",
              "lastName": "Rojas",
              "birthDate": "2001-06-15"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // ---------------------------------------------------------------- POST /api/users

    @Test
    @DisplayName("TEST-CTRL-USR-001 / AC-CTRL-007: registro válido → 201 con UserResponse y sin Location")
    void registerReturns201WithUser() throws Exception {
        // ARRANGE
        when(userService.register(eq(andreaRegisterRequest()))).thenReturn(andreaResponse());

        // ACT + ASSERT
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ANDREA_REGISTER_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@email.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.firstName").value("Andrea"))
                .andExpect(jsonPath("$.lastName").value("Rojas"))
                .andExpect(jsonPath("$.phone").value(nullValue()))
                .andExpect(jsonPath("$.city").value(nullValue()))
                .andExpect(jsonPath("$.birthDate").value("2001-06-15"));

        // US-1: el record llega igual campo a campo, incluida la fecha deserializada.
        verify(userService).register(eq(andreaRegisterRequest()));
    }

    @Test
    @DisplayName("TEST-CTRL-USR-002: email con formato inválido → 400 y no se llama al servicio")
    void registerReturns400WhenEmailIsInvalid() throws Exception {
        // ARRANGE
        String invalidEmail = ANDREA_REGISTER_JSON.replace("\"andrea@email.com\"", "\"andrea\""); // DATO DE PRUEBA

        // ACT
        ResultActions result = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidEmail));

        // ASSERT
        expectError(result, HttpStatus.BAD_REQUEST, "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details.email").value("Email must be a valid email address"));

        verify(userService, never()).register(any());
    }

    @Test
    @DisplayName("TEST-CTRL-USR-003: username duplicado → 409 con ErrorResponse")
    void registerReturns409WhenUsernameIsDuplicated() throws Exception {
        // ARRANGE
        when(userService.register(eq(andreaRegisterRequest())))
                .thenThrow(new DuplicateResourceException("Username already exists: andrea"));

        // ACT
        ResultActions result = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ANDREA_REGISTER_JSON));

        // ASSERT
        expectError(result, HttpStatus.CONFLICT, "Username already exists: andrea")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(userService).register(eq(andreaRegisterRequest()));
    }

    @Test
    @DisplayName("AC-CTRL-008 / US-4: email duplicado → 409 sin exponer detalles internos")
    void registerReturns409WhenEmailIsDuplicated() throws Exception {
        // ARRANGE
        when(userService.register(eq(andreaRegisterRequest())))
                .thenThrow(new DuplicateResourceException("Email already exists: andrea@email.com"));

        // ACT
        ResultActions result = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ANDREA_REGISTER_JSON));

        // ASSERT: solo las 5 claves de ErrorResponse, details vacío y nada interno.
        expectError(result, HttpStatus.CONFLICT, "Email already exists: andrea@email.com")
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("uk_users_email"))))
                .andExpect(content().string(not(containsString("trace"))));

        verify(userService).register(eq(andreaRegisterRequest()));
    }

    @Test
    @DisplayName("BR-USER-005: fecha de nacimiento futura → 409 (regla del servicio)")
    void registerReturns409WhenBirthDateIsInTheFuture() throws Exception {
        // ARRANGE: el controlador no valida la fecha; la regla es del servicio (§11.4).
        LocalDate futureBirthDate = LocalDate.of(2030, 1, 1); // DATO DE PRUEBA
        RegisterUserRequest request = new RegisterUserRequest(ANDREA_USERNAME, ANDREA_EMAIL,
                "Andrea", "Rojas", null, null, futureBirthDate);
        String json = ANDREA_REGISTER_JSON.replace("\"2001-06-15\"", "\"2030-01-01\"");
        when(userService.register(eq(request)))
                .thenThrow(new BusinessRuleException("Birth date cannot be in the future: 2030-01-01"));

        // ACT
        ResultActions result = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));

        // ASSERT
        expectError(result, HttpStatus.CONFLICT, "Birth date cannot be in the future: 2030-01-01")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(userService).register(eq(request));
    }

    @Test
    @DisplayName("§11.2 / US-3: cuerpo vacío → 400 con exactamente los 5 campos obligatorios y sin llamar al servicio")
    void registerReturns400WhenRequiredFieldsAreMissing() throws Exception {
        // ACT
        ResultActions result = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));

        // ASSERT
        expectError(result, HttpStatus.BAD_REQUEST, "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(5))
                .andExpect(jsonPath("$.details.username").value("Username is required"))
                .andExpect(jsonPath("$.details.email").value("Email is required"))
                .andExpect(jsonPath("$.details.firstName").value("First name is required"))
                .andExpect(jsonPath("$.details.lastName").value("Last name is required"))
                .andExpect(jsonPath("$.details.birthDate").value("Birth date is required"));

        verify(userService, never()).register(any());
    }

    // ---------------------------------------------------------------- GET /api/users/by-email

    @Test
    @DisplayName("TEST-CTRL-USR-004: GET /api/users/by-email?email=andrea@email.com → 200; el valor llega sin cambios")
    @WithSecurityPrincipal(roles = Role.USER)
    void findByEmailReturns200WithUser() throws Exception {
        // ARRANGE
        when(userService.findByEmail(ANDREA_EMAIL)).thenReturn(andreaResponse());

        // ACT + ASSERT
        mockMvc.perform(get("/api/users/by-email").param("email", ANDREA_EMAIL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@email.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.birthDate").value("2001-06-15"));

        verify(userService).findByEmail(eq(ANDREA_EMAIL));
        verify(userService, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("FR-CTRL-USR-002: email inexistente → 404 con ErrorResponse")
    @WithSecurityPrincipal(roles = Role.USER)
    void findByEmailReturns404WhenUserDoesNotExist() throws Exception {
        // ARRANGE
        when(userService.findByEmail("nadie@email.com")) // DATO DE PRUEBA
                .thenThrow(new ResourceNotFoundException("User not found: nadie@email.com"));

        // ACT
        ResultActions result = mockMvc.perform(get("/api/users/by-email").param("email", "nadie@email.com"));

        // ASSERT
        expectError(result, HttpStatus.NOT_FOUND, "User not found: nadie@email.com")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(userService).findByEmail(eq("nadie@email.com"));
    }

    @Test
    @DisplayName("QT-CTRL-007: sin el parámetro email → 400 y no se llama al servicio")
    @WithSecurityPrincipal(roles = Role.USER)
    void findByEmailReturns400WhenParameterIsMissing() throws Exception {
        // ACT
        ResultActions result = mockMvc.perform(get("/api/users/by-email"));

        // ASSERT
        expectError(result, HttpStatus.BAD_REQUEST, "Required parameter 'email' is missing")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(userService, never()).findByEmail(anyString());
        verify(userService, never()).findByUsername(anyString());
        verify(userService, never()).register(any());
    }

    // ---------------------------------------------------------------- GET /api/users/by-username

    @Test
    @DisplayName("TEST-CTRL-USR-005: GET /api/users/by-username?username=andrea → 200; el valor llega sin cambios")
    @WithSecurityPrincipal(roles = Role.USER)
    void findByUsernameReturns200WithUser() throws Exception {
        // ARRANGE
        when(userService.findByUsername(ANDREA_USERNAME)).thenReturn(andreaResponse());

        // ACT + ASSERT
        mockMvc.perform(get("/api/users/by-username").param("username", ANDREA_USERNAME))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@email.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.birthDate").value("2001-06-15"));

        verify(userService).findByUsername(eq(ANDREA_USERNAME));
        verify(userService, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("FR-CTRL-USR-003: username inexistente → 404 con ErrorResponse")
    @WithSecurityPrincipal(roles = Role.USER)
    void findByUsernameReturns404WhenUserDoesNotExist() throws Exception {
        // ARRANGE
        when(userService.findByUsername("inexistente")) // DATO DE PRUEBA
                .thenThrow(new ResourceNotFoundException("User not found: inexistente"));

        // ACT
        ResultActions result = mockMvc.perform(get("/api/users/by-username").param("username", "inexistente"));

        // ASSERT
        expectError(result, HttpStatus.NOT_FOUND, "User not found: inexistente")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(userService).findByUsername(eq("inexistente"));
    }

    @Test
    @DisplayName("QT-CTRL-007: sin el parámetro username → 400 y no se llama al servicio")
    @WithSecurityPrincipal(roles = Role.USER)
    void findByUsernameReturns400WhenParameterIsMissing() throws Exception {
        // ACT
        ResultActions result = mockMvc.perform(get("/api/users/by-username"));

        // ASSERT
        expectError(result, HttpStatus.BAD_REQUEST, "Required parameter 'username' is missing")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(userService, never()).findByEmail(anyString());
        verify(userService, never()).findByUsername(anyString());
        verify(userService, never()).register(any());
    }

    // ---------------------------------------------------------------- Auxiliares

    /**
     * Contrato común de {@code ErrorResponse} (§13): estado, JSON, exactamente 5 claves,
     * {@code timestamp} del reloj fijo, {@code status}, {@code error} y {@code message}.
     */
    private static ResultActions expectError(ResultActions result, HttpStatus status, String message)
            throws Exception {
        return result
                .andExpect(status().is(status.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.timestamp").value(FixedClockTestConfiguration.EXPECTED_TIMESTAMP))
                .andExpect(jsonPath("$.status").value(status.value()))
                .andExpect(jsonPath("$.error").value(status.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(message));
    }
}
