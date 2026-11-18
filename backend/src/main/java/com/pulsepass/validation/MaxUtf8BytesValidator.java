package com.pulsepass.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

/**
 * Validador de {@link MaxUtf8Bytes} (P4-06-B): cuenta los bytes del texto codificado en UTF-8.
 * Un valor {@code null} es válido; su obligatoriedad corresponde a {@code @NotBlank}.
 */
public class MaxUtf8BytesValidator implements ConstraintValidator<MaxUtf8Bytes, CharSequence> {

    private int maxBytes;

    @Override
    public void initialize(MaxUtf8Bytes constraint) {
        this.maxBytes = constraint.value();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return value.toString().getBytes(StandardCharsets.UTF_8).length <= maxBytes;
    }
}
