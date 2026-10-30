package kz.iitu.txlab;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test")
class MigrationLifecycleTest {
    @Autowired DataSource datasource;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @TempDir Path edited;
    @Test void emptySchemaRestartNewVersionsAndChecksumValidation() throws Exception {
        assertThat(jdbc.queryForObject("select current_database()",String.class)).endsWith("_test");
        String schema="migration_probe_"+UUID.randomUUID().toString().replace("-","");
        Map<String,Object> evidence=new LinkedHashMap<>();
        try {
            Flyway first=Flyway.configure().dataSource(datasource).schemas(schema).defaultSchema(schema).target("2").load();
            int initiallyApplied=first.migrate().migrationsExecuted;
            assertThat(initiallyApplied).isEqualTo(2);
            List<Map<String,Object>> initial=history(schema);
            assertThat(initial).hasSize(2);evidence.put("initialVersions",initial);evidence.put("initialApplied",initiallyApplied);
            int restartApplied=first.migrate().migrationsExecuted;
            assertThat(restartApplied).isZero();assertThat(history(schema)).isEqualTo(initial);
            evidence.put("restartApplied",restartApplied);evidence.put("historyUnchangedOnRestart",true);
            Flyway latest=Flyway.configure().dataSource(datasource).schemas(schema).defaultSchema(schema).load();
            assertThat(latest.migrate().migrationsExecuted).isEqualTo(2);
            assertThat(history(schema)).hasSize(4);evidence.put("afterNewMigrations",history(schema));
            assertThat(jdbc.queryForObject("select count(*) from pg_indexes where schemaname=? and indexname='idx_accounts_owner_name'",Integer.class,schema)).isEqualTo(1);
            evidence.put("ownerIndexCreated",true);
            for(String file:List.of("V1__create_accounts.sql","V2__add_status.sql","V3__create_audit_records.sql","V4__index_accounts_owner.sql")){
                String sql=Files.readString(Path.of("src/main/resources/db/migration",file));
                if(file.startsWith("V1"))sql=sql.replace("1000.00","1001.00");
                Files.writeString(edited.resolve(file),sql);
            }
            Flyway modified=Flyway.configure().dataSource(datasource).schemas(schema).defaultSchema(schema).locations("filesystem:"+edited.toAbsolutePath()).load();
            assertThat(modified.validateWithResult().validationSuccessful).isFalse();
            evidence.put("editedAppliedMigrationRejected",true);
            evidence.put("note","Only temporary test copies were changed; the applied source migrations remain intact");
            Path output=Path.of("target/evidence/migration-lifecycle.json");Files.createDirectories(output.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(),evidence);
        } finally {
            // Generated test-only schema on txlab9_test; never the application schema.
            jdbc.execute("drop schema if exists \""+schema+"\" cascade");
        }
    }
    private List<Map<String,Object>> history(String schema){
        return jdbc.queryForList("select installed_rank,version,script,checksum,installed_on,success from \""+schema+"\".flyway_schema_history where version is not null order by installed_rank");
    }
}
