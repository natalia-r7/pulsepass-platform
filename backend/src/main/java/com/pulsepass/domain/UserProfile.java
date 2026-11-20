package com.pulsepass.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Perfil individual de un usuario (PRD Persistencia §6.1, FR-USR-003..004, BR-004).
 * Se mapea a la tabla {@code user_profiles} creada en V1. Es el lado propietario de
 * la relación 1:1: tiene la FK user_id, cuyo UNIQUE (uk_user_profiles_user_id)
 * garantiza un único perfil por usuario.
 */
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    /** Lado propietario de User 1:1 UserProfile. Sin cascade. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Requerido por JPA para crear la entidad al leerla de la base de datos. */
    protected UserProfile() {
    }

    /**
     * Crea un perfil con sus campos obligatorios (P1-08-b). El usuario no se recibe aquí:
     * se enlaza con {@link User#setProfile(UserProfile)}, que sincroniza ambos lados.
     * phone y city son opcionales (V1-3).
     */
    public UserProfile(String firstName, String lastName, LocalDate birthDate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public User getUser() {
        return user;
    }

    /**
     * Visibilidad de paquete, no pública (P1-08-b): solo {@link User#setProfile(UserProfile)}
     * debe llamarlo, para que los dos lados de la relación nunca queden desincronizados.
     */
    void setUser(User user) {
        this.user = user;
    }

    /** Dos perfiles son iguales solo si ya están persistidos y tienen el mismo id (P1-D4). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserProfile other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    /** Valor fijo por tipo: no cambia cuando la base de datos asigna el id (P1-D4). */
    @Override
    public int hashCode() {
        return UserProfile.class.hashCode();
    }
}
