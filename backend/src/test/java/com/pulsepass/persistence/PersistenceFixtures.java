package com.pulsepass.persistence;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;

import java.time.LocalDateTime;

/**
 * Datos de prueba de la Fase 1 - Persistencia (P1-12-a).
 *
 * <p>Solo construye objetos: no guarda nada. Cada prueba decide qué persiste.
 *
 * <p>Convención de comentarios:
 * <ul>
 *   <li>{@code PRD §16}: valor tomado de los datos de referencia del PRD de Persistencia.</li>
 *   <li>{@code DATO DE PRUEBA}: valor ficticio elegido para la prueba (P1-D2); no es un requisito.</li>
 * </ul>
 *
 * <p>Los datos de este archivo pertenecen exclusivamente al PRD de Persistencia y no se
 * mezclan con los escenarios de los PRD de Servicios, Controladores, Seguridad o Frontend.
 * Las demás entidades se agregarán en las siguientes iteraciones de P1-12.
 */
final class PersistenceFixtures {

    /**
     * DATO DE PRUEBA: §16.2 no indica la fecha de CMF-2026. Fecha fija (no now()) para que
     * las pruebas den el mismo resultado cualquier día que se ejecuten.
     */
    static final LocalDateTime CMF_2026_EVENT_DATE = LocalDateTime.of(2030, 6, 1, 20, 0);

    /** DATO DE PRUEBA: §16.2 no indica la edad mínima de CMF-2026. */
    static final int CMF_2026_MINIMUM_AGE = 0;

    private PersistenceFixtures() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Venue de referencia VEN-SMR-01 (PRD §16.1). Cada llamada devuelve un objeto nuevo,
     * sin persistir.
     */
    static Venue venSmr01() {
        // address: §16.1 no la indica; queda en null (columna opcional, V1-3).
        return new Venue(
                "VEN-SMR-01",                // PRD §16.1 code
                "Marina Convention Center",  // PRD §16.1 name
                "Santa Marta",               // PRD §16.1 city
                5000,                        // PRD §16.1 capacity
                true);                       // DATO DE PRUEBA: §16.1 no indica el estado activo
    }

    /**
     * DATO DE PRUEBA: venue adicional, no definido en §16, para comprobar que las consultas
     * por venue no mezclan eventos de sedes distintas.
     */
    static Venue otherVenue() {
        return new Venue(
                "VEN-TST-02",                // DATO DE PRUEBA
                "Sede de Prueba",            // DATO DE PRUEBA
                "Barranquilla",              // DATO DE PRUEBA
                100,                         // DATO DE PRUEBA
                true);                       // DATO DE PRUEBA
    }

    /**
     * Evento principal CMF-2026 asociado al venue recibido (PRD §16.2). Sin artistas: se
     * asocian en las pruebas de la relación N:M. Cada llamada devuelve un objeto nuevo.
     */
    static Event cmf2026(Venue venue) {
        // description y streamingUrl: §16.2 no los indica; quedan en null (columnas opcionales).
        return new Event(
                "CMF-2026",                  // PRD §16.2 eventCode
                "Caribbean Music Fest 2026", // PRD §16.2 name
                EventCategory.MUSIC,         // PRD §16.2 category
                EventStatus.PUBLISHED,       // PRD §16.2 status
                CMF_2026_EVENT_DATE,         // DATO DE PRUEBA (ver constante)
                CMF_2026_MINIMUM_AGE,        // DATO DE PRUEBA (ver constante)
                venue);
    }

    /**
     * DATO DE PRUEBA: evento adicional, no definido en §16, con el código, estado y fecha que
     * necesite cada prueba (por ejemplo, para comparar estados u ordenar por fecha).
     */
    static Event testEvent(String eventCode, EventStatus status, LocalDateTime eventDate, Venue venue) {
        return new Event(
                eventCode,                       // DATO DE PRUEBA
                "Evento de prueba " + eventCode, // DATO DE PRUEBA
                EventCategory.CULTURE,           // DATO DE PRUEBA
                status,                          // DATO DE PRUEBA
                eventDate,                       // DATO DE PRUEBA
                0,                               // DATO DE PRUEBA
                venue);
    }
}
