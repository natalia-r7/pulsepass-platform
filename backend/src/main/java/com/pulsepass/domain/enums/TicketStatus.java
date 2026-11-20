package com.pulsepass.domain.enums;

/**
 * Estado de un ticket (PRD Persistencia §6.2, §9.2, FR-TKT-005).
 * Se persiste por nombre y no por ordinal (BR-008); la base de datos solo
 * acepta estos valores (ck_tickets_status).
 */
public enum TicketStatus {
    RESERVED,
    PAID,
    CANCELLED,
    USED
}
