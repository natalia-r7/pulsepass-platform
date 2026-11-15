package com.pulsepass.persistence;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.domain.Artist;
import com.pulsepass.repository.ArtistRepository;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración del esquema versionado (PRD Persistencia §17, §22).
 * Cubre QT-001, QT-002, NFR-002..NFR-005 y el contenido de V2 (§15, A8).
 * Se ejecuta contra PostgreSQL 17 real mediante Testcontainers (TestcontainersConfiguration).
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class FlywayMigrationIT {

    @Autowired
    private Flyway flyway;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ArtistRepository artistRepository;

    @Test
    @DisplayName("QT-001: Flyway aplica V1, V2 y V3 desde una base vacía")
    void flywayAppliesV1V2V3() {
        MigrationInfo[] applied = flyway.info().applied();

        List<String> versions = Arrays.stream(applied)
                .map(info -> info.getVersion().getVersion())
                .toList();

        assertThat(versions).containsExactly("1", "2", "3");
        assertThat(applied).allSatisfy(info ->
                assertThat(info.getState()).isEqualTo(MigrationState.SUCCESS));
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("3");
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    @DisplayName("QT-002: Hibernate valida el esquema sin crearlo ni actualizarlo")
    void hibernateOnlyValidatesSchema() {
        // Configuración efectiva de Hibernate (spring.jpa.hibernate.ddl-auto=validate en application.yml).
        Object ddlAuto = entityManagerFactory.getProperties().get("hibernate.hbm2ddl.auto");

        assertThat(ddlAuto).isEqualTo("validate");
    }

    @Test
    @DisplayName("NFR-004/NFR-005: las pruebas usan PostgreSQL 17 mediante Testcontainers, no H2")
    void testsRunOnPostgreSql17() throws SQLException {
        // Metadatos JDBC de la conexión: no se ejecuta SQL (P1-11-d).
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            assertThat(metaData.getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(metaData.getDatabaseMajorVersion()).isEqualTo(17);
        }
    }

    @Test
    @DisplayName("V2 (§15, A8): catálogo inicial de cinco artistas activos, sin país ni género")
    void v2InsertsInitialArtists() {
        List<Artist> artists = artistRepository.findAll();

        assertThat(artists)
                .extracting(Artist::getStageName)
                .containsExactlyInAnyOrder(
                        "Solar Beat", "Neon Waves", "Caribbean Sound", "Ocean Drive", "Digital Pulse");
        // active = true es una inferencia de los escenarios de PRD posteriores (A8).
        assertThat(artists).allSatisfy(artist -> {
            assertThat(artist.getActive()).isTrue();
            assertThat(artist.getCountry()).isNull();
            assertThat(artist.getGenre()).isNull();
        });
    }
}
