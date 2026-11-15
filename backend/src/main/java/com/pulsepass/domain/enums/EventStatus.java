package com.pulsepass.domain.enums;

/**
 * Estado de publicación de un evento (PRD Persistencia §6.2, §9.1, FR-EVT-003).
 * Se persiste por nombre y no por ordinal (BR-008); la base de datos solo
 * acepta estos valores (ck_events_status).
 */
public enum EventStatus {
    DRAFT,
    PUBLISHED,
    SOLD_OUT,
    CANCELLED,
    FINISHED
}
