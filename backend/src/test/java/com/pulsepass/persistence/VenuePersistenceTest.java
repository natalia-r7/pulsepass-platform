package com.pulsepass.persistence;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.domain.Venue;
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

import java.util.Optional;

import static com.pulsepass.persistence.PersistenceFixtures.venSmr01;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de persistencia de Venue (PRD Persistencia §22: FR-VEN-* -> VenuePersistenceTest).
 * Cubre FR-VEN-001..003, AC-001, UC-01 y QT-009 contra PostgreSQL 17 (Testcontainers).
 *
 * <p>Las comprobaciones de lectura usan flush() + clear() (P1-12-b) para que los datos se
 * lean de PostgreSQL y no de la caché de primer nivel de JPA. Cada prueba de rechazo provoca
 * una única violación, al final (P1-12-d), y verifica el nombre real de la restricción en el
 * error de PostgreSQL (P1-12-c).
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class VenuePersistenceTest {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("AC-001 / FR-VEN-001 / UC-01: VEN-SMR-01 se persiste y se recupera por código con capacidad mayor que cero")
    void persistsVenueAndFindsItByCode() {
        venueRepository.saveAndFlush(venSmr01());
        entityManager.clear();

        Optional<Venue> found = venueRepository.findByCode("VEN-SMR-01");

        assertThat(found).isPresent();
        Venue venue = found.get();
        assertThat(venue.getId()).isNotNull();
        assertThat(venue.getName()).isEqualTo("Marina Convention Center");
        assertThat(venue.getCity()).isEqualTo("Santa Marta");
        assertThat(venue.getCapacity()).isEqualTo(5000).isGreaterThan(0);
        assertThat(venue.getActive()).isTrue();
    }

    @Test
    @DisplayName("FR-VEN-001: un venue persistido se recupera por su ID")
    void findsPersistedVenueById() {
        Long id = venueRepository.saveAndFlush(venSmr01()).getId();
        entityManager.clear();

        Optional<Venue> found = venueRepository.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("VEN-SMR-01");
    }

    @Test
    @DisplayName("FR-VEN-002 / QT-009: un segundo venue con el mismo código es rechazado por uk_venues_code")
    void rejectsDuplicateCode() {
        venueRepository.saveAndFlush(venSmr01());
        Venue duplicate = venSmr01(); // objeto nuevo con el mismo código VEN-SMR-01

        assertThatThrownBy(() -> venueRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertPostgresConstraint(ex, PSQLState.UNIQUE_VIOLATION, "uk_venues_code"));
    }

    @Test
    @DisplayName("FR-VEN-003: PostgreSQL rechaza una capacidad igual a cero (ck_venues_capacity)")
    void rejectsZeroCapacity() {
        Venue venue = venSmr01();
        venue.setCapacity(0);

        assertThatThrownBy(() -> venueRepository.saveAndFlush(venue))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertPostgresConstraint(ex, PSQLState.CHECK_VIOLATION, "ck_venues_capacity"));
    }

    @Test
    @DisplayName("FR-VEN-003: PostgreSQL rechaza una capacidad negativa (ck_venues_capacity)")
    void rejectsNegativeCapacity() {
        Venue venue = venSmr01();
        venue.setCapacity(-1);

        assertThatThrownBy(() -> venueRepository.saveAndFlush(venue))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertPostgresConstraint(ex, PSQLState.CHECK_VIOLATION, "ck_venues_capacity"));
    }

    @Test
    @DisplayName("V1-3: un venue sin dirección se persiste con address NULL")
    void persistsVenueWithoutAddress() {
        Venue venue = venSmr01();
        assertThat(venue.getAddress()).isNull();

        venueRepository.saveAndFlush(venue);
        entityManager.clear();

        Venue found = venueRepository.findByCode("VEN-SMR-01").orElseThrow();
        assertThat(found.getAddress()).isNull();
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
