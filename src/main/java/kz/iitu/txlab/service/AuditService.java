package kz.iitu.txlab.service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import kz.iitu.txlab.domain.*;

@Service
public class AuditService {
    private final AuditRepository audits;private final JdbcTemplate jdbc;
    public AuditService(AuditRepository audits,JdbcTemplate jdbc){this.audits=audits;this.jdbc=jdbc;}
    @Transactional(propagation=Propagation.REQUIRED)
    public long required(String runId){return write(runId,"REQUIRED");}
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public long requiresNew(String runId){return write(runId,"REQUIRES_NEW");}
    private long write(String runId,String action){
        long id=jdbc.queryForObject("select txid_current()",Long.class);
        audits.saveAndFlush(new AuditRecord(runId,action,id));return id;
    }
}
