package kz.iitu.springlab.catalog;
import kz.iitu.springlab.web.Genre;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component @Profile({"dev","test","classroom"})
public class CatalogueSeed implements ApplicationRunner {
    private final BookRepository books;private final AuthorRepository authors;
    private final PublisherRepository publishers;private final TagRepository tags;
    public CatalogueSeed(BookRepository b,AuthorRepository a,PublisherRepository p,TagRepository t){books=b;authors=a;publishers=p;tags=t;}
    @Override @Transactional public void run(ApplicationArguments args){
        if(books.count()!=0)return;
        Author bloch=authors.save(new Author("Joshua Bloch","USA"));
        Author martin=authors.save(new Author("Robert Martin","USA"));
        Author walls=authors.save(new Author("Craig Walls","USA"));
        Publisher addison=publishers.save(new Publisher("Addison-Wesley"));
        Publisher manning=publishers.save(new Publisher("Manning"));
        Tag java=tags.save(new Tag("Java")),spring=tags.save(new Tag("Spring"));
        Tag architecture=tags.save(new Tag("Architecture")),practices=tags.save(new Tag("Best practices"));
        seed(bloch,addison,"Effective Java",2018,java,practices);
        seed(martin,addison,"Clean Code",2008,architecture,practices);
        seed(walls,manning,"Spring in Action",2022,spring);
        seed(bloch,addison,"Java Puzzlers",2005,java);
        seed(bloch,addison,"API Design Notes",2016,practices);
        seed(martin,addison,"Clean Architecture",2017,architecture);
        seed(martin,manning,"Agile Software Development",2002,practices);
        seed(walls,manning,"Spring Boot in Action",2016,spring,java);
        books.flush();
    }
    private void seed(Author author,Publisher publisher,String title,int year,Tag... labels){
        Book book=new Book(title,Genre.PROGRAMMING,year);
        author.addBook(book);publisher.addBook(book);
        for(Tag tag:labels)book.addTag(tag);
        books.save(book);
    }
}
