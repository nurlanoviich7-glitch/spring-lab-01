package kz.iitu.txlab.service;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import kz.iitu.txlab.domain.*;

@Service @Transactional(readOnly=true)
public class AccountService {
    private final AccountRepository repository;
    public AccountService(AccountRepository repository){this.repository=repository;}
    public List<Account> seedAccounts(){return repository.findByOwnerNameInOrderByIdAsc(List.of("Alice","Bob"));}
    public Account require(long id){return repository.findById(id).orElseThrow(()->new MissingAccount(id));}
    @Transactional public Account create(String owner,BigDecimal balance){return repository.saveAndFlush(new Account(owner,balance));}
    @Transactional public Account replaceBalance(long id,BigDecimal value,long expectedVersion){
        Account account=require(id);
        if(account.getVersion()!=expectedVersion)throw new BusinessConflict("Stale version: reload the account before updating");
        account.replaceBalance(value);repository.flush();return account;
    }
}
