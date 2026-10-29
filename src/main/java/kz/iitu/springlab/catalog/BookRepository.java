package kz.iitu.springlab.catalog;
import kz.iitu.springlab.web.Genre;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface BookRepository extends JpaRepository<Book,Long> {
    List<Book> findByGenre(Genre genre);
    List<Book> findByTitleContainingIgnoreCase(String fragment);
    List<Book> findByYearBetweenOrderByYearDesc(int from,int to);
    List<Book> findTop3ByGenreOrderByYearDesc(Genre genre);
    long countByAuthorName(String name);
    @Query("select b from Book b where b.genre=:genre and b.year>=:year order by b.title")
    List<Book> search(@Param("genre") Genre genre,@Param("year") int year);
    @Modifying(clearAutomatically=true,flushAutomatically=true)
    @Query("update Book b set b.archived=true, b.version=b.version+1 where b.year<:year and b.archived=false")
    int archiveOlderThan(@Param("year") int year);
    @Query("select b from Book b join fetch b.author") List<Book> findAllWithAuthor();
    @EntityGraph(attributePaths={"author","publisher"})
    Page<Book> findByGenre(Genre genre,Pageable pageable);
    // Variant 1 traverses Book.publisher.name and derives its join.
    @EntityGraph(attributePaths={"author","publisher"})
    List<Book> findByPublisherNameIgnoreCase(String name);
    @EntityGraph(attributePaths={"author","publisher"}) List<Book> findAllByOrderByIdAsc();
    @Override @EntityGraph(attributePaths={"author","publisher"}) Optional<Book> findById(Long id);
    @EntityGraph(attributePaths={"author","publisher"}) Page<Book> findAllBy(Pageable pageable);
    @EntityGraph(attributePaths={"author","publisher"}) Slice<Book> findSliceBy(Pageable pageable);
    @EntityGraph(attributePaths={"author","publisher"})
    @Query(value="""
            select b from Book b
            where (:genre is null or b.genre=:genre)
              and (:author is null or locate(lower(cast(:author as string)),lower(b.author.name))>0)
              and (:publisher is null or lower(b.publisher.name)=lower(cast(:publisher as string)))
            """,countQuery="""
            select count(b) from Book b
            where (:genre is null or b.genre=:genre)
              and (:author is null or locate(lower(cast(:author as string)),lower(b.author.name))>0)
              and (:publisher is null or lower(b.publisher.name)=lower(cast(:publisher as string)))
            """)
    Page<Book> findFiltered(@Param("genre") Genre genre,@Param("author") String author,
                            @Param("publisher") String publisher,Pageable pageable);
    boolean existsByTitleKeyAndAuthorNameKey(String titleKey,String authorKey);
    boolean existsByTitleKeyAndAuthorNameKeyAndIdNot(String titleKey,String authorKey,long id);
}
