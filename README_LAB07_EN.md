# Laboratory Work 7 REST API

Authors: Akhmet Didar and Magazhan Nurdaulet. Group: IS-2405.

Variant 1 adds optional pagination to the book collection. The project extends the completed Lab 6 catalogue on a separate `lab07` branch. The existing HTML form remains available.

## Open and run in IntelliJ IDEA

1. Extract the project archive if you received a ZIP. Open the project folder containing `build.gradle` and `settings.gradle`, rather than the nested `gradle` folder or an individual Java/XML file.
2. Use Java 25 for the project SDK and Gradle JVM. Import the Gradle project when IntelliJ offers it.
3. Run the shared `Lab07Application` configuration, or run `kz.iitu.springlab.SpringLab01Application` from its main method. Stop an earlier copy using port 8080 before starting another.
4. Open [Swagger UI](http://127.0.0.1:8080/swagger-ui.html), [OpenAPI JSON](http://127.0.0.1:8080/v3/api-docs), or the [HTML catalogue](http://127.0.0.1:8080/books).

From PowerShell in the project folder, the equivalent build command is `./gradlew.bat test bootJar`. Run the resulting application with Java 25 using `java -jar build/libs/spring-lab-01-0.0.1-SNAPSHOT.jar`.

## API contract

| Request | Success | Response |
|---|---|---|
| `GET /api/books` | 200 | JSON array of `BookResponse` records |
| `GET /api/books?author=Bloch` | 200 | Filtered array |
| `GET /api/books?page=0&size=2` | 200 | Page envelope with `content`, `page`, `size`, `totalElements`, `totalPages` |
| `GET /api/books/{id}` | 200 | One response DTO; 404 if absent |
| `POST /api/books` | 201 | Created DTO and `Location` header |
| `PUT /api/books/{id}` | 200 | Replaced DTO; 404 if absent |
| `DELETE /api/books/{id}` | 204 | Empty body; another delete returns 404 |

The create and replace request contains `title`, `author`, `year` and `genre`. It does not expose editable `id` or `createdAt` fields. Titles are required and limited to 120 characters; author names are required and cannot contain digits; years must be 1450 through 2100; a genre is required. The existing enum values are `PROGRAMMING`, `FICTION`, `SCIENCE`, `HISTORY` and `OTHER`.

```json
{"title":"REST API Demonstration","author":"Didar Akhmet","year":2026,"genre":"PROGRAMMING"}
```

Send this JSON with `Content-Type: application/json`. In Swagger UI, expand POST, select **Try it out**, enter the JSON, and select **Execute**. The response headers show the new resource URI. Follow that URI with GET to prove the created resource exists.

## Pagination and errors

Pagination is opt-in: supplying either `page` or `size` selects the page envelope. Pages start at zero. Omitted page defaults to 0 and omitted size to 10. Size must be 1 through 100. Filtering happens before paging, and ordering is stable by identifier. A valid page beyond the last page returns an empty `content` array while retaining the real total count. Negative page values, invalid sizes and nonnumeric parameters return 400.

API errors use `application/problem+json`. Missing books return 404; a duplicate title-and-author pair returns 409, with surrounding whitespace and case normalised for comparison. Invalid JSON or validation failures return 400 through Spring's `ResponseEntityExceptionHandler` pipeline. This project adds `errors[].field` and `errors[].message` for field validation. These are application extensions, not fields guaranteed by the base class.

The REST-scoped `ApiExceptionHandler` has `@Order(-1)` and owns the API's business-error mappings. The separate `MvcProtocolExceptionHandler`, ordered at 0, handles standard framework failures such as 405 and 415 that can occur before any controller is selected. It does not map catalogue exceptions. Both paths assign explicit problem type URIs; the HTML controller's missing-book exception retains its page handler. This explicit framework fallback is needed because Boot's automatic `ResponseEntityExceptionHandler` configuration backs off when the custom scoped handler is present.

The duplicate extension is named `conflictingTitle`, preserving the standard problem `title`. No response should contain a stack trace or Java exception class name. Advice is scoped to REST controllers, so missing HTML pages retain an HTML 404 response.

## What the data model stores

The catalogue is in memory and resets on restart. A book stores its server-assigned identifier, title, author, year, genre and creation time. PUT preserves the original identifier and creation time. The old HTML form's optional ISBN and availability controls remain form binding and validation demonstrations; these are not published as new persisted API fields.

The English defence guide explains the contract and gives a five-minute demonstration. Test totals and observed HTTP outcomes are reported only from the actual build and captured runtime evidence.
