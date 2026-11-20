package com.pulsepass.repository;

import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.UserRoleId;
import com.pulsepass.domain.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Acceso a datos de {@link UserRole} (PRD Seguridad §9.1, NFR-SEC-003).
 */
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    /**
     * Roles de un usuario en una sola consulta sobre pk_user_roles, que empieza por user_id
     * (P4-02-c): devuelve los valores de {@link Role} sin cargar entidades ni unir con users.
     * Orden alfabético para un resultado determinista; lista vacía si el usuario no tiene roles.
     */
    @Query("select r.id.role from UserRole r where r.id.userId = :userId order by r.id.role")
    List<Role> findRolesByUserId(@Param("userId") Long userId);
}
