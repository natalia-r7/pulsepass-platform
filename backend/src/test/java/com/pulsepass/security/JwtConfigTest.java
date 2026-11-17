package com.pulsepass.security;

import com.pulsepass.config.ClockConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de la configuración del JWT dentro de Spring (PRD Seguridad §12.2, §14.2; P4-03-c).
 * <p>
 * {@link ApplicationContextRunner} levanta un contexto mínimo solo con {@link JwtConfig},
 * {@link JwtService} y {@link ClockConfig}, sin base de datos ni servidor. Permite comprobar que
 * la aplicación no arranca con una clave ausente o débil (§12.2).
 */
class JwtConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JwtConfig.class, JwtService.class, ClockConfig.class);

    @Test
    @DisplayName("§14.2: con application.yml y la clave de pruebas, usa issuer pulsepass-api y PT15M y crea JwtService")
    void loadsDefaultsFromApplicationYml() {
        // Carga application.yml y config/application.yml como lo hace la aplicación.
        contextRunner
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(JwtService.class);
                    JwtProperties properties = context.getBean(JwtProperties.class);
                    assertThat(properties.issuer()).isEqualTo("pulsepass-api");
                    assertThat(properties.accessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
                    assertThat(context.getBean(JwtService.class).issueAccessToken(1L).expiresInSeconds())
                            .isEqualTo(900);
                });
    }

    @Test
    @DisplayName("§12.2: sin clave, la aplicación no arranca")
    void failsToStartWithoutSecret() {
        contextRunner
                .withPropertyValues(
                        "pulsepass.security.jwt.issuer=pulsepass-api",
                        "pulsepass.security.jwt.access-token-ttl=PT15M")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("pulsepass.security.jwt.secret-base64 must be provided");
                });
    }

    @Test
    @DisplayName("§12.2: con una clave de menos de 32 bytes, la aplicación no arranca y el error no muestra la clave")
    void failsToStartWithWeakSecret() {
        String weakSecret = Base64.getEncoder().encodeToString(
                "jwt-config-test-weak-key".getBytes(StandardCharsets.UTF_8));   // DATO DE PRUEBA: 24 bytes

        contextRunner
                .withPropertyValues(
                        "pulsepass.security.jwt.issuer=pulsepass-api",
                        "pulsepass.security.jwt.secret-base64=" + weakSecret,
                        "pulsepass.security.jwt.access-token-ttl=PT15M")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("pulsepass.security.jwt.secret-base64 must decode to at least 32 bytes")
                            .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain(weakSecret));
                });
    }
}
