package kz.iitu.springlab.catalog;
import jakarta.persistence.*;
import java.util.*;
@Entity @Table(name="tag")
public class Tag {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=60) private String name;
    @ManyToMany(mappedBy="tags") private Set<Book> books=new HashSet<>();
    protected Tag() { }
    public Tag(String name){this.name=name;}
    public Long getId(){return id;} public String getName(){return name;}
    public Set<Book> getBooks(){return books;}
}
