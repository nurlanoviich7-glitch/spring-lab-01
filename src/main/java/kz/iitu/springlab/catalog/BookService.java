package kz.iitu.springlab.catalog;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class BookService {
    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    public List<Book> findAll(String author) {
        if (author == null || author.isBlank()) {
            return repository.findAll();
        }
        String fragment = author.strip().toLowerCase(Locale.ROOT);
        return repository.findAll().stream()
                .filter(book -> book.author().toLowerCase(Locale.ROOT).contains(fragment))
                .toList();
    }

    public Optional<Book> findById(long id) {
        return repository.findById(id);
    }

    public Book requireById(long id) {
        return repository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    }

    public List<Book> searchByTitle(String title) {
        if (title == null || title.isBlank()) {
            return repository.findAll();
        }
        String fragment = title.strip().toLowerCase(Locale.ROOT);
        return repository.findAll().stream()
                .filter(book -> book.title().toLowerCase(Locale.ROOT).contains(fragment))
                .toList();
    }

    public Book create(Book book) {
        return repository.create(validated(book));
    }

    public Optional<Book> replace(long id, Book book) {
        return repository.replace(id, validated(book));
    }

    public boolean delete(long id) {
        return repository.deleteById(id);
    }

    private Book validated(Book book) {
        if (book == null || book.title() == null || book.title().isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        if (book.author() == null || book.author().isBlank()) {
            throw new IllegalArgumentException("Author must not be blank");
        }
        if (book.year() < 1 || book.year() > 9999) {
            throw new IllegalArgumentException("Year must be between 1 and 9999");
        }
        return new Book(book.id(), book.title().strip(), book.author().strip(), book.year());
    }
}
