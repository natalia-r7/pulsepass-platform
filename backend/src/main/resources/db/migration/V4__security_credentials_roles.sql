-- =====================================================================
-- PulsePass - V4: credenciales y roles (Fase 4 - Seguridad)
-- PRD Seguridad §9.1, §9.2, §9.3; FR-SEC-AUTH-001; NFR-SEC-009.
--
-- * users no se modifica: la identidad de login se vincula a users.id (§9.1).
-- * user_credentials (1:1): user_id es PK y FK a la vez (§9.1).
--   password_hash VARCHAR(100): un hash BCrypt ocupa 60 caracteres; el
--   margen es una decisión técnica (P4-D10). Sin password_changed_at (P4-D10).
-- * user_roles (N:1): PK compuesta (user_id, role), que impide repetir un rol.
--   El rol se guarda sin prefijo: USER, no ROLE_USER (A9, V1-11).
-- * Sin índices adicionales: ambas PK empiezan por user_id y ya cubren
--   la búsqueda por usuario (A10).
-- * Usuarios existentes (P4-D11): reciben ROLE_USER, pero NO credenciales.
--   No se inventa ninguna contraseña (§9.2): esos usuarios no pueden
--   iniciar sesión hasta una activación administrativa documentada.
-- * Nunca se asigna STAFF ni ADMIN en esta migración (§9.2, NFR-SEC-009).
-- =====================================================================


-- ---------------------------------------------------------------------
-- user_credentials: hash de la contraseña de cada usuario (§9.1, SEC-003)
-- ---------------------------------------------------------------------
CREATE TABLE user_credentials (
    user_id       BIGINT       NOT NULL,
    password_hash VARCHAR(100) NOT NULL,

    CONSTRAINT pk_user_credentials      PRIMARY KEY (user_id),
    CONSTRAINT fk_user_credentials_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE NO ACTION
);


-- ---------------------------------------------------------------------
-- user_roles: roles de cada usuario (§9.1; ROLE_USER, ROLE_STAFF, ROLE_ADMIN)
-- ---------------------------------------------------------------------
CREATE TABLE user_roles (
    user_id BIGINT      NOT NULL,
    role    VARCHAR(20) NOT NULL,

    CONSTRAINT pk_user_roles      PRIMARY KEY (user_id, role),
    CONSTRAINT ck_user_roles_role CHECK (role IN ('USER', 'STAFF', 'ADMIN')),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE NO ACTION
);


-- ---------------------------------------------------------------------
-- Usuarios existentes (§9.2, P4-D11): ROLE_USER, sin credenciales.
-- En una base nueva no hay usuarios y no se inserta ninguna fila.
-- ---------------------------------------------------------------------
INSERT INTO user_roles (user_id, role)
SELECT id, 'USER'
FROM users;
