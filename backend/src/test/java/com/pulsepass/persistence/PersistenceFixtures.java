package com.pulsepass.persistence;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Datos de prueba de la Fase 1 - Persistencia (P1-12-a).
 *
 * <p>Solo construye objetos: no guarda nada. Cada prueba decide qué persiste.
 *
 * <p>Convención de comentarios:
 * <ul>
 *   <li>{@code PRD §16}: valor tomado de los datos de referencia del PRD de Persistencia.</li>
 *   <li>{@code DATO DE PRUEBA}: valor ficticio elegido para la prueba (P1-D2); no es un requisito.</li>
 * </ul>
 *
 * <p>Los datos de este archivo pertenecen exclusivamente al PRD de Persistencia y no se
 * mezclan con los escenarios de los PRD de Servicios, Controladores, Seguridad o Frontend.
 * Las demás entidades se agregarán en las siguientes iteraciones de P1-12.
 */
final class PersistenceFixtures {

    /**
     * DATO DE PRUEBA: §16.2 no indica la fecha de CMF-2026. Fecha fija (no now()) para que
     * las pruebas den el mismo resultado cualquier día que se ejecuten.
     */
    static final LocalDateTime CMF_2026_EVENT_DATE = LocalDateTime.of(2030, 6, 1, 20, 0);

    /** DATO DE PRUEBA: §16.2 no indica la edad mínima de CMF-2026. */
    static final int CMF_2026_MINIMUM_AGE = 0;

    /**
     * Artistas de CMF-2026 (PRD §16.2). Ya existen en la base de datos porque los inserta V2:
     * las pruebas los consultan por nombre y nunca los vuelven a insertar.
     */
    static final List<String> CMF_2026_ARTIST_NAMES = List.of(
            "Solar Beat",       // PRD §16.2
            "Neon Waves",       // PRD §16.2
            "Caribbean Sound"); // PRD §16.2

    private PersistenceFixtures() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Venue de referencia VEN-SMR-01 (PRD §16.1). Cada llamada devuelve un objeto nuevo,
     * sin persistir.
     */
    static Venue venSmr01() {
        // address: §16.1 no la indica; queda en null (columna opcional, V1-3).
        return new Venue(
                "VEN-SMR-01",                // PRD §16.1 code
                "Marina Convention Center",  // PRD §16.1 name
                "Santa Marta",               // PRD §16.1 city
                5000,                        // PRD §16.1 capacity
                true);                       // DATO DE PRUEBA: §16.1 no indica el estado activo
    }

    /**
     * DATO DE PRUEBA: venue adicional, no definido en §16, para comprobar que las consultas
     * por venue no mezclan eventos de sedes distintas.
     */
    static Venue otherVenue() {
        return new Venue(
                "VEN-TST-02",                // DATO DE PRUEBA
                "Sede de Prueba",            // DATO DE PRUEBA
                "Barranquilla",              // DATO DE PRUEBA
                100,                         // DATO DE PRUEBA
                true);                       // DATO DE PRUEBA
    }

    /**
     * Evento principal CMF-2026 asociado al venue recibido (PRD §16.2). Sin artistas: se
     * asocian en las pruebas de la relación N:M. Cada llamada devuelve un objeto nuevo.
     */
    static Event cmf2026(Venue venue) {
        // description y streamingUrl: §16.2 no los indica; quedan en null (columnas opcionales).
        return new Event(
                "CMF-2026",                  // PRD §16.2 eventCode
                "Caribbean Music Fest 2026", // PRD §16.2 name
                EventCategory.MUSIC,         // PRD §16.2 category
                EventStatus.PUBLISHED,       // PRD §16.2 status
                CMF_2026_EVENT_DATE,         // DATO DE PRUEBA (ver constante)
                CMF_2026_MINIMUM_AGE,        // DATO DE PRUEBA (ver constante)
                venue);
    }

    /**
     * DATO DE PRUEBA: evento adicional, no definido en §16, con el código, estado y fecha que
     * necesite cada prueba (por ejemplo, para comparar estados u ordenar por fecha).
     */
    static Event testEvent(String eventCode, EventStatus status, LocalDateTime eventDate, Venue venue) {
        return new Event(
                eventCode,                       // DATO DE PRUEBA
                "Evento de prueba " + eventCode, // DATO DE PRUEBA
                EventCategory.CULTURE,           // DATO DE PRUEBA
                status,                          // DATO DE PRUEBA
                eventDate,                       // DATO DE PRUEBA
                0,                               // DATO DE PRUEBA
                venue);
    }

    /**
     * DATO DE PRUEBA: artista nuevo, no definido en §16 ni en V2, con el nombre artístico que
     * necesite cada prueba. Cada llamada devuelve un objeto nuevo, sin persistir.
     */
    static Artist testArtist(String stageName) {
        Artist artist = new Artist(
                stageName,                   // DATO DE PRUEBA
                true);                       // DATO DE PRUEBA
        artist.setCountry("Colombia");       // DATO DE PRUEBA
        artist.setGenre("Electrónica");      // DATO DE PRUEBA
        return artist;
    }

    /**
     * Usuario con su perfil, para los nombres de §16.3 (Andrea, Carlos, Laura, Miguel).
     * §16.3 solo indica el nombre; el resto son datos de prueba. El perfil se enlaza con
     * {@link User#setProfile(UserProfile)} (P1-D3), así que basta guardar el usuario para que el
     * cascade PERSIST guarde también el perfil. Cada llamada devuelve objetos nuevos, sin persistir.
     */
    static User userWithProfile(String firstName) {
        String login = firstName.toLowerCase(Locale.ROOT);
        User user = new User(
                login,                               // DATO DE PRUEBA: username derivado del nombre
                login + "@pulsepass.test",           // DATO DE PRUEBA: dominio reservado .test
                true);                               // DATO DE PRUEBA: §16.3 no indica el estado
        // phone y city: no se asignan; quedan en null (columnas opcionales, V1-3).
        user.setProfile(new UserProfile(
                firstName,                           // PRD §16.3 (nombre del usuario)
                "Prueba",                            // DATO DE PRUEBA
                LocalDate.of(1995, 1, 15)));         // DATO DE PRUEBA
        return user;
    }

    /**
     * DATO DE PRUEBA: usuario sin perfil, con el username y el email que necesite cada prueba
     * (por ejemplo, para provocar duplicados). Cada llamada devuelve un objeto nuevo, sin persistir.
     */
    static User testUser(String username, String email) {
        return new User(
                username,                            // DATO DE PRUEBA
                email,                               // DATO DE PRUEBA
                true);                               // DATO DE PRUEBA
    }

    /** DATO DE PRUEBA: perfil adicional, por ejemplo para intentar un segundo perfil (AC-004). */
    static UserProfile testProfile(String firstName) {
        return new UserProfile(
                firstName,                           // DATO DE PRUEBA
                "Prueba",                            // DATO DE PRUEBA
                LocalDate.of(1995, 1, 15));          // DATO DE PRUEBA
    }
}
