package kz.iitu.springlab.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomConstraintsTest {
    private final NoDigitsValidator noDigits = new NoDigitsValidator();
    private final IsbnValidator isbn = new IsbnValidator();

    @Test
    void noDigitsChecksUnicodeCodePointsAndLeavesRequirednessToNotBlank() {
        assertThat(noDigits.isValid(null, null)).isTrue();
        assertThat(noDigits.isValid("", null)).isTrue();
        assertThat(noDigits.isValid("Joshua Bloch", null)).isTrue();
        assertThat(noDigits.isValid("Bloch 2018", null)).isFalse();
        assertThat(noDigits.isValid("Author \u0661", null)).isFalse();
        String supplementaryDigit = new String(Character.toChars(0x1D7CE));
        assertThat(noDigits.isValid("Author " + supplementaryDigit, null)).isFalse();
    }

    @Test
    void isbnAcceptsOptionalBlankValuesAndRealIsbnTenAndThirteenChecksums() {
        assertThat(isbn.isValid(null, null)).isTrue();
        assertThat(isbn.isValid("  ", null)).isTrue();
        assertThat(isbn.isValid("0134685997", null)).isTrue();
        assertThat(isbn.isValid("9780134685991", null)).isTrue();
        assertThat(isbn.isValid("0-8044-2957-X", null)).isTrue();
        assertThat(isbn.isValid("0 8044 2957 x", null)).isTrue();
        assertThat(isbn.isValid("978-0-13-468599-1", null)).isTrue();
    }

    @Test
    void isbnRejectsWrongLengthWrongChecksumAndMisplacedCheckLetter() {
        assertThat(isbn.isValid("0134685998", null)).isFalse();
        assertThat(isbn.isValid("9780134685992", null)).isFalse();
        assertThat(isbn.isValid("123", null)).isFalse();
        assertThat(isbn.isValid("X134685997", null)).isFalse();
        assertThat(isbn.isValid("978013468599X", null)).isFalse();
        // A correct EAN-13 checksum does not make a non-ISBN prefix an ISBN.
        assertThat(isbn.isValid("4006381333931", null)).isFalse();
        assertThat(isbn.isValid("letters", null)).isFalse();
        assertThat(isbn.isValid("---", null)).isFalse();
    }
}
