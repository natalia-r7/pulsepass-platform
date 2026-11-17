package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
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
}
