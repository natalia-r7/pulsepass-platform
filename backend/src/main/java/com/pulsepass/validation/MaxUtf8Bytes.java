package com.pulsepass.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El texto anotado no puede ocupar más de {@link #value()} bytes en UTF-8 (PRD Seguridad §9.3;
 * P4-D14, P4-06-B).
 * <p>
 * Existe porque {@code @Size} cuenta caracteres, no bytes: una «ñ» es 1 carácter pero 2 bytes.
 * BCrypt solo usa los primeros 72 bytes y {@code BCryptPasswordEncoder} rechaza con
 * {@code IllegalArgumentException} una contraseña más larga, que sin esta validación acabaría
 * en un 500. Un valor {@code null} se considera válido: la obligatoriedad la cubre
 * {@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

    /** Número máximo de bytes UTF-8 permitidos. */
    int value();

    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
