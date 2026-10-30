package kz.iitu.txlab.service;
import java.math.BigDecimal;
import java.util.*;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import kz.iitu.txlab.domain.*;

@Service
public class ExperimentService {
    private static final Logger log=LoggerFactory.getLogger(ExperimentService.class);
    private final AccountService accounts;private final TransferService transfers;private final PropagationService propagation;
    private final AuditRepository audits;private final IsolationService isolation;private final OptimisticLockService locking;
    public ExperimentService(AccountService a,TransferService t,PropagationService p,AuditRepository r,IsolationService i,OptimisticLockService l){
        accounts=a;transfers=t;propagation=p;audits=r;isolation=i;locking=l;
    }
    public Map<String,Object> run(String name){
        if(!Set.of("success","insufficient","unchecked","checked-default","checked-rollback","required","requires-new","read-committed","repeatable-read","optimistic-lock").contains(name))
            throw new IllegalArgumentException("Unknown experiment");
        String run=UUID.randomUUID().toString();
        Account a=accounts.create("Experiment "+run+" A",new BigDecimal("1000.00"));
        Account b=accounts.create("Experiment "+run+" B",new BigDecimal("500.00"));
        Map<String,Object> result=new LinkedHashMap<>();result.put("experiment",name);result.put("runId",run);
        result.put("accountIds",List.of(a.getId(),b.getId()));result.put("before",balances(a.getId(),b.getId()));
        String message="Committed";
        try {
            switch(name){
                case "success" -> transfers.transfer(a.getId(),b.getId(),new BigDecimal("100.00"));
                case "insufficient" -> transfers.transfer(a.getId(),b.getId(),new BigDecimal("1001.00"));
                case "unchecked" -> transfers.uncheckedAfterDebit(a.getId(),b.getId(),new BigDecimal("100.00"));
                case "checked-default" -> transfers.checkedDefault(a.getId(),b.getId(),new BigDecimal("100.00"));
                case "checked-rollback" -> transfers.checkedWithRollback(a.getId(),b.getId(),new BigDecimal("100.00"));
                case "required","requires-new" -> propagation.outer(a.getId(),run,name.equals("requires-new"));
                case "read-committed","repeatable-read" -> result.put("isolation",isolation.observe(a.getId(),name.equals("repeatable-read")));
                case "optimistic-lock" -> {
                    result.put("locking",locking.conflict(a.getId()));
                    message="First update committed; stale update rolled back";
                }
                default -> throw new IllegalArgumentException("Unknown experiment");
            }
        } catch(OuterRollbackException e){
            message=e.getMessage();result.put("outerTransaction",e.outerTransaction);result.put("auditTransaction",e.auditTransaction);
        } catch(CheckedTransferException|IllegalStateException|BusinessConflict e){message=e.getClass().getSimpleName()+": "+e.getMessage();}
        result.put("outcome",message);result.put("after",balances(a.getId(),b.getId()));
        result.put("auditRows",audits.countByRunId(run));
        result.put("database","PostgreSQL");
        log.info("LAB09 experiment={} before={} after={} auditRows={} outcome={}",name,result.get("before"),result.get("after"),result.get("auditRows"),message);
        return result;
    }
    private List<BigDecimal> balances(long a,long b){return List.of(accounts.require(a).getBalance(),accounts.require(b).getBalance());}
}
