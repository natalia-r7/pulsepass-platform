package com.pulsepass.exception;

/**
 * Se lanza cuando el recurso solicitado no existe (PRD Servicios §35,
 * BR-VENUE-001, BR-EVENT-002, entre otras).
 * <p>
 * Ejemplo de mensaje del PRD: {@code "Event not found: CMF-2026"}.
 * Los mensajes van en inglés (P2-D17) y deben identificar el recurso
 * (NFR-005).
 * <p>
 * Es una excepción no comprobada ({@link RuntimeException}): así, cuando sale
 * de un método {@code @Transactional}, Spring revierte la transacción por defecto.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
