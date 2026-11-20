package com.pulsepass.persistence;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import org.hibernate.PropertyValueException;
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
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.pulsepass.persistence.PersistenceFixtures.CMF_2026_EVENT_DATE;
import static com.pulsepass.persistence.PersistenceFixtures.cmf2026;
import static com.pulsepass.persistence.PersistenceFixtures.otherVenue;
import static com.pulsepass.persistence.PersistenceFixtures.testEvent;
import static com.pulsepass.persistence.PersistenceFixtures.venSmr01;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de persistencia de Event (PRD Persistencia §22: FR-EVT-* -> EventRepositoryIT).
 * Cubre FR-EVT-001..006, FR-VEN-004, AC-002, AC-006, QT-003 y QT-007 contra PostgreSQL 17.
 *
 * <p>Las lecturas usan flush() + clear() (P1-12-b). Cada prueba de rechazo provoca una
 * única violación (P1-12-d). Las restricciones de PostgreSQL se verifican por SQLSTATE y
 * nombre real (P1-12-c); el rechazo de Hibernate antes del INSERT se distingue de ellas
 * (P1-12-h).
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class EventRepositoryIT {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private TestEntityManager entityManager;

    /** Solo para leer cómo se almacenan los enums (P1-D7); nunca para escribir. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("AC-002 / FR-EVT-001 / QT-003: CMF-2026 asociado a VEN-SMR-01 se recupera por eventCode junto con su venue")
    void persistsEventAndFindsItWithItsVenue() {
        Venue venue = venueRepository.saveAndFlush(venSmr01());
        eventRepository.saveAndFlush(cmf2026(venue));
        entityManager.clear();

        Event found = eventRepository.findByEventCode("CMF-2026").orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getName()).isEqualTo("Caribbean Music Fest 2026");
        assertThat(found.getCategory()).isEqualTo(EventCategory.MUSIC);
        assertThat(found.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(found.getEventDate()).isEqualTo(CMF_2026_EVENT_DATE);
        assertThat(found.getVenue().getCode()).isEqualTo("VEN-SMR-01");
        assertThat(found.getVenue().getName()).isEqualTo("Marina Convention Center");
    }

    @Test
    @DisplayName("FR-EVT-002: un segundo evento con el mismo eventCode es rechazado por uk_events_event_code")
    void rejectsDuplicateEventCode() {
        Venue venue = venueRepository.saveAndFlush(venSmr01());
        eventRepository.saveAndFlush(cmf2026(venue));
        Event duplicate = cmf2026(venue); // objeto nuevo con el mismo código CMF-2026

        assertThatThrownBy(() -> eventRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertPostgresConstraint(ex, PSQLState.UNIQUE_VIOLATION, "uk_events_event_code"));
    }

    @Test
    @DisplayName("BR-001 / FR-EVT-001: un evento sin venue es rechazado por Hibernate antes del INSERT")
    void rejectsEventWithoutVenue() {
        Event withoutVenue = cmf2026(null);

        // Con @ManyToOne(optional = false), Hibernate detecta el venue null antes de enviar el
        // INSERT (PropertyValueException); PostgreSQL no llega a recibir la sentencia (P1-12-h).
        assertThatThrownBy(() -> eventRepository.saveAndFlush(withoutVenue))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> {
                    PropertyValueException hibernateException = findCause(ex, PropertyValueException.class);
                    assertThat((Throwable) hibernateException)
                            .as("El rechazo debe provenir de Hibernate (PropertyValueException)")
                            .isNotNull();
                    assertThat(hibernateException.getPropertyName()).isEqualTo("venue");
                    assertThat((Throwable) findCause(ex, PSQLException.class))
                            .as("PostgreSQL no debe haber recibido el INSERT")
                            .isNull();
                });
    }

    @Test
    @DisplayName("FR-EVT-003 / FR-EVT-004 / BR-008: categoría y estado se almacenan como texto legible, no como ordinal")
    void storesEnumsAsReadableText() {
        Venue venue = venueRepository.saveAndFlush(venSmr01());
        eventRepository.saveAndFlush(cmf2026(venue));

        // Lectura directa de las columnas (P1-D7): confirma el valor guardado en PostgreSQL.
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT category, status FROM events WHERE event_code = ?", "CMF-2026");

        assertThat(row.get("category")).isEqualTo("MUSIC");
        assertThat(row.get("status")).isEqualTo("PUBLISHED");
        // Valores fuera del catálogo: el tipo enum de Java impide crearlos y ck_events_status /
        // ck_events_category los rechazarían en la base de datos (P1-12-e; sin escrituras SQL).
    }

    @Test
    @DisplayName("FR-EVT-005 / AC-006 / QT-007: los PUBLISHED se devuelven por fecha ascendente, sin DRAFT ni CANCELLED")
    void findsPublishedEventsInChronologicalOrder() {
        Venue venue = venueRepository.saveAndFlush(venSmr01());
        // DATO DE PRUEBA: eventos y fechas elegidos para comprobar orden y filtro por estado.
        // Se guardan desordenados a propósito.
        eventRepository.save(testEvent("TST-PUB-B", EventStatus.PUBLISHED, LocalDateTime.of(2030, 3, 15, 20, 0), venue));
        eventRepository.save(testEvent("TST-DRAFT", EventStatus.DRAFT, LocalDateTime.of(2030, 1, 1, 20, 0), venue));
        eventRepository.save(testEvent("TST-PUB-A", EventStatus.PUBLISHED, LocalDateTime.of(2030, 1, 10, 20, 0), venue));
        eventRepository.save(testEvent("TST-CANCEL", EventStatus.CANCELLED, LocalDateTime.of(2030, 2, 1, 20, 0), venue));
        eventRepository.save(testEvent("TST-PUB-C", EventStatus.PUBLISHED, LocalDateTime.of(2030, 5, 30, 20, 0), venue));
        entityManager.flush();
        entityManager.clear();

        List<Event> published = eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED);

        assertThat(published)
                .extracting(Event::getEventCode)
                .containsExactly("TST-PUB-A", "TST-PUB-B", "TST-PUB-C");
        assertThat(published)
                .extracting(Event::getStatus)
                .containsOnly(EventStatus.PUBLISHED)
                .doesNotContain(EventStatus.DRAFT, EventStatus.CANCELLED);
    }

    @Test
    @DisplayName("FR-VEN-004 / QT-007: los eventos se consultan por el código de su venue, sin mezclar otras sedes")
    void findsEventsByVenueCode() {
        Venue venSmr01 = venueRepository.saveAndFlush(venSmr01());
        Venue other = venueRepository.saveAndFlush(otherVenue());
        eventRepository.save(cmf2026(venSmr01));
        // DATO DE PRUEBA: un segundo evento en VEN-SMR-01 y otro en una sede distinta.
        eventRepository.save(testEvent("TST-SMR-2", EventStatus.DRAFT, LocalDateTime.of(2030, 9, 1, 20, 0), venSmr01));
        eventRepository.save(testEvent("TST-OTHER", EventStatus.PUBLISHED, LocalDateTime.of(2030, 7, 1, 20, 0), other));
        entityManager.flush();
        entityManager.clear();

        List<Event> events = eventRepository.findByVenue_Code("VEN-SMR-01");

        assertThat(events)
                .extracting(Event::getEventCode)
                .containsExactlyInAnyOrder("CMF-2026", "TST-SMR-2");
    }

    @Test
    @DisplayName("FR-EVT-006: un evento sin streamingUrl se persiste con la columna en NULL")
    void persistsEventWithoutStreamingUrl() {
        Venue venue = venueRepository.saveAndFlush(venSmr01());
        Event event = cmf2026(venue);
        assertThat(event.getStreamingUrl()).isNull();

        eventRepository.saveAndFlush(event);
        entityManager.clear();

        assertThat(eventRepository.findByEventCode("CMF-2026").orElseThrow().getStreamingUrl()).isNull();
    }

    @Test
    @DisplayName("FR-EVT-006: un evento híbrido persiste y recupera su streamingUrl")
    void persistsEventWithStreamingUrl() {
        Venue venue = venueRepository.saveAndFlush(venSmr01());
        Event event = cmf2026(venue);
        String url = "https://stream.pulsepass.test/cmf-2026"; // DATO DE PRUEBA
        event.setStreamingUrl(url);

        eventRepository.saveAndFlush(event);
        entityManager.clear();

        assertThat(eventRepository.findByEventCode("CMF-2026").orElseThrow().getStreamingUrl()).isEqualTo(url);
    }

    @Test
    @DisplayName("V1-7: PostgreSQL rechaza una edad mínima negativa (ck_events_minimum_age)")
    void rejectsNegativeMinimumAge() {
        Venue venue = venueRepository.saveAndFlush(venSmr01());
        Event event = cmf2026(venue);
        event.setMinimumAge(-1);

        assertThatThrownBy(() -> eventRepository.saveAndFlush(event))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertPostgresConstraint(ex, PSQLState.CHECK_VIOLATION, "ck_events_minimum_age"));
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
