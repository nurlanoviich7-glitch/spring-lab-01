package kz.iitu.springlab.catalog;
import jakarta.persistence.EntityManager;
import kz.iitu.springlab.web.Genre;
import kz.iitu.springlab.web.dto.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Supplier;

@Service @Transactional(readOnly=true)
public class Lab8EvidenceService {
    private final BookRepository books;private final BookService service;
    private final EntityManager em;private final BookMapper mapper;
    public Lab8EvidenceService(BookRepository b,BookService s,EntityManager e,BookMapper m){books=b;service=s;em=e;mapper=m;}
    public record QueryEvidence(String method,Object result,int sqlCount,List<String> sql) { }
    private QueryEvidence measure(String name,Supplier<?> operation){
        em.clear();SqlRecorder.begin();Object result;
        try {result=operation.get();}catch(RuntimeException e){SqlRecorder.end();throw e;}
        List<String> sql=SqlRecorder.end();return new QueryEvidence(name,result,sql.size(),sql);
    }
    private List<String> titles(List<Book> result){return result.stream().map(Book::title).toList();}
    public List<QueryEvidence> queries(){return List.of(
        measure("findByGenre(PROGRAMMING)",()->titles(books.findByGenre(Genre.PROGRAMMING))),
        measure("findByTitleContainingIgnoreCase(java)",()->titles(books.findByTitleContainingIgnoreCase("java"))),
        measure("findByYearBetweenOrderByYearDesc(2005,2022)",()->titles(books.findByYearBetweenOrderByYearDesc(2005,2022))),
        measure("findTop3ByGenreOrderByYearDesc(PROGRAMMING)",()->titles(books.findTop3ByGenreOrderByYearDesc(Genre.PROGRAMMING))),
        measure("countByAuthorName(Joshua Bloch)",()->books.countByAuthorName("Joshua Bloch")),
        measure("search(PROGRAMMING,2016)",()->titles(books.search(Genre.PROGRAMMING,2016))));}
    public Map<String,QueryEvidence> nPlusOne(){
        QueryEvidence before=measure("findAll then author names",()->books.findAll().stream().map(Book::author).toList());
        QueryEvidence after=measure("findAllWithAuthor then author names",()->books.findAllWithAuthor().stream().map(Book::author).toList());
        return Map.of("before",before,"after",after);
    }
    public Map<String,QueryEvidence> paging(){
        Pageable request=PageRequest.of(1,3,Sort.by(Sort.Direction.DESC,"year").and(Sort.by("id")));
        QueryEvidence page=measure("Page page=1 size=3",()->{
            Page<Book> p=books.findAllBy(request);
            return Map.of("titles",titles(p.getContent()),"number",p.getNumber(),"totalElements",p.getTotalElements(),"totalPages",p.getTotalPages());
        });
        QueryEvidence slice=measure("Slice page=1 size=3",()->{
            Slice<Book> p=books.findSliceBy(request);
            return Map.of("titles",titles(p.getContent()),"number",p.getNumber(),"hasNext",p.hasNext());
        });
        return Map.of("page",page,"slice",slice);
    }
    public QueryEvidence publisher(String name){return measure("findByPublisherNameIgnoreCase("+name+")",
        ()->mapper.toResponseList(books.findByPublisherNameIgnoreCase(name)));}
    @Transactional public QueryEvidence archive(int year){
        if(year<1450||year>2101)throw new IllegalArgumentException("Year must be between 1450 and 2101");
        return measure("archiveOlderThan("+year+")",()->service.archiveOlderThan(year));
    }
    public Map<String,Object> details(long id){
        Book b=service.requireById(id);
        return Map.of("id",b.id(),"title",b.title(),"author",b.author(),"authorId",b.getAuthor().getId(),
            "publisher",b.getPublisher().getName(),"publisherId",b.getPublisher().getId(),
            "tags",b.getTags().stream().map(Tag::getName).sorted().toList(),
            "archived",b.isArchived(),"version",b.getVersion(),"createdAt",b.createdAt());
    }
}
