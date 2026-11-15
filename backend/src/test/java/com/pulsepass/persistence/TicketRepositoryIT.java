package com.pulsepass.persistence;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.pulsepass.persistence.PersistenceFixtures.REFERENCE_PURCHASE_DATE;
import static com.pulsepass.persistence.PersistenceFixtures.andreaTicket;
import static com.pulsepass.persistence.PersistenceFixtures.carlosTicket;
import static com.pulsepass.persistence.PersistenceFixtures.cmf2026;
import static com.pulsepass.persistence.PersistenceFixtures.lauraTicket;
import static com.pulsepass.persistence.PersistenceFixtures.miguelTicket;
import static com.pulsepass.persistence.PersistenceFixtures.testEvent;
import static com.pulsepass.persistence.PersistenceFixtures.testTicket;
import static com.pulsepass.persistence.PersistenceFixtures.userWithProfile;
import static com.pulsepass.persistence.PersistenceFixtures.venSmr01;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de persistencia de Ticket (PRD Persistencia §22: FR-TKT-* -> TicketRepositoryIT).
 * Cubre FR-TKT-001..008, AC-005, AC-008, QT-006, QT-007, QT-008 y QT-009 contra PostgreSQL 17.
 *
 * <p>Usa los tickets de referencia de §16.3 (Andrea, Carlos, Laura y Miguel) sobre CMF-2026.
 * Las lecturas usan flush() + clear() (P1-12-b); cada prueba de rechazo provoca una única
 * violación (P1-12-d). Las restricciones de PostgreSQL se verifican por SQLSTATE y nombre real
 * (P1-12-c); los rechazos de Hibernate antes del INSERT se distinguen de ellas (P1-12-h).
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class TicketRepositoryIT {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private TestEntityManager entityManager;

    /** Solo para leer cómo se almacenan los enums (P1-D7); nunca para escribir ni para consultas de negocio. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("FR-TKT-001 / QT-006: un ticket se persiste y se recupera con su User y su Event")
    void persistsTicketWithUserAndEvent() {
        Event event = saveCmf2026();
        User andrea = saveUser("Andrea");
        Long ticketId = ticketRepository.saveAndFlush(andreaTicket(andrea, event)).getId();
        entityManager.clear();

        Ticket found = ticketRepository.findById(ticketId).orElseThrow();

        assertThat(found.getTicketCode()).isEqualTo("TCK-0001");
        assertThat(found.getType()).isEqualTo(TicketType.VIP);
        // isEqualByComparingTo: compara el valor numérico sin importar la escala (250000 = 250000.00).
        assertThat(found.getPrice()).isEqualByComparingTo("250000");
        assertThat(found.getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(found.getPurchaseDate()).isEqualTo(REFERENCE_PURCHASE_DATE);
        assertThat(found.getUser().getEmail()).isEqualTo("andrea@pulsepass.test");
        assertThat(found.getEvent().getEventCode()).isEqualTo("CMF-2026");
    }

    @Test
    @DisplayName("FR-TKT-001 / BR-005: un ticket sin usuario es rechazado por Hibernate antes del INSERT")
    void rejectsTicketWithoutUser() {
        Event event = saveCmf2026();
        Ticket withoutUser = andreaTicket(null, event);

        assertRejectedByHibernateBeforeInsert(() -> ticketRepository.saveAndFlush(withoutUser), "user");
    }

    @Test
    @DisplayName("FR-TKT-001 / BR-005: un ticket sin evento es rechazado por Hibernate antes del INSERT")
    void rejectsTicketWithoutEvent() {
        User andrea = saveUser("Andrea");
        Ticket withoutEvent = andreaTicket(andrea, null);

        assertRejectedByHibernateBeforeInsert(() -> ticketRepository.saveAndFlush(withoutEvent), "event");
    }

    @Test
    @DisplayName("AC-005 / FR-TKT-002 / QT-009: un segundo ticket TCK-0001 es rechazado por uk_tickets_ticket_code")
    void rejectsDuplicateTicketCode() {
        Event event = saveCmf2026();
        User andrea = saveUser("Andrea");
        ticketRepository.saveAndFlush(andreaTicket(andrea, event));
        Ticket duplicate = andreaTicket(andrea, event); // objeto nuevo con el mismo código TCK-0001

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertPostgresConstraint(ex, PSQLState.UNIQUE_VIOLATION, "uk_tickets_ticket_code"));
    }

    @Test
    @DisplayName("FR-TKT-003: PostgreSQL rechaza un precio negativo (ck_tickets_price)")
    void rejectsNegativePrice() {
        Event event = saveCmf2026();
        User andrea = saveUser("Andrea");
        Ticket ticket = testTicket("TST-NEG", TicketType.GENERAL, new BigDecimal("-1.00"), // DATO DE PRUEBA
                TicketStatus.PAID, andrea, event);

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(ticket))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertPostgresConstraint(ex, PSQLState.CHECK_VIOLATION, "ck_tickets_price"));
    }

    @Test
    @DisplayName("FR-TKT-003: un precio igual a cero cumple CHECK price >= 0 y se persiste")
    void acceptsZeroPrice() {
        Event event = saveCmf2026();
        User andrea = saveUser("Andrea");
        Long ticketId = ticketRepository.saveAndFlush(testTicket("TST-ZERO", TicketType.STUDENT, // DATO DE PRUEBA
                BigDecimal.ZERO, TicketStatus.RESERVED, andrea, event)).getId();
        entityManager.clear();

        assertThat(ticketRepository.findById(ticketId).orElseThrow().getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("FR-TKT-004 / FR-TKT-005 / BR-008: tipo y estado del ticket se almacenan como texto, no como ordinal")
    void storesEnumsAsReadableText() {
        Event event = saveCmf2026();
        User andrea = saveUser("Andrea");
        ticketRepository.saveAndFlush(andreaTicket(andrea, event));

        // Lectura directa de las columnas (P1-D7): confirma el valor guardado en PostgreSQL.
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT type, status FROM tickets WHERE ticket_code = ?", "TCK-0001");

        assertThat(row.get("type")).isEqualTo("VIP");
        assertThat(row.get("status")).isEqualTo("PAID");
        // Valores fuera del catálogo: el tipo enum de Java impide crearlos y ck_tickets_type /
        // ck_tickets_status los rechazarían en la base de datos (P1-12-e; sin escrituras SQL).
    }

    @Test
    @DisplayName("FR-TKT-006 / QT-007: los tickets de un usuario se buscan por email ignorando mayúsculas")
    void findsTicketsByUserEmailIgnoringCase() {
        Event event = saveCmf2026();
        User andrea = saveUser("Andrea");
        User carlos = saveUser("Carlos");
        ticketRepository.save(andreaTicket(andrea, event));
        // DATO DE PRUEBA: un segundo ticket de Andrea, en otro estado.
        ticketRepository.save(testTicket("TST-AND-2", TicketType.GENERAL, new BigDecimal("120000.00"),
                TicketStatus.RESERVED, andrea, event));
        ticketRepository.save(carlosTicket(carlos, event));
        entityManager.flush();
        entityManager.clear();

        List<Ticket> tickets = ticketRepository.findByUser_EmailIgnoreCase("ANDREA@PulsePass.TEST");

        assertThat(tickets)
                .extracting(Ticket::getTicketCode)
                .containsExactlyInAnyOrder("TCK-0001", "TST-AND-2");
    }

    @Test
    @DisplayName("FR-TKT-006 / QT-007: los tickets de un usuario se filtran además por estado")
    void findsTicketsByUserEmailAndStatus() {
        Event event = saveCmf2026();
        User andrea = saveUser("Andrea");
        ticketRepository.save(andreaTicket(andrea, event));
        // DATO DE PRUEBA: un segundo ticket de Andrea, RESERVED, que el filtro PAID debe excluir.
        ticketRepository.save(testTicket("TST-AND-2", TicketType.GENERAL, new BigDecimal("120000.00"),
                TicketStatus.RESERVED, andrea, event));
        entityManager.flush();
        entityManager.clear();

        List<Ticket> paid = ticketRepository.findByUser_EmailIgnoreCaseAndStatus("Andrea@PulsePass.test", TicketStatus.PAID);

        assertThat(paid)
                .extracting(Ticket::getTicketCode)
                .containsExactly("TCK-0001");
    }

    @Test
    @DisplayName("FR-TKT-007 / QT-007: solo se recuperan los tickets PAID del evento solicitado")
    void findsPaidTicketsByEventCode() {
        Event cmf = saveReferenceScenario();
        saveOtherEventWithPaidTicket(cmf.getVenue());
        entityManager.flush();
        entityManager.clear();

        List<Ticket> paid = ticketRepository.findByEvent_EventCodeAndStatus("CMF-2026", TicketStatus.PAID);

        assertThat(paid)
                .extracting(Ticket::getTicketCode)
                .containsExactlyInAnyOrder("TCK-0001", "TCK-0002");
    }

    @Test
    @DisplayName("AC-008 / FR-TKT-008 / QT-008: el conteo de ventas solo incluye los tickets PAID, no RESERVED ni CANCELLED")
    void countsOnlyPaidTicketsOfTheEvent() {
        Event cmf = saveReferenceScenario();
        saveOtherEventWithPaidTicket(cmf.getVenue());
        entityManager.flush();
        entityManager.clear();

        long paidCount = ticketRepository.countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID);

        // §16.3: Andrea y Carlos PAID (cuentan); Laura RESERVED y Miguel CANCELLED (no cuentan).
        // El ticket PAID de otro evento tampoco cuenta.
        assertThat(paidCount).isEqualTo(2L);
    }

    @Test
    @DisplayName("P1-D1: un ticket RESERVED se persiste sin fecha de compra (purchase_date NULL)")
    void persistsReservedTicketWithoutPurchaseDate() {
        Event event = saveCmf2026();
        User laura = saveUser("Laura");
        Ticket ticket = lauraTicket(laura, event);
        assertThat(ticket.getPurchaseDate()).isNull();

        Long ticketId = ticketRepository.saveAndFlush(ticket).getId();
        entityManager.clear();

        Ticket found = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(TicketStatus.RESERVED);
        assertThat(found.getPurchaseDate()).isNull();
    }

    // ----------------------------------------------------------------------------------------
    // Preparación de datos (persisten los fixtures; PersistenceFixtures solo los construye)
    // ----------------------------------------------------------------------------------------

    private Event saveCmf2026() {
        Venue venue = venueRepository.saveAndFlush(venSmr01());
        return eventRepository.saveAndFlush(cmf2026(venue));
    }

    private User saveUser(String firstName) {
        return userRepository.saveAndFlush(userWithProfile(firstName));
    }

    /** CMF-2026 con los cuatro tickets de §16.3. */
    private Event saveReferenceScenario() {
        Event event = saveCmf2026();
        ticketRepository.save(andreaTicket(saveUser("Andrea"), event));
        ticketRepository.save(carlosTicket(saveUser("Carlos"), event));
        ticketRepository.save(lauraTicket(saveUser("Laura"), event));
        ticketRepository.save(miguelTicket(saveUser("Miguel"), event));
        return event;
    }

    /** DATO DE PRUEBA: otro evento con un ticket PAID, para comprobar que no se mezcla con CMF-2026. */
    private Event saveOtherEventWithPaidTicket(Venue venue) {
        Event other = eventRepository.saveAndFlush(
                testEvent("TST-OTHER", EventStatus.PUBLISHED, LocalDateTime.of(2030, 7, 1, 20, 0), venue));
        User buyer = userRepository.saveAndFlush(PersistenceFixtures.testUser("comprador", "comprador@pulsepass.test"));
        Ticket ticket = testTicket("TST-OTH-1", TicketType.GENERAL, new BigDecimal("50000.00"),
                TicketStatus.PAID, buyer, other);
        ticket.setPurchaseDate(REFERENCE_PURCHASE_DATE);
        ticketRepository.save(ticket);
        return other;
    }

    // ----------------------------------------------------------------------------------------
    // Verificación de rechazos
    // ----------------------------------------------------------------------------------------

    /**
     * Con @ManyToOne(optional = false), Hibernate detecta la relación null antes de enviar el
     * INSERT (PropertyValueException); PostgreSQL no llega a recibir la sentencia (P1-12-h).
     */
    private static void assertRejectedByHibernateBeforeInsert(Runnable action, String propertyName) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> {
                    PropertyValueException hibernateException = findCause(ex, PropertyValueException.class);
                    assertThat((Throwable) hibernateException)
                            .as("El rechazo debe provenir de Hibernate (PropertyValueException)")
                            .isNotNull();
                    assertThat(hibernateException.getPropertyName()).isEqualTo(propertyName);
                    assertThat((Throwable) findCause(ex, PSQLException.class))
                            .as("PostgreSQL no debe haber recibido el INSERT")
                            .isNull();
                });
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
