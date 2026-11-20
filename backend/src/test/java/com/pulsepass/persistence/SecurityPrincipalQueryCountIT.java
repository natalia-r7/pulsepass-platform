package com.pulsepass.persistence;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.security.CustomUserDetailsService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.function.Supplier;

import static com.pulsepass.persistence.PersistenceFixtures.userWithProfile;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coste en consultas SQL de cargar la identidad (PRD Seguridad NFR-SEC-003: «evitar consultas N+1
 * para principal y roles; medir carga de autenticación»; P4-05-j).
 * <p>
 * Contra PostgreSQL 17 real, con las estadísticas de Hibernate activadas solo en esta prueba, mide
 * las sentencias de {@link CustomUserDetailsService} y comprueba que su número es fijo: no crece con
 * el número de roles del usuario (no hay N+1). No añade consultas ni métodos de repositorio.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import({TestcontainersConfiguration.class, CustomUserDetailsService.class})
class SecurityPrincipalQueryCountIT {

    /** DATO DE PRUEBA: hash ficticio. */
    private static final String PASSWORD_HASH = "hash-de-prueba-para-consultas-0000000000000000000000000000000";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("NFR-SEC-003 / P4-05-j: cargar el principal de un token usa un número fijo de consultas, sin N+1 por roles")
    void loadingPrincipalUsesConstantNumberOfQueries() {
        Long withOneRole = persistUser("Andrea", Role.USER);
        Long withThreeRoles = persistUser("Carlos", Role.USER, Role.STAFF, Role.ADMIN);

        long oneRole = statementsFor(() -> userDetailsService.loadPrincipalById(withOneRole));
        long threeRoles = statementsFor(() -> userDetailsService.loadPrincipalById(withThreeRoles));

        assertThat(oneRole).isEqualTo(threeRoles).isEqualTo(2);
    }

    @Test
    @DisplayName("NFR-SEC-003: cargar el usuario del login usa un número fijo de consultas, sin N+1 por roles")
    void loadingLoginUserUsesConstantNumberOfQueries() {
        persistUser("Andrea", Role.USER);
        persistUser("Carlos", Role.USER, Role.STAFF, Role.ADMIN);

        long oneRole = statementsFor(() -> userDetailsService.loadUserByUsername("andrea"));
        long threeRoles = statementsFor(() -> userDetailsService.loadUserByUsername("carlos"));

        assertThat(oneRole).isEqualTo(threeRoles).isEqualTo(4);
    }

    // ------------------------------------------------------------------ ayudantes

    /** Usuario activo con perfil (DATO DE PRUEBA), credencial y los roles indicados. */
    private Long persistUser(String firstName, Role... roles) {
        User user = userRepository.saveAndFlush(userWithProfile(firstName));
        userCredentialRepository.saveAndFlush(new UserCredential(user, PASSWORD_HASH));
        for (Role role : roles) {
            userRoleRepository.saveAndFlush(new UserRole(user.getId(), role));
        }
        return user.getId();
    }

    /** Sentencias SQL preparadas durante la llamada, con la caché de primer nivel vacía. */
    private long statementsFor(Supplier<?> call) {
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        call.get();
        return statistics.getPrepareStatementCount();
    }
}
