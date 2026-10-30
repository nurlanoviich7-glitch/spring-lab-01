package kz.iitu.txlab.service;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class IsolationService {
    private final PlatformTransactionManager manager;private final JdbcTemplate jdbc;
    public IsolationService(PlatformTransactionManager manager,JdbcTemplate jdbc){this.manager=manager;this.jdbc=jdbc;}
    public Map<String,Object> observe(long accountId,boolean repeatableRead){
        TransactionTemplate reader=new TransactionTemplate(manager);
        reader.setReadOnly(true);reader.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        reader.setIsolationLevel(repeatableRead?TransactionDefinition.ISOLATION_REPEATABLE_READ:TransactionDefinition.ISOLATION_READ_COMMITTED);
        Map<String,Object> result=reader.execute(status->{
            Map<String,Object> values=new LinkedHashMap<>();
            values.put("isolation",jdbc.queryForObject("show transaction_isolation",String.class));
            values.put("readerConnection",jdbc.queryForObject("select pg_backend_pid()",Integer.class));
            values.put("firstRead",balance(accountId));
            TransactionTemplate writer=new TransactionTemplate(manager);
            writer.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            writer.executeWithoutResult(s->{
                values.put("writerConnection",jdbc.queryForObject("select pg_backend_pid()",Integer.class));
                jdbc.update("update accounts set balance=balance+100.00,version=version+1 where id=?",accountId);
            }); // Writer commits while the reader's transaction remains open.
            values.put("secondRead",balance(accountId));return values;
        });
        result.put("committedBalance",balance(accountId));
        result.put("changedOnSecondRead",!result.get("firstRead").equals(result.get("secondRead")));
        result.put("method","Two independent transactions; repeated JDBC SELECT avoids the JPA first-level cache");
        return result;
    }
    private BigDecimal balance(long id){return jdbc.queryForObject("select balance from accounts where id=?",BigDecimal.class,id);}
}
