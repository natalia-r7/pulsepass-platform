package com.pulsepass.service;

/**
 * Consultas de autorización sobre la identidad de la petición actual (PRD Seguridad §7.1, §14.5).
 * <p>
 * Separa la decisión «quién puede ver o hacer qué» de las reglas de negocio de los servicios.
 * En P4-04b solo expone {@link #isStaffOrAdmin()}, que usa la visibilidad de los eventos no
 * publicados (P4-D2). Las comprobaciones de propiedad de los recursos se añadirán en P4-08.
 */
public interface AuthorizationService {

    /**
     * Indica si la petición actual la hace una identidad autenticada, no anónima, con el rol
     * STAFF o ADMIN (PRD Seguridad §11: FR-SEC-EVT-02).
     *
     * @return {@code true} solo para STAFF o ADMIN autenticados; {@code false} sin identidad,
     *         para un anónimo o para cualquier otro rol
     */
    boolean isStaffOrAdmin();
}
