package kz.iitu.springlab.catalog;
import jakarta.persistence.*;
import java.util.*;
@Entity @Table(name="publisher")
public class Publisher {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=120) private String name;
    @Column(name="name_key",nullable=false,unique=true,length=120) private String nameKey;
    @OneToMany(mappedBy="publisher",cascade={CascadeType.PERSIST,CascadeType.MERGE})
    private List<Book> books=new ArrayList<>();
    protected Publisher() { }
    public Publisher(String name){this.name=name.strip();}
    @PrePersist @PreUpdate void normalise(){nameKey=name.strip().toLowerCase(Locale.ROOT);}
    public void addBook(Book book){if(!books.contains(book))books.add(book);book.setPublisher(this);}
    public Long getId(){return id;} public String getName(){return name;}
    public List<Book> getBooks(){return books;}
}
