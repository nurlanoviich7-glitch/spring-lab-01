package kz.iitu.springlab.catalog;
import jakarta.persistence.EntityManager;
import kz.iitu.springlab.web.Genre;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @Transactional(readOnly=true)
public class BookService {
    private final BookRepository repository;
    private final AuthorRepository authors;
    private final PublisherRepository publishers;
    private final EntityManager em;
    public BookService(BookRepository repository,AuthorRepository authors,PublisherRepository publishers,EntityManager em) {
        this.repository=repository;this.authors=authors;this.publishers=publishers;this.em=em;
    }
    public List<Book> findAll(String author) {
        String fragment=normaliseFilter(author);
        return repository.findAllByOrderByIdAsc().stream()
            .filter(b->fragment==null||b.author().toLowerCase(Locale.ROOT).contains(fragment.toLowerCase(Locale.ROOT))).toList();
    }
    public Page<Book> find(Genre genre,String author,String publisher,Pageable pageable) {
        Set<String> allowed=Set.of("id","title","year","genre","createdAt","archived","author.name","publisher.name");
        for(Sort.Order order:pageable.getSort()) if(!allowed.contains(order.getProperty()))
            throw new IllegalArgumentException("Unsupported sort property: "+order.getProperty());
        Sort sort=pageable.getSort().isSorted()?pageable.getSort():Sort.by("title");
        if(sort.getOrderFor("id")==null) sort=sort.and(Sort.by("id"));
        Pageable stable=PageRequest.of(pageable.getPageNumber(),pageable.getPageSize(),sort);
        return repository.findFiltered(genre,normaliseFilter(author),normaliseFilter(publisher),stable);
    }
    public Optional<Book> findById(long id){return repository.findById(id);}
    public Book requireById(long id){return findById(id).orElseThrow(()->new BookNotFoundException(id));}
    public List<Book> searchByTitle(String title){
        String fragment=normaliseFilter(title);
        if(fragment==null)return repository.findAllByOrderByIdAsc();
        List<Book> books=repository.findByTitleContainingIgnoreCase(fragment);
        books.forEach(Book::author);
        return books;
    }
    @Transactional public Book create(Book input){
        validate(input);
        String title=input.title().strip(),name=input.author().strip();
        if(repository.existsByTitleKeyAndAuthorNameKey(key(title),key(name)))throw new DuplicateTitleException(title);
        Author author=authors.findByNameKey(key(name)).orElseGet(()->authors.save(new Author(name,"Unspecified")));
        Publisher publisher=publishers.findByNameKey("independent").orElseGet(()->publishers.save(new Publisher("Independent")));
        Book saved=new Book(title,input.genre(),input.year());
        author.addBook(saved);publisher.addBook(saved);
        return repository.saveAndFlush(saved);
    }
    @Transactional public Optional<Book> replace(long id,Book input){
        if(!repository.existsById(id))return Optional.empty();
        return Optional.of(update(id,input));
    }
    @Transactional public Book update(long id,Book input){
        validate(input);
        Book saved=requireById(id);
        String title=input.title().strip(),name=input.author().strip();
        if(repository.existsByTitleKeyAndAuthorNameKeyAndIdNot(key(title),key(name),id))throw new DuplicateTitleException(title);
        Author author=authors.findByNameKey(key(name)).orElseGet(()->authors.save(new Author(name,"Unspecified")));
        saved.setTitle(title);saved.setYear(input.year());saved.setGenre(input.genre());saved.setAuthor(author);
        // Dirty checking persists the managed book. A fresh context also avoids
        // exposing an old inverse collection when an existing book changes author.
        em.flush();em.clear();
        return requireById(id);
    }
    @Transactional public boolean delete(long id){
        Optional<Book> book=repository.findById(id);
        if(book.isEmpty())return false;
        repository.delete(book.get());repository.flush();return true;
    }
    @Transactional public void deleteRequired(long id){if(!delete(id))throw new BookNotFoundException(id);}
    @Transactional public int archiveOlderThan(int year){return repository.archiveOlderThan(year);}
    private String normaliseFilter(String value){return value==null||value.isBlank()?null:value.strip();}
    private String key(String value){return value.strip().toLowerCase(Locale.ROOT);}
    private void validate(Book book){
        if(book==null||book.title()==null||book.title().isBlank())throw new IllegalArgumentException("Title must not be blank");
        if(book.title().strip().length()>120)throw new IllegalArgumentException("Title must have at most 120 characters");
        if(book.author()==null||book.author().isBlank())throw new IllegalArgumentException("Author must not be blank");
        if(book.year()<1450||book.year()>2100)throw new IllegalArgumentException("Year must be between 1450 and 2100");
        if(book.genre()==null)throw new IllegalArgumentException("Genre must be selected");
    }
}
