package kz.iitu.springlab.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IsbnValidator implements ConstraintValidator<ValidIsbn, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        StringBuilder compact = new StringBuilder();
        value.codePoints().filter(point -> point != '-' && !Character.isWhitespace(point))
                .forEach(compact::appendCodePoint);
        String isbn = compact.toString();
        if (isbn.length() == 10) {
            int sum = 0;
            for (int index = 0; index < 10; index++) {
                char character = isbn.charAt(index);
                int digit;
                if (index == 9 && (character == 'X' || character == 'x')) {
                    digit = 10;
                } else if (character >= '0' && character <= '9') {
                    digit = character - '0';
                } else {
                    return false;
                }
                sum += (10 - index) * digit;
            }
            return sum % 11 == 0;
        }
        if (isbn.length() == 13) {
            if (!isbn.startsWith("978") && !isbn.startsWith("979")) {
                return false;
            }
            int sum = 0;
            for (int index = 0; index < 13; index++) {
                char character = isbn.charAt(index);
                if (character < '0' || character > '9') {
                    return false;
                }
                sum += (index % 2 == 0 ? 1 : 3) * (character - '0');
            }
            return sum % 10 == 0;
        }
        return false;
    }
}
