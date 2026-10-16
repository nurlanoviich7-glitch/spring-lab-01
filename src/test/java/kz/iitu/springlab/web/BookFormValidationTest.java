package kz.iitu.springlab.web;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookFormValidationTest {
    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        factory.close();
    }

    private BookForm validForm() {
        BookForm form = new BookForm();
        form.setTitle("Effective Java");
        form.setAuthor("Joshua Bloch");
        form.setYear(2018);
        form.setGenre(Genre.PROGRAMMING);
        form.setAvailable(true);
        form.setIsbn("9780134685991");
        return form;
    }

    @Test
    void constraintsReportEachInvalidFieldInsteadOfSavingPartialInput() {
        BookForm form = validForm();
        form.setTitle(" ");
        form.setAuthor("Bloch 2018");
        form.setYear(1200);
        form.setGenre(null);
        form.setIsbn("9780134685992");

        assertThat(validator.validate(form)).extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("title", "author", "year", "genre", "isbn");
    }

    @Test
    void titleLengthAndYearBoundsAreInclusive() {
        BookForm form = validForm();
        form.setTitle("T".repeat(120));
        form.setYear(1450);
        assertThat(validator.validate(form)).isEmpty();
        form.setYear(2100);
        assertThat(validator.validate(form)).isEmpty();
        form.setTitle("T".repeat(121));
        form.setYear(2101);
        assertThat(validator.validate(form)).extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("title", "year");
        form.setTitle("Title");
        form.setYear(1449);
        assertThat(validator.validate(form)).extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("year");
    }

    @Test
    void blankAuthorIsRequiredWhileIsbnAndUncheckedAvailabilityAreOptional() {
        BookForm form = validForm();
        form.setIsbn("");
        form.setAvailable(false);
        assertThat(validator.validate(form)).isEmpty();
        form.setAuthor("");
        assertThat(validator.validate(form)).extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("author");
    }
}
