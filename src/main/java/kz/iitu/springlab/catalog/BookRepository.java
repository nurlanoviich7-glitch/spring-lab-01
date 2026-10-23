package kz.iitu.springlab.catalog;

import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class BookRepository {
    private final ConcurrentHashMap<Long, Book> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public BookRepository() {
        create(new Book(null, "Effective Java", "Joshua Bloch", 2018));
        create(new Book(null, "Clean Code", "Robert Martin", 2008));
        create(new Book(null, "Spring in Action", "Craig Walls", 2022));
    }

    public List<Book> findAll() {
        return storage.values().stream()
                .sorted(Comparator.comparing(Book::id))
                .toList();
    }

    public Optional<Book> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public synchronized Book create(Book book) {
        rejectDuplicate(book, null);
        long id = sequence.incrementAndGet();
        Book stored = new Book(id, book.title(), book.author(), book.year(),
                book.genre(), Instant.now());
        storage.put(id, stored);
        return stored;
    }

    public synchronized Optional<Book> replace(long id, Book book) {
        Book previous = storage.get(id);
        if (previous == null) {
            return Optional.empty();
        }
        rejectDuplicate(book, id);
        Book stored = new Book(id, book.title(), book.author(), book.year(),
                book.genre(), previous.createdAt());
        storage.put(id, stored);
        return Optional.of(stored);
    }

    public synchronized boolean deleteById(long id) {
        return storage.remove(id) != null;
    }

    private void rejectDuplicate(Book candidate, Long excludedId) {
        boolean duplicate = storage.values().stream()
                .filter(book -> !book.id().equals(excludedId))
                .anyMatch(book -> key(book.title()).equals(key(candidate.title()))
                        && key(book.author()).equals(key(candidate.author())));
        if (duplicate) {
            throw new DuplicateTitleException(candidate.title());
        }
    }

    private String key(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }
}
