package kz.iitu.springlab.catalog;

public class BookNotFoundException extends RuntimeException {
    private final long bookId;

    public BookNotFoundException(long bookId) {
        super("Book " + bookId + " was not found");
        this.bookId = bookId;
    }

    public long getBookId() {
        return bookId;
    }
}
