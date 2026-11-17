package com.pulsepass.controller;

import com.pulsepass.dto.response.VenueResponse;

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
}
