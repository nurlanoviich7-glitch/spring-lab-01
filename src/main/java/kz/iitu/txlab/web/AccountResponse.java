package kz.iitu.txlab.web;
import java.math.BigDecimal;
import kz.iitu.txlab.domain.Account;
public record AccountResponse(long id,String ownerName,BigDecimal balance,long version,String status) {
    public static AccountResponse of(Account a){return new AccountResponse(a.getId(),a.getOwnerName(),a.getBalance(),a.getVersion(),a.getStatus().name());}
}
