package com.pulsepass;

import com.pulsepass.config.ClockConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Reloj fijo para las pruebas web con {@code @WebMvcTest} (P3-D7).
 * <p>
 * {@code @WebMvcTest} no carga {@code ClockConfig}, pero {@code GlobalExceptionHandler}
 * necesita un {@link Clock}. Esta configuración lo aporta con una hora conocida, así que el
 * {@code timestamp} de los errores es determinista. Al ser {@code @TestConfiguration}, el
 * escaneo de componentes la ignora: solo se usa cuando una prueba la importa con {@code @Import}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FixedClockTestConfiguration {

    /**
     * DATO DE PRUEBA: «ahora» en las pruebas web. Lleva nanosegundos para comprobar que el
     * {@code timestamp} se trunca a segundos.
     */
    public static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 15, 12, 0, 0, 123_456_789);

    /** {@code timestamp} esperado en el JSON: {@link #NOW} truncado a segundos. */
    public static final String EXPECTED_TIMESTAMP = "2026-11-15T12:00:00";

    @Bean
    public Clock clock() {
        return Clock.fixed(NOW.atZone(ClockConfig.APPLICATION_ZONE).toInstant(), ClockConfig.APPLICATION_ZONE);
    }
}
