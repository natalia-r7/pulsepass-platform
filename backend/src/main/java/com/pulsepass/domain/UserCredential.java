package com.pulsepass.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Credencial de inicio de sesión de un usuario (PRD Seguridad §9.1, SEC-003).
 * Se mapea a la tabla {@code user_credentials} creada en V4: relación 1:1 con {@link User}
 * en la que {@code user_id} es a la vez PK y FK. {@link User} no se modifica ni conoce esta
 * entidad (§9.1: evita serializar el hash por accidente).
 * <p>
 * {@code @MapsId} (P4-02-a): el id se copia del usuario al persistir, así que es {@code null}
 * hasta entonces y {@code save()} de Spring Data ejecuta {@code persist()}, nunca
 * {@code merge()}. Una segunda credencial para el mismo usuario provoca la violación de
 * {@code pk_user_credentials} en lugar de sobrescribir el hash existente.
 */
@Entity
@Table(name = "user_credentials")
public class UserCredential {

    /** PK = FK a users.id; la asigna Hibernate desde {@link #user} (columna user_id). */
    @Id
    private Long userId;

    /** Lado propietario de la relación 1:1; LAZY: leer la credencial no carga el usuario. */
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Hash BCrypt de la contraseña; nunca la contraseña en claro (SEC-003). Sin setter: el
     * cambio de contraseña está fuera del alcance (§5.2). No se sobrescribe toString para
     * que el hash no llegue a los logs (NFR-SEC-001).
     */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /** Requerido por JPA para crear la entidad al leerla de la base de datos. */
    protected UserCredential() {
    }

    /** Crea la credencial de un usuario ya persistido con el hash de su contraseña. */
    public UserCredential(User user, String passwordHash) {
        this.user = user;
        this.passwordHash = passwordHash;
    }

    public Long getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    /** Dos credenciales son iguales solo si ya están persistidas y tienen el mismo id (P1-D4). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserCredential other)) {
            return false;
        }
        return userId != null && userId.equals(other.getUserId());
    }

    /** Valor fijo por tipo: no cambia cuando Hibernate asigna el id al persistir (P1-D4). */
    @Override
    public int hashCode() {
        return UserCredential.class.hashCode();
    }
}
