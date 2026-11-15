package com.pulsepass.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Usuario de PulsePass (PRD Persistencia §6.1, FR-USR-001..002).
 * Representa la identidad de dominio, no credenciales de seguridad (R-004).
 * Se mapea a la tabla {@code users} creada en V1; los UNIQUE de username y
 * email los impone PostgreSQL.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "active", nullable = false)
    private Boolean active;

    /**
     * Perfil individual del usuario (BR-004, FR-USR-004). Lado inverso de la relación 1:1:
     * la FK user_id vive en UserProfile (mappedBy = "user").
     * Cascade PERSIST y MERGE (P1-D3): al guardar el usuario se guarda su perfil.
     * No se declara fetch (P1-08-a): el valor predeterminado de @OneToOne es EAGER, y en el
     * lado inverso Hibernate necesita consultar user_profiles para saber si el perfil existe,
     * por lo que el perfil se carga junto con el usuario.
     */
    @OneToOne(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private UserProfile profile;

    /** Requerido por JPA para crear la entidad al leerla de la base de datos. */
    protected User() {
    }

    /** Crea un usuario con sus campos obligatorios; el perfil se asocia con setProfile. */
    public User(String username, String email, Boolean active) {
        this.username = username;
        this.email = email;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public UserProfile getProfile() {
        return profile;
    }

    /**
     * Asocia el perfil y mantiene sincronizados ambos lados de la relación 1:1 (P1-D3).
     * Es la única forma de enlazar User y UserProfile (P1-08-b). No impide asignar un
     * segundo perfil: esa violación la rechaza PostgreSQL (uk_user_profiles_user_id, AC-004).
     */
    public void setProfile(UserProfile profile) {
        this.profile = profile;
        if (profile != null) {
            profile.setUser(this);
        }
    }

    /** Dos usuarios son iguales solo si ya están persistidos y tienen el mismo id (P1-D4). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    /** Valor fijo por tipo: no cambia cuando la base de datos asigna el id (P1-D4). */
    @Override
    public int hashCode() {
        return User.class.hashCode();
    }
}
