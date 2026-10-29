package kz.iitu.springlab.catalog;
import jakarta.persistence.*;
import kz.iitu.springlab.web.Genre;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name="book", uniqueConstraints=@UniqueConstraint(name="uk_book_title_author", columnNames={"title_key","author_id"}))
public class Book {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=120) private String title;
    @Column(name="title_key",nullable=false,length=120) private String titleKey;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Genre genre;
    @Column(name="publication_year",nullable=false) private int year;
    @Column(nullable=false) private boolean archived;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Version private long version;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="author_id",nullable=false) private Author author;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="publisher_id",nullable=false) private Publisher publisher;
    @ManyToMany
    @JoinTable(name="book_tag",joinColumns=@JoinColumn(name="book_id"),inverseJoinColumns=@JoinColumn(name="tag_id"))
    private Set<Tag> tags=new HashSet<>();
    protected Book() { }
    public Book(String title, Genre genre, int year) { this.title=title; this.genre=genre; this.year=year; }
    // Retained form and DTO input constructors. The service resolves the transient author.
    public Book(Long ignoredId,String title,String author,int year,Genre genre,Instant ignoredTime) {
        this(title,genre,year);
        if(author!=null) this.author=new Author(author,"Unspecified");
    }
    public Book(Long ignoredId,String title,String author,int year) {
        this(ignoredId,title,author,year,Genre.PROGRAMMING,null);
    }
    @PrePersist void beforeInsert() { createdAt=Instant.now(); normaliseTitle(); }
    @PreUpdate void normaliseTitle() { titleKey=title.strip().toLowerCase(Locale.ROOT); }
    public void addTag(Tag tag) { tags.add(tag); tag.getBooks().add(this); }
    public void removeTag(Tag tag) { tags.remove(tag); tag.getBooks().remove(this); }
    public Long getId(){return id;} public String getTitle(){return title;}
    public Genre getGenre(){return genre;} public int getYear(){return year;}
    public boolean isArchived(){return archived;} public Instant getCreatedAt(){return createdAt;}
    public long getVersion(){return version;} public Author getAuthor(){return author;}
    public Publisher getPublisher(){return publisher;} public Set<Tag> getTags(){return tags;}
    public void setTitle(String v){title=v;} public void setGenre(Genre v){genre=v;}
    public void setYear(int v){year=v;} public void setArchived(boolean v){archived=v;}
    public void setAuthor(Author v){author=v;} public void setPublisher(Publisher v){publisher=v;}
    public Long id(){return id;} public String title(){return title;}
    public String author(){return author==null?null:author.getName();}
    public int year(){return year;} public Genre genre(){return genre;}
    public Instant createdAt(){return createdAt;}
}
