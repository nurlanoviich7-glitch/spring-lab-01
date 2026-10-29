package kz.iitu.springlab.catalog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface TagRepository extends JpaRepository<Tag,Long> {  }
