package com.pulsepass.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Activa {@link JwtProperties} (P4-03-c). Se usa una clase propia en lugar de
 * {@code @ConfigurationPropertiesScan} para no modificar la clase principal de la aplicación.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {
}
