package com.pulsepass.controller;

import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.dto.response.VenueResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos de prueba de la capa de controladores (P3-D12).
 * <p>
 * Construye los DTO que devuelven los servicios simulados, con el escenario del PRD de
 * Controladores (§23), que es el mismo de los PRD anteriores. Es independiente de
 * {@code ServiceFixtures}, que construye entidades: los controladores solo ven DTO.
 * <p>
 * Convención de comentarios: {@code PRD §23} indica un valor del escenario;
 * {@code DATO DE PRUEBA}, un valor que el PRD no define. Se irá ampliando con cada controlador.
 */
public final class ControllerFixtures {

    /** Código de VEN-SMR-01 (PRD §23.1). */
    public static final String VEN_SMR_01_CODE = "VEN-SMR-01";

    private ControllerFixtures() {
        // Clase de utilidades: no se instancia.
    }

    /** VEN-SMR-01 (PRD §23.1). {@code id} es DATO DE PRUEBA; §23 no da la dirección. */
    public static VenueResponse venSmr01Response() {
        return new VenueResponse(
                1L,                          // DATO DE PRUEBA
                VEN_SMR_01_CODE,             // PRD §23.1 code
                "Marina Convention Center",  // PRD §23.1 name
                "Santa Marta",               // PRD §23.1 city
                null,                        // §23 no da la dirección
                3,                           // PRD §23.1 capacity
                true);                       // PRD §23.1 active
    }

    // ---------------------------------------------------------------- Artists (P3-06 AR-1)

    // §23.3 solo da el nombre artístico. country y genre quedan en null, como en V2 y en
    // ServiceFixtures (A8); active = true es la misma inferencia documentada para V2.

    /** Nombre artístico de Solar Beat (PRD §23.3). */
    public static final String SOLAR_BEAT = "Solar Beat";

    /** Nombre artístico de Neon Waves (PRD §23.3). */
    public static final String NEON_WAVES = "Neon Waves";

    /** Nombre artístico de Caribbean Sound (PRD §23.3). */
    public static final String CARIBBEAN_SOUND = "Caribbean Sound";

    /** Solar Beat (PRD §23.3). */
    public static ArtistResponse solarBeatResponse() {
        return artist(1L, SOLAR_BEAT); // DATO DE PRUEBA: id
    }

    /** Neon Waves (PRD §23.3). */
    public static ArtistResponse neonWavesResponse() {
        return artist(2L, NEON_WAVES); // DATO DE PRUEBA: id
    }

    /** Caribbean Sound (PRD §23.3). */
    public static ArtistResponse caribbeanSoundResponse() {
        return artist(3L, CARIBBEAN_SOUND); // DATO DE PRUEBA: id
    }

    private static ArtistResponse artist(Long id, String stageName) {
        return new ArtistResponse(
                id,
                stageName,                   // PRD §23.3
                null,                        // §23 no da el país
                null,                        // §23 no da el género
                true);                       // DATO DE PRUEBA: active
    }

    // ---------------------------------------------------------------- Users (P3-07 US-1)

    // §23.4 solo dice «Andrea — adulta — active=true». El email viene del PRD de Servicios §47.
    // username, lastName y birthDate son DATO DE PRUEBA, los mismos que en ServiceFixtures
    // (25 años en la fecha de CMF-2026). phone y city quedan en null.

    /** Email de Andrea (PRD de Servicios §47). */
    public static final String ANDREA_EMAIL = "andrea@email.com";

    /** DATO DE PRUEBA: username de Andrea (el mismo que en ServiceFixtures). */
    public static final String ANDREA_USERNAME = "andrea";

    /** DATO DE PRUEBA: fecha de nacimiento de Andrea (la misma que en ServiceFixtures). */
    private static final LocalDate ANDREA_BIRTH_DATE = LocalDate.of(2001, 6, 15);

    /**
     * Contraseña de Andrea: la del ejemplo del PRD Seguridad §10.2 (P4-06-O). Ficticia, solo
     * para pruebas (§17.3).
     */
    public static final String ANDREA_PASSWORD = "UnaFraseSeguraLarga2026!";

    /** Registro de Andrea, sin phone ni city (opcionales). */
    public static RegisterUserRequest andreaRegisterRequest() {
        return new RegisterUserRequest(
                ANDREA_USERNAME,             // DATO DE PRUEBA
                ANDREA_EMAIL,                // PRD Servicios §47
                "Andrea",                    // PRD §23.4
                "Rojas",                     // DATO DE PRUEBA
                null,                        // phone (opcional)
                null,                        // city (opcional)
                ANDREA_BIRTH_DATE,           // DATO DE PRUEBA
                ANDREA_PASSWORD);            // PRD Seguridad §10.2
    }

    /** Usuario Andrea tal como lo devuelve el servicio tras registrarla. */
    public static UserResponse andreaResponse() {
        return new UserResponse(
                1L,                          // DATO DE PRUEBA
                ANDREA_USERNAME,             // DATO DE PRUEBA
                ANDREA_EMAIL,                // PRD Servicios §47
                true,                        // PRD §23.4 active
                "Andrea",                    // PRD §23.4
                "Rojas",                     // DATO DE PRUEBA
                null,                        // phone
                null,                        // city
                ANDREA_BIRTH_DATE);          // DATO DE PRUEBA
    }

    // ---------------------------------------------------------------- Events (P3-08 EV-1)

