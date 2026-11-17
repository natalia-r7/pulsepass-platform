package com.pulsepass.controller;

import com.pulsepass.FixedClockTestConfiguration;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.security.WebSecurityTestConfiguration;
import com.pulsepass.security.WithSecurityPrincipal;
import com.pulsepass.service.TicketService;
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

import java.util.List;

import static com.pulsepass.controller.ControllerFixtures.ANDREA_EMAIL;
import static com.pulsepass.controller.ControllerFixtures.ANDREA_SECOND_TICKET_CODE;
import static com.pulsepass.controller.ControllerFixtures.ANDREA_TICKET_CODE;
import static com.pulsepass.controller.ControllerFixtures.CMF_2026_CODE;
import static com.pulsepass.controller.ControllerFixtures.CMF_2026_EVENT_DATE;
import static com.pulsepass.controller.ControllerFixtures.CMF_2026_NAME;
import static com.pulsepass.controller.ControllerFixtures.andreaPurchaseRequest;
import static com.pulsepass.controller.ControllerFixtures.andreaTicketResponse;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de {@link TicketController} (PRD Controladores §8.5, §11.3, §20, §21, §22.5;
 * TEST-CTRL-TKT-001..011, AC-CTRL-009..014; P3-09 TKC-1..TKC-5).
 * <p>
 * {@code @WebMvcTest} carga solo este controlador y {@code GlobalExceptionHandler}; el servicio
 * es un {@code @MockitoBean} (QT-CTRL-002). Sin base de datos ni Testcontainers (§21,
 * NFR-CTRL-008). {@link FixedClockTestConfiguration} aporta el {@code Clock} que necesita el
 * handler. Los JSON van escritos como texto (P3-D12). El orden de los tickets de un usuario lo
 * garantiza el servicio (P2-D7), así que la prueba lo comprueba tal cual.
 */
@WebMvcTest(TicketController.class)
@Import({FixedClockTestConfiguration.class, WebSecurityTestConfiguration.class})
@WithSecurityPrincipal(roles = Role.USER)
class TicketControllerTest {

    /** JSON de la compra de Andrea; debe coincidir campo a campo con {@code andreaPurchaseRequest()}. */
    private static final String ANDREA_PURCHASE_JSON = """
            {
              "userEmail": "andrea@email.com",
              "eventCode": "CMF-2026",
              "type": "VIP"
            }
            """;

    /** Fecha de compra tal como aparece en el JSON de respuesta. */
    private static final String PURCHASE_DATE_JSON = "2026-11-15T12:00:00";

    /** DATO DE PRUEBA: código de un ticket que no existe. */
    private static final String UNKNOWN_TICKET_CODE = "TKT-000000";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    // ---------------------------------------------------------------- POST /api/tickets

    @Test
    @DisplayName("TEST-CTRL-TKT-001 / AC-CTRL-009: compra válida → 201 con el TicketResponse PAID y sin Location")
    void purchaseReturns201WithTicket() throws Exception {
        // ARRANGE
        when(ticketService.purchase(eq(andreaPurchaseRequest())))
                .thenReturn(andreaTicketResponse(ANDREA_TICKET_CODE, TicketStatus.PAID));

        // ACT + ASSERT
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ANDREA_PURCHASE_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ticketCode").value(ANDREA_TICKET_CODE))
                .andExpect(jsonPath("$.type").value("VIP"))
                .andExpect(jsonPath("$.price").value(200000.0))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.purchaseDate").value(PURCHASE_DATE_JSON))
                .andExpect(jsonPath("$.userEmail").value(ANDREA_EMAIL))
                .andExpect(jsonPath("$.eventCode").value(CMF_2026_CODE))
                .andExpect(jsonPath("$.eventName").value(CMF_2026_NAME));

        // El record llega igual campo a campo, incluido el enum deserializado.
        verify(ticketService).purchase(eq(andreaPurchaseRequest()));
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-002 / TKC-3: cuerpo vacío → 400 con exactamente los 3 campos obligatorios y sin llamar al servicio")
    void purchaseReturns400WhenRequiredFieldsAreMissing() throws Exception {
        // ACT
        ResultActions result = mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));

        // ASSERT
        expectError(result, HttpStatus.BAD_REQUEST, "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(3))
                .andExpect(jsonPath("$.details.userEmail").value("User email is required"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.type").value("Ticket type is required"));

        verify(ticketService, never()).purchase(any());
    }

    @Test
    @DisplayName("§11.3: userEmail con formato inválido → 400 y no se llama al servicio")
    void purchaseReturns400WhenUserEmailIsInvalid() throws Exception {
        // ARRANGE
        String json = ANDREA_PURCHASE_JSON.replace("\"andrea@email.com\"", "\"andrea\""); // DATO DE PRUEBA

        // ACT
        ResultActions result = mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));

        // ASSERT
        expectError(result, HttpStatus.BAD_REQUEST, "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details.userEmail").value("User email must be a valid email address"));

