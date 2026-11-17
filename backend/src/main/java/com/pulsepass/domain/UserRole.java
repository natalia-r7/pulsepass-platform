package com.pulsepass.domain;

import com.pulsepass.domain.enums.Role;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

/**
 * Rol asignado a un usuario (PRD Seguridad §9.1). Se mapea a la tabla {@code user_roles} de V4.
 * <p>
 * Sin relación {@code @ManyToOne} con {@link User} (P4-02-b): la relación N:1 la garantiza
 * {@code fk_user_roles_user} en la base de datos, y la seguridad solo necesita el
 * {@code userId} para buscar los roles de un usuario.
 * <p>
 * {@link Persistable} (P4-02-b): la clave compuesta nunca es {@code null}, así que sin esto
 * {@code save()} de Spring Data ejecutaría {@code merge()} y un rol repetido se ignoraría en
 * silencio. Con {@link #isNew()} a {@code true} hasta persistir o cargar, {@code save()}
 * ejecuta {@code persist()} y un duplicado viola {@code pk_user_roles}.
 */
@Entity
@Table(name = "user_roles")
public class UserRole implements Persistable<UserRoleId> {

    @EmbeddedId
    private UserRoleId id;

    /** No es una columna: indica a Spring Data si la entidad todavía no se ha persistido. */
    @Transient
    private boolean isNew = true;

    /** Requerido por JPA para crear la entidad al leerla de la base de datos. */
    protected UserRole() {
    }

    /** Asigna un rol a un usuario ya persistido. */
    public UserRole(Long userId, Role role) {
        this.id = new UserRoleId(userId, role);
    }

    @Override
    public UserRoleId getId() {
        return id;
    }

    public Long getUserId() {
        return id.getUserId();
    }

    public Role getRole() {
        return id.getRole();
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    /** Tras guardarla o leerla, la entidad ya existe en la base de datos. */
    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    /** Dos asignaciones son iguales si tienen la misma clave (usuario y rol). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserRole other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
