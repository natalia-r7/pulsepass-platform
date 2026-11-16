package com.pulsepass.exception;

/**
 * Se lanza cuando la operación generaría un conflicto de unicidad
 * (PRD Servicios §35, BR-EVENT-001, entre otras).
 * <p>
 * Ejemplo de mensaje del PRD: {@code "Username already exists."}.
 * Los mensajes van en inglés (P2-D17) y deben identificar el recurso
 * (NFR-005).
 * <p>
 * Es una excepción no comprobada ({@link RuntimeException}): así, cuando sale
 * de un método {@code @Transactional}, Spring revierte la transacción por defecto.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
