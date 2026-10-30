package kz.iitu.txlab.service;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import kz.iitu.txlab.domain.Account;

@Service
public class OptimisticLockService {
    private final EntityManagerFactory factory;
    public OptimisticLockService(EntityManagerFactory factory){this.factory=factory;}
    public Map<String,Object> conflict(long accountId){
        EntityManager first=factory.createEntityManager(),second=factory.createEntityManager();
        Map<String,Object> result=new LinkedHashMap<>();
        try {
            first.getTransaction().begin();second.getTransaction().begin();
            Account a=first.find(Account.class,accountId),b=second.find(Account.class,accountId);
            result.put("firstLoadedVersion",a.getVersion());result.put("staleLoadedVersion",b.getVersion());
            a.credit(new BigDecimal("100.00"));first.getTransaction().commit();
            result.put("firstCommittedVersion",a.getVersion());
            b.credit(new BigDecimal("50.00"));
            try {second.getTransaction().commit();throw new IllegalStateException("Expected a stale update conflict");}
            catch(PersistenceException exception){
                Throwable cause=exception;
                while(cause.getCause()!=null)cause=cause.getCause();
                result.put("conflict",true);result.put("exception",exception.getClass().getSimpleName());
                result.put("rootCause",cause.getClass().getSimpleName());
            }
            EntityManager fresh=factory.createEntityManager();
            try {Account stored=fresh.find(Account.class,accountId);result.put("finalBalance",stored.getBalance());result.put("finalVersion",stored.getVersion());}
            finally {fresh.close();}
            result.put("safeResponse","Reload current state and report HTTP 409; do not blindly retry a transfer");
            return result;
        } finally {
            if(first.getTransaction().isActive())first.getTransaction().rollback();
            if(second.getTransaction().isActive())second.getTransaction().rollback();
            first.close();second.close();
        }
    }
}
