package kz.iitu.springlab.catalog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PublisherRepository extends JpaRepository<Publisher,Long> { Optional<Publisher> findByNameKey(String nameKey); }
