package com.pulsepass.domain.enums;

/**
 * Rol de seguridad de un usuario (PRD Seguridad §5.1, §9.1).
 * Se persiste por nombre, sin prefijo, en {@code user_roles.role}; la base de datos solo
 * acepta estos valores (ck_user_roles_role). Spring Security añade el prefijo {@code ROLE_}
 * al construir las authorities (P4-05), no el dominio.
 */
public enum Role {
    USER,
    STAFF,
    ADMIN
}
