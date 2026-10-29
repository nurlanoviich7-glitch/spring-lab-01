package kz.iitu.springlab.web;
import io.swagger.v3.oas.annotations.Operation;
import kz.iitu.springlab.catalog.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/lab8")
@io.swagger.v3.oas.annotations.tags.Tag(name="Lab 8",description="PostgreSQL evidence and variant 1")
public class Lab8Controller {
    private final Lab8EvidenceService evidence;private final JdbcTemplate jdbc;
    public Lab8Controller(Lab8EvidenceService evidence,JdbcTemplate jdbc){this.evidence=evidence;this.jdbc=jdbc;}
    @GetMapping("/queries") @Operation(summary="Run five derived queries and the JPQL select")
    public Object queries(){return evidence.queries();}
    @GetMapping("/n-plus-one") @Operation(summary="Measure lazy author loading and fetch join with fresh contexts")
    public Object nPlusOne(){return evidence.nPlusOne();}
    @GetMapping("/page-vs-slice") @Operation(summary="Capture the SQL of Page and Slice for the same request")
    public Object paging(){return evidence.paging();}
    @GetMapping("/publishers") @Operation(summary="Variant 1 derived query by publisher name")
    public Object publisher(@RequestParam(name="name",defaultValue="Manning")String name){return evidence.publisher(name);}
    @PostMapping("/archive") @Operation(summary="Archive older books with one transactional bulk update")
    public Object archive(@RequestParam(name="year",defaultValue="2010")int year){return evidence.archive(year);}
    @GetMapping("/books/{id}") public Object details(@PathVariable(name="id")long id){return evidence.details(id);}
    @GetMapping("/database") @Operation(summary="Read the actual PostgreSQL schema and catalogue rows")
    public Object database(){
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("server",jdbc.queryForMap("select version() as version,current_database() as database,current_user as username"));
        result.put("tables",jdbc.queryForList("select table_name from information_schema.tables where table_schema='public' and table_type='BASE TABLE' order by table_name"));
        result.put("columns",jdbc.queryForList("select table_name,column_name,data_type,is_nullable from information_schema.columns where table_schema='public' order by table_name,ordinal_position"));
        result.put("foreignKeys",jdbc.queryForList("""
            select tc.table_name,kcu.column_name,ccu.table_name as referenced_table,ccu.column_name as referenced_column
            from information_schema.table_constraints tc
            join information_schema.key_column_usage kcu on tc.constraint_name=kcu.constraint_name and tc.constraint_schema=kcu.constraint_schema
            join information_schema.constraint_column_usage ccu on ccu.constraint_name=tc.constraint_name and ccu.constraint_schema=tc.constraint_schema
            where tc.constraint_type='FOREIGN KEY' and tc.table_schema='public' order by tc.table_name,kcu.column_name
            """));
        result.put("books",jdbc.queryForList("""
            select b.id,b.title,b.publication_year,b.genre,b.archived,b.version,b.author_id,a.name as author,
                   b.publisher_id,p.name as publisher
            from book b join author a on a.id=b.author_id join publisher p on p.id=b.publisher_id order by b.id
            """));
        result.put("bookTags",jdbc.queryForList("select book_id,tag_id from book_tag order by book_id,tag_id"));
        return result;
    }
}
