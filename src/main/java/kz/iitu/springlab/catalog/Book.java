package kz.iitu.springlab.catalog;

import kz.iitu.springlab.web.Genre;
import java.time.Instant;

public record Book(Long id, String title, String author, int year,
                   Genre genre, Instant createdAt) {
    public Book(Long id, String title, String author, int year) {
        this(id, title, author, year, Genre.PROGRAMMING, null);
    }
}
