package com.pulsepass.dto.response;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contrato único de errores HTTP (PRD Controladores §13, NFR-CTRL-003).
 * <p>
 * Lo construye exclusivamente {@code GlobalExceptionHandler}. {@code timestamp} es la hora
 * local de America/Bogota tomada del {@code Clock} de la aplicación, truncada a segundos
 * (P3-D7). {@code error} es la frase estándar del estado HTTP («Not Found»). {@code details}
 * contiene campo → mensaje en los errores de validación y está vacío en los demás.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details
) {

    /** Copia defensiva e inmodificable de {@code details}, conservando su orden. */
    public ErrorResponse {
        details = details == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }
}
