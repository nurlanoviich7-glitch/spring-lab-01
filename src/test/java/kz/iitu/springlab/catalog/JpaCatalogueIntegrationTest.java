package kz.iitu.springlab.catalog;
import jakarta.persistence.*;
import kz.iitu.springlab.web.Genre;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test") @Transactional
class JpaCatalogueIntegrationTest {
    @Autowired BookRepository books;
    @Autowired AuthorRepository authors;
    @Autowired PublisherRepository publishers;
    @Autowired TagRepository tags;
    @Autowired BookService service;
    @Autowired Lab8EvidenceService evidence;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory factory;
    @Autowired JdbcTemplate jdbc;
    Book effective(){return books.findByTitleContainingIgnoreCase("Effective Java").getFirst();}

    @Test void connectsToPostgresqlAndKeepsTestDatabaseSeparate(){
        assertThat(jdbc.queryForObject("select version()",String.class)).startsWith("PostgreSQL 16");
        assertThat(jdbc.queryForObject("select current_database()",String.class)).isEqualTo("springlab_test");
    }
    @Test void seedHasRequiredEntitiesAndSharedAssociations(){
        assertThat(books.count()).isEqualTo(8);assertThat(authors.count()).isEqualTo(3);
        assertThat(tags.count()).isEqualTo(4);assertThat(publishers.count()).isEqualTo(2);
        assertThat(books.countByAuthorName("Joshua Bloch")).isEqualTo(3);
        assertThat(effective().getTags()).hasSize(2);
    }
    @Test void actualSchemaContainsForeignKeysAndNoInverseJoinTable(){
        assertThat(jdbc.queryForList("select table_name from information_schema.tables where table_schema='public'",String.class))
            .containsExactlyInAnyOrder("author","book","tag","book_tag","publisher");
        assertThat(jdbc.queryForList("select column_name from information_schema.columns where table_schema='public' and table_name='book_tag'",String.class))
            .containsExactlyInAnyOrder("book_id","tag_id");
        assertThat(jdbc.queryForList("select column_name from information_schema.columns where table_schema='public' and table_name='author'",String.class)).doesNotContain("books");
        assertThat(jdbc.queryForObject("select count(*) from information_schema.table_constraints where table_schema='public' and constraint_type='FOREIGN KEY'",Long.class)).isEqualTo(4);
    }
    @Test void genreIsStoredAsTextAndGeneratedIdsAndTimestampsExist(){
        assertThat(jdbc.queryForObject("select genre from book where title='Effective Java'",String.class)).isEqualTo("PROGRAMMING");
        assertThat(effective().id()).isPositive();assertThat(effective().createdAt()).isNotNull();
    }
    @Test void derivedGenreReturnsTheSeedBooks(){assertThat(books.findByGenre(Genre.PROGRAMMING)).hasSize(8);}
    @Test void derivedTitleSearchIgnoresCase(){assertThat(books.findByTitleContainingIgnoreCase("jAvA")).extracting(Book::title).containsExactlyInAnyOrder("Effective Java","Java Puzzlers");}
    @Test void derivedContainsEscapesLiteralSqlWildcards(){assertThat(books.findByTitleContainingIgnoreCase("%" )).isEmpty();assertThat(books.findByTitleContainingIgnoreCase("_")).isEmpty();}
    @Test void derivedYearRangeHasDescendingOrder(){
        List<Book> result=books.findByYearBetweenOrderByYearDesc(2005,2022);
        assertThat(result).hasSize(7);assertThat(result.stream().map(Book::year).toList()).isSortedAccordingTo(Comparator.reverseOrder());
    }
    @Test void derivedTopThreeLimitsAndSortsAtTheDatabase(){assertThat(books.findTop3ByGenreOrderByYearDesc(Genre.PROGRAMMING)).extracting(Book::year).containsExactly(2022,2018,2017);}
    @Test void nestedAuthorPropertyCountReturnsThree(){assertThat(books.countByAuthorName("Joshua Bloch")).isEqualTo(3);}
    @Test void namedJpqlParametersApplyBothConditionsAndTitleOrder(){
        List<Book> result=books.search(Genre.PROGRAMMING,2016);
        assertThat(result).hasSize(5);assertThat(result).allMatch(b->b.year()>=2016);
        assertThat(result.stream().map(Book::title).toList()).isSorted();
    }
    @Test void bulkUpdateUsesOneStatementClearsContextAndAdvancesVersions(){
        Book old=books.findByTitleContainingIgnoreCase("Clean Code").getFirst();long id=old.id(),version=old.getVersion();
        var result=evidence.archive(2010);
        assertThat(result.result()).isEqualTo(3);assertThat(result.sqlCount()).isEqualTo(1);
        assertThat(em.contains(old)).isFalse();
        Book reloaded=books.findById(id).orElseThrow();assertThat(reloaded.isArchived()).isTrue();assertThat(reloaded.getVersion()).isEqualTo(version+1);
        assertThat(evidence.archive(2010).result()).isEqualTo(0);
    }
    @Test void lazyAuthorLoadingIsMeasuredBeforeAndAfterFetchJoin(){
        var result=evidence.nPlusOne();assertThat(result.get("before").sqlCount()).isEqualTo(4);
        assertThat(result.get("after").sqlCount()).isEqualTo(1);
        assertThat(result.get("after").result()).isEqualTo(result.get("before").result());
    }
    @Test void pageCountsTotalWhileSliceUsesOneStatement(){
        var result=evidence.paging();assertThat(result.get("page").sqlCount()).isEqualTo(2);
        assertThat(result.get("slice").sqlCount()).isEqualTo(1);
        assertThat(result.get("page").sql().stream().anyMatch(s->s.toLowerCase().contains("count("))).isTrue();
        assertThat(result.get("slice").sql().stream().noneMatch(s->s.toLowerCase().contains("count("))).isTrue();
    }
    @Test void pageHasRealTotalAndCorrectSecondPageOrder(){
        Page<Book> page=service.find(null,null,null,PageRequest.of(1,3,Sort.by(Sort.Direction.DESC,"year")));
        assertThat(page.getTotalElements()).isEqualTo(8);assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getContent()).extracting(Book::year).containsExactly(2016,2016,2008);
    }
    @Test void outOfRangePageKeepsTheTotal(){
        Page<Book> page=service.find(null,null,null,PageRequest.of(99,3));
        assertThat(page.getContent()).isEmpty();assertThat(page.getTotalElements()).isEqualTo(8);
    }
    @Test void publisherVariantUsesDerivedCaseInsensitiveQuery(){
        assertThat(books.findByPublisherNameIgnoreCase("mAnNiNg")).hasSize(3).allMatch(b->b.getPublisher().getName().equals("Manning"));
        assertThat(books.findByPublisherNameIgnoreCase("Addison-Wesley")).hasSize(5);
    }
    @Test void helperWritesTheOwningAuthorAndPublisherForeignKeys(){
        Author author=new Author("Example Writer","KZ");Publisher publisher=publishers.findByNameKey("manning").orElseThrow();
        Book book=new Book("Association example",Genre.SCIENCE,2025);
        author.addBook(book);publisher.addBook(book);authors.saveAndFlush(author);long id=book.id(),authorId=author.getId();em.clear();
        assertThat(jdbc.queryForObject("select author_id from book where id=?",Long.class,id)).isEqualTo(authorId);
        assertThat(books.findById(id).orElseThrow().getPublisher().getName()).isEqualTo("Manning");
    }
    @Test void removingAnOrphanDeletesOnlyTheBook(){
        Author author=authors.findByNameKey("joshua bloch").orElseThrow();Book book=author.getBooks().getFirst();long id=book.id();
        author.removeBook(book);em.flush();em.clear();assertThat(books.findById(id)).isEmpty();
        assertThat(tags.count()).isEqualTo(4);assertThat(authors.count()).isEqualTo(3);
    }
    @Test void authorCascadeDeletesItsBooksButPreservesSharedTagsAndPublishers(){
        Author author=authors.findByNameKey("joshua bloch").orElseThrow();authors.delete(author);em.flush();em.clear();
        assertThat(books.count()).isEqualTo(5);assertThat(tags.count()).isEqualTo(4);assertThat(publishers.count()).isEqualTo(2);
    }
    @Test void removingATagLinkKeepsTheSharedTag(){
        Book book=effective();Tag tag=book.getTags().iterator().next();book.removeTag(tag);em.flush();em.clear();
        assertThat(books.findById(book.id()).orElseThrow().getTags()).hasSize(1);assertThat(tags.count()).isEqualTo(4);
    }
    @Test void dirtyCheckingUpdatesManagedBookWithoutCallingSave(){
        Book book=effective();long version=book.getVersion();var created=book.createdAt();book.setTitle("Effective Java updated");em.flush();em.clear();
        Book loaded=books.findById(book.id()).orElseThrow();assertThat(loaded.title()).isEqualTo("Effective Java updated");
        assertThat(loaded.getVersion()).isEqualTo(version+1);assertThat(loaded.createdAt()).isEqualTo(created);
    }
    @Test void serviceIgnoresClientMetadataAndRejectsCaseInsensitiveDuplicates(){
        Book saved=service.create(new Book(1L,"  Persistent Book  ","Example Writer",2025));
        assertThat(saved.id()).isGreaterThan(8);assertThat(saved.title()).isEqualTo("Persistent Book");assertThat(saved.createdAt()).isNotNull();
        assertThatThrownBy(()->service.create(new Book(null," EFFECTIVE JAVA "," JOSHUA BLOCH ",2025))).isInstanceOf(DuplicateTitleException.class);
    }
    @Test void replacementRetainsIdentityTimestampPublisherAndTags(){
        Book before=effective();var created=before.createdAt();int labels=before.getTags().size();long id=before.id();
        Book after=service.update(id,new Book(null,"Revised Title","New Writer",2025));
        assertThat(after.id()).isEqualTo(id);assertThat(after.createdAt()).isEqualTo(created);assertThat(after.author()).isEqualTo("New Writer");
        assertThat(after.getPublisher().getName()).isEqualTo("Addison-Wesley");assertThat(after.getTags()).hasSize(labels);
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void optimisticVersionRejectsAStaleConcurrentUpdate(){
        long id;EntityManager setup=factory.createEntityManager();
        try{setup.getTransaction().begin();Book book=new Book("Concurrent sample",Genre.PROGRAMMING,2025);
            book.setAuthor(setup.createQuery("select a from Author a order by a.id",Author.class).setMaxResults(1).getSingleResult());
            book.setPublisher(setup.createQuery("select p from Publisher p order by p.id",Publisher.class).setMaxResults(1).getSingleResult());
            setup.persist(book);setup.getTransaction().commit();id=book.id();}finally{setup.close();}
        EntityManager first=factory.createEntityManager(),second=factory.createEntityManager();
        try{first.getTransaction().begin();second.getTransaction().begin();Book one=first.find(Book.class,id),two=second.find(Book.class,id);
            one.setTitle("First concurrent update");first.getTransaction().commit();two.setTitle("Stale concurrent update");
            assertThatThrownBy(()->second.getTransaction().commit()).isInstanceOf(RollbackException.class);
        }finally{if(second.getTransaction().isActive())second.getTransaction().rollback();first.close();second.close();
            EntityManager cleanup=factory.createEntityManager();try{cleanup.getTransaction().begin();cleanup.remove(cleanup.find(Book.class,id));cleanup.getTransaction().commit();}finally{cleanup.close();}}
    }
}
