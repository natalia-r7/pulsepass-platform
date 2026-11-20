package com.pulsepass.service;

/**
 * Consultas de autorización sobre la identidad de la petición actual (PRD Seguridad §7.1, §11.1,
 * §14.5).
 * <p>
 * Separa la decisión «quién puede ver o hacer qué» de las reglas de negocio de los servicios.
 * {@link #isStaffOrAdmin()} decide la visibilidad de los eventos no publicados (P4-D2, P4-04b).
 * Los demás métodos comprueban la propiedad de los recursos para las expresiones
 * {@code @PreAuthorize} de {@code UserService} y {@code TicketService}, que usan el bean con el
 * nombre {@code authz} (P4-D5, P4-08-B). Comparan siempre el id persistente del recurso con el id
 * del usuario autenticado, nunca solo un email (§11.1). Sin identidad, o con una anónima,
 * devuelven {@code false}.
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

    /**
     * Indica si el usuario con ese email (sin distinguir mayúsculas) es el usuario autenticado,
     * comparando sus ids persistentes (FR-SEC-USR-02, FR-SEC-TKT-01, FR-SEC-TKT-03).
     *
     * @return {@code false} también si no existe ningún usuario con ese email (P4-D1: 403)
     */
    boolean isCurrentUserEmail(String email);

    /**
     * Indica si el usuario con ese username exacto es el usuario autenticado, comparando sus ids
     * persistentes (FR-SEC-USR-03).
     *
     * @return {@code false} también si no existe ningún usuario con ese username (P4-D1: 403)
     */
    boolean isCurrentUsername(String username);

    /**
     * Indica si el usuario autenticado puede operar sobre el ticket como propietario: el
     * propietario guardado en la base de datos es el usuario autenticado (FR-SEC-TKT-02,
     * FR-SEC-TKT-05).
     *
     * @return {@code true} si el ticket no existe, para que el servicio responda su 404 (P4-D1)
     */
    boolean canAccessTicket(String ticketCode);
}
