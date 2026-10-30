package kz.iitu.txlab.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import kz.iitu.txlab.service.BusinessConflict;

@Entity
@Table(name="accounts")
public class Account {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="owner_name",nullable=false,length=120)
    private String ownerName;
    @Column(nullable=false,precision=19,scale=2)
    private BigDecimal balance;
    @Version @Column(nullable=false)
    private long version;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20)
    private AccountStatus status=AccountStatus.ACTIVE;

    protected Account() { }
    public Account(String ownerName,BigDecimal balance) {
        if(ownerName==null||ownerName.isBlank()||ownerName.length()>120)
            throw new IllegalArgumentException("Owner name must contain 1 to 120 characters");
        this.ownerName=ownerName;this.balance=nonNegative(balance);
    }
    public void debit(BigDecimal amount) {
        requireActive();amount=positive(amount);
        if(balance.compareTo(amount)<0)throw new BusinessConflict("Insufficient funds");
        balance=balance.subtract(amount);
    }
    public void credit(BigDecimal amount) { requireActive();balance=balance.add(positive(amount)); }
    public void replaceBalance(BigDecimal value) { requireActive();balance=nonNegative(value); }
    public void freeze(){status=AccountStatus.FROZEN;}
    private void requireActive(){if(status!=AccountStatus.ACTIVE)throw new BusinessConflict("Account is not active");}
    private static BigDecimal nonNegative(BigDecimal value) {
        if(value==null||value.signum()<0)throw new IllegalArgumentException("Balance must be non-negative");
        try {value=value.setScale(2,RoundingMode.UNNECESSARY);}
        catch(ArithmeticException e){throw new IllegalArgumentException("Use at most two decimal places");}
        if(value.precision()>19)throw new IllegalArgumentException("Amount exceeds decimal(19,2)");
        return value;
    }
    private static BigDecimal positive(BigDecimal value){
        value=nonNegative(value);
        if(value.signum()==0)throw new IllegalArgumentException("Amount must be positive");
        return value;
    }
    public Long getId(){return id;} public String getOwnerName(){return ownerName;}
    public BigDecimal getBalance(){return balance;} public long getVersion(){return version;}
    public AccountStatus getStatus(){return status;}
}
