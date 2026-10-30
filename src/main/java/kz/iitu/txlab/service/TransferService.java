package kz.iitu.txlab.service;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import kz.iitu.txlab.domain.*;

@Service
public class TransferService {
    private final AccountRepository repository;
    public TransferService(AccountRepository repository){this.repository=repository;}
    private Account require(long id){return repository.findById(id).orElseThrow(()->new MissingAccount(id));}
    private void distinct(long from,long to){if(from==to)throw new IllegalArgumentException("Choose different accounts");}

    @Transactional
    public void transfer(long fromId,long toId,BigDecimal amount){
        distinct(fromId,toId);
        Account from=require(fromId),to=require(toId);
        from.debit(amount);
        to.credit(amount);
        repository.flush(); // Managed entities are persisted by dirty checking.
    }

    // The following three methods are called only by isolated classroom experiments.
    @Transactional
    public void uncheckedAfterDebit(long fromId,long toId,BigDecimal amount){
        distinct(fromId,toId);Account from=require(fromId);require(toId);
        from.debit(amount);repository.flush();
        throw new IllegalStateException("Unchecked exception after the flushed debit");
    }

    @Transactional
    public void checkedDefault(long fromId,long toId,BigDecimal amount) throws CheckedTransferException {
        distinct(fromId,toId);Account from=require(fromId);require(toId);
        from.debit(amount);repository.flush();throw new CheckedTransferException();
    }

    @Transactional(rollbackFor=CheckedTransferException.class)
    public void checkedWithRollback(long fromId,long toId,BigDecimal amount) throws CheckedTransferException {
        distinct(fromId,toId);Account from=require(fromId);require(toId);
        from.debit(amount);repository.flush();throw new CheckedTransferException();
    }
}