    // §23.2 da code, name, category, status inicial y minimumAge. eventDate es DATO DE PRUEBA,
    // la misma fecha de ServiceFixtures; description queda en null (opcional, §23 no la da).
    // El venue es VEN-SMR-01 (§23.1) y los artistas, los de §23.3.

    /** Código de CMF-2026 (PRD §23.2). */
    public static final String CMF_2026_CODE = "CMF-2026";

    /** Nombre de CMF-2026 (PRD §23.2). */
    public static final String CMF_2026_NAME = "Caribbean Music Fest 2026";

    /** DATO DE PRUEBA: §23 no da la fecha de CMF-2026 (la misma que en ServiceFixtures). */
    public static final LocalDateTime CMF_2026_EVENT_DATE = LocalDateTime.of(2026, 12, 12, 20, 0);

    /** Creación de CMF-2026 en VEN-SMR-01, sin descripción (opcional). */
    public static CreateEventRequest cmf2026CreateRequest() {
        return new CreateEventRequest(
                CMF_2026_CODE,               // PRD §23.2
                CMF_2026_NAME,               // PRD §23.2
                null,                        // description (opcional)
                EventCategory.MUSIC,         // PRD §23.2
                CMF_2026_EVENT_DATE,         // DATO DE PRUEBA
                18,                          // PRD §23.2 minimumAge
                VEN_SMR_01_CODE);            // PRD §23.1
    }

    /**
     * CMF-2026 tal como lo devuelve el servicio, con el estado y los artistas que pida cada
     * prueba (por ejemplo, DRAFT sin artistas tras crearlo).
     */
    public static EventResponse cmf2026Response(EventStatus status, ArtistResponse... artists) {
        return new EventResponse(
                1L,                          // DATO DE PRUEBA
                CMF_2026_CODE,               // PRD §23.2
                CMF_2026_NAME,               // PRD §23.2
                null,                        // description
                EventCategory.MUSIC,         // PRD §23.2
                status,
                CMF_2026_EVENT_DATE,         // DATO DE PRUEBA
                18,                          // PRD §23.2 minimumAge
                VEN_SMR_01_CODE,             // PRD §23.1
                venSmr01Response().name(),   // PRD §23.1
                List.of(artists));           // PRD §23.3
    }

    /** Resumen de CMF-2026 (EventSummaryResponse) con el estado indicado. */
    public static EventSummaryResponse cmf2026SummaryResponse(EventStatus status) {
        return new EventSummaryResponse(
                1L,                          // DATO DE PRUEBA
                CMF_2026_CODE,               // PRD §23.2
                CMF_2026_NAME,               // PRD §23.2
                EventCategory.MUSIC,         // PRD §23.2
                status,
                CMF_2026_EVENT_DATE,         // DATO DE PRUEBA
                VEN_SMR_01_CODE,             // PRD §23.1
                venSmr01Response().name());  // PRD §23.1
    }

    // ---------------------------------------------------------------- Tickets (P3-09 TKC-4)

    // PRD de Servicios §54: «Andrea quiere comprar una entrada VIP para CMF-2026». Los códigos
    // siguen el formato de P2-D3 (TKT- + 6 hexadecimales) y son DATO DE PRUEBA. El precio es el
    // importe de VIP aprobado en P2-12 TP-2 (DATO DE DISEÑO, no un valor del PRD).

    /** DATO DE PRUEBA: código del ticket de Andrea (el del ejemplo del PRD de Frontend). */
    public static final String ANDREA_TICKET_CODE = "TKT-8F2A1C";

    /** DATO DE PRUEBA: código de un segundo ticket de Andrea. */
    public static final String ANDREA_SECOND_TICKET_CODE = "TKT-91B07D";

    /** DATO DE DISEÑO (P2-12 TP-2): precio de un ticket VIP. */
    public static final BigDecimal VIP_PRICE = new BigDecimal("200000.00");

    /** DATO DE PRUEBA: fecha de compra (el «ahora» de las pruebas de servicio). */
    public static final LocalDateTime PURCHASE_DATE = LocalDateTime.of(2026, 11, 15, 12, 0);

    /** Compra de un ticket VIP de CMF-2026 por Andrea (PRD de Servicios §54). */
    public static PurchaseTicketRequest andreaPurchaseRequest() {
        return new PurchaseTicketRequest(
                ANDREA_EMAIL,                // PRD Servicios §47
                CMF_2026_CODE,               // PRD §23.2
                TicketType.VIP);             // PRD Servicios §54
    }

    /**
     * Ticket VIP de Andrea en CMF-2026, con el código y el estado que pida cada prueba. El
     * {@code id} (DATO DE PRUEBA) es 1 para {@link #ANDREA_TICKET_CODE} y 2 para cualquier otro.
     */
    public static TicketResponse andreaTicketResponse(String ticketCode, TicketStatus status) {
        return new TicketResponse(
                ANDREA_TICKET_CODE.equals(ticketCode) ? 1L : 2L, // DATO DE PRUEBA
                ticketCode,                  // DATO DE PRUEBA
                TicketType.VIP,              // PRD Servicios §54
                VIP_PRICE,                   // DATO DE DISEÑO (TP-2)
                status,
                PURCHASE_DATE,               // DATO DE PRUEBA
                ANDREA_EMAIL,                // PRD Servicios §47
                CMF_2026_CODE,               // PRD §23.2
                CMF_2026_NAME);              // PRD §23.2
    }
}
