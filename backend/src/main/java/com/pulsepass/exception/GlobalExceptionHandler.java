package com.pulsepass.exception;

import com.pulsepass.dto.response.ErrorResponse;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Manejo centralizado de errores HTTP (PRD Controladores §14, §15; CTRL-007, CTRL-008).
 * <p>
 * Todas las respuestas de error usan {@link ErrorResponse} (NFR-CTRL-003). Extiende
 * {@link ResponseEntityExceptionHandler} para que también los errores propios de Spring MVC
 * (parámetro ausente, tipo incompatible, ruta inexistente, 405, 415…) usen ese contrato,
 * conservando su estado HTTP real (P3-D3).
 * <p>
 * Los métodos heredados de {@code ResponseEntityExceptionHandler} declaran
 * {@code ResponseEntity<Object>} (firma impuesta por Spring); su cuerpo es siempre un
 * {@code ErrorResponse}. Los manejadores propios declaran {@code ResponseEntity<ErrorResponse>}.
 * <p>
 * Mensajes en inglés (P3-D5). Una respuesta 500 nunca incluye el mensaje ni la traza de la
 * excepción original (NFR-CTRL-007); la excepción se registra solo en el log del servidor.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String VALIDATION_FAILED = "Validation failed";

    static final String MALFORMED_JSON = "Malformed JSON request";

    static final String UNEXPECTED_ERROR = "Unexpected error";

    /** Separador de varios mensajes de un mismo campo (P3-D6). */
    private static final String MESSAGE_SEPARATOR = "; ";

    private final Clock clock;

    /** El {@link Clock} es el bean de {@code ClockConfig} (P2-D4, P3-D7). */
    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    // ---------------------------------------------------------------- Excepciones del dominio

    /** Recurso inexistente → 404 (§14). */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Conflicto de unicidad → 409 (§14). */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Regla de negocio incumplida → 409 (§14). */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Cualquier otra excepción → 500 con un mensaje genérico (§14, NFR-CTRL-007). Incluye
     * {@code DataIntegrityViolationException} (P3-D10) e {@code IllegalStateException} (TK-5).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        logger.error("Unexpected error while processing the request", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR);
    }

    // ---------------------------------------------------------------- Errores de Spring MVC

    /** Bean Validation → 400, con un mensaje por campo (§13, §14, P3-D6). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, TreeSet<String>> messagesByField = new TreeMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            messagesByField.computeIfAbsent(fieldError.getField(), field -> new TreeSet<>())
                    .add(fieldError.getDefaultMessage());
        }
        for (ObjectError globalError : ex.getBindingResult().getGlobalErrors()) {
            messagesByField.computeIfAbsent(globalError.getObjectName(), name -> new TreeSet<>())
                    .add(globalError.getDefaultMessage());
        }

        Map<String, String> details = new TreeMap<>();
        messagesByField.forEach((field, messages) ->
                details.put(field, String.join(MESSAGE_SEPARATOR, messages)));

        return handleExceptionInternal(ex, body(status, VALIDATION_FAILED, details), headers, status, request);
    }

    /** JSON mal formado o ilegible (incluye un valor de enum desconocido) → 400 (§14). */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        return handleExceptionInternal(ex, body(status, MALFORMED_JSON), headers, status, request);
    }

    /** Parámetro de consulta obligatorio ausente → 400 (P3-D3). */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers, HttpStatusCode status,
                                                                          WebRequest request) {
        String message = "Required parameter '" + ex.getParameterName() + "' is missing";
        return handleExceptionInternal(ex, body(status, message), headers, status, request);
    }

    /** Parámetro de ruta o de consulta con un valor incompatible con su tipo → 400 (P3-D3). */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String parameter = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : ex.getPropertyName();
        String message = "Invalid value for parameter '" + parameter + "'";
        return handleExceptionInternal(ex, body(status, message), headers, status, request);
    }

    /** Ruta inexistente → 404 (P3-D3). */
    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex,
                                                                    HttpHeaders headers, HttpStatusCode status,
                                                                    WebRequest request) {
        return handleExceptionInternal(ex, body(status, noEndpointMessage(request)), headers, status, request);
    }

    /** Ruta inexistente cuando no hay manejador de recursos estáticos → 404 (P3-D3). */
    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex,
                                                                   HttpHeaders headers, HttpStatusCode status,
                                                                   WebRequest request) {
        return handleExceptionInternal(ex, body(status, noEndpointMessage(request)), headers, status, request);
    }

    /** Método HTTP no permitido → 405; Spring añade la cabecera {@code Allow} (P3-D3). */
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                         HttpHeaders headers, HttpStatusCode status,
                                                                         WebRequest request) {
        String message = "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint";
        return handleExceptionInternal(ex, body(status, message), headers, status, request);
    }

    /** Content-Type no admitido → 415; Spring añade la cabecera {@code Accept} (P3-D3). */
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpHeaders headers, HttpStatusCode status,
                                                                     WebRequest request) {
        // Solo tipo/subtipo («text/plain»), sin parámetros como «;charset=UTF-8».
        MediaType contentType = ex.getContentType();
        String message = contentType == null
                ? "Content type is not supported"
                : "Content type '" + contentType.getType() + "/" + contentType.getSubtype() + "' is not supported";
        return handleExceptionInternal(ex, body(status, message), headers, status, request);
    }

    /**
     * Punto común de todas las respuestas de Spring MVC.
     * <ul>
     *   <li>Estado 5xx (por ejemplo {@code MissingPathVariableException} → 500 o
     *       {@code AsyncRequestTimeoutException} → 503): se conserva el estado, pero el mensaje
     *       es siempre «Unexpected error» y {@code details} está vacío, como en
     *       {@link #handleUnexpected}; la excepción solo se registra en el log (NFR-CTRL-007).
     *       Es el único registro: estas excepciones no pasan por {@code handleUnexpected}.</li>
     *   <li>Estado 4xx sin mensaje propio (errores MVC poco frecuentes, como 406 o 413): se
     *       construye un {@link ErrorResponse} con el detalle estándar de Spring.</li>
     *   <li>Si el cuerpo ya es un {@link ErrorResponse}, se respeta.</li>
     * </ul>
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        Object errorBody;
        if (statusCode.is5xxServerError()) {
            logger.error("Unexpected error while processing the request", ex);
            errorBody = body(statusCode, UNEXPECTED_ERROR);
        } else if (body instanceof ErrorResponse) {
            errorBody = body;
        } else {
            errorBody = body(statusCode, defaultMessage(ex, statusCode));
        }
        return super.handleExceptionInternal(ex, errorBody, headers, statusCode, request);
    }

    // ---------------------------------------------------------------- Auxiliares

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(body(status, message));
    }

    private ErrorResponse body(HttpStatusCode status, String message) {
        return body(status, message, Map.of());
    }

    /** Construye el {@link ErrorResponse} con la hora del {@link Clock} inyectado (P3-D7). */
    private ErrorResponse body(HttpStatusCode status, String message, Map<String, String> details) {
        LocalDateTime timestamp = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        return new ErrorResponse(timestamp, status.value(), reasonPhrase(status), message, details);
    }

    private static String reasonPhrase(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        return known != null ? known.getReasonPhrase() : "Error";
    }

    private static String noEndpointMessage(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return "No endpoint found for " + servletRequest.getRequest().getMethod()
                    + " " + servletRequest.getRequest().getRequestURI();
        }
        return "No endpoint found";
    }

    /** Detalle estándar de Spring para los errores MVC 4xx sin mensaje propio; nunca la traza. */
    private static String defaultMessage(Exception ex, HttpStatusCode status) {
        if (ex instanceof org.springframework.web.ErrorResponse springError
                && springError.getBody().getDetail() != null) {
            return springError.getBody().getDetail();
        }
        return reasonPhrase(status);
    }
}
