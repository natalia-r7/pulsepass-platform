package com.pulsepass.domain.enums;

/**
 * Categoría de un evento (PRD Persistencia §6.2, FR-EVT-004).
 * Se persiste por nombre y no por ordinal (BR-008); la base de datos solo
 * acepta estos valores (ck_events_category).
 */
public enum EventCategory {
    MUSIC,
    SPORTS,
    TECHNOLOGY,
    EDUCATION,
    CULTURE,
    ENTERTAINMENT
}
