package com.pulsepass.exception;

import com.pulsepass.dto.response.ErrorResponse;
import com.pulsepass.security.SecurityPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * <p>
 * Los cuerpos los construye {@link ErrorResponseFactory}, la misma fábrica que usan los 401 y 403
 * de la cadena de seguridad (P4-04-d). Las denegaciones que llegan a Spring MVC (por ejemplo, de
 * la seguridad a nivel de método) responden 401 o 403, nunca 500 (P4-D19, P4-04-g).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String VALIDATION_FAILED = "Validation failed";

    static final String MALFORMED_JSON = "Malformed JSON request";

    static final String UNEXPECTED_ERROR = "Unexpected error";

    /** Separador de varios mensajes de un mismo campo (P3-D6). */
    private static final String MESSAGE_SEPARATOR = "; ";

    /** Cabecera de los 401 (RFC 6750, P4-04-f): indica el esquema esperado, sin detalles del error. */
    private static final String BEARER_CHALLENGE = "Bearer";

    private final ErrorResponseFactory errorResponseFactory;

    /** Distingue una identidad anónima de una autenticada (P4-D19). */
    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    public GlobalExceptionHandler(ErrorResponseFactory errorResponseFactory) {
        this.errorResponseFactory = errorResponseFactory;
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

    // ---------------------------------------------------------------- Seguridad (P4-D19)

    /**
     * Denegación dentro de Spring MVC (por ejemplo, de {@code @PreAuthorize}). Sin este manejador
     * la capturaría {@link #handleUnexpected} y respondería 500. Sin identidad o con una identidad
     * anónima → 401; con una identidad autenticada sin permiso → 403 (§13, SEC-009).
     * <p>
     * Registra el intento con el mismo formato que los manejadores de la cadena de seguridad
     * (P4-04-j): 401 en INFO y 403 en WARN, con método y ruta, sin la query string ni cabeceras.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || trustResolver.isAnonymous(authentication)) {
            logAuthenticationRequired(request);
            return unauthorized(ErrorResponseFactory.AUTHENTICATION_REQUIRED);
        }
        logger.warn("Access denied: " + request.getMethod() + " " + request.getRequestURI()
                + " userId=" + principalId(authentication));
        return error(HttpStatus.FORBIDDEN, ErrorResponseFactory.ACCESS_DENIED);
    }

    /**
     * Autenticación fallida dentro de Spring MVC (el login, P4-07) → 401 genérico (FR-SEC-AUTH-003).
     * Se registra en INFO con el formato de los 401 (P4-04-j); nunca el identificador ni la contraseña.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailure(AuthenticationException ex,
                                                                     HttpServletRequest request) {
        logAuthenticationRequired(request);
        return unauthorized(ErrorResponseFactory.INVALID_CREDENTIALS);
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

    /** 401 con la cabecera {@code WWW-Authenticate: Bearer} (P4-04-f). */
    private ResponseEntity<ErrorResponse> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, BEARER_CHALLENGE)
                .body(body(HttpStatus.UNAUTHORIZED, message));
    }

    private ErrorResponse body(HttpStatusCode status, String message) {
        return errorResponseFactory.create(status, message);
    }

    private ErrorResponse body(HttpStatusCode status, String message, Map<String, String> details) {
        return errorResponseFactory.create(status, message, details);
    }

    private static String reasonPhrase(HttpStatusCode status) {
        return ErrorResponseFactory.reasonPhrase(status);
    }

    /**
     * Log de un 401 (P4-04-j): solo método y ruta. {@code getRequestURI()} no incluye la query
     * string, que puede llevar datos personales, y no se registra ninguna cabecera (NFR-SEC-006).
     */
    private void logAuthenticationRequired(HttpServletRequest request) {
        logger.info("Authentication required: " + request.getMethod() + " " + request.getRequestURI());
    }

    /** Id del usuario para el log: nunca la contraseña, el token ni datos personales (NFR-SEC-006). */
    private static String principalId(Authentication authentication) {
        return authentication.getPrincipal() instanceof SecurityPrincipal principal
                ? String.valueOf(principal.userId())
                : "unknown";
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
