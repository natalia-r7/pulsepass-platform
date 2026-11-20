package com.pulsepass.exception;

import com.pulsepass.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Única fuente de los cuerpos {@link ErrorResponse} (PRD Controladores §13; PRD Seguridad §13;
 * P4-04-d).
 * <p>
 * La usan {@link GlobalExceptionHandler}, para los errores dentro de Spring MVC, y los manejadores
 * de seguridad, para los 401 y 403 que se producen en la cadena de filtros, antes de llegar a
 * {@code DispatcherServlet} (§13: «escribir la misma estructura JSON»). Así todos los errores
 * tienen el mismo {@code timestamp} (hora del {@link Clock} truncada a segundos, P3-D7), la
 * misma frase de estado y la misma serialización.
 * <p>
 * Se escribe con el {@link JsonMapper} de Jackson 3 que configura Spring Boot, el mismo que usa
 * Spring MVC; nunca con el {@code ObjectMapper} de Jackson 2 que trae JJWT (P4-04-d).
 */
@Component
public class ErrorResponseFactory {

    /** 401 en una ruta protegida sin identidad válida (P4-D9). */
    public static final String AUTHENTICATION_REQUIRED = "Authentication is required";

    /** 403 para una identidad válida sin permiso (P4-D9). */
    public static final String ACCESS_DENIED = "Access denied";

    /** 401 cuando falla una autenticación dentro de Spring MVC, como el login (P4-D9). */
    public static final String INVALID_CREDENTIALS = "Invalid credentials";

    private final Clock clock;

    private final JsonMapper jsonMapper;

    /** El {@link Clock} es el bean de {@code ClockConfig} (P2-D4, P3-D7). */
    public ErrorResponseFactory(Clock clock, JsonMapper jsonMapper) {
        this.clock = clock;
        this.jsonMapper = jsonMapper;
    }

    /** Cuerpo de error sin detalles ({@code details} vacío). */
    public ErrorResponse create(HttpStatusCode status, String message) {
        return create(status, message, Map.of());
    }

    /** Cuerpo de error con la hora del {@link Clock} truncada a segundos (P3-D7). */
    public ErrorResponse create(HttpStatusCode status, String message, Map<String, String> details) {
        LocalDateTime timestamp = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        return new ErrorResponse(timestamp, status.value(), reasonPhrase(status), message, details);
    }

    /**
     * Escribe el error como JSON en la respuesta, para los errores que ocurren fuera de Spring MVC
     * (los manejadores 401 y 403 de la cadena de seguridad). No escribe nada si la respuesta ya se
     * envió, para que el cuerpo se emita una sola vez (§13).
     */
    public void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), create(status, message));
    }

    /** Frase estándar del estado HTTP («Not Found»), o «Error» si el estado no es conocido. */
    static String reasonPhrase(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        return known != null ? known.getReasonPhrase() : "Error";
    }
}
