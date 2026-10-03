package kz.iitu.springlab.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookServiceTest {
    private BookService service;

    @BeforeEach
    void setUp() {
        service = new BookService(new BookRepository());
    }

    @Test
    void authorFilterMatchesFragmentsWithoutCaseSensitivity() {
        assertThat(service.findAll("  bLoCh  ")).extracting(Book::title).containsExactly("Effective Java");
        assertThat(service.findAll("ar")).extracting(Book::author).containsExactly("Robert Martin");
        assertThat(service.findAll("No such author")).isEmpty();
        assertThat(service.findAll(null)).hasSize(3);
        assertThat(service.findAll("  ")).hasSize(3);
    }

    @Test
    void createTrimsTextAndAllocatesAnIdIndependentOfTheBody() {
        Book saved = service.create(new Book(1L, "  Pro Spring 6  ", "  Cosmina  ", 2023));

        assertThat(saved).isEqualTo(new Book(4L, "Pro Spring 6", "Cosmina", 2023));
        assertThat(service.findById(1).orElseThrow().title()).isEqualTo("Effective Java");
    }

    @Test
    void titleSearchReturnsAllForBlankAndEmptyListForNoMatches() {
        assertThat(service.searchByTitle("  sPrInG  "))
                .extracting(Book::title).containsExactly("Spring in Action");
        assertThat(service.searchByTitle("No such title")).isEmpty();
        assertThat(service.searchByTitle(" ")).hasSize(3);
        assertThat(service.searchByTitle(null)).hasSize(3);
    }

    @Test
    void invalidInputIsRejectedBeforeItChangesTheRepository() {
        assertThatThrownBy(() -> service.create(new Book(null, " ", "Author", 2023)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Title must not be blank");
        assertThatThrownBy(() -> service.create(new Book(null, "Title", null, 2023)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Author must not be blank");
        assertThatThrownBy(() -> service.create(new Book(null, "Title", "Author", 0)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Year must be between 1 and 9999");
        assertThatThrownBy(() -> service.replace(1, new Book(null, "Title", "Author", 10000)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(service.findAll(null)).hasSize(3);
        assertThat(service.findById(1).orElseThrow().title()).isEqualTo("Effective Java");
    }
}
