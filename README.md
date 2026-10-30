# Laboratory Work 9: Spring Transactions and Flyway

**Akhmet Didar and Magazhan Nurdaulet, IS-2405**

This standalone laboratory application implements atomic account transfers, rollback rules, propagation, isolation and optimistic locking on PostgreSQL. Flyway manages the schema and Hibernate only validates it. The lab09 branch follows the completed Lab 8 history; its current source tree is the separate Lab 9 application. The lab08 branch and its book catalogue remain unchanged.

## Run in IntelliJ IDEA

1. Open this folder or its `pom.xml` as a Maven project. Use Java 21 or newer. The prepared workstation uses the installed Java 25 SDK.
2. On the prepared workstation run `Start-Database.ps1`. It reuses the existing portable PostgreSQL server on loopback port 5438 and creates separate `txlab9`, `txlab9_test` and `txlab9_verify` databases if missing. It does not change the Lab 8 database.
3. Run the shared **Lab09Application** configuration.
4. Open <http://127.0.0.1:8089/lab9>.

The source ZIP excludes PostgreSQL binaries, database data, IDE caches and build outputs. On another workstation use an installed PostgreSQL 16+ server, or run `docker compose up -d`. The compose server uses loopback port **5439** to avoid the prepared Lab 8 server. For Docker set `DB_URL=jdbc:postgresql://127.0.0.1:5439/txlab9` and `TEST_DB_URL=jdbc:postgresql://127.0.0.1:5439/txlab9_test`. The classroom username/password are `springlab` / `springlab`.

## Build and configuration

Windows: `mvnw.cmd clean verify`. macOS/Linux: `./mvnw clean verify`. Maven downloads its official distribution and the declared dependencies when necessary. Java 21 is the compilation target. Spring Boot 3.5.16 is used to satisfy the assignment's Spring Boot 3.x requirement. Tests require the separate PostgreSQL database, not H2.

| Setting | Default | Environment override |
| --- | --- | --- |
| HTTP | 127.0.0.1:8089 | PORT |
| JDBC | jdbc:postgresql://127.0.0.1:5438/txlab9 | DB_URL |
| User / password | springlab / springlab | DB_USER / DB_PASSWORD |
| Test JDBC | jdbc:postgresql://127.0.0.1:5438/txlab9_test | TEST_DB_URL |

The test reset checks that the current database name ends in `_test` before modifying test rows. Hibernate uses `ddl-auto=validate` and `open-in-view=false`. Flyway clean is disabled. The staged migration test uses a generated schema only inside the test database.

## Experiments

Each experiment creates its own pair with balances 1000.00 / 500.00. The experiment endpoints leave Alice and Bob unchanged. The transfer form intentionally changes Alice and Bob. All data is classroom sample data.

| POST /api/experiments/{name} | Expected database observation |
| --- | --- |
| success | 900.00 / 600.00, both versions increment |
| insufficient | 1000.00 / 500.00, no changes |
| unchecked | Flushed debit rolls back, 1000.00 / 500.00 |
| checked-default | Default checked-exception rule commits debit, 900.00 / 500.00 |
| checked-rollback | rollbackFor restores 1000.00 / 500.00 |
| required | Outer debit and audit roll back; audit count 0; same transaction id |
| requires-new | Outer debit rolls back; audit count 1; different transaction ids |
| read-committed | First read 1000.00, second 1100.00 after independent writer commits |
| repeatable-read | First and second reads 1000.00; committed balance outside is 1100.00 |
| optimistic-lock | First version 0 -> 1 commits; stale version 0 is rejected; balance 1100.00 |

Isolation uses two real PostgreSQL transactions on different connections. JDBC SELECT is intentional: repeatedly loading the same managed JPA entity could show the first-level cache rather than the database isolation behavior. REQUIRES_NEW auditing is on a separate bean, so its proxy is invoked. The audit table has no foreign key to the uncommitted outer update and therefore avoids a cross-transaction dependency on that row.

## Public API

- `GET /api/accounts`: the Alice and Bob sample accounts.
- `GET /api/accounts/{id}`: account, balance, status and version.
- `POST /api/transfers`: JSON `{"fromId":1,"toId":2,"amount":100.00}`.
- `PUT /api/accounts/{id}/balance`: classroom update with `{"balance":1100.00,"expectedVersion":0}`. A stale version returns 409 and does not overwrite current data.
- `POST /api/experiments/{name}`: structured observations, including before and after balances read back from PostgreSQL.
- `GET /api/migrations`: real Flyway history, checksums and the owner-name index.

Invalid input returns 400, missing accounts 404, and insufficient funds or optimistic conflicts 409 with problem JSON. The code uses BigDecimal and decimal(19,2), rejects excess fractional precision and does not use floating-point balance arithmetic.

## Schema versioning

V1 creates accounts and inserts Alice/Bob. V2 adds ACTIVE status. V3 creates independent audit records. V4 adds the owner-name index. Migrations are immutable after application. The lifecycle test starts an empty generated schema, applies only V1/V2, restarts with zero new migrations and unchanged history, applies V3/V4, then confirms that a modified temporary copy of V1 fails checksum validation. Original migration files are not changed by that test.

The demonstration database is independent of tests and packaged HTTP verification. See the report and `Defence_LAB09_EN.md` for observations, a demonstration script and answers.

Repository: <https://github.com/nurlanoviich7-glitch/spring-lab-01>. Branch: lab09. Remote publication is verified separately from a local commit.

Primary references:
- <https://docs.spring.io/spring-boot/3.5/system-requirements.html>
- <https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html>
- <https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html>
- <https://www.postgresql.org/docs/16/transaction-iso.html>
- <https://documentation.red-gate.com/flyway/flyway-concepts/migrations/versioned-migrations>
