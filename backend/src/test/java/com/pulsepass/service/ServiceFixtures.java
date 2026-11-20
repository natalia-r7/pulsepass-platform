package com.pulsepass.service;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Datos de prueba de la Fase 2 - Servicios (P2-D12).
 *
 * <p>Solo construye objetos: no guarda nada ni asigna IDs. Cada prueba decide qué hace con
 * ellos. Cada llamada devuelve un objeto nuevo.
 *
 * <p>Convención de comentarios:
 * <ul>
 *   <li>{@code PRD §47}: valor tomado del escenario principal de aceptación del PRD de
 *       Servicios.</li>
 *   <li>{@code DATO DE PRUEBA}: valor ficticio elegido para la prueba (D2 de la Fase 1);
 *       no es un requisito.</li>
 * </ul>
 *
 * <p>Los datos de este archivo pertenecen exclusivamente al PRD de Servicios. Es
 * independiente de {@code PersistenceFixtures}, cuyos datos son del PRD de Persistencia y en
 * algunos casos difieren (por ejemplo, la capacidad de VEN-SMR-01). No incluye tickets ni
 * precios: la estrategia de precios se decide en P2-12 (P2-D2). El «tercer usuario adulto»
 * de AC-008 es {@link #sofia()}, cuyos datos son todos DATO DE PRUEBA (P2-13 TK-8).
 *
 * <p>Es pública porque la usan pruebas de otros paquetes, como {@code com.pulsepass.mapper}.
 */
public final class ServiceFixtures {

    /**
     * DATO DE PRUEBA: §47 no indica la fecha de CMF-2026. Es una fecha fija (no now()) para
     * que las pruebas den el mismo resultado cualquier día. Las edades de los usuarios de
     * §47 se cumplen en esta fecha.
     */
    public static final LocalDateTime CMF_2026_EVENT_DATE = LocalDateTime.of(2026, 12, 12, 20, 0);

    private ServiceFixtures() {
        // Clase de utilidades: no se instancia.
    }

    // ---------------------------------------------------------------- Venue

    /** Venue VEN-SMR-01 del escenario principal (PRD §47). address queda en null (V1-3). */
    public static Venue venSmr01() {
        return new Venue(
                "VEN-SMR-01",                // PRD §47 code
                "Marina Convention Center",  // PRD §47 name
                "Santa Marta",               // PRD §47 city
                3,                           // PRD §47 capacity
                true);                       // PRD §47 active
    }

    // ---------------------------------------------------------------- Event

    /**
     * Evento CMF-2026 en estado inicial DRAFT (PRD §47), sin artistas asociados.
     * description y streamingUrl quedan en null.
     */
    public static Event cmf2026(Venue venue) {
        return new Event(
                "CMF-2026",                  // PRD §47 code
                "Caribbean Music Fest 2026", // PRD §47 name
                EventCategory.MUSIC,         // PRD §47 category
                EventStatus.DRAFT,           // PRD §47 status inicial
                CMF_2026_EVENT_DATE,         // DATO DE PRUEBA
                18,                          // PRD §47 minimum age
                venue);
    }

    // ---------------------------------------------------------------- Artists

    // country y genre quedan en null: §47 no los indica.

    /** Artista Solar Beat (PRD §47). */
    public static Artist solarBeat() {
        return new Artist("Solar Beat", true); // PRD §47 name; DATO DE PRUEBA: active
    }

    /** Artista Neon Waves (PRD §47). */
    public static Artist neonWaves() {
        return new Artist("Neon Waves", true); // PRD §47 name; DATO DE PRUEBA: active
    }

    /** Artista Caribbean Sound (PRD §47). */
    public static Artist caribbeanSound() {
        return new Artist("Caribbean Sound", true); // PRD §47 name; DATO DE PRUEBA: active
    }

    // ---------------------------------------------------------------- Users

    // §47 da el nombre, el email, la edad y el estado activo. username, lastName y birthDate
    // son DATO DE PRUEBA; cada birthDate está elegida para que la edad en
    // CMF_2026_EVENT_DATE sea la de §47, lejos del día del cumpleaños. phone y city quedan en
    // null (V1-3).

    /** Andrea: adulta y activa (PRD §47). */
    public static User andrea() {
        return user("andrea", "andrea@email.com", true,
                "Andrea", "Rojas", LocalDate.of(2001, 6, 15)); // 25 años en el evento
    }

    /** Carlos: adulto y activo (PRD §47). */
    public static User carlos() {
        return user("carlos", "carlos@email.com", true,
                "Carlos", "Pérez", LocalDate.of(2005, 6, 15)); // 21 años en el evento
    }

    /** Laura: menor de edad y activa (PRD §47). */
    public static User laura() {
        return user("laura", "laura@email.com", true,
                "Laura", "Díaz", LocalDate.of(2009, 6, 15)); // 17 años en el evento
    }

    /** Miguel: adulto e inactivo (PRD §47). */
    public static User miguel() {
        return user("miguel", "miguel@email.com", false,
                "Miguel", "Torres", LocalDate.of(1996, 6, 15)); // 30 años en el evento
    }

    /**
     * Sofía: el «tercer usuario adulto» de AC-008, adulta y activa. §47 no la define: todos sus
     * datos son DATO DE PRUEBA aprobado en P2-13 (TK-8), no un requisito del PRD.
     */
    public static User sofia() {
        return user("sofia", "sofia@email.com", true,
                "Sofía", "Gómez", LocalDate.of(1998, 6, 15)); // DATO DE PRUEBA; 28 años en el evento
    }

    /**
     * Crea un User con su UserProfile enlazado mediante {@link User#setProfile(UserProfile)},
     * que sincroniza los dos lados de la relación (P1-D3).
     */
    private static User user(String username, String email, boolean active,
                             String firstName, String lastName, LocalDate birthDate) {
        User user = new User(username, email, active);
        user.setProfile(new UserProfile(firstName, lastName, birthDate));
        return user;
    }
}
