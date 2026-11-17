package com.pulsepass.persistence;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.pulsepass.persistence.PersistenceFixtures.testUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de la migración V4 y del modelo de credenciales y roles (PRD Seguridad §9.1, §9.2;
 * P4-02). Se ejecuta contra PostgreSQL 17 real mediante Testcontainers.
 *
 * <p>Las restricciones de unicidad (PK) se prueban con los repositorios, para demostrar además
 * que {@code save()} ejecuta {@code persist()} (P4-02-a, P4-02-b). Los NULL, los valores no
 * permitidos y las FK hacia usuarios inexistentes se prueban con SQL directo: con
 * {@code check_nullability: true} Hibernate rechazaría el NULL antes de llegar a PostgreSQL, y
 * el enum {@link Role} no admite valores inválidos. Cada prueba de rechazo provoca una única
 * violación y verifica la restricción real de V4 (P1-12-c, P1-12-d).
 *
 * <p>Las pruebas de duplicados y de migración incremental no usan la transacción de la prueba
 * ({@code NOT_SUPPORTED}): necesitan datos confirmados para comprobar, después del error, que el
 * registro original sigue intacto, y Flyway trabaja con sus propias conexiones. Cada una borra
 * lo que crea en un bloque {@code finally}.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class SecurityMigrationIT {

    /** DATO DE PRUEBA: textos ficticios con el tamaño de un hash BCrypt, no hashes reales. */
    private static final String FIRST_HASH = "hash-de-prueba-original-0000000000000000000000000000000000000";
    private static final String SECOND_HASH = "hash-de-prueba-segundo-11111111111111111111111111111111111111";

    /** Esquema aislado para la migración incremental (P4-02-d); se elimina al terminar. */
    private static final String MIGRATION_SCHEMA = "p4_migration_check";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("§9.2: V4 crea user_credentials y user_roles con las columnas, tipos y nulos previstos")
    void v4CreatesTablesWithExpectedColumns() {
        assertThat(columnsOf("user_credentials")).containsExactly(
                "user_id bigint NOT NULL",
                "password_hash character varying(100) NOT NULL");
        assertThat(columnsOf("user_roles")).containsExactly(
                "user_id bigint NOT NULL",
                "role character varying(20) NOT NULL");
    }

    @Test
    @DisplayName("§9.2 / A10: V4 define sus restricciones con nombre y no crea índices adicionales")
    void v4DefinesNamedConstraintsAndNoExtraIndexes() {
        assertThat(constraintsOf("user_credentials")).containsExactlyInAnyOrder(
                "pk_user_credentials p", "fk_user_credentials_user f");
        assertThat(constraintsOf("user_roles")).containsExactlyInAnyOrder(
                "pk_user_roles p", "ck_user_roles_role c", "fk_user_roles_user f");

        // Solo existen los índices de las PK (A10: user_id ya encabeza ambas PK).
        assertThat(indexesOf("user_credentials")).containsExactly("pk_user_credentials");
        assertThat(indexesOf("user_roles")).containsExactly("pk_user_roles");
    }

    @Test
    @DisplayName("§9.1 / P4-02-a: una credencial nueva se crea con persist() y toma como PK el id del usuario (@MapsId)")
    void newCredentialIsPersistedWithUserIdAsPrimaryKey() {
        User user = userRepository.saveAndFlush(testUser("credencial.nueva", "credencial.nueva@pulsepass.test"));
        UserCredential credential = new UserCredential(user, FIRST_HASH);
        // Antes de guardar, el id es null: Spring Data la considera nueva y llama a persist().
        assertThat(credential.getUserId()).isNull();

        UserCredential saved = userCredentialRepository.saveAndFlush(credential);

        // persist() gestiona la misma instancia; merge() habría devuelto una copia distinta.
        assertThat(saved).isSameAs(credential);
        assertThat(entityManager.getEntityManager().contains(credential)).isTrue();
        assertThat(saved.getUserId()).isEqualTo(user.getId());

        entityManager.clear();
        UserCredential found = userCredentialRepository.findById(user.getId()).orElseThrow();
        assertThat(found.getPasswordHash()).isEqualTo(FIRST_HASH);
        assertThat(countRows("user_credentials", user.getId())).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("§9.1 / P4-02-a: una segunda credencial del mismo usuario viola pk_user_credentials y no sobrescribe el hash")
    void duplicateCredentialIsRejectedWithoutOverwritingTheHash() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Long userId = tx.execute(status -> {
            User user = userRepository.save(testUser("credencial.duplicada", "credencial.duplicada@pulsepass.test"));
            userCredentialRepository.save(new UserCredential(user, FIRST_HASH));
            return user.getId();
        });
        try {
            // Una credencial NUEVA para el mismo usuario: persist() + INSERT, nunca UPDATE.
            assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
                User user = userRepository.findById(userId).orElseThrow();
                userCredentialRepository.saveAndFlush(new UserCredential(user, SECOND_HASH));
            }))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .satisfies(ex -> assertPostgresConstraint(ex, PSQLState.UNIQUE_VIOLATION, "pk_user_credentials"));

            // El registro original sigue intacto: una sola fila y el primer hash.
            assertThat(countRows("user_credentials", userId)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT password_hash FROM user_credentials WHERE user_id = ?", String.class, userId))
                    .isEqualTo(FIRST_HASH);
        } finally {
            deleteUserAndSecurityRows(userId);
        }
    }

    @Test
    @DisplayName("§9.1: una credencial de un usuario inexistente viola fk_user_credentials_user")
    void credentialForMissingUserIsRejected() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO user_credentials (user_id, password_hash) VALUES (?, ?)", -1L, FIRST_HASH))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertPostgresConstraint(ex, PSQLState.FOREIGN_KEY_VIOLATION, "fk_user_credentials_user"));
    }

    @Test
    @DisplayName("§9.2: password_hash no admite NULL en la base de datos")
    void passwordHashCannotBeNull() {
        User user = userRepository.saveAndFlush(testUser("hash.nulo", "hash.nulo@pulsepass.test"));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO user_credentials (user_id, password_hash) VALUES (?, NULL)", user.getId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertPostgresNotNull(ex, "password_hash"));
    }

    @Test
    @DisplayName("§9.1 / P4-02-b / P4-02-c: un rol nuevo se crea con persist() y findRolesByUserId los devuelve ordenados")
    void newRolesArePersistedAndReturnedInOrder() {
        User user = userRepository.saveAndFlush(testUser("roles.varios", "roles.varios@pulsepass.test"));
        UserRole userRole = new UserRole(user.getId(), Role.USER);
        // Persistable: la entidad se declara nueva aunque su clave compuesta no sea null.
        assertThat(userRole.isNew()).isTrue();

        UserRole saved = userRoleRepository.saveAndFlush(userRole);

        // persist() gestiona la misma instancia y @PostPersist la marca como existente.
        assertThat(saved).isSameAs(userRole);
        assertThat(entityManager.getEntityManager().contains(userRole)).isTrue();
        assertThat(saved.isNew()).isFalse();

        userRoleRepository.saveAndFlush(new UserRole(user.getId(), Role.STAFF));
        entityManager.clear();

        assertThat(userRoleRepository.findRolesByUserId(user.getId()))
                .containsExactly(Role.STAFF, Role.USER);
    }

    @Test
    @DisplayName("P4-02-c: findRolesByUserId devuelve una lista vacía si el usuario no tiene roles")
    void userWithoutRolesHasEmptyList() {
        User user = userRepository.saveAndFlush(testUser("sin.roles", "sin.roles@pulsepass.test"));

        assertThat(userRoleRepository.findRolesByUserId(user.getId())).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("§9.1 / P4-02-b: repetir un rol del mismo usuario viola pk_user_roles y no altera el registro existente")
    void duplicateRoleIsRejectedWithoutChangingExistingRow() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Long userId = tx.execute(status -> {
            User user = userRepository.save(testUser("rol.duplicado", "rol.duplicado@pulsepass.test"));
            userRoleRepository.save(new UserRole(user.getId(), Role.USER));
            return user.getId();
        });
        try {
            // Un rol NUEVO con la misma clave: persist() + INSERT; sin Persistable sería merge() y se ignoraría.
            assertThatThrownBy(() -> tx.executeWithoutResult(status ->
                    userRoleRepository.saveAndFlush(new UserRole(userId, Role.USER))))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .satisfies(ex -> assertPostgresConstraint(ex, PSQLState.UNIQUE_VIOLATION, "pk_user_roles"));

            // El registro original sigue intacto: una sola fila con el rol USER.
            assertThat(jdbcTemplate.queryForList(
                    "SELECT role FROM user_roles WHERE user_id = ?", String.class, userId))
                    .containsExactly("USER");
        } finally {
            deleteUserAndSecurityRows(userId);
        }
    }

    @ParameterizedTest(name = "rol «{0}»")
    @ValueSource(strings = {"SUPERADMIN", "user", "ROLE_USER"})
    @DisplayName("§9.1 / A9: un rol distinto de USER, STAFF y ADMIN viola ck_user_roles_role")
    void invalidRoleIsRejected(String invalidRole) {
        User user = userRepository.saveAndFlush(testUser("rol.invalido", "rol.invalido@pulsepass.test"));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role) VALUES (?, ?)", user.getId(), invalidRole))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertPostgresConstraint(ex, PSQLState.CHECK_VIOLATION, "ck_user_roles_role"));
    }

    @Test
    @DisplayName("§9.1: un rol de un usuario inexistente viola fk_user_roles_user")
    void roleForMissingUserIsRejected() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role) VALUES (?, ?)", -1L, "USER"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertPostgresConstraint(ex, PSQLState.FOREIGN_KEY_VIOLATION, "fk_user_roles_user"));
    }

    @Test
    @DisplayName("V1-11 / BR-008: el rol se guarda por su nombre, no por su ordinal")
    void roleIsStoredByName() {
        User user = userRepository.saveAndFlush(testUser("rol.texto", "rol.texto@pulsepass.test"));
        userRoleRepository.saveAndFlush(new UserRole(user.getId(), Role.ADMIN));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT role FROM user_roles WHERE user_id = ?", String.class, user.getId()))
                .isEqualTo("ADMIN");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("§9.2 / P4-D11: al migrar de V3 a V4, cada usuario existente recibe solo ROLE_USER y ninguna credencial")
    void migrationFromV3GivesExistingUsersRoleUserWithoutCredentials() {
        try {
            flywayFor(MIGRATION_SCHEMA).target("3").load().migrate();
            // DATO DE PRUEBA: un usuario activo y otro inactivo, creados antes de V4.
            jdbcTemplate.update("INSERT INTO " + MIGRATION_SCHEMA + ".users (username, email, active) VALUES (?, ?, ?)",
                    "legado.activo", "legado.activo@pulsepass.test", true);
            jdbcTemplate.update("INSERT INTO " + MIGRATION_SCHEMA + ".users (username, email, active) VALUES (?, ?, ?)",
                    "legado.inactivo", "legado.inactivo@pulsepass.test", false);
            List<Long> userIds = jdbcTemplate.queryForList(
                    "SELECT id FROM " + MIGRATION_SCHEMA + ".users ORDER BY id", Long.class);

            Flyway complete = flywayFor(MIGRATION_SCHEMA).load();
            complete.migrate();

            assertAppliedVersions(complete, "1", "2", "3", "4");
            List<Map<String, Object>> roles = jdbcTemplate.queryForList(
                    "SELECT user_id, role FROM " + MIGRATION_SCHEMA + ".user_roles ORDER BY user_id, role");
            assertThat(roles).containsExactly(
                    Map.of("user_id", userIds.get(0), "role", "USER"),
                    Map.of("user_id", userIds.get(1), "role", "USER"));
            assertThat(countAll(MIGRATION_SCHEMA + ".user_credentials")).isZero();
        } finally {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + MIGRATION_SCHEMA + " CASCADE");
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("§9.2: V4 sobre una base nueva no inserta roles ni credenciales")
    void migrationOnEmptyDatabaseInsertsNoRows() {
        try {
            Flyway complete = flywayFor(MIGRATION_SCHEMA).load();
            complete.migrate();

            assertAppliedVersions(complete, "1", "2", "3", "4");
            assertThat(countAll(MIGRATION_SCHEMA + ".user_roles")).isZero();
            assertThat(countAll(MIGRATION_SCHEMA + ".user_credentials")).isZero();
        } finally {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + MIGRATION_SCHEMA + " CASCADE");
        }
    }

    // ------------------------------------------------------------------ ayudantes

    /** Flyway sobre un esquema propio: las migraciones crean allí sus tablas, no en public. */
    private org.flywaydb.core.api.configuration.FluentConfiguration flywayFor(String schema) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .locations("classpath:db/migration");
    }

    /**
     * Migraciones versionadas aplicadas, en orden. Se descarta la entrada sin versión con la que
     * Flyway registra que él mismo creó el esquema aislado («Flyway Schema Creation»).
     */
    private static void assertAppliedVersions(Flyway flyway, String... expected) {
        List<MigrationInfo> applied = Arrays.stream(flyway.info().applied())
                .filter(info -> info.getVersion() != null)
                .toList();
        assertThat(applied).extracting(info -> info.getVersion().getVersion()).containsExactly(expected);
        assertThat(applied).allSatisfy(info -> assertThat(info.getState()).isEqualTo(MigrationState.SUCCESS));
    }

    /** Columnas de una tabla de public como «nombre tipo[(longitud)] NULL|NOT NULL», en orden. */
    private List<String> columnsOf(String table) {
        return jdbcTemplate.query("""
                        SELECT column_name, data_type, character_maximum_length, is_nullable
                        FROM information_schema.columns
                        WHERE table_schema = 'public' AND table_name = ?
                        ORDER BY ordinal_position""",
                (rs, rowNum) -> rs.getString("column_name") + " " + rs.getString("data_type")
                        + (rs.getObject("character_maximum_length") == null
                                ? "" : "(" + rs.getInt("character_maximum_length") + ")")
                        + ("NO".equals(rs.getString("is_nullable")) ? " NOT NULL" : " NULL"),
                table);
    }

    /** Restricciones PK (p), FK (f) y CHECK (c) de una tabla de public, como «nombre tipo». */
    private List<String> constraintsOf(String table) {
        return jdbcTemplate.queryForList("""
                        SELECT conname || ' ' || contype::text
                        FROM pg_constraint
                        WHERE conrelid = ('public.' || ?)::regclass""",
                String.class, table);
    }

    private List<String> indexesOf(String table) {
        return jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = ? ORDER BY indexname",
                String.class, table);
    }

    private int countRows(String table, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE user_id = ?", Integer.class, userId);
        return count == null ? 0 : count;
    }

    private int countAll(String qualifiedTable) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + qualifiedTable, Integer.class);
        return count == null ? 0 : count;
    }

    /** Limpieza de las pruebas sin transacción: primero las filas hijas, después el usuario. */
    private void deleteUserAndSecurityRows(Long userId) {
        if (userId == null) {
            return;
        }
        jdbcTemplate.update("DELETE FROM user_credentials WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    /**
     * Recorre la cadena de causas hasta la PSQLException de PostgreSQL y comprueba el código
     * SQLSTATE y el nombre real de la restricción en su ServerErrorMessage (P1-12-c).
     */
    private static void assertPostgresConstraint(Throwable ex, PSQLState expectedState, String expectedConstraint) {
        ServerErrorMessage serverError = serverErrorOf(ex);
        assertThat(serverError.getSQLState()).isEqualTo(expectedState.getState());
        assertThat(serverError.getConstraint()).isEqualTo(expectedConstraint);
    }

    /** Como {@link #assertPostgresConstraint}, para NOT NULL: PostgreSQL informa la columna. */
    private static void assertPostgresNotNull(Throwable ex, String expectedColumn) {
        ServerErrorMessage serverError = serverErrorOf(ex);
        assertThat(serverError.getSQLState()).isEqualTo(PSQLState.NOT_NULL_VIOLATION.getState());
        assertThat(serverError.getColumn()).isEqualTo(expectedColumn);
    }

    private static ServerErrorMessage serverErrorOf(Throwable ex) {
        PSQLException psqlException = findCause(ex, PSQLException.class);
        // Cast a Throwable: SQLException también implementa Iterable, y AssertJ no sabría
        // qué variante de assertThat usar.
        assertThat((Throwable) psqlException)
                .as("La excepción debe provenir de PostgreSQL (PSQLException)")
                .isNotNull();
        ServerErrorMessage serverError = psqlException.getServerErrorMessage();
        assertThat(serverError).isNotNull();
        return serverError;
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
