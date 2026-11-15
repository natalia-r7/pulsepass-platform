package com.pulsepass.persistence;

import com.pulsepass.domain.Venue;

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
}
