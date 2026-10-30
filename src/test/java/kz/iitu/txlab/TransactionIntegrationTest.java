package kz.iitu.txlab;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import kz.iitu.txlab.domain.*;
import kz.iitu.txlab.service.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class TransactionIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ExperimentService experiments;
    @Autowired AccountService accounts;
    @Autowired TransferService transfers;
    @Autowired AccountRepository repository;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @BeforeEach void resetOnlyTestData(){
        assertThat(jdbc.queryForObject("select current_database()",String.class)).endsWith("_test");
        jdbc.update("delete from audit_records");
        jdbc.update("delete from accounts where owner_name like 'Experiment %'");
        jdbc.update("update accounts set balance=1000.00,version=0,status='ACTIVE' where owner_name='Alice'");
        jdbc.update("update accounts set balance=500.00,version=0,status='ACTIVE' where owner_name='Bob'");
    }
    private BigDecimal balance(long id){return jdbc.queryForObject("select balance from accounts where id=?",BigDecimal.class,id);}
    private long version(long id){return jdbc.queryForObject("select version from accounts where id=?",Long.class,id);}
    @ParameterizedTest
    @CsvSource({"success,900.00,600.00","insufficient,1000.00,500.00","unchecked,1000.00,500.00",
        "checked-default,900.00,500.00","checked-rollback,1000.00,500.00"})
    void rollbackRulesAreObservedInFreshDatabaseReads(String mode,String expectedA,String expectedB){
        Map<String,Object> result=experiments.run(mode);
        List<?> ids=(List<?>)result.get("accountIds");
        long a=((Number)ids.get(0)).longValue(),b=((Number)ids.get(1)).longValue();
        assertThat(balance(a)).isEqualByComparingTo(expectedA);assertThat(balance(b)).isEqualByComparingTo(expectedB);
        boolean committed=mode.equals("success")||mode.equals("checked-default");
        assertThat(version(a)).isEqualTo(committed?1:0);
        if(mode.equals("success"))assertThat(balance(a).add(balance(b))).isEqualByComparingTo("1500.00");
    }
    @ParameterizedTest @ValueSource(strings={"required","requires-new"})
    void propagationControlsWhetherTheAuditSurvives(String mode){
        Map<String,Object> result=experiments.run(mode);
        boolean independent=mode.equals("requires-new");
        long auditCount=jdbc.queryForObject("select count(*) from audit_records where run_id=?",Long.class,result.get("runId"));
        assertThat(auditCount).isEqualTo(independent?1:0);
        assertThat(result.get("outerTransaction").equals(result.get("auditTransaction"))).isEqualTo(!independent);
        long a=((Number)((List<?>)result.get("accountIds")).get(0)).longValue();
        assertThat(balance(a)).isEqualByComparingTo("1000.00");assertThat(version(a)).isZero();
    }
    @ParameterizedTest @ValueSource(strings={"read-committed","repeatable-read"})
    void isolationUsesIndependentConnectionsAndDatabaseReads(String mode){
        Map<String,Object> result=experiments.run(mode);
        Map<?,?> observation=(Map<?,?>)result.get("isolation");
        assertThat(observation.get("readerConnection")).isNotEqualTo(observation.get("writerConnection"));
        assertThat((BigDecimal)observation.get("firstRead")).isEqualByComparingTo("1000.00");
        assertThat((BigDecimal)observation.get("secondRead")).isEqualByComparingTo(mode.equals("read-committed")?"1100.00":"1000.00");
        assertThat((BigDecimal)observation.get("committedBalance")).isEqualByComparingTo("1100.00");
        assertThat(observation.get("isolation")).isEqualTo(mode.replace('-',' '));
    }
    @Test void staleEntityCannotOverwriteTheFirstCommit(){
        Map<String,Object> result=experiments.run("optimistic-lock");
        Map<?,?> lock=(Map<?,?>)result.get("locking");
        assertThat(lock.get("conflict")).isEqualTo(true);
        assertThat(lock.get("firstLoadedVersion")).isEqualTo(0L);
        assertThat(lock.get("staleLoadedVersion")).isEqualTo(0L);
        assertThat(lock.get("firstCommittedVersion")).isEqualTo(1L);
        assertThat((BigDecimal)lock.get("finalBalance")).isEqualByComparingTo("1100.00");
        long a=((Number)((List<?>)result.get("accountIds")).get(0)).longValue();
        assertThat(balance(a)).isEqualByComparingTo("1100.00");assertThat(version(a)).isEqualTo(1);
    }
    @Test void successfulTransferChangesBothBalancesAndVersions(){
        transfers.transfer(1,2,new BigDecimal("100.00"));
        assertThat(balance(1)).isEqualByComparingTo("900.00");assertThat(balance(2)).isEqualByComparingTo("600.00");
        assertThat(version(1)).isEqualTo(1);assertThat(version(2)).isEqualTo(1);
    }
    @Test void creditFailureRollsBackTheEarlierDebit(){
        jdbc.update("update accounts set status='FROZEN' where id=2");
        assertThatThrownBy(()->transfers.transfer(1,2,new BigDecimal("100.00"))).isInstanceOf(BusinessConflict.class);
        assertThat(balance(1)).isEqualByComparingTo("1000.00");assertThat(balance(2)).isEqualByComparingTo("500.00");
    }
    @Test void sameAccountIsRejectedWithoutChangingMoney(){
        assertThatThrownBy(()->transfers.transfer(1,1,new BigDecimal("100.00"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(balance(1)).isEqualByComparingTo("1000.00");
    }
    @Test void missingDestinationCannotDebitTheSource(){
        assertThatThrownBy(()->transfers.transfer(1,999999,new BigDecimal("100.00"))).isInstanceOf(MissingAccount.class);
        assertThat(balance(1)).isEqualByComparingTo("1000.00");
    }
    @Test void successfulTransferHasAConcreteHttpResponse() throws Exception {
        mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON)
            .content("{\"fromId\":1,\"toId\":2,\"amount\":100.00}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("committed"))
            .andExpect(jsonPath("$.accounts[0].balance").value(900.0)).andExpect(jsonPath("$.accounts[1].balance").value(600.0));
    }
    @ParameterizedTest @ValueSource(strings={"0","-10","0.001","100000000000000000.00","null"})
    void invalidAmountsReturn400WithoutChangingBalances(String amount) throws Exception {
        mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON)
            .content("{\"fromId\":1,\"toId\":2,\"amount\":"+amount+"}"))
            .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        assertThat(balance(1)).isEqualByComparingTo("1000.00");assertThat(balance(2)).isEqualByComparingTo("500.00");
    }
    @Test void insufficientFundsReturn409WithoutPartialUpdate() throws Exception {
        mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON)
            .content("{\"fromId\":1,\"toId\":2,\"amount\":1001.00}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Insufficient funds"));
        assertThat(balance(1)).isEqualByComparingTo("1000.00");
    }
    @Test void missingAccountReturns404() throws Exception {mvc.perform(get("/api/accounts/999999")).andExpect(status().isNotFound());}
    @Test void invalidJsonReturns400() throws Exception {mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content("{" )).andExpect(status().isBadRequest());}
    @Test void staleHttpUpdateIs409AndDoesNotOverwriteFirstUpdate() throws Exception {
        mvc.perform(put("/api/accounts/1/balance").contentType(MediaType.APPLICATION_JSON)
            .content("{\"balance\":1100.00,\"expectedVersion\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(put("/api/accounts/1/balance").contentType(MediaType.APPLICATION_JSON)
            .content("{\"balance\":1050.00,\"expectedVersion\":0}"))
            .andExpect(status().isConflict());
        assertThat(balance(1)).isEqualByComparingTo("1100.00");assertThat(version(1)).isEqualTo(1);
    }
    @Test void negativeReplacementCannotCreateANegativeBalance() throws Exception {
        mvc.perform(put("/api/accounts/1/balance").contentType(MediaType.APPLICATION_JSON)
            .content("{\"balance\":-1,\"expectedVersion\":0}" )).andExpect(status().isBadRequest());
        assertThat(balance(1)).isEqualByComparingTo("1000.00");
    }
    @Test void experimentAccountsDoNotChangeAliceOrBob(){
        experiments.run("success");assertThat(balance(1)).isEqualByComparingTo("1000.00");assertThat(balance(2)).isEqualByComparingTo("500.00");
    }
    @Test void allFourMigrationsAppliedAndStatusMapped() throws Exception {
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success=true and version is not null",Integer.class)).isEqualTo(4);
        assertThat(accounts.require(1).getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(jdbc.queryForObject("select count(*) from pg_indexes where schemaname='public' and indexname='idx_accounts_owner_name'",Integer.class)).isEqualTo(1);
        mvc.perform(get("/api/migrations")).andExpect(status().isOk()).andExpect(jsonPath("$.hibernateDdl").value("validate"));
    }
    @Test void unknownExperimentIs400AndCreatesNoRows() throws Exception {
        long before=repository.count();mvc.perform(post("/api/experiments/unknown")).andExpect(status().isBadRequest());
        assertThat(repository.count()).isEqualTo(before);
    }
    @Test void homepageIsAvailable() throws Exception {
        mvc.perform(get("/lab9")).andExpect(status().isFound()).andExpect(header().string("Location","/lab9/index.html"));
        mvc.perform(get("/lab9/")).andExpect(status().isFound()).andExpect(header().string("Location","/lab9/index.html"));
        mvc.perform(get("/lab9/index.html")).andExpect(status().isOk());
    }
}
