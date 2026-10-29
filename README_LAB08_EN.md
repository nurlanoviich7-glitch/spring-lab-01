# Laboratory Work 8 PostgreSQL and Spring Data JPA

**Akhmet Didar and Magazhan Nurdaulet, IS-2405**

Variant 1 adds a Publisher entity and a derived query by publisher name. This branch extends the completed Lab 7 source state, commit `19e21d4`. The catalogue now stores books in PostgreSQL. The earlier HTML forms, validation and REST CRUD remain available. Collection GET now always returns a database page.

## Start in IntelliJ IDEA

1. Open this folder as a Gradle project and use Java 25.
2. Start PostgreSQL before starting Java. On the prepared workstation, run `Start-Database.ps1` from the project folder. The portable cluster is an ignored local directory and listens on `127.0.0.1:5438`.
3. Run the shared **Lab08Application** configuration.
4. Open <http://127.0.0.1:8088/lab8>, <http://127.0.0.1:8088/books/new> or <http://127.0.0.1:8088/swagger-ui.html>.

The project ZIP contains source and the Gradle wrapper. PostgreSQL binaries and database data are external runtime prerequisites and are excluded from Git and the ZIP. On another workstation, use PostgreSQL 16 or newer, or start the included `compose.yml` with `docker compose up -d`. Create `springlab`, `springlab_test` and `springlab_verify` for the checks. The development defaults are the classroom credentials `springlab` / `springlab`, restricted to loopback.

## Configuration

| Setting | Default | Override |
| --- | --- | --- |
| HTTP port | 8088 | `PORT` |
| JDBC URL | `jdbc:postgresql://127.0.0.1:5438/springlab` | `DB_URL` |
| Database user | springlab | `DB_USER` |
| Database password | springlab | `DB_PASSWORD` |
| Test database | `jdbc:postgresql://127.0.0.1:5438/springlab_test` | `TEST_DB_URL` |

The default `dev` profile uses `ddl-auto=update`, so restarting Java preserves the demonstration records. An explicit `classroom` profile uses `create-drop` for the assignment experiment and destroys its selected schema. The `test` profile uses create-drop only in the separate test database. The `prod` profile uses validate and expects a separately managed schema. `open-in-view=false` applies throughout.

## Data model

`book.author_id` owns the Author association. `Author.books` is inverse, with `mappedBy="author"`, cascade ALL and orphan removal. `Book.tags` owns the `book_tag(book_id, tag_id)` join table. Tags are shared and do not cascade REMOVE. For variant 1, `book.publisher_id` owns the Publisher association and `Publisher.books` is inverse. Book uses an identity identifier, EnumType.STRING, a server creation timestamp and an optimistic version.

The development runner inserts eight books, three authors, four tags and two publishers only when the book table is empty. At least one author has three books and at least one book has two tags. This is a demonstration dataset, including the sample title API Design Notes.

## Routes for the demonstration

| Route | Result |
| --- | --- |
| `/lab8` | Live PostgreSQL rows and experiment controls |
| `/api/books?page=1&size=3&sort=year,desc` | Database page with total count |
| `/api/books?genre=PROGRAMMING&publisher=Manning` | Database filtering and paging |
| `/api/lab8/queries` | Five derived queries, JPQL results and captured SQL |
| `/api/lab8/n-plus-one` | Fresh-context SQL counts before and after fetch join |
| `/api/lab8/page-vs-slice` | Page and Slice SQL for the same full middle page |
| `/api/lab8/database` | Actual columns, foreign keys and rows |
| `/api/lab8/publishers?name=Manning` | Variant 1 derived query and SQL |
| `/api/lab8/books/1` | Author, publisher, tags, archived flag and version |
| `POST /api/lab8/archive?year=2010` | Transactional bulk update |

Page numbers start at zero. Default size is 5 and the maximum is 100. A stable id tie-breaker follows the requested sort. Invalid sort names, sizes and page values return 400 problem JSON. POST returns 201 and Location, PUT retains id and createdAt, and DELETE returns 204 followed by 404 on repeat.

## Verification

Run `gradlew.bat test bootJar` against the separate PostgreSQL test database. The recorded build passed **62 tests with no failures, errors or skipped tests**. A separate packaged-application check passed **33 HTTP responses and 94 assertions**, including application restart persistence.

With the original eight-book seed, lazy loading author names requires four statements: one book query and three distinct author queries. The fetch join requires one. A full middle Page of three books requires a content query and count query. Slice requests one extra row and requires one query. Spring can omit a Page count for a short final page, so two is the measured result for this particular request, not a universal rule.

The bulk update increments version explicitly because JPQL bulk operations bypass ordinary entity dirty checking. It clears the persistence context to prevent stale managed values. Named parameters use explicit string casts in optional text filters so PostgreSQL can type null values. Entity graphs fetch only to-one associations for paged queries, avoiding collection-fetch pagination in memory.

## Repository and references

Branch: `lab08`. Repository: <https://github.com/nurlanoviich7-glitch/spring-lab-01>. Publication of a branch must be verified separately from a local commit.

See `Defence_LAB08_EN.md` for the English oral explanation and answers to all demonstration questions. SQL and runtime evidence accompany the report.

Primary documentation:
- <https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html>
- <https://docs.spring.io/spring-boot/reference/data/sql.html>
- <https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html>
- <https://www.postgresql.org/docs/16/>
