package com.pulsepass.exception;

/**
 * Se lanza cuando el recurso existe pero la operación no es válida según una
 * regla de negocio (PRD Servicios §35, BR-EVENT-003, entre otras; también los
 * casos de P2-D5).
 * <p>
 * Ejemplo de mensaje del PRD: {@code "User does not meet minimum age."}.
 * Los mensajes van en inglés (P2-D17) y deben identificar la regla
 * (NFR-005).
 * <p>
 * Es una excepción no comprobada ({@link RuntimeException}): así, cuando sale
 * de un método {@code @Transactional}, Spring revierte la transacción por defecto.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
