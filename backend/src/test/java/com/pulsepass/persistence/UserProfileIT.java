package com.pulsepass.persistence;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.repository.UserRepository;
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

import java.time.LocalDate;

import static com.pulsepass.persistence.PersistenceFixtures.testProfile;
import static com.pulsepass.persistence.PersistenceFixtures.testUser;
import static com.pulsepass.persistence.PersistenceFixtures.userWithProfile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de la relación User 1:1 UserProfile (PRD Persistencia §22: FR-USR-* -> UserProfileIT).
 * Cubre FR-USR-001..004, AC-004, QT-004 y QT-007 contra PostgreSQL 17.
 *
 * <p>Respeta P1-D3: el perfil se enlaza con {@link User#setProfile(UserProfile)} y se guarda por
 * cascade PERSIST/MERGE desde User; no existe UserProfileRepository. Las lecturas usan
 * flush() + clear() (P1-12-b); cada prueba de rechazo provoca una única violación (P1-12-d) y
 * verifica la restricción real de PostgreSQL definida en V1 (P1-12-c).
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class UserProfileIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("FR-USR-001 / FR-USR-004 / QT-004: User y su UserProfile se guardan por cascade y se recuperan juntos")
    void persistsUserWithProfileByCascade() {
        Long userId = userRepository.saveAndFlush(userWithProfile("Andrea")).getId();
        entityManager.clear();

        User found = userRepository.findById(userId).orElseThrow();

        assertThat(found.getUsername()).isEqualTo("andrea");
        assertThat(found.getEmail()).isEqualTo("andrea@pulsepass.test");
        assertThat(found.getActive()).isTrue();
        UserProfile profile = found.getProfile();
        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isNotNull();
        assertThat(profile.getFirstName()).isEqualTo("Andrea");
        assertThat(profile.getLastName()).isEqualTo("Prueba");
        assertThat(profile.getBirthDate()).isEqualTo(LocalDate.of(1995, 1, 15));
        // El lado propietario guardó la FK user_id: la relación se navega en ambos sentidos.
        assertThat(profile.getUser().getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("FR-USR-002: un segundo usuario con el mismo username es rechazado por uk_users_username")
    void rejectsDuplicateUsername() {
        userRepository.saveAndFlush(testUser("usuario.prueba", "primero@pulsepass.test"));   // DATO DE PRUEBA
        User duplicate = testUser("usuario.prueba", "segundo@pulsepass.test");               // mismo username

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertPostgresConstraint(ex, PSQLState.UNIQUE_VIOLATION, "uk_users_username"));
    }

    @Test
    @DisplayName("FR-USR-002: un segundo usuario con el mismo email es rechazado por uk_users_email")
    void rejectsDuplicateEmail() {
        userRepository.saveAndFlush(testUser("primero", "compartido@pulsepass.test"));   // DATO DE PRUEBA
        User duplicate = testUser("segundo", "compartido@pulsepass.test");               // mismo email

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertPostgresConstraint(ex, PSQLState.UNIQUE_VIOLATION, "uk_users_email"));
    }

    @Test
    @DisplayName("AC-004 / FR-USR-003: la base de datos rechaza un segundo perfil para el mismo usuario (uk_user_profiles_user_id)")
    void rejectsSecondProfileForSameUser() {
        User user = userRepository.saveAndFlush(userWithProfile("Andrea"));

        // P1-12-g: se usa la API pública aprobada. setProfile enlaza el nuevo perfil con el mismo
        // usuario; el perfil anterior sigue en la base de datos. Al guardar, el cascade inserta el
        // segundo perfil con el mismo user_id y PostgreSQL lo rechaza: es la base de datos, no el
        // código Java, quien impide la violación de la relación 1:1.
        user.setProfile(testProfile("Andrea")); // DATO DE PRUEBA

        assertThatThrownBy(() -> userRepository.saveAndFlush(user))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertPostgresConstraint(ex, PSQLState.UNIQUE_VIOLATION, "uk_user_profiles_user_id"));
    }

    @Test
    @DisplayName("§14 / QT-007: la búsqueda por email no distingue mayúsculas y minúsculas")
    void findsUserByEmailIgnoringCase() {
        Long userId = userRepository.saveAndFlush(userWithProfile("Carlos")).getId();
        entityManager.clear();

        User found = userRepository.findByEmailIgnoreCase("CARLOS@PulsePass.TEST").orElseThrow();

        assertThat(found.getId()).isEqualTo(userId);
        assertThat(found.getEmail()).isEqualTo("carlos@pulsepass.test");
    }

    @Test
    @DisplayName("V1-3: un perfil sin teléfono ni ciudad se persiste con phone y city en NULL")
    void persistsProfileWithoutPhoneAndCity() {
        User user = userWithProfile("Laura");
        assertThat(user.getProfile().getPhone()).isNull();
        assertThat(user.getProfile().getCity()).isNull();

        Long userId = userRepository.saveAndFlush(user).getId();
        entityManager.clear();

        UserProfile profile = userRepository.findById(userId).orElseThrow().getProfile();
        assertThat(profile.getPhone()).isNull();
        assertThat(profile.getCity()).isNull();
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
