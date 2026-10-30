package kz.iitu.txlab.web;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import kz.iitu.txlab.service.*;

@RestController
public class TransactionController {
    private final AccountService accounts;private final TransferService transfers;
    private final ExperimentService experiments;private final JdbcTemplate jdbc;
    public TransactionController(AccountService a,TransferService t,ExperimentService e,JdbcTemplate j){accounts=a;transfers=t;experiments=e;jdbc=j;}
    public record TransferRequest(@Positive long fromId,@Positive long toId,
        @NotNull @DecimalMin(value="0.00",inclusive=false) @Digits(integer=17,fraction=2) BigDecimal amount) { }
    public record BalanceRequest(@NotNull @DecimalMin("0.00") @Digits(integer=17,fraction=2) BigDecimal balance,
        @NotNull @Min(0) Long expectedVersion) { }
    @GetMapping({"/","/lab9","/lab9/"}) public ResponseEntity<Void> home(){return ResponseEntity.status(302).location(URI.create("/lab9/index.html")).build();}
    @GetMapping("/api/accounts") public List<AccountResponse> list(){return accounts.seedAccounts().stream().map(AccountResponse::of).toList();}
    @GetMapping("/api/accounts/{id}") public AccountResponse get(@PathVariable long id){return AccountResponse.of(accounts.require(id));}
    @PostMapping("/api/transfers") public Map<String,Object> transfer(@Valid @RequestBody TransferRequest request){
        transfers.transfer(request.fromId(),request.toId(),request.amount());
        return Map.of("status","committed","accounts",List.of(get(request.fromId()),get(request.toId())));
    }
    @PutMapping("/api/accounts/{id}/balance")
    public AccountResponse replace(@PathVariable long id,@Valid @RequestBody BalanceRequest request){
        return AccountResponse.of(accounts.replaceBalance(id,request.balance(),request.expectedVersion()));
    }
    @PostMapping("/api/experiments/{name}") public Map<String,Object> experiment(@PathVariable String name){return experiments.run(name);}
    @GetMapping("/api/migrations") public Map<String,Object> migrations(){
        return Map.of("database",jdbc.queryForMap("select version() as server,current_database() as database,current_user as username"),
            "hibernateDdl","validate","tool","Flyway",
            "history",jdbc.queryForList("select installed_rank,version,description,script,checksum,installed_on,success from flyway_schema_history order by installed_rank"),
            "indexes",jdbc.queryForList("select indexname,indexdef from pg_indexes where schemaname=current_schema() and tablename='accounts' order by indexname"));
    }
}
