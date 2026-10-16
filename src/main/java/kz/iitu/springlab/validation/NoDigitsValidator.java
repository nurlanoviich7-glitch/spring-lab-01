package kz.iitu.springlab.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoDigitsValidator implements ConstraintValidator<NoDigits, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null/blank values are checked independently by @NotBlank.
        return value == null || value.codePoints().noneMatch(Character::isDigit);
    }
}
