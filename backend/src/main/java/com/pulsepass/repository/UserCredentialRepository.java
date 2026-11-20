package com.pulsepass.repository;

import com.pulsepass.domain.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link UserCredential} (PRD Seguridad §14.5: «acceso mínimo a
 * credenciales»). La clave es el id del usuario: findById(userId) devuelve su credencial con
 * una sola consulta por PK, sin cargar {@code User} (relación LAZY). No necesita métodos propios.
 */
public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
}
