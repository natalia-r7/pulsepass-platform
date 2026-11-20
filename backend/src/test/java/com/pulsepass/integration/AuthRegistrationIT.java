package com.pulsepass.integration;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.domain.UserRole;
import com.pulsepass.repository.UserRoleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración del registro con contraseña sobre PostgreSQL real (PRD Seguridad §9.3, §15 UC-SEC-01,
 * §17.3; FR-SEC-AUTH-001, NFR-SEC-001, NFR-SEC-004, NFR-SEC-009; PRD E2E TC-AUTH-001, TC-SEC-016;
 * P4-06-I, K, L y M).
 *
 * <p>Contexto completo con la cadena de seguridad real, Bean Validation, Jackson, la transacción de
 * {@code AuthServiceImpl}, BCrypt y Flyway sobre Testcontainers. Las peticiones van sin cabecera
 * {@code Authorization}: el registro es público (FR-SEC-USR-01).
 *
 * <p>Aquí las transacciones se confirman de verdad (no hay {@code @Transactional} en la prueba): por
 * eso cada prueba anota los ids que crea y {@link #deleteCreatedRows()} los borra con
 * {@code JdbcTemplate}, en orden de claves foráneas, sin tocar otros datos (P4-06-M).
 *
 * <p>{@code UserRoleRepository} es un spy (P4-06-L): delega en el repositorio real salvo en la
 * prueba de atomicidad, donde guarda el rol de verdad, envía a PostgreSQL los INSERT pendientes con
 * {@code flush()} y después lanza una excepción. Spring lo reinicia después de cada prueba.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthRegistrationIT {

    /** Contraseña ficticia del ejemplo del PRD Seguridad §10.2 (§17.3: solo valores ficticios). */
    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Proxy compartido: dentro de una transacción usa el EntityManager de esa transacción. */
    @PersistenceContext
    private EntityManager entityManager;

    @MockitoSpyBean
    private UserRoleRepository userRoleRepository;

    /** Ids de usuario creados por la prueba en curso, para borrarlos al terminar. */
    private final List<Long> createdUserIds = new ArrayList<>();

    @AfterEach
    void deleteCreatedRows() {
        for (Long id : createdUserIds) {
            jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM user_credentials WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM user_profiles WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
        }
        createdUserIds.clear();
    }

    @Test
    @DisplayName("UC-SEC-01 / FR-SEC-AUTH-001 / TC-SEC-016: registro público → 201; usuario, perfil, hash BCrypt y solo ROLE_USER, aunque se inyecten roles")
    void publicRegistrationPersistsUserCredentialAndOnlyUserRole() throws Exception {
        // ARRANGE: DATO DE PRUEBA; role y roles son campos ajenos al contrato (TC-SEC-016).
        String json = registerJson("registro.andrea", "registro.andrea@pulsepass.test", PASSWORD,
                ", \"role\": \"ADMIN\", \"roles\": [\"ADMIN\", \"STAFF\"]");

        // ACT: sin cabecera Authorization (ruta pública).
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                // ASSERT: 201 con el UserResponse de siempre y sin contraseña, hash ni token (§10.1).
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.username").value("registro.andrea"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.roles").doesNotExist())
                .andReturn();
        Long userId = idOf("registro.andrea");
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(PASSWORD).doesNotContain("$2a$");

        // ASSERT: usuario activo y su perfil (BR-USER-003, BR-USER-004).
        assertThat(jdbcTemplate.queryForObject("SELECT active FROM users WHERE id = ?", Boolean.class, userId))
                .isTrue();
        assertThat(countWhere("user_profiles", userId)).isEqualTo(1);

        // ASSERT: hash BCrypt de fuerza 10 que corresponde a la contraseña (SEC-003, P4-D10).
        String hash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM user_credentials WHERE user_id = ?", String.class, userId);
        assertThat(hash).startsWith("$2a$10$").hasSize(60).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, hash)).isTrue();

        // ASSERT: exactamente un rol, USER; nunca ADMIN ni STAFF (NFR-SEC-009, TC-SEC-016).
        assertThat(jdbcTemplate.queryForList("SELECT role FROM user_roles WHERE user_id = ?", String.class, userId))
                .containsExactly("USER");
    }

    @Test
    @DisplayName("NFR-SEC-004: los cuatro INSERT llegan a PostgreSQL y, si después falla el registro, se revierten todos")
    void registrationIsRolledBackWhenSavingRoleFails() throws Exception {
        // ARRANGE: al guardar el rol (último paso de AuthServiceImpl.register), el spy:
        //   1. ejecuta el save real del rol con su respuesta por defecto, la misma que usa sin stub:
        //      delegar en el repositorio real (callRealMethod() no sirve, porque el repositorio es
        //      una interfaz de Spring Data y save es abstracto para Mockito);
        //   2. hace flush(): Hibernate envía a PostgreSQL los INSERT pendientes (credencial y rol;
        //      usuario y perfil ya se insertaron al guardarlos, por su id IDENTITY);
        //   3. lee, con la misma conexión de la transacción, las filas del nuevo usuario en las
        //      cuatro tablas y anota si hay una transacción real y cuál es;
        //   4. lanza la excepción que provoca el rollback.
        AtomicReference<Map<String, Long>> rowsBeforeRollback = new AtomicReference<>();
        AtomicReference<String> transactionName = new AtomicReference<>();
        AtomicReference<Boolean> transactionActive = new AtomicReference<>();
        Answer<?> realSave = mockingDetails(userRoleRepository).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocation -> {
            realSave.answer(invocation);
            entityManager.flush();
            Long userId = invocation.<UserRole>getArgument(0).getUserId();
            rowsBeforeRollback.set(Map.of(
                    "users", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Long.class, userId),
                    "user_profiles", countWhere("user_profiles", userId),
                    "user_credentials", countWhere("user_credentials", userId),
                    "user_roles", countWhere("user_roles", userId)));
            transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            transactionName.set(TransactionSynchronizationManager.getCurrentTransactionName());
            throw new IllegalStateException("Simulated failure after saving the role"); // DATO DE PRUEBA
        }).when(userRoleRepository).save(any());
        long usersBefore = count("users");
        long profilesBefore = count("user_profiles");
        long credentialsBefore = count("user_credentials");
        long rolesBefore = count("user_roles");
        String json = registerJson("registro.fallido", "registro.fallido@pulsepass.test", PASSWORD, "");

        // ACT
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                // ASSERT: error inesperado → 500 genérico del handler (§13).
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected error"));

        // ASSERT: el fallo ocurrió dentro de la transacción real de AuthServiceImpl.register (P4-D16).
        assertThat(transactionActive.get()).isTrue();
        assertThat(transactionName.get()).isEqualTo("com.pulsepass.service.impl.AuthServiceImpl.register");

        // ASSERT: antes del rollback, las cuatro filas estaban en PostgreSQL (vistas desde la
        // conexión de la transacción, que ve sus propios INSERT aún sin confirmar).
        assertThat(rowsBeforeRollback.get()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "users", 1L, "user_profiles", 1L, "user_credentials", 1L, "user_roles", 1L));

        // ASSERT: la transacción se deshizo por completo.
        assertThat(jdbcTemplate.queryForList("SELECT id FROM users WHERE username = ?", Long.class,
                "registro.fallido")).isEmpty();
        assertThat(count("users")).isEqualTo(usersBefore);
        assertThat(count("user_profiles")).isEqualTo(profilesBefore);
        assertThat(count("user_credentials")).isEqualTo(credentialsBefore);
        assertThat(count("user_roles")).isEqualTo(rolesBefore);
    }

    @Test
    @DisplayName("BR-USER-001 / PRD E2E §12.1: username duplicado → 409 y no se crea otra credencial ni otro rol")
    void duplicateUsernameReturns409WithoutCreatingCredentials() throws Exception {
        // ARRANGE: un primer registro correcto.
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("registro.duplicado", "registro.duplicado@pulsepass.test", PASSWORD, "")))
                .andExpect(status().isCreated());
        idOf("registro.duplicado");
        long credentialsBefore = count("user_credentials");
        long rolesBefore = count("user_roles");

        // ACT: mismo username, otro email (DATO DE PRUEBA).
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("registro.duplicado", "otro.duplicado@pulsepass.test", PASSWORD, "")))
                // ASSERT
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists: registro.duplicado"));

        assertThat(count("user_credentials")).isEqualTo(credentialsBefore);
        assertThat(count("user_roles")).isEqualTo(rolesBefore);
        assertThat(jdbcTemplate.queryForList("SELECT id FROM users WHERE email = ?", Long.class,
                "otro.duplicado@pulsepass.test")).isEmpty();
    }

    @Test
    @DisplayName("Seguridad §9.3: contraseña de menos de 12 caracteres → 400 y no se guarda nada")
    void shortPasswordReturns400WithoutPersisting() throws Exception {
        // ARRANGE: DATO DE PRUEBA, 11 caracteres.
        long usersBefore = count("users");
        String json = registerJson("registro.corta", "registro.corta@pulsepass.test", "Corta12345!", "");

        // ACT + ASSERT
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.password").value("Password must be at least 12 characters"));

        assertThat(count("users")).isEqualTo(usersBefore);
    }

    // ---------------------------------------------------------------- Auxiliares

    /** Cuerpo de registro; {@code extra} se añade tal cual dentro del objeto JSON. */
    private static String registerJson(String username, String email, String password, String extra) {
        return """
                {"username": "%s", "email": "%s", "firstName": "Andrea", "lastName": "Registro",
                 "birthDate": "2001-06-15", "password": "%s"%s}""".formatted(username, email, password, extra);
    }

    /** Id del usuario con ese username; lo anota para borrarlo al terminar. */
    private Long idOf(String username) {
        Long id = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
        createdUserIds.add(id);
        return id;
    }

    private long count(String table) {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return total == null ? 0 : total;
    }

    private long countWhere(String table, Long userId) {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE user_id = ?",
                Long.class, userId);
        return total == null ? 0 : total;
    }
}
