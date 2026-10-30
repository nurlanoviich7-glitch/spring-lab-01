package kz.iitu.txlab.domain;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditRepository extends JpaRepository<AuditRecord,Long> {
    long countByRunId(String runId);
}
