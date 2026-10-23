package kz.iitu.springlab.catalog;

public class DuplicateTitleException extends RuntimeException {
    private final String title;

    public DuplicateTitleException(String title) {
        super("A book with the same title and author already exists");
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
