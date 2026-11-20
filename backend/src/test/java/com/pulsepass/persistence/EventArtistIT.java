package com.pulsepass.persistence;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;

import static com.pulsepass.persistence.PersistenceFixtures.CMF_2026_ARTIST_NAMES;
import static com.pulsepass.persistence.PersistenceFixtures.cmf2026;
import static com.pulsepass.persistence.PersistenceFixtures.testArtist;
import static com.pulsepass.persistence.PersistenceFixtures.testEvent;
import static com.pulsepass.persistence.PersistenceFixtures.venSmr01;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de la relación Event N:M Artist (PRD Persistencia §22: FR-ART-* -> EventArtistIT).
 * Cubre FR-ART-001..004, AC-003, QT-005 y QT-008 (JPQL con JOIN) contra PostgreSQL 17.
 *
 * <p>Los artistas de §16.2 (Solar Beat, Neon Waves, Caribbean Sound) los inserta V2: aquí se
 * consultan con {@link ArtistRepository#findByStageName(String)} y nunca se vuelven a insertar.
 * Las lecturas usan flush() + clear() (P1-12-b); cada prueba de rechazo provoca una única
 * violación (P1-12-d) y verifica la restricción real de PostgreSQL (P1-12-c).
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class EventArtistIT {

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("FR-ART-001: un artista nuevo se persiste y se recupera por ID y por nombre artístico")
    void persistsArtistAndFindsIt() {
        Long id = artistRepository.saveAndFlush(testArtist("Artista de Prueba")).getId(); // DATO DE PRUEBA
        entityManager.clear();

        Artist byId = artistRepository.findById(id).orElseThrow();
        Artist byName = artistRepository.findByStageName("Artista de Prueba").orElseThrow();

        assertThat(byId.getStageName()).isEqualTo("Artista de Prueba");
        assertThat(byId.getCountry()).isEqualTo("Colombia");
        assertThat(byId.getGenre()).isEqualTo("Electrónica");
        assertThat(byId.getActive()).isTrue();
        assertThat(byName.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("FR-ART-002: un segundo artista con el mismo stageName es rechazado por uk_artists_stage_name")
    void rejectsDuplicateStageName() {
        artistRepository.saveAndFlush(testArtist("Artista Duplicado")); // DATO DE PRUEBA
        Artist duplicate = testArtist("Artista Duplicado");             // objeto nuevo, mismo nombre

        assertThatThrownBy(() -> artistRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertPostgresConstraint(ex, PSQLState.UNIQUE_VIOLATION, "uk_artists_stage_name"));
    }

    @Test
    @DisplayName("AC-003 / FR-ART-003 / QT-005: CMF-2026 con los tres artistas de V2 los conserva sin pares duplicados")
    void associatesThreeArtistsWithoutDuplicatePairs() {
        Venue venue = venueRepository.saveAndFlush(venSmr01());
        Event event = cmf2026(venue);
        List<Artist> artists = CMF_2026_ARTIST_NAMES.stream()
                .map(name -> artistRepository.findByStageName(name).orElseThrow())
                .toList();
        Artist solarBeat = artists.getFirst();

        artists.forEach(event::addArtist);
        // P1-12-f: se agrega dos veces el mismo artista. Se comprueba el comportamiento del Set
        // mediante JPA, no un INSERT duplicado directo en event_artists (su PK compuesta es el
        // respaldo en la base de datos).
        event.addArtist(solarBeat);
        assertThat(event.getArtists()).hasSize(3);

        eventRepository.saveAndFlush(event);
        entityManager.clear();

        Event reloaded = eventRepository.findByEventCode("CMF-2026").orElseThrow();
        assertThat(reloaded.getArtists())
                .extracting(Artist::getStageName)
                .containsExactlyInAnyOrderElementsOf(CMF_2026_ARTIST_NAMES);

        // Segundo intento con la instancia leída antes del clear(): es otro objeto Java, pero tiene
        // el mismo id, así que equals/hashCode (P1-D4) lo reconocen y el Set no lo agrega.
        reloaded.addArtist(solarBeat);
        entityManager.flush();
        entityManager.clear();

        Event reloadedAgain = eventRepository.findByEventCode("CMF-2026").orElseThrow();
        assertThat(reloadedAgain.getArtists())
                .extracting(Artist::getStageName)
                .containsExactlyInAnyOrderElementsOf(CMF_2026_ARTIST_NAMES);
    }

    @Test
    @DisplayName("FR-ART-004 / QT-008: un artista participa en varios eventos y la consulta JPQL los recupera")
    void findsAllEventsOfAnArtistWithJpql() {
        Venue venue = venueRepository.saveAndFlush(venSmr01());
        Artist solarBeat = artistRepository.findByStageName("Solar Beat").orElseThrow();
        Artist neonWaves = artistRepository.findByStageName("Neon Waves").orElseThrow();

        Event cmf = cmf2026(venue);
        cmf.addArtist(solarBeat);
        cmf.addArtist(neonWaves);
        // DATO DE PRUEBA: un segundo evento con Solar Beat y un tercero sin él.
        Event second = testEvent("TST-SOLAR-2", EventStatus.PUBLISHED, LocalDateTime.of(2030, 8, 1, 20, 0), venue);
        second.addArtist(solarBeat);
        Event withoutSolarBeat = testEvent("TST-NEON-ONLY", EventStatus.PUBLISHED, LocalDateTime.of(2030, 9, 1, 20, 0), venue);
        withoutSolarBeat.addArtist(neonWaves);
        eventRepository.save(cmf);
        eventRepository.save(second);
        eventRepository.save(withoutSolarBeat);
        entityManager.flush();
        entityManager.clear();

        List<Event> events = eventRepository.findByArtistStageName("Solar Beat");

        assertThat(events)
                .extracting(Event::getEventCode)
                .containsExactlyInAnyOrder("CMF-2026", "TST-SOLAR-2");
    }

    /**
     * Recorre la cadena de causas hasta la PSQLException de PostgreSQL y comprueba el código
     * SQLSTATE y el nombre real de la restricción en su ServerErrorMessage (P1-12-c).
     */
    private static void assertPostgresConstraint(Throwable ex, PSQLState expectedState, String expectedConstraint) {
        PSQLException psqlException = findCause(ex, PSQLException.class);
        // Cast a Throwable: SQLException también implementa Iterable, y AssertJ no sabría
        // qué variante de assertThat usar.
        assertThat((Throwable) psqlException)
                .as("La excepción debe provenir de PostgreSQL (PSQLException)")
                .isNotNull();

        ServerErrorMessage serverError = psqlException.getServerErrorMessage();
        assertThat(serverError).isNotNull();
        assertThat(serverError.getSQLState()).isEqualTo(expectedState.getState());
        assertThat(serverError.getConstraint()).isEqualTo(expectedConstraint);
    }

    private static <T extends Throwable> T findCause(Throwable ex, Class<T> type) {
        Throwable current = ex;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
