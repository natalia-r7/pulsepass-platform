package com.pulsepass.persistence;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.Venue;
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
import java.util.Optional;

import static com.pulsepass.service.ServiceFixtures.andrea;
import static com.pulsepass.service.ServiceFixtures.carlos;
import static com.pulsepass.service.ServiceFixtures.cmf2026;
import static com.pulsepass.service.ServiceFixtures.venSmr01;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de los 9 métodos de repositorio que añade la Fase 2 (PRD Servicios §32, P2-D6,
 * P2-D7, P2-D8) contra PostgreSQL 17 (Testcontainers).
 *
 * <p>Es una clase nueva: no modifica las pruebas de la Fase 1 (P2-D8). Los datos salen de
 * {@code ServiceFixtures} (PRD Servicios §47, P2-D12); los datos adicionales se crean aquí y
 * van marcados como {@code DATO DE PRUEBA} (R-B1). Los artistas de V2 ya están en la base de
 * datos: se consultan y nunca se vuelven a insertar.
 *
 * <p>Como en la Fase 1 (P1-12-b), después de guardar se llama a {@code clear()}, para que las
 * consultas lean de PostgreSQL y no de la caché de primer nivel de JPA. {@code @DataJpaTest}
 * revierte cada prueba al terminar.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class ServiceRepositoryIT {

    /** DATO DE PRUEBA: no es un precio de negocio (P2-D2). */
    private static final BigDecimal TEST_PRICE = new BigDecimal("100.00");

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TestEntityManager entityManager;

    // ---------------------------------------------------------------- VenueRepository

    @Test
    @DisplayName("§32 / BR-VENUE-002: findByActiveTrueOrderByNameAsc devuelve solo los venues activos, ordenados por nombre")
    void findsOnlyActiveVenuesOrderedByName() {
        // ARRANGE
        venueRepository.saveAndFlush(venSmr01());
        venueRepository.saveAndFlush(
                new Venue("VEN-TST-AUD", "Auditorio Central", "Santa Marta", 100, true));  // DATO DE PRUEBA
        venueRepository.saveAndFlush(
                new Venue("VEN-TST-INA", "Zona Inactiva", "Santa Marta", 100, false));     // DATO DE PRUEBA
        entityManager.clear();

        // ACT
        List<Venue> venues = venueRepository.findByActiveTrueOrderByNameAsc();

        // ASSERT
        assertThat(venues)
                .extracting(Venue::getName)
                .containsExactly("Auditorio Central", "Marina Convention Center");
    }

    // ---------------------------------------------------------------- EventRepository

    @Test
    @DisplayName("§32 / BR-EVENT-001: existsByEventCode devuelve true si el evento existe")
    void existsByEventCodeReturnsTrueForExistingEvent() {
        // ARRANGE
        saveCmf2026();

        // ACT + ASSERT
        assertThat(eventRepository.existsByEventCode("CMF-2026")).isTrue();
    }

    @Test
    @DisplayName("§32 / BR-EVENT-001: existsByEventCode devuelve false si el evento no existe")
    void existsByEventCodeReturnsFalseForUnknownEvent() {
        // ARRANGE
        saveCmf2026();

        // ACT + ASSERT
        assertThat(eventRepository.existsByEventCode("XXX-0000")).isFalse(); // DATO DE PRUEBA
    }

    // ---------------------------------------------------------------- ArtistRepository

    @Test
    @DisplayName("§32 / P2-D6: findByStageNameIgnoreCase encuentra un artista de V2 escrito en mayúsculas")
    void findsArtistByStageNameIgnoringCase() {
        // ACT
        Optional<Artist> found = artistRepository.findByStageNameIgnoreCase("SOLAR BEAT");

        // ASSERT
        assertThat(found).isPresent();
        assertThat(found.get().getStageName()).isEqualTo("Solar Beat");
    }

    @Test
    @DisplayName("§32 / P2-D6: findByStageNameIgnoreCase devuelve vacío si el artista no existe")
    void findByStageNameIgnoreCaseReturnsEmptyForUnknownArtist() {
        // ACT + ASSERT
        assertThat(artistRepository.findByStageNameIgnoreCase("Artista Inexistente")).isEmpty(); // DATO DE PRUEBA
    }

    @Test
    @DisplayName("§32 / P2-D6: findByActiveTrueOrderByStageNameAsc devuelve los 5 artistas activos de V2 en orden y excluye los inactivos")
    void findsOnlyActiveArtistsOrderedByStageName() {
        // ARRANGE
        artistRepository.saveAndFlush(new Artist("Artista Inactivo", false)); // DATO DE PRUEBA
        entityManager.clear();

        // ACT
        List<Artist> artists = artistRepository.findByActiveTrueOrderByStageNameAsc();

        // ASSERT
        assertThat(artists)
                .extracting(Artist::getStageName)
                .containsExactly("Caribbean Sound", "Digital Pulse", "Neon Waves", "Ocean Drive", "Solar Beat");
    }

    // ---------------------------------------------------------------- UserRepository

    @Test
    @DisplayName("§32 / P2-D6: findByUsername encuentra al usuario por su username")
    void findsUserByUsername() {
        // ARRANGE
        userRepository.saveAndFlush(andrea());
        entityManager.clear();

        // ACT
        Optional<User> found = userRepository.findByUsername("andrea");

        // ASSERT
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("andrea@email.com");
        assertThat(found.get().getProfile().getFirstName()).isEqualTo("Andrea");
    }

    @Test
    @DisplayName("§32 / P2-D6: findByUsername devuelve vacío si el username no existe")
    void findByUsernameReturnsEmptyForUnknownUsername() {
        // ARRANGE
        userRepository.saveAndFlush(andrea());
        entityManager.clear();

        // ACT + ASSERT
        assertThat(userRepository.findByUsername("inexistente")).isEmpty(); // DATO DE PRUEBA
    }

    @Test
    @DisplayName("§32 / BR-USER-002: existsByEmailIgnoreCase devuelve true con el email en mayúsculas")
    void existsByEmailIgnoreCaseReturnsTrueIgnoringCase() {
        // ARRANGE
        userRepository.saveAndFlush(andrea());
        entityManager.clear();

        // ACT + ASSERT
        assertThat(userRepository.existsByEmailIgnoreCase("ANDREA@EMAIL.COM")).isTrue();
    }

    @Test
    @DisplayName("§32 / BR-USER-002: existsByEmailIgnoreCase devuelve false si el email no existe")
    void existsByEmailIgnoreCaseReturnsFalseForUnknownEmail() {
        // ARRANGE
        userRepository.saveAndFlush(andrea());
        entityManager.clear();

        // ACT + ASSERT
        assertThat(userRepository.existsByEmailIgnoreCase("nadie@email.com")).isFalse(); // DATO DE PRUEBA
    }

    @Test
    @DisplayName("§32 / BR-USER-001: existsByUsername devuelve true si el username existe")
    void existsByUsernameReturnsTrueForExistingUsername() {
        // ARRANGE
        userRepository.saveAndFlush(andrea());
        entityManager.clear();

        // ACT + ASSERT
        assertThat(userRepository.existsByUsername("andrea")).isTrue();
    }

    @Test
    @DisplayName("§32 / BR-USER-001: existsByUsername devuelve false si el username no existe")
    void existsByUsernameReturnsFalseForUnknownUsername() {
        // ARRANGE
        userRepository.saveAndFlush(andrea());
        entityManager.clear();

        // ACT + ASSERT
        assertThat(userRepository.existsByUsername("inexistente")).isFalse(); // DATO DE PRUEBA
    }

    // ---------------------------------------------------------------- TicketRepository

    @Test
    @DisplayName("§32 / P2-D6: findByTicketCode encuentra el ticket por su código")
    void findsTicketByCode() {
        // ARRANGE
        Event event = saveCmf2026();
        User user = userRepository.saveAndFlush(andrea());
        saveTicket("TKT-0A1B2C", TicketStatus.PAID,                    // DATO DE PRUEBA: código
                LocalDateTime.of(2026, 11, 20, 10, 30), user, event);  // DATO DE PRUEBA
        entityManager.clear();

        // ACT
        Optional<Ticket> found = ticketRepository.findByTicketCode("TKT-0A1B2C");

        // ASSERT
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(found.get().getUser().getEmail()).isEqualTo("andrea@email.com");
    }

    @Test
    @DisplayName("§32 / P2-D6: findByTicketCode devuelve vacío si el código no existe")
    void findByTicketCodeReturnsEmptyForUnknownCode() {
        // ACT + ASSERT
        assertThat(ticketRepository.findByTicketCode("TKT-FFFFFF")).isEmpty(); // DATO DE PRUEBA
    }

    @Test
    @DisplayName("§32 / P2-D7: los tickets del usuario salen por fecha de compra descendente, con NULL al final, sin mezclar otros usuarios")
    void findsUserTicketsByPurchaseDateDescWithNullsLast() {
        // ARRANGE
        Event event = saveCmf2026();
        User andrea = userRepository.saveAndFlush(andrea());
        User carlos = userRepository.saveAndFlush(carlos());
        // Se guardan en un orden distinto del esperado, para que el resultado no dependa del
        // orden de inserción. Todos los códigos y fechas son DATO DE PRUEBA.
        saveTicket("TKT-000003", TicketStatus.RESERVED, null, andrea, event);                       // sin compra (P1-D1)
        saveTicket("TKT-000001", TicketStatus.PAID, LocalDateTime.of(2026, 11, 1, 10, 0), andrea, event);  // antiguo
        saveTicket("TKT-000002", TicketStatus.PAID, LocalDateTime.of(2026, 11, 10, 10, 0), andrea, event); // reciente
        saveTicket("TKT-000004", TicketStatus.PAID, LocalDateTime.of(2026, 11, 12, 10, 0), carlos, event); // de otro usuario
        entityManager.clear();

        // ACT
        List<Ticket> tickets = ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc("ANDREA@EMAIL.COM");

        // ASSERT
        assertThat(tickets)
                .extracting(Ticket::getTicketCode)
                .containsExactly("TKT-000002", "TKT-000001", "TKT-000003");
        assertThat(tickets.get(2).getPurchaseDate()).isNull();
    }

    @Test
    @DisplayName("§32 / P2-D7: un usuario sin tickets produce una lista vacía")
    void findsNoTicketsForUserWithoutTickets() {
        // ARRANGE
        userRepository.saveAndFlush(andrea());
        entityManager.clear();

        // ACT + ASSERT
        assertThat(ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc("andrea@email.com")).isEmpty();
    }

    // ---------------------------------------------------------------- Auxiliares

    /** Guarda VEN-SMR-01 y CMF-2026 (PRD §47) y devuelve el evento guardado. */
    private Event saveCmf2026() {
        Venue venue = venueRepository.saveAndFlush(venSmr01());
        return eventRepository.saveAndFlush(cmf2026(venue));
    }

    /** Guarda un ticket GENERAL con el precio de prueba; purchaseDate puede ser null (P1-D1). */
    private void saveTicket(String ticketCode, TicketStatus status, LocalDateTime purchaseDate,
                            User user, Event event) {
        Ticket ticket = new Ticket(ticketCode, TicketType.GENERAL, TEST_PRICE, status, user, event);
        ticket.setPurchaseDate(purchaseDate);
        ticketRepository.saveAndFlush(ticket);
    }
}
