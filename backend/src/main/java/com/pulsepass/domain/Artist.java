package com.pulsepass.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Artista que participa en eventos (PRD Persistencia §6.1, FR-ART-001..002).
 * Se mapea a la tabla {@code artists} creada en V1. El UNIQUE de stage_name
 * lo impone PostgreSQL. La relación con Event se define en Event (lado dueño).
 */
@Entity
@Table(name = "artists")
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "stage_name", nullable = false, length = 150)
    private String stageName;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "genre", length = 100)
    private String genre;

    @Column(name = "active", nullable = false)
    private Boolean active;

    /** Requerido por JPA para crear la entidad al leerla de la base de datos. */
    protected Artist() {
    }

    /** Crea un artista con sus campos obligatorios; country y genre son opcionales (A8). */
    public Artist(String stageName, Boolean active) {
        this.stageName = stageName;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    /** Dos artistas son iguales solo si ya están persistidos y tienen el mismo id (P1-D4). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Artist other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    /** Valor fijo por tipo: no cambia cuando la base de datos asigna el id (P1-D4). */
    @Override
    public int hashCode() {
        return Artist.class.hashCode();
    }
}
