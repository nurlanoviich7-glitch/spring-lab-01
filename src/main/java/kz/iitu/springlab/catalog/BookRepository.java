package kz.iitu.springlab.catalog;

import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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

    public Book create(Book book) {
        long id = sequence.incrementAndGet();
        Book stored = new Book(id, book.title(), book.author(), book.year());
        storage.put(id, stored);
        return stored;
    }

    public Optional<Book> replace(long id, Book book) {
        // An atomic existing-only replacement cannot recreate a deleted record.
        return Optional.ofNullable(storage.computeIfPresent(id,
                (key, previous) -> new Book(key, book.title(), book.author(), book.year())));
    }

    public boolean deleteById(long id) {
        return storage.remove(id) != null;
    }
}
