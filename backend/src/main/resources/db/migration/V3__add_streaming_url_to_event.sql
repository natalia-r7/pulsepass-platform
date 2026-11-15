-- =====================================================================
-- PulsePass - V3: URL de streaming para eventos híbridos (Fase 1 - Persistencia)
-- PRD Persistencia §15 y FR-EVT-006: "Agrega streaming_url VARCHAR(500)
-- nullable a events sin modificar V1."
--
-- * Admite NULL: un evento puede no tener transmisión (FR-EVT-006).
-- * Sin DEFAULT (V1-8) y sin restricción de formato (el PRD no la exige).
-- =====================================================================

ALTER TABLE events
    ADD COLUMN streaming_url VARCHAR(500);