        verify(ticketService, never()).purchase(any());
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-003: usuario inexistente → 404 con ErrorResponse")
    void purchaseReturns404WhenUserDoesNotExist() throws Exception {
        // ARRANGE
        when(ticketService.purchase(eq(andreaPurchaseRequest())))
                .thenThrow(new ResourceNotFoundException("User not found: andrea@email.com"));

        // ACT
        ResultActions result = mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ANDREA_PURCHASE_JSON));

        // ASSERT
        expectError(result, HttpStatus.NOT_FOUND, "User not found: andrea@email.com")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(ticketService).purchase(eq(andreaPurchaseRequest()));
    }

    @Test
    @DisplayName("FR-CTRL-TKT-001: evento inexistente → 404 con ErrorResponse")
    void purchaseReturns404WhenEventDoesNotExist() throws Exception {
        // ARRANGE
        when(ticketService.purchase(eq(andreaPurchaseRequest())))
                .thenThrow(new ResourceNotFoundException("Event not found: CMF-2026"));

        // ACT
        ResultActions result = mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ANDREA_PURCHASE_JSON));

        // ASSERT
        expectError(result, HttpStatus.NOT_FOUND, "Event not found: CMF-2026")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(ticketService).purchase(eq(andreaPurchaseRequest()));
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-004 / AC-CTRL-010: usuario menor de edad → 409 (regla del servicio)")
    void purchaseReturns409WhenUserIsUnderMinimumAge() throws Exception {
        // ARRANGE: el controlador no evalúa la edad; la regla es del servicio (§11.4).
        when(ticketService.purchase(eq(andreaPurchaseRequest())))
                .thenThrow(new BusinessRuleException(
                        "User does not meet minimum age for event CMF-2026: laura@email.com"));

        // ACT
        ResultActions result = mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ANDREA_PURCHASE_JSON));

        // ASSERT
        expectError(result, HttpStatus.CONFLICT,
                "User does not meet minimum age for event CMF-2026: laura@email.com")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(ticketService).purchase(eq(andreaPurchaseRequest()));
    }

    @Test
    @DisplayName("P2-13 TK-5 / NFR-CTRL-007: sin código único → 500 genérico, sin el mensaje interno")
    void purchaseReturns500WhenTicketCodeCannotBeGenerated() throws Exception {
        // ARRANGE
        when(ticketService.purchase(eq(andreaPurchaseRequest())))
                .thenThrow(new IllegalStateException("Could not generate a unique ticket code"));

        // ACT
        ResultActions result = mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ANDREA_PURCHASE_JSON));

        // ASSERT
        expectError(result, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error")
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(content().string(not(containsString("Could not generate"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("trace"))));

        verify(ticketService).purchase(eq(andreaPurchaseRequest()));
    }

    // ---------------------------------------------------------------- GET /api/tickets/{ticketCode}

    @Test
    @DisplayName("TEST-CTRL-TKT-005: GET /api/tickets/TKT-8F2A1C → 200 con el ticket")
    void findByCodeReturns200WithTicket() throws Exception {
        // ARRANGE
        when(ticketService.findByCode(ANDREA_TICKET_CODE))
                .thenReturn(andreaTicketResponse(ANDREA_TICKET_CODE, TicketStatus.PAID));

        // ACT + ASSERT
        mockMvc.perform(get("/api/tickets/{ticketCode}", ANDREA_TICKET_CODE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value(ANDREA_TICKET_CODE))
                .andExpect(jsonPath("$.type").value("VIP"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.userEmail").value(ANDREA_EMAIL))
                .andExpect(jsonPath("$.eventCode").value(CMF_2026_CODE));

        verify(ticketService).findByCode(eq(ANDREA_TICKET_CODE));
    }

    @Test
    @DisplayName("FR-CTRL-TKT-002: ticket inexistente → 404 con ErrorResponse")
    void findByCodeReturns404WhenTicketDoesNotExist() throws Exception {
        // ARRANGE
        when(ticketService.findByCode(UNKNOWN_TICKET_CODE))
                .thenThrow(new ResourceNotFoundException("Ticket not found: " + UNKNOWN_TICKET_CODE));

        // ACT
        ResultActions result = mockMvc.perform(get("/api/tickets/{ticketCode}", UNKNOWN_TICKET_CODE));

        // ASSERT
        expectError(result, HttpStatus.NOT_FOUND, "Ticket not found: TKT-000000")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(ticketService).findByCode(eq(UNKNOWN_TICKET_CODE));
    }

    // ---------------------------------------------------------------- GET /api/tickets/by-user

    @Test
    @DisplayName("TEST-CTRL-TKT-006: GET /api/tickets/by-user → 200 en el orden del servicio; «by-user» no se toma como código")
    void findByUserEmailReturns200WithList() throws Exception {
        // ARRANGE: el servicio los devuelve del más reciente al más antiguo (P2-D7).
        when(ticketService.findByUserEmail(ANDREA_EMAIL)).thenReturn(List.of(
                andreaTicketResponse(ANDREA_TICKET_CODE, TicketStatus.PAID),
                andreaTicketResponse(ANDREA_SECOND_TICKET_CODE, TicketStatus.CANCELLED)));

        // ACT + ASSERT
        mockMvc.perform(get("/api/tickets/by-user").param("email", ANDREA_EMAIL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ticketCode").value(ANDREA_TICKET_CODE))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[1].ticketCode").value(ANDREA_SECOND_TICKET_CODE))
                .andExpect(jsonPath("$[1].status").value("CANCELLED"));

        verify(ticketService).findByUserEmail(eq(ANDREA_EMAIL));
        verify(ticketService, never()).findByCode(anyString());
    }

    @Test
    @DisplayName("QT-CTRL-007: sin el parámetro email → 400 y no se llama al servicio")
    void findByUserEmailReturns400WhenParameterIsMissing() throws Exception {
        // ACT
        ResultActions result = mockMvc.perform(get("/api/tickets/by-user"));

        // ASSERT
        expectError(result, HttpStatus.BAD_REQUEST, "Required parameter 'email' is missing")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(ticketService, never()).findByUserEmail(anyString());
        verify(ticketService, never()).findByCode(anyString());
    }

    @Test
    @DisplayName("P2-13 TK-7: usuario sin tickets → 200 con una lista vacía (no 404)")
    void findByUserEmailReturns200WithEmptyList() throws Exception {
        // ARRANGE
        when(ticketService.findByUserEmail("nadie@email.com")).thenReturn(List.of()); // DATO DE PRUEBA

        // ACT + ASSERT
        mockMvc.perform(get("/api/tickets/by-user").param("email", "nadie@email.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(ticketService).findByUserEmail(eq("nadie@email.com"));
    }

    // ---------------------------------------------------------------- GET /api/events/{eventCode}/tickets/paid

    @Test
    @DisplayName("TEST-CTRL-TKT-007: GET /api/events/CMF-2026/tickets/paid → 200 con los tickets PAID")
    @WithSecurityPrincipal(roles = Role.STAFF)
    void findPaidTicketsByEventReturns200WithList() throws Exception {
        // ARRANGE
        when(ticketService.findPaidTicketsByEvent(CMF_2026_CODE))
                .thenReturn(List.of(andreaTicketResponse(ANDREA_TICKET_CODE, TicketStatus.PAID)));

        // ACT + ASSERT
        mockMvc.perform(get("/api/events/{eventCode}/tickets/paid", CMF_2026_CODE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ticketCode").value(ANDREA_TICKET_CODE))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].eventCode").value(CMF_2026_CODE));

        verify(ticketService).findPaidTicketsByEvent(eq(CMF_2026_CODE));
    }

    @Test
    @DisplayName("P2-13 TK-7: evento sin tickets pagados → 200 con una lista vacía (no 404)")
    @WithSecurityPrincipal(roles = Role.STAFF)
    void findPaidTicketsByEventReturns200WithEmptyList() throws Exception {
        // ARRANGE
        when(ticketService.findPaidTicketsByEvent("XXX-0000")).thenReturn(List.of()); // DATO DE PRUEBA

        // ACT + ASSERT
        mockMvc.perform(get("/api/events/{eventCode}/tickets/paid", "XXX-0000"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(ticketService).findPaidTicketsByEvent(eq("XXX-0000"));
    }

    // ---------------------------------------------------------------- PATCH /api/tickets/{ticketCode}/cancel

    @Test
    @DisplayName("TEST-CTRL-TKT-008 / AC-CTRL-011: cancelar un ticket PAID → 200 con el ticket CANCELLED")
    void cancelReturns200WithCancelledTicket() throws Exception {
        // ARRANGE
        when(ticketService.cancel(ANDREA_TICKET_CODE))
                .thenReturn(andreaTicketResponse(ANDREA_TICKET_CODE, TicketStatus.CANCELLED));

        // ACT + ASSERT
        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", ANDREA_TICKET_CODE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value(ANDREA_TICKET_CODE))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService).cancel(eq(ANDREA_TICKET_CODE));
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-009 / AC-CTRL-012: cancelar un ticket USED → 409 con ErrorResponse")
    void cancelReturns409WhenTicketIsUsed() throws Exception {
        // ARRANGE
        when(ticketService.cancel(ANDREA_TICKET_CODE))
                .thenThrow(new BusinessRuleException("Only PAID tickets can be cancelled: " + ANDREA_TICKET_CODE));

        // ACT
        ResultActions result = mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", ANDREA_TICKET_CODE));

        // ASSERT
        expectError(result, HttpStatus.CONFLICT, "Only PAID tickets can be cancelled: TKT-8F2A1C")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(ticketService).cancel(eq(ANDREA_TICKET_CODE));
    }

    @Test
    @DisplayName("BR-TICKET-012 / P2-D20: cancelar cuando el evento ya ocurrió → 409 con ErrorResponse")
    void cancelReturns409WhenEventDateHasPassed() throws Exception {
        // ARRANGE
        String message = "Event date must be in the future: " + CMF_2026_EVENT_DATE;
        when(ticketService.cancel(ANDREA_TICKET_CODE)).thenThrow(new BusinessRuleException(message));

        // ACT
        ResultActions result = mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", ANDREA_TICKET_CODE));

        // ASSERT
        expectError(result, HttpStatus.CONFLICT, "Event date must be in the future: 2026-12-12T20:00")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(ticketService).cancel(eq(ANDREA_TICKET_CODE));
    }

    @Test
    @DisplayName("FR-CTRL-TKT-005: cancelar un ticket inexistente → 404 con ErrorResponse")
    void cancelReturns404WhenTicketDoesNotExist() throws Exception {
        // ARRANGE
        when(ticketService.cancel(UNKNOWN_TICKET_CODE))
                .thenThrow(new ResourceNotFoundException("Ticket not found: " + UNKNOWN_TICKET_CODE));

        // ACT
        ResultActions result = mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", UNKNOWN_TICKET_CODE));

        // ASSERT
        expectError(result, HttpStatus.NOT_FOUND, "Ticket not found: TKT-000000")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(ticketService).cancel(eq(UNKNOWN_TICKET_CODE));
    }

    // ---------------------------------------------------------------- PATCH /api/tickets/{ticketCode}/use

    @Test
    @DisplayName("TEST-CTRL-TKT-010 / AC-CTRL-013: usar un ticket PAID → 200 con el ticket USED")
    @WithSecurityPrincipal(roles = Role.STAFF)
    void markAsUsedReturns200WithUsedTicket() throws Exception {
        // ARRANGE
        when(ticketService.markAsUsed(ANDREA_TICKET_CODE))
                .thenReturn(andreaTicketResponse(ANDREA_TICKET_CODE, TicketStatus.USED));

        // ACT + ASSERT
        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", ANDREA_TICKET_CODE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value(ANDREA_TICKET_CODE))
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed(eq(ANDREA_TICKET_CODE));
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-011 / AC-CTRL-014: usar un ticket CANCELLED → 409 con ErrorResponse")
    @WithSecurityPrincipal(roles = Role.STAFF)
    void markAsUsedReturns409WhenTicketIsCancelled() throws Exception {
        // ARRANGE
        when(ticketService.markAsUsed(ANDREA_TICKET_CODE))
                .thenThrow(new BusinessRuleException("Only PAID tickets can be used: " + ANDREA_TICKET_CODE));

        // ACT
        ResultActions result = mockMvc.perform(patch("/api/tickets/{ticketCode}/use", ANDREA_TICKET_CODE));

        // ASSERT
        expectError(result, HttpStatus.CONFLICT, "Only PAID tickets can be used: TKT-8F2A1C")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(ticketService).markAsUsed(eq(ANDREA_TICKET_CODE));
    }

    @Test
    @DisplayName("FR-CTRL-TKT-006: usar un ticket inexistente → 404 con ErrorResponse")
    @WithSecurityPrincipal(roles = Role.STAFF)
    void markAsUsedReturns404WhenTicketDoesNotExist() throws Exception {
        // ARRANGE
        when(ticketService.markAsUsed(UNKNOWN_TICKET_CODE))
                .thenThrow(new ResourceNotFoundException("Ticket not found: " + UNKNOWN_TICKET_CODE));

        // ACT
        ResultActions result = mockMvc.perform(patch("/api/tickets/{ticketCode}/use", UNKNOWN_TICKET_CODE));

        // ASSERT
        expectError(result, HttpStatus.NOT_FOUND, "Ticket not found: TKT-000000")
                .andExpect(jsonPath("$.details").isEmpty());

        verify(ticketService).markAsUsed(eq(UNKNOWN_TICKET_CODE));
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
