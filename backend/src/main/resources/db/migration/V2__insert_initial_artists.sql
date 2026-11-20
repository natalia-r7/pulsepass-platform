-- =====================================================================
-- PulsePass - V2: catálogo inicial de artistas (Fase 1 - Persistencia)
-- PRD Persistencia §15: "Inserta el catálogo inicial de artistas de prueba:
-- Solar Beat, Neon Waves, Caribbean Sound, Ocean Drive y Digital Pulse."
--
-- * El id no se indica: lo asigna la identidad (V1-1), así el contador de
--   PostgreSQL avanza y los INSERT posteriores no chocan con estos registros.
-- * country y genre quedan en NULL: el PRD no proporciona estos datos (A8).
-- * active = TRUE es una INFERENCIA de los escenarios de los PRD posteriores
--   (Servicios, E2E y Frontend usan estos artistas como activos); no es un
--   valor indicado textualmente para V2 en el PRD de Persistencia (A8).
-- =====================================================================

INSERT INTO artists (stage_name, country, genre, active) VALUES
    ('Solar Beat',      NULL, NULL, TRUE),
    ('Neon Waves',      NULL, NULL, TRUE),
    ('Caribbean Sound', NULL, NULL, TRUE),
    ('Ocean Drive',     NULL, NULL, TRUE),
    ('Digital Pulse',   NULL, NULL, TRUE);
