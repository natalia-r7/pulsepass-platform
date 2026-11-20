-- =====================================================================
-- PulsePass — Fase 5 (E2E manual con Postman): fixtures de la base E2E
-- =====================================================================
-- EXCLUSIVO de la base pulsepass_e2e (PRD E2E §15.1–§15.2). Nunca contra otra base.
-- Ver tests/e2e/fixtures/README.md para cuándo y cómo ejecutar cada paso.
--
-- Uso (un paso por ejecución; cada paso es UNA transacción):
--   docker exec -i pulsepass-e2e-db psql -X -U pulsepass_e2e -d pulsepass_e2e \
--     -v step=<venues|promote|deactivate|revoke-staff> -v run_id=<runId> \
--     [-v inactive_venue=on] < tests/e2e/fixtures/seed-e2e.sql
--
-- Pasos:
--   venues        Inserta los locales E2E-{run_id}-V0 (capacidad 100) y E2E-{run_id}-V1
--                 (capacidad 1). Con inactive_venue=on, también E2E-{run_id}-VX (inactivo).
--   promote       staff.{run_id}@example.test pasa de {USER} a {STAFF} y
--                 admin.{run_id}@example.test, de {USER} a {ADMIN} (P5-02, F5-C).
--   deactivate    inactive.{run_id}@example.test pasa a active = false (TC-AUTH-013).
--   revoke-staff  staff.{run_id}@example.test pierde el rol STAFF (TC-SEC-017).
--
-- Salvaguardas (P5-02-c):
--   * psql no sustituye variables dentro de $$...$$: los valores pasan a la transacción con
--     set_config(..., true) y el bloque DO los lee con current_setting(). Nunca SQL dinámico.
--   * Las variables ausentes valen '' y la validación las rechaza.
--   * Base pulsepass_e2e, migración V4 aplicada, paso de una lista cerrada, run_id ^[a-z0-9]{1,20}$.
--   * Los emails se derivan de run_id: solo se tocan los actores de esa ejecución.
--   * SELECT ... INTO STRICT (0 o más de 1 fila = error), FOR UPDATE, precondiciones de rol y
--     estado, y ROW_COUNT = 1 después de cada INSERT, UPDATE o DELETE.
--   * Cualquier error aborta la transacción; con ON_ERROR_STOP psql sale con código 3 sin
--     llegar al COMMIT y el servidor deshace todo. No hay confirmaciones parciales.
-- Sin contraseñas, hashes ni tokens (FR-E2E-013).
-- Nombres, ciudad y capacidades de los locales: DATO DE PRUEBA (el PRD no los define).
-- =====================================================================

\set ON_ERROR_STOP on
\set ON_ERROR_ROLLBACK off

\if :{?step}
\else
\set step ''
\endif
\if :{?run_id}
\else
\set run_id ''
\endif
\if :{?inactive_venue}
\else
\set inactive_venue 'off'
\endif

BEGIN;

-- Fuera de $$ psql sí interpola :'var' como un literal citado (sin inyección). El true de
-- set_config limita el valor a esta transacción.
SELECT set_config('pulsepass_e2e.step', :'step', true) AS step,
       set_config('pulsepass_e2e.run_id', :'run_id', true) AS run_id,
       set_config('pulsepass_e2e.inactive_venue', :'inactive_venue', true) AS inactive_venue;

DO $$
DECLARE
    v_step  text := current_setting('pulsepass_e2e.step');
    v_run   text := current_setting('pulsepass_e2e.run_id');
    v_inv   text := current_setting('pulsepass_e2e.inactive_venue');
    v_email text;
    v_role  text;
    v_id    bigint;
    v_n     integer;
    v_roles text[];
