package kz.iitu.springlab.catalog;
import jakarta.persistence.*;
import java.util.*;
@Entity @Table(name="author")
public class Author {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=120) private String name;
    @Column(name="name_key",nullable=false,unique=true,length=120) private String nameKey;
    @Column(nullable=false,length=80) private String country;
    @OneToMany(mappedBy="author",cascade=CascadeType.ALL,orphanRemoval=true)
    private List<Book> books=new ArrayList<>();
    protected Author() { }
    public Author(String name,String country){this.name=name.strip();this.country=country;}
    @PrePersist @PreUpdate void normalise(){nameKey=name.strip().toLowerCase(Locale.ROOT);}
    public void addBook(Book book){if(!books.contains(book))books.add(book);book.setAuthor(this);}
    public void removeBook(Book book){if(books.remove(book))book.setAuthor(null);}
    public Long getId(){return id;} public String getName(){return name;}
    public String getCountry(){return country;} public List<Book> getBooks(){return books;}
}
