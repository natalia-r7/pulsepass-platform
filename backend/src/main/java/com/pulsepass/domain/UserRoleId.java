package com.pulsepass.domain;

import com.pulsepass.domain.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clave compuesta de {@code user_roles}: (user_id, role), que es {@code pk_user_roles} en V4
 * (PRD Seguridad §9.1).
 * <p>
 * JPA exige que una clave compuesta sea {@link Serializable} y tenga equals/hashCode por valor.
 * A diferencia de P1-D4, el hashCode no es constante: la clave se fija en el constructor y no
 * cambia nunca, así que su valor es estable.
 */
@Embeddable
public class UserRoleId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Se persiste por nombre y no por ordinal, como los demás enums (BR-008, V1-11). */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    /** Requerido por JPA para crear la clave al leerla de la base de datos. */
    protected UserRoleId() {
    }

    public UserRoleId(Long userId, Role role) {
        this.userId = userId;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    /** Dos claves son iguales si coinciden el usuario y el rol. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserRoleId other)) {
            return false;
        }
        return Objects.equals(userId, other.userId) && role == other.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, role);
    }
}
