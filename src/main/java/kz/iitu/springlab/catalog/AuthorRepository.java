package kz.iitu.springlab.catalog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AuthorRepository extends JpaRepository<Author,Long> { Optional<Author> findByNameKey(String nameKey); }
