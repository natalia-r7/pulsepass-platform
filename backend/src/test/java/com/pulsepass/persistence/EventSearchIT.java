package com.pulsepass.persistence;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.pulsepass.persistence.PersistenceFixtures.cmf2026;
import static com.pulsepass.persistence.PersistenceFixtures.otherVenue;
import static com.pulsepass.persistence.PersistenceFixtures.testEvent;
import static com.pulsepass.persistence.PersistenceFixtures.testTicket;
import static com.pulsepass.persistence.PersistenceFixtures.testUser;
import static com.pulsepass.persistence.PersistenceFixtures.venSmr01;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de las consultas de búsqueda (PRD Persistencia §22: FR-SRC-* -> EventSearchIT).
 * Cubre FR-SRC-001..004, AC-007 y QT-008 contra PostgreSQL 17.
 *
 * <p>Los artistas se leen de V2 (nunca se reinsertan). VEN-SMR-01 está en Santa Marta (§16.1);
 * la sede adicional de PersistenceFixtures está en Barranquilla (DATO DE PRUEBA). Todas las
 * fechas son fijas; las lecturas usan flush() + clear() (P1-12-b).
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class EventSearchIT {

    /** DATO DE PRUEBA: fecha de referencia fija para FR-SRC-003 y FR-SRC-004. */
    private static final LocalDateTime REFERENCE_DATE = LocalDateTime.of(2030, 6, 1, 0, 0);

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("FR-SRC-001 / AC-007 / QT-008: varios eventos con Solar Beat aparecen una sola vez al buscar por artista")
    void findsEventsByArtistWithoutDuplicates() {
        Venue santaMarta = venueRepository.saveAndFlush(venSmr01());
        Artist solarBeat = artist("Solar Beat");
        Artist neonWaves = artist("Neon Waves");
        Artist caribbeanSound = artist("Caribbean Sound");

        // PRD §16.2: CMF-2026 con sus tres artistas (Solar Beat entre ellos).
        Event cmf = cmf2026(santaMarta);
        cmf.addArtist(solarBeat);
        cmf.addArtist(neonWaves);
        cmf.addArtist(caribbeanSound);
        eventRepository.save(cmf);
        // DATO DE PRUEBA: otro evento con Solar Beat y uno sin él.
        eventRepository.save(event("TST-SB-2", EventStatus.PUBLISHED, LocalDateTime.of(2030, 9, 1, 20, 0), santaMarta, solarBeat));
        eventRepository.save(event("TST-NO-SB", EventStatus.PUBLISHED, LocalDateTime.of(2030, 9, 2, 20, 0), santaMarta, neonWaves));
        entityManager.flush();
        entityManager.clear();

        List<Event> events = eventRepository.findByArtistStageName("Solar Beat");

        assertThat(events)
                .extracting(Event::getEventCode)
                .containsExactlyInAnyOrder("CMF-2026", "TST-SB-2")
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("FR-SRC-002: filtra por ciudad y artista exactos, sin mezclar otras ciudades ni otros artistas")
    void findsEventsByCityAndArtist() {
        Venue santaMarta = venueRepository.saveAndFlush(venSmr01());
        Venue barranquilla = venueRepository.saveAndFlush(otherVenue());
        Artist solarBeat = artist("Solar Beat");
        Artist neonWaves = artist("Neon Waves");

        // DATO DE PRUEBA: combinaciones de ciudad y artista.
        eventRepository.save(event("TST-SM-SB-1", EventStatus.PUBLISHED, LocalDateTime.of(2030, 7, 1, 20, 0), santaMarta, solarBeat));   // cumple
        eventRepository.save(event("TST-SM-SB-2", EventStatus.DRAFT, LocalDateTime.of(2030, 8, 1, 20, 0), santaMarta, solarBeat));       // cumple: sin filtro de estado
        eventRepository.save(event("TST-SM-NW", EventStatus.PUBLISHED, LocalDateTime.of(2030, 7, 2, 20, 0), santaMarta, neonWaves));     // otro artista
        eventRepository.save(event("TST-BQ-SB", EventStatus.PUBLISHED, LocalDateTime.of(2030, 7, 3, 20, 0), barranquilla, solarBeat));   // otra ciudad
        entityManager.flush();
        entityManager.clear();

        List<Event> events = eventRepository.findByVenueCityAndArtistStageName("Santa Marta", "Solar Beat");

        // Sin ORDER BY en la consulta (P1-D10): se comprueba el contenido, no el orden.
        assertThat(events)
                .extracting(Event::getEventCode)
                .containsExactlyInAnyOrder("TST-SM-SB-1", "TST-SM-SB-2");
    }

    @Test
    @DisplayName("FR-SRC-003: recomendados PUBLISHED, posteriores a la fecha, de la ciudad, con texto del artista sin distinguir mayúsculas, sin duplicados y por fecha")
    void findsRecommendedEvents() {
        Venue santaMarta = venueRepository.saveAndFlush(venSmr01());
        Venue barranquilla = venueRepository.saveAndFlush(otherVenue());
        Artist solarBeat = artist("Solar Beat");
        Artist caribbeanSound = artist("Caribbean Sound");
        Artist neonWaves = artist("Neon Waves");

        // DATO DE PRUEBA. El texto "BEA" coincide con "Solar Beat" y "Caribbean Sound" (en distinta
        // mayúscula) y no con "Neon Waves". Cada evento excluido incumple una sola condición.
        // Se guardan en desorden a propósito para comprobar el ORDER BY.
        eventRepository.save(event("TST-REC-B", EventStatus.PUBLISHED, LocalDateTime.of(2030, 9, 1, 20, 0),
                santaMarta, solarBeat, caribbeanSound));                                    // cumple; dos artistas coinciden (DISTINCT)
        eventRepository.save(event("TST-REC-A", EventStatus.PUBLISHED, LocalDateTime.of(2030, 7, 1, 20, 0),
                santaMarta, solarBeat));                                                    // cumple; fecha anterior a TST-REC-B
        eventRepository.save(event("TST-X-DRAFT", EventStatus.DRAFT, LocalDateTime.of(2030, 8, 1, 20, 0),
                santaMarta, solarBeat));                                                    // excluido: no PUBLISHED
        eventRepository.save(event("TST-X-EQUAL", EventStatus.PUBLISHED, REFERENCE_DATE,
                santaMarta, solarBeat));                                                    // excluido: fecha igual (> es estricto)
        eventRepository.save(event("TST-X-PAST", EventStatus.PUBLISHED, LocalDateTime.of(2030, 5, 1, 20, 0),
                santaMarta, solarBeat));                                                    // excluido: fecha anterior
        eventRepository.save(event("TST-X-CITY", EventStatus.PUBLISHED, LocalDateTime.of(2030, 8, 15, 20, 0),
                barranquilla, solarBeat));                                                  // excluido: otra ciudad
        eventRepository.save(event("TST-X-ARTIST", EventStatus.PUBLISHED, LocalDateTime.of(2030, 8, 20, 20, 0),
                santaMarta, neonWaves));                                                    // excluido: artista no coincide
        entityManager.flush();
        entityManager.clear();

        List<Event> upperCaseText = eventRepository.findRecommended(REFERENCE_DATE, "Santa Marta", "BEA");
        List<Event> lowerCaseText = eventRepository.findRecommended(REFERENCE_DATE, "Santa Marta", "bea");

        assertThat(upperCaseText)
                .extracting(Event::getEventCode)
                .containsExactly("TST-REC-A", "TST-REC-B");   // orden ascendente y TST-REC-B una sola vez
        assertThat(lowerCaseText)
                .extracting(Event::getEventCode)
                .containsExactly("TST-REC-A", "TST-REC-B");   // mismo resultado: no distingue mayúsculas
    }

    @Test
    @DisplayName("FR-SRC-004: tickets de eventos posteriores a la fecha, ordenados por la fecha del evento")
    void findsTicketsOfFutureEventsInChronologicalOrder() {
        Venue santaMarta = venueRepository.saveAndFlush(venSmr01());
        User buyer = userRepository.saveAndFlush(testUser("comprador", "comprador@pulsepass.test")); // DATO DE PRUEBA

        // DATO DE PRUEBA: eventos con fechas antes, igual y después de la fecha de referencia.
        Event december = eventRepository.save(event("TST-DEC", EventStatus.PUBLISHED, LocalDateTime.of(2030, 12, 1, 20, 0), santaMarta));
        Event past = eventRepository.save(event("TST-PAST", EventStatus.PUBLISHED, LocalDateTime.of(2030, 1, 15, 20, 0), santaMarta));
        Event august = eventRepository.save(event("TST-AUG", EventStatus.PUBLISHED, LocalDateTime.of(2030, 8, 1, 20, 0), santaMarta));
        Event equal = eventRepository.save(event("TST-EQUAL", EventStatus.PUBLISHED, REFERENCE_DATE, santaMarta));
        Event october = eventRepository.save(event("TST-OCT", EventStatus.PUBLISHED, LocalDateTime.of(2030, 10, 1, 20, 0), santaMarta));

        // Los tickets se crean en un orden distinto al de las fechas de sus eventos.
        ticketRepository.save(ticket("TKT-DEC", buyer, december));
        ticketRepository.save(ticket("TKT-PAST", buyer, past));
        ticketRepository.save(ticket("TKT-AUG", buyer, august));
        ticketRepository.save(ticket("TKT-EQUAL", buyer, equal));
        ticketRepository.save(ticket("TKT-OCT", buyer, october));
        entityManager.flush();
        entityManager.clear();

        List<Ticket> tickets = ticketRepository.findByEvent_EventDateAfterOrderByEvent_EventDateAsc(REFERENCE_DATE);

        // Excluye el evento pasado y el de fecha igual (After = > estricto).
        assertThat(tickets)
                .extracting(Ticket::getTicketCode)
                .containsExactly("TKT-AUG", "TKT-OCT", "TKT-DEC");
    }

    // ----------------------------------------------------------------------------------------
    // Apoyo de la prueba (construye con PersistenceFixtures; la prueba decide qué guardar)
    // ----------------------------------------------------------------------------------------

    /** Artista sembrado por V2, leído de la base de datos (no se reinserta). */
    private Artist artist(String stageName) {
        return artistRepository.findByStageName(stageName).orElseThrow();
    }

    /** DATO DE PRUEBA: evento de prueba con los artistas indicados (sin persistir). */
    private static Event event(String code, EventStatus status, LocalDateTime date, Venue venue, Artist... artists) {
        Event event = testEvent(code, status, date, venue);
        for (Artist artist : artists) {
            event.addArtist(artist);
        }
        return event;
    }

    /** DATO DE PRUEBA: ticket PAID con precio fijo para las búsquedas de FR-SRC-004 (sin persistir). */
    private static Ticket ticket(String code, User user, Event event) {
        return testTicket(code, TicketType.GENERAL, new BigDecimal("50000.00"), TicketStatus.PAID, user, event);
    }
}
