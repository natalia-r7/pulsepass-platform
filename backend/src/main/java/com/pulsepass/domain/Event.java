package com.pulsepass.domain;

import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Evento de PulsePass (PRD Persistencia §6.1, FR-EVT-001..006).
 * Se mapea a la tabla {@code events} (V1) más la columna streaming_url (V3).
 * Es el lado dueño de sus dos relaciones: Venue 1:N Event (BR-001) y
 * Event N:M Artist mediante event_artists (BR-003, FR-ART-003).
 */
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "event_code", nullable = false, length = 30)
    private String eventCode;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private EventCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "minimum_age", nullable = false)
    private Integer minimumAge;

    /** Columna agregada en V3 (FR-EVT-006); admite NULL. */
    @Column(name = "streaming_url", length = 500)
    private String streamingUrl;

    /** Todo evento pertenece a exactamente un venue (BR-001). Sin cascade (P1-07-d). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    /** Artistas del evento; la PK de event_artists impide pares repetidos (FR-ART-003). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "event_artists",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id"))
    private Set<Artist> artists = new HashSet<>();

    /** Requerido por JPA para crear la entidad al leerla de la base de datos. */
    protected Event() {
    }

    /**
     * Crea un evento con todos sus campos obligatorios (P1-07-a).
     * description y streamingUrl son opcionales y se asignan con sus setters.
     */
    public Event(String eventCode, String name, EventCategory category, EventStatus status,
                 LocalDateTime eventDate, Integer minimumAge, Venue venue) {
        this.eventCode = eventCode;
        this.name = name;
        this.category = category;
        this.status = status;
        this.eventDate = eventDate;
        this.minimumAge = minimumAge;
        this.venue = venue;
    }

    public Long getId() {
        return id;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EventCategory getCategory() {
        return category;
    }

    public void setCategory(EventCategory category) {
        this.category = category;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public Integer getMinimumAge() {
        return minimumAge;
    }

    public void setMinimumAge(Integer minimumAge) {
        this.minimumAge = minimumAge;
    }

    public String getStreamingUrl() {
        return streamingUrl;
    }

    public void setStreamingUrl(String streamingUrl) {
        this.streamingUrl = streamingUrl;
    }

    public Venue getVenue() {
        return venue;
    }

    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    /** Vista de solo lectura: el Set administrado por Hibernate no se expone ni se reemplaza (P1-07-b). */
    public Set<Artist> getArtists() {
        return Collections.unmodifiableSet(artists);
    }

    /** Asocia un artista; el Set ignora un artista ya asociado (sin reglas de negocio, P1-07-b). */
    public void addArtist(Artist artist) {
        artists.add(artist);
    }

    /** Quita la asociación con un artista. */
    public void removeArtist(Artist artist) {
        artists.remove(artist);
    }

    /** Dos eventos son iguales solo si ya están persistidos y tienen el mismo id (P1-D4). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Event other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    /** Valor fijo por tipo: no cambia cuando la base de datos asigna el id (P1-D4). */
    @Override
    public int hashCode() {
        return Event.class.hashCode();
    }
}
