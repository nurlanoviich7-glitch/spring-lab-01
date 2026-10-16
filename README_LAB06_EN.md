# Laboratory work 6 — forms and validation

**Authors:** Akhmet Didar and Magazhan Nurdaulet, IS-2405
**Individual assignment:** variant 1 — an optional ISBN field with a custom constraint.

This project extends the actual Lab 5 Gradle application with a bound Thymeleaf form, Jakarta Bean Validation, English and Russian messages, custom author/ISBN constraints, and separate exception advice for pages and the API. The Lab 5 REST operations and earlier laboratory demonstrations remain available.

## Run in IntelliJ IDEA

Open the directory containing `settings.gradle` as a Gradle project. Set the project SDK and Gradle JVM to **JDK 25**, allow Gradle to import the dependencies, and run `kz.iitu.springlab.SpringLab01Application` with IntelliJ's Run button.

The application listens locally on **port 8086**. Open [the new-book form](http://localhost:8086/books/new), [the catalogue](http://localhost:8086/books), or [the JSON API](http://localhost:8086/api/books). Stop an earlier Lab 6 instance before launching another instance on the same port.

Alternatively, run from PowerShell in the project directory:

```powershell
.\gradlew.bat clean test bootJar
java -jar build/libs/spring-lab-01-0.0.1-SNAPSHOT.jar
```

The Gradle Wrapper is included. Its first build may download the Gradle distribution and dependencies. Test results appear in `build/reports/tests/test/index.html`; the actual completed build result is reported with the delivered work.

## What to demonstrate

1. Open `/books/new`: `BookForm` and all five `Genre` values are supplied in the model.
2. Submit an empty title, year `1200`, and no genre: HTTP 200 redisplays the same form with field errors and retained input.
3. Enter author `Bloch 2018`: the custom `@NoDigits` constraint rejects digits, including Unicode digits.
4. Enter invalid ISBN `9780134685992`: the ISBN message appears. A blank ISBN passes. Valid examples are ISBN-13 `9780134685991` and ISBN-10 `0134685997`; hyphens and whitespace are accepted as separators. ISBN-10 permits `X` as its final check digit, while ISBN-13 requires a `978` or `979` prefix and a valid checksum.
5. Use `?lang=ru` and `?lang=en`: captions and validation messages change language. The choice remains in the session.
6. Submit a valid form: POST returns 302 to `/books`; the subsequent GET displays a localized flash message once. Refreshing the GET creates no duplicate.
7. Open `/books/999` and request `/api/books/999`: the same service exception becomes a real 404 HTML page or a 404 `application/problem+json` response with an `instance` URI.

`requests.http` contains executable IntelliJ HTTP client examples for these cases and the retained Lab 5 API.

## Implementation map

| File or package | Purpose |
|---|---|
| `web/BookForm.java` | Mutable form object with getters/setters; title required/max 120, author required/no digits, year 1450–2100, required genre, available checkbox, optional ISBN |
| `web/BookPageController.java` | GET form/detail/list, allowed binding fields, `@Valid` followed immediately by `BindingResult`, redisplay on errors, localized PRG success |
| `validation` | `@NoDigits` and `@ValidIsbn`, their validators, and the mandatory message/groups/payload annotation members |
| `config/LocaleConfiguration.java` | English default, session locale resolver, `lang` interceptor |
| `messages.properties`, `messages_ru.properties` | English fallback and Russian captions, constraints, binding messages, flash and missing-book messages |
| `web/PageExceptionHandler.java` | Advice scoped specifically to `BookPageController`; renders `error/not-found` with HTTP 404 |
| `web/ApiExceptionHandler.java` | Advice scoped specifically to `BookRestController`; missing-book ProblemDetail and retained bad-input responses |
| `catalog/BookService.java` | `requireById` throws `BookNotFoundException`; optional lookup, create, replacement and deletion remain available |

The binder permits only `title`, `author`, `year`, `genre`, `available`, and `isbn`. A submitted `id` cannot replace a stored record. The original domain remains `Book(Long id, String title, String author, int year)`: genre, availability and ISBN demonstrate form binding/validation and are not persisted by that record. The in-memory catalogue resets when the application restarts.

The API keeps the Lab 5 storage rules; the stricter 1450–2100 publication-year rule belongs to the new browser form. A missing single-book GET now uses central exception advice; missing PUT and DELETE operations preserve their Lab 5 404 behavior. Unsupported XML responses still return 406.

## Verification and submission

The tests cover validation boundaries, Unicode author digits, ISBN length/prefix/checksum, conversion errors with retained rejected text, genre refill, two session languages, allowed binding fields, PRG/flash behavior, HTML versus ProblemDetail errors, and retained REST/catalogue/AOP behavior. Run the complete suite with `.\gradlew.bat test` after changing code or resources.

The required branch is `lab06`. Before submitting, check your local changes and use your actual repository/branch URL in the report. A local branch or commit is not evidence of a pushed branch or a pull request. The report and defence should describe the running result shown at the demonstration.
