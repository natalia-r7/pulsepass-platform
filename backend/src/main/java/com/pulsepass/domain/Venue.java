package com.pulsepass.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Sede donde se realizan los eventos (PRD Persistencia §6.1, FR-VEN-001..003).
 * Se mapea a la tabla {@code venues} creada en V1. Las restricciones reales
 * (UNIQUE de code, CHECK capacity > 0) las impone PostgreSQL.
 */
@Entity
@Table(name = "venues")
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "active", nullable = false)
    private Boolean active;

    /** Requerido por JPA para crear la entidad al leerla de la base de datos. */
    protected Venue() {
    }

    /** Crea una sede con todos sus campos obligatorios; address es opcional (V1-3). */
    public Venue(String code, String name, String city, Integer capacity, Boolean active) {
        this.code = code;
        this.name = name;
        this.city = city;
        this.capacity = capacity;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    /** Dos sedes son iguales solo si ya están persistidas y tienen el mismo id (P1-D4). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Venue other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    /** Valor fijo por tipo: no cambia cuando la base de datos asigna el id (P1-D4). */
    @Override
    public int hashCode() {
        return Venue.class.hashCode();
    }
}
