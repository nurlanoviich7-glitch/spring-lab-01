package kz.iitu.txlab;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import kz.iitu.txlab.domain.Account;
import kz.iitu.txlab.service.BusinessConflict;
import static org.assertj.core.api.Assertions.*;
class AccountTest {
    @ParameterizedTest @ValueSource(strings={"0","-1","0.001"})
    void invalidDebitIsRejected(String value){
        Account a=new Account("Alice",new BigDecimal("1000.00"));
        assertThatThrownBy(()->a.debit(new BigDecimal(value))).isInstanceOf(IllegalArgumentException.class);
        assertThat(a.getBalance()).isEqualByComparingTo("1000.00");
    }
    @Test void moneyUsesDecimalArithmetic(){
        Account a=new Account("Alice",new BigDecimal("0.30"));a.debit(new BigDecimal("0.10"));
        assertThat(a.getBalance()).isEqualByComparingTo("0.20");
    }
    @Test void insufficientDebitDoesNotChangeAccount(){
        Account a=new Account("Alice",new BigDecimal("10.00"));
        assertThatThrownBy(()->a.debit(new BigDecimal("11.00"))).isInstanceOf(BusinessConflict.class);
        assertThat(a.getBalance()).isEqualByComparingTo("10.00");
    }
    @Test void frozenAccountCannotAcceptCredit(){
        Account a=new Account("Alice",new BigDecimal("10.00"));a.freeze();
        assertThatThrownBy(()->a.credit(BigDecimal.ONE)).isInstanceOf(BusinessConflict.class);
    }
    @Test void invalidOpeningBalanceIsRejected(){assertThatThrownBy(()->new Account("Alice",new BigDecimal("-1"))).isInstanceOf(IllegalArgumentException.class);}
    @Test void nullAmountIsRejected(){
        Account a=new Account("Alice",BigDecimal.ZERO);assertThatThrownBy(()->a.credit(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
