package kz.iitu.txlab.domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AccountRepository extends JpaRepository<Account,Long> {
    List<Account> findByOwnerNameInOrderByIdAsc(List<String> names);
}
