package com.pulsepass.domain.enums;

/**
 * Tipo de ticket (PRD Persistencia §6.2, FR-TKT-004).
 * Se persiste como valor estable por nombre (BR-008); la base de datos solo
 * acepta estos valores (ck_tickets_type).
 */
public enum TicketType {
    GENERAL,
    VIP,
    BACKSTAGE,
    STUDENT
}
