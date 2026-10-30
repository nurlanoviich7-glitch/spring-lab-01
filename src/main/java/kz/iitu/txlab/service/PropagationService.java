package kz.iitu.txlab.service;
import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import kz.iitu.txlab.domain.*;

@Service
public class PropagationService {
    private final AuditService audit;private final AccountRepository accounts;private final JdbcTemplate jdbc;
    public PropagationService(AuditService audit,AccountRepository accounts,JdbcTemplate jdbc){
        this.audit=audit;this.accounts=accounts;this.jdbc=jdbc;
    }
    @Transactional
    public void outer(long accountId,String runId,boolean independentAudit){
        long outerId=jdbc.queryForObject("select txid_current()",Long.class);
        Account account=accounts.findById(accountId).orElseThrow();
        account.debit(new BigDecimal("100.00"));accounts.flush();
        // A separate Spring bean ensures this call crosses a transactional proxy.
        long auditId=independentAudit?audit.requiresNew(runId):audit.required(runId);
        throw new OuterRollbackException(outerId,auditId);
    }
}
