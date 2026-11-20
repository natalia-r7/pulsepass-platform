package com.pulsepass.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validation;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Apoyo para las pruebas de Bean Validation de los request DTO (P3-03).
 *
 * <p>Usa el {@link Validator} de Jakarta directamente, sin Spring: la prueba comprueba las
 * anotaciones del DTO, no el controlador. Agrupa las violaciones por campo para comparar los
 * mensajes exactos (P3-D5).
 */
final class RequestValidationSupport {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();

    private static final Validator VALIDATOR = FACTORY.getValidator();

    private RequestValidationSupport() {
        // Clase de utilidades: no se instancia.
    }

    /** Valida el objeto y devuelve, por campo, los mensajes de sus violaciones. */
    static <T> Map<String, Set<String>> violationsOf(T request) {
        Map<String, Set<String>> messages = new TreeMap<>();
        for (ConstraintViolation<T> violation : VALIDATOR.validate(request)) {
            messages.computeIfAbsent(violation.getPropertyPath().toString(), field -> new TreeSet<>())
                    .add(violation.getMessage());
        }
        return messages;
    }

    /** Texto de {@code length} repeticiones de {@code c} (DATO DE PRUEBA para los límites). */
    static String text(char c, int length) {
        return String.valueOf(c).repeat(length);
    }

    /**
     * Email válido de exactamente {@code length} caracteres (DATO DE PRUEBA). La parte local
     * tiene 64 caracteres y el dominio usa etiquetas de 63 como máximo, los límites que acepta
     * {@code @Email}. Sirve para longitudes entre 140 y 254.
     */
    static String emailOfLength(int length) {
        String local = text('a', 64);
        String lastLabel = text('d', length - 64 - 1 - 63 - 1 - 63 - 1 - ".com".length());
        return local + "@" + text('b', 63) + "." + text('c', 63) + "." + lastLabel + ".com";
    }
}
