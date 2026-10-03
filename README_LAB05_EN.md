# Laboratory Work 5

Students: Akhmet Didar and Magazhan Nurdaulet
Group: IS-2405

This project continues the existing Lab 4 implementation and adds a book catalogue with REST operations, an MVC page, static resources and Post/Redirect/Get. It uses Java 25, Spring Boot 4.1.1, Gradle 9.7.1 and Thymeleaf. Books are kept in memory, so restarting the application restores the three initial books.

## Run in IntelliJ IDEA

Load `build.gradle` as a Gradle project. Open `src/main/java/kz/iitu/springlab/SpringLab01Application.java` and run its main method. The original plain Java `src/Main.java` starter is preserved; run the Spring Boot application for the laboratory.

- Start page: http://localhost:8085/
- Catalogue: http://localhost:8085/books
- Filter: http://localhost:8085/books?author=Bloch
- REST list: http://localhost:8085/api/books
- Variant 1 title search: http://localhost:8085/api/books/search?title=JAVA
- Static stylesheet: http://localhost:8085/css/app.css

Port 8085 keeps this project usable alongside the earlier laboratories on ports 8080 and 8083. To use another port, add `--server.port=8080` to the run configuration or the JAR command. The application listens on 127.0.0.1.

## Build and run from PowerShell

```powershell
.\gradlew.bat test bootJar
java -jar build/libs/spring-lab-01-0.0.1-SNAPSHOT.jar
```

The Gradle wrapper downloads Gradle and dependencies on its first run. Java 25 is required. Existing Lab 4 tests and endpoints are retained.

## REST operations

| Request | Result |
| --- | --- |
| GET /api/books?author=Bloch&limit=1 | 200 and a JSON array |
| GET /api/books/1 | 200 and a book, or 404 for an absent ID |
| POST /api/books with JSON | 201, the saved book and a Location header |
| PUT /api/books/1 with JSON | 200 and the replacement, or 404 |
| DELETE /api/books/1 | 204, then 404 when repeated |

POST always assigns a new identifier. PUT uses the path identifier. The author filter matches fragments without regard to case. The list defaults to limit 10; zero returns an empty list and negative values return 400. Titles and authors must be nonblank, and years must be between 1 and 9999.

Open `requests.http` in IntelliJ for ready-to-run requests. The five error demonstrations are a non-numeric path ID (400), an unmapped URL (404), DELETE without an ID (405), a plain-text POST (415) and an XML Accept header (406).

## Browser form and refresh

On `/books`, enter a title, author and year, then submit. The server sends a 302 redirect to `/books`. The following GET shows a flash message once. Refreshing performs another GET, so it does not insert the book again and the message disappears.

## Individual assignment

Variant 1 is title-fragment search at `/api/books/search?title=...`. It ignores case and returns 200 with `[]` when there is no match. It was selected following the earlier laboratories; confirm it against your group-list position before submission.

`Defence_LAB05_EN.md` contains all sixteen defence answers and a demonstration sequence. The original Lab 4 code remains available through its existing endpoints.
