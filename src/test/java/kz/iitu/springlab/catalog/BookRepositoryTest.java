package kz.iitu.springlab.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class BookRepositoryTest {
    @Test
    void seedsAreReturnedInStableIdentifierOrder() {
        BookRepository repository = new BookRepository();

        assertThat(repository.findAll()).extracting(Book::id).containsExactly(1L, 2L, 3L);
        Book seed = repository.findById(1).orElseThrow();
        assertThat(seed.title()).isEqualTo("Effective Java");
        assertThat(seed.author()).isEqualTo("Joshua Bloch");
        assertThat(seed.year()).isEqualTo(2018);
        assertThat(seed.createdAt()).isNotNull();
        assertThat(repository.findById(999)).isEmpty();
    }

    @Test
    void concurrentCreationAllocatesUniqueIdsAndIgnoresSuppliedIds() throws Exception {
        BookRepository repository = new BookRepository();
        List<Callable<Book>> requests = IntStream.range(0, 100)
                .mapToObj(index -> (Callable<Book>) () ->
                        repository.create(new Book(1L, "Book " + index, "Author", 2024)))
                .toList();

        try (var executor = Executors.newFixedThreadPool(8)) {
            var saved = executor.invokeAll(requests);
            for (var result : saved) {
                assertThat(result.get().id()).isGreaterThan(3L);
            }
        }

        var ids = repository.findAll().stream().map(Book::id).toList();
        assertThat(ids).hasSize(103).doesNotHaveDuplicates().isSorted();
        assertThat(ids.getLast()).isEqualTo(103L);
        assertThat(repository.findById(1).orElseThrow().title()).isEqualTo("Effective Java");
    }

    @Test
    void replacementUsesPathIdentifierAndCannotCreateOrResurrect() {
        BookRepository repository = new BookRepository();
        Book replacement = new Book(500L, "Replacement", "Author", 2025);

        var createdAt = repository.findById(1).orElseThrow().createdAt();
        Book updated = repository.replace(1, replacement).orElseThrow();
        assertThat(updated.id()).isEqualTo(1L);
        assertThat(updated.title()).isEqualTo("Replacement");
        assertThat(updated.author()).isEqualTo("Author");
        assertThat(updated.year()).isEqualTo(2025);
        assertThat(updated.createdAt()).isEqualTo(createdAt);
        assertThat(repository.replace(999, replacement)).isEmpty();
        assertThat(repository.deleteById(1)).isTrue();
        assertThat(repository.replace(1, replacement)).isEmpty();
        assertThat(repository.deleteById(1)).isFalse();
        assertThat(repository.findById(500)).isEmpty();
        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void competingDuplicateRequestsCanCreateOnlyOneRecord() throws Exception {
        BookRepository repository = new BookRepository();
        List<Callable<Boolean>> requests = IntStream.range(0, 20)
                .mapToObj(index -> (Callable<Boolean>) () -> {
                    String title = index % 2 == 0 ? "Concurrent Title" : "  CONCURRENT TITLE  ";
                    try {
                        repository.create(new Book(null, title, "Same Author", 2025));
                        return true;
                    } catch (DuplicateTitleException expected) {
                        return false;
                    }
                }).toList();

        int created = 0;
        try (var executor = Executors.newFixedThreadPool(8)) {
            for (var result : executor.invokeAll(requests)) {
                if (result.get()) {
                    created++;
                }
            }
        }
        assertThat(created).isEqualTo(1);
        assertThat(repository.findAll()).hasSize(4);
        assertThat(repository.findAll()).extracting(Book::id).containsExactly(1L, 2L, 3L, 4L);
    }
}
