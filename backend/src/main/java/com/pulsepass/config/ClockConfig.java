package com.pulsepass.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara el {@link Clock} que usan los servicios para obtener la fecha y hora
 * actuales (decisión P2-D4).
 * <p>
 * Los servicios reciben este {@code Clock} por constructor y nunca llaman a
 * {@code LocalDateTime.now()} sin argumentos. Así, las pruebas unitarias pueden
 * pasar un {@code Clock.fixed(...)} sin levantar Spring (NFR-001), y reglas como
 * BR-EVENT-004 («fecha futura al momento de creación») se comprueban con una
 * fecha conocida.
 * <p>
 * El paquete {@code config} no aparece en la estructura sugerida del PRD (§8):
 * es una decisión técnica aprobada en P2-03.
 */
@Configuration
public class ClockConfig {

    /** Zona horaria de la aplicación (P2-D4). */
    public static final ZoneId APPLICATION_ZONE = ZoneId.of("America/Bogota");

    @Bean
    public Clock clock() {
        return Clock.system(APPLICATION_ZONE);
    }
}