BEGIN
    -- A. Salvaguardas generales -------------------------------------------------------
    IF current_database() <> 'pulsepass_e2e' THEN
        RAISE EXCEPTION 'Not the E2E database: %', current_database();
    END IF;
    IF to_regclass('public.flyway_schema_history') IS NULL
       OR NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '4' AND success) THEN
        RAISE EXCEPTION 'Flyway migrations V1..V4 are not applied';
    END IF;
    IF v_step NOT IN ('venues', 'promote', 'deactivate', 'revoke-staff') THEN
        RAISE EXCEPTION 'Invalid step: "%" (expected venues, promote, deactivate or revoke-staff)', v_step;
    END IF;
    IF v_run !~ '^[a-z0-9]{1,20}$' THEN
        RAISE EXCEPTION 'Invalid run_id: "%" (expected ^[a-z0-9]{1,20}$)', v_run;
    END IF;
    IF v_inv NOT IN ('on', 'off') OR (v_inv = 'on' AND v_step <> 'venues') THEN
        RAISE EXCEPTION 'Invalid inactive_venue: "%" (on/off, only with step venues)', v_inv;
    END IF;

    -- B. Operación del paso -----------------------------------------------------------
    CASE v_step

    WHEN 'venues' THEN
        IF EXISTS (SELECT 1 FROM venues WHERE code LIKE 'E2E-' || v_run || '-%') THEN
            RAISE EXCEPTION 'Venues for run_id "%" already exist; use a new run_id', v_run;
        END IF;

        INSERT INTO venues (code, name, city, address, capacity, active)
        VALUES ('E2E-' || v_run || '-V0', 'PulsePass E2E Sede Principal', 'Bogotá', NULL, 100, TRUE);
        GET DIAGNOSTICS v_n = ROW_COUNT;
        IF v_n <> 1 THEN RAISE EXCEPTION 'Expected 1 row for venue V0, got %', v_n; END IF;

        INSERT INTO venues (code, name, city, address, capacity, active)
        VALUES ('E2E-' || v_run || '-V1', 'PulsePass E2E Sede Aforo 1', 'Bogotá', NULL, 1, TRUE);
        GET DIAGNOSTICS v_n = ROW_COUNT;
        IF v_n <> 1 THEN RAISE EXCEPTION 'Expected 1 row for venue V1, got %', v_n; END IF;

        IF v_inv = 'on' THEN
            INSERT INTO venues (code, name, city, address, capacity, active)
            VALUES ('E2E-' || v_run || '-VX', 'PulsePass E2E Sede Inactiva', 'Bogotá', NULL, 50, FALSE);
            GET DIAGNOSTICS v_n = ROW_COUNT;
            IF v_n <> 1 THEN RAISE EXCEPTION 'Expected 1 row for venue VX, got %', v_n; END IF;
        END IF;

    WHEN 'promote' THEN
        -- STAFF y ADMIN en la misma transacción: los dos o ninguno.
        FOREACH v_role IN ARRAY ARRAY['STAFF', 'ADMIN'] LOOP
            v_email := lower(v_role) || '.' || v_run || '@example.test';

            SELECT u.id INTO STRICT v_id FROM users u WHERE lower(u.email) = v_email FOR UPDATE;

            SELECT coalesce(array_agg(r.role::text ORDER BY r.role), '{}') INTO v_roles
            FROM user_roles r WHERE r.user_id = v_id;
            IF v_roles <> ARRAY['USER']::text[] THEN
                RAISE EXCEPTION 'User % must have exactly roles {USER}, has %', v_email, v_roles;
            END IF;
            IF NOT EXISTS (SELECT 1 FROM users u WHERE u.id = v_id AND u.active) THEN
                RAISE EXCEPTION 'User % is not active', v_email;
            END IF;
            IF NOT EXISTS (SELECT 1 FROM user_credentials c WHERE c.user_id = v_id) THEN
                RAISE EXCEPTION 'User % has no credential (must be registered through the API)', v_email;
            END IF;

            DELETE FROM user_roles WHERE user_id = v_id AND role = 'USER';
            GET DIAGNOSTICS v_n = ROW_COUNT;
            IF v_n <> 1 THEN RAISE EXCEPTION 'Expected 1 USER role row for %, got %', v_email, v_n; END IF;

            INSERT INTO user_roles (user_id, role) VALUES (v_id, v_role);
            GET DIAGNOSTICS v_n = ROW_COUNT;
            IF v_n <> 1 THEN RAISE EXCEPTION 'Expected 1 % role row for %, got %', v_role, v_email, v_n; END IF;

            SELECT coalesce(array_agg(r.role::text ORDER BY r.role), '{}') INTO v_roles
            FROM user_roles r WHERE r.user_id = v_id;
            IF v_roles <> ARRAY[v_role]::text[] THEN
                RAISE EXCEPTION 'User % must end with exactly roles {%}, has %', v_email, v_role, v_roles;
            END IF;
        END LOOP;

    WHEN 'deactivate' THEN
        v_email := 'inactive.' || v_run || '@example.test';

        SELECT u.id INTO STRICT v_id FROM users u WHERE lower(u.email) = v_email FOR UPDATE;

        SELECT coalesce(array_agg(r.role::text ORDER BY r.role), '{}') INTO v_roles
        FROM user_roles r WHERE r.user_id = v_id;
        IF v_roles <> ARRAY['USER']::text[] THEN
            RAISE EXCEPTION 'User % must have exactly roles {USER}, has %', v_email, v_roles;
        END IF;

        UPDATE users SET active = FALSE WHERE id = v_id AND active;
        GET DIAGNOSTICS v_n = ROW_COUNT;
        IF v_n <> 1 THEN RAISE EXCEPTION 'Expected 1 active user row for %, got %', v_email, v_n; END IF;

    WHEN 'revoke-staff' THEN
        v_email := 'staff.' || v_run || '@example.test';

        SELECT u.id INTO STRICT v_id FROM users u WHERE lower(u.email) = v_email FOR UPDATE;

        SELECT coalesce(array_agg(r.role::text ORDER BY r.role), '{}') INTO v_roles
        FROM user_roles r WHERE r.user_id = v_id;
        IF v_roles <> ARRAY['STAFF']::text[] THEN
            RAISE EXCEPTION 'User % must have exactly roles {STAFF}, has %', v_email, v_roles;
        END IF;

        DELETE FROM user_roles WHERE user_id = v_id AND role = 'STAFF';
        GET DIAGNOSTICS v_n = ROW_COUNT;
        IF v_n <> 1 THEN RAISE EXCEPTION 'Expected 1 STAFF role row for %, got %', v_email, v_n; END IF;

    END CASE;
END $$;

-- C. Evidencia: estado del run_id dentro de la transacción, antes del COMMIT.
SELECT code, name, capacity, active
FROM venues
WHERE code LIKE 'E2E-' || current_setting('pulsepass_e2e.run_id') || '-%'
ORDER BY code;

SELECT u.email,
       u.active,
       coalesce((SELECT string_agg(r.role, ',' ORDER BY r.role) FROM user_roles r WHERE r.user_id = u.id), '') AS roles
FROM users u
WHERE lower(u.email) LIKE '%.' || current_setting('pulsepass_e2e.run_id') || '@example.test'
ORDER BY u.email;

COMMIT;
