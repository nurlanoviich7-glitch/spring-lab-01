# Laboratory Work 6 — English defence guide

Authors: Akhmet Didar and Magazhan Nurdaulet. Group: IS-2405.

Confirmed individual assignment: **Variant 1 — an optional ISBN field with length and checksum validation**. The application uses Java 25, Spring Boot 4.1.1 and Jakarta Validation. Use the actual build output and recorded responses for execution claims; this guide does not assert a test count or remote publication.

## Answers to all 18 questions

1. **Why is the form object a separate class from the entity?**

   `BookForm` defines the fields a browser may edit and the validation rules for that interaction. `Book` is the immutable catalogue record. Binding a dedicated form reduces mass-assignment exposure, and `@InitBinder` additionally allows only the six intended fields. Modern Spring can also bind through constructors; mutable getters and setters are the design chosen here, not a universal requirement. [Spring model binding](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html)

2. **What does th:object do, and what does *{title} refer to after it?**

   `th:object="${form}"` selects the form-backing object. Inside that scope, `*{title}` refers to its `title` property. The same selected object connects inputs, field errors and the error summary to the correct Spring binding result. [Thymeleaf form binding](https://www.thymeleaf.org/doc/tutorials/3.1/thymeleafspring.html#creating-a-form)

3. **What exactly does th:field generate? Why not write name and value by hand?**

   It manages the control's name, identifier and value, or its checked/selected state. It integrates conversion and rejected values from Spring binding; checkboxes also receive a hidden field marker. Writing attributes by hand can lose invalid input, selection state or conversion behaviour. [Thymeleaf inputs and checkboxes](https://www.thymeleaf.org/doc/tutorials/3.1/thymeleafspring.html#inputs)

4. **Why must the GET handler put an empty object into the model?**

   The template needs a named `form` object before it can evaluate `th:object` and bind its fields. GET `/books/new` adds `new BookForm()` and the available genres, then returns `books/form`. An initial GET has no submitted values or validation errors to redisplay.

5. **What happens if @Valid is removed from the parameter?**

   Request binding and type conversion still happen, but this parameter no longer triggers its Bean Validation constraints automatically. A nonnumeric year can still cause a binding error. Without another validation path, blank-title, year-range, no-digits and ISBN violations would not be collected by this form-validation step. Business checks elsewhere may still reject data.

6. **What happens if BindingResult is moved after Model?**

   Spring expects it immediately after the form parameter. With this object-validation signature, invalid input would be raised as `MethodArgumentNotValidException` before the controller's error branch runs. The handout's term `BindingResultException` is not the exception name to memorise. Direct method-parameter constraints can instead lead to `HandlerMethodValidationException` in modern Spring MVC. [Current MVC validation rules](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)

7. **Why does the error branch have to put the genre list into the model again?**

   The POST is a new request; the GET handler's model is not reused automatically. On errors, the controller returns the form view directly, so it must supply the genre options again. It keeps the submitted `form` and its `BindingResult`, rather than replacing them with an empty form.

8. **Where do the errors go when validation fails? Why is nothing thrown?**

   Binding failures and constraint violations are exposed through the form's adjacent `BindingResult`. Spring makes it available to the view under the form's binding-result model key. Ordinary violations are handled as data here: the controller checks `hasErrors()` and redisplays the page. Missing adjacency or a different validation mechanism can produce an exception instead. [Spring validation integration](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html)

9. **What is the difference between @NotNull, @NotEmpty and @NotBlank in your form?**

   `@NotNull` rejects only null, so it is appropriate for the selected `Genre`. `@NotEmpty` also rejects an empty string, collection, map or array, but a whitespace-only string can pass. `@NotBlank` rejects null and text containing only whitespace, so it protects title and author. `@Size(max=120)` adds the title-length rule. [Jakarta Validation specification](https://jakarta.ee/specifications/bean-validation/3.1/jakarta-validation-spec-3.1)

10. **Where does the text of your error message come from? Trace it from the annotation to the page.**

    The annotation contains a key such as `message="{book.title.required}"`. Boot's validation integration resolves it through the application's `MessageSource` for the current locale. The violation becomes a field error, and `th:errors="*{title}"` displays its message. A conversion failure such as year `abc` uses a Spring binding-message key such as `typeMismatch.form.year`, not the `@Min` message. [Boot validation messages](https://docs.spring.io/spring-boot/reference/io/validation.html)

11. **What is the difference between a field error and a global error? Which of yours are which?**

    A field error belongs to one property; our title, author, year, genre and ISBN violations are field errors. A global error concerns the whole object, for example a relationship between two years. This selected variant does not introduce a class-level global constraint. `#fields.globalErrors()` reads global errors, while the summary's `allErrors()` includes both kinds. [Thymeleaf global errors](https://www.thymeleaf.org/doc/tutorials/3.1/thymeleafspring.html#global-errors)

12. **Why must messages.properties exist even though your users read Russian?**

    With Boot's default basename, the default bundle is required for message-source auto-configuration; `messages_ru.properties` alone does not activate it. Our default file contains English messages and provides fallback keys. This is Boot's default configuration rule, not a restriction that every manually configured `MessageSource` must use that filename. [Boot internationalization](https://docs.spring.io/spring-boot/reference/features/internationalization.html)

13. **How does the application decide which language to use? What does your interceptor do?**

    `SessionLocaleResolver` starts with English and stores the chosen locale in the session. `LocaleChangeInterceptor` reads the `lang` parameter and changes that locale before controller processing. `?lang=ru` therefore affects captions and validation messages; the browser's session cookie keeps the choice on later POSTs. System-locale message fallback is disabled for predictable bundle fallback. [LocaleChangeInterceptor](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/i18n/LocaleChangeInterceptor.html)

14. **Name the three mandatory members of a constraint annotation and say why they are required.**

    `message` supplies the default message template, `groups` assigns the constraint to validation groups, and `payload` carries optional metadata for validation clients, such as severity. Groups and payload default to empty arrays. These members are part of the constraint-annotation contract; `@Constraint`, runtime retention and the target declaration separately connect and expose its validator. [Jakarta constraint definitions](https://jakarta.ee/specifications/bean-validation/3.1/jakarta-validation-spec-3.1#constraintsdefinitionimplementation)

15. **Why does your isValid return true for a null value?**

    `@NoDigits` answers whether a supplied author contains digits; `@NotBlank` handles the separate presence rule. The optional ISBN validator accepts null and blank input because Variant 1 explicitly requires that policy. A nonblank invalid ISBN still fails. Treating null as valid is this constraint's design choice, not a rule that all constraints must follow.

16. **How did you scope the two @ControllerAdvice classes? What would happen without scoping?**

    Page advice targets `BookPageController` and API advice targets `BookRestController` with `assignableTypes`, so their audiences are disjoint. The former returns an HTML view with status 404; the latter returns a 404 `ProblemDetail`. Without scoping, competing handlers could return the wrong representation. `annotations=Controller.class` alone is insufficient because `@RestController` also includes `@Controller`. [ControllerAdvice API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/bind/annotation/ControllerAdvice.html), [RestController API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/bind/annotation/RestController.html)

17. **Which handler wins if two of them match the same exception?**

    Applicable local controller handlers are considered before advice. Advice beans are ordered, and the first applicable advice with a match is used. Within one advice, the closest exception match is preferred; a root match is preferred over a cause match. A cause match in higher-priority advice can still beat a root match in lower-priority advice. Do not rely on declaration order or claim that specificity always wins globally. [Spring exception mapping](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html), [Advice ordering](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html)

18. **Why is th:utext dangerous for anything a user typed?**

    `th:utext` outputs text without HTML escaping. A submitted title containing markup could become executable page content. Use `th:text` for displayed user data and `th:field` for bound controls, preserving their escaping behaviour. Validation does not replace output escaping. [Thymeleaf escaped and unescaped text](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html#unescaped-text)

## Variant 1: explain the ISBN validator

- Null or blank input passes because the field is optional. The validator removes hyphens and whitespace from a nonblank value; a value containing only hyphens fails after normalization.
- ISBN-10 uses nine ASCII digits followed by an ASCII digit or final `X`, worth 10. The weighted total with weights 10 through 1 must be divisible by 11. This implementation also accepts lowercase final `x` as a deliberate input convenience.
- ISBN-13 contains 13 ASCII digits and begins with 978 or 979. Its weighted total, including the check digit, uses alternating weights 1 and 3 and must be divisible by 10.
- `9780134685991` and `020161622X` are checksum-valid examples. `9780134685992`, a wrong length, an internal `X` and non-ASCII digits are rejected. A checksum check does not establish that an identifier has actually been assigned to a publication.

The lab specifies optional blank handling; the ISBN structure and check-digit rules come from the [International ISBN Agency](https://www.isbn-international.org/index.php/node/10) and its [official user manual hosted by Norway's National Library](https://www.nb.no/content/uploads/2018/04/ISBN-International-Users-Manual-7th-edition_absolutely_final.pdf).

`@NoDigits` examines Unicode code points, so names containing Arabic-Indic digits or a supplementary digit such as `𝟙` are rejected too. `String.chars()` would inspect UTF-16 units and miss some supplementary digits. `Character.isDigit` checks decimal digits, rather than every symbol with a numeric meaning. [Java 25 String](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/String.html#codePoints()), [Character.isDigit](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Character.html#isDigit(int))

## A 5–6 minute demonstration

Start the application before the timer begins. Open `http://localhost:8086/books/new`, both advice classes, `BookForm`, the POST handler, template and validators. Keep one browser session for language selection and flash delivery. Prepare the API request and known-valid ISBN in advance.

| Time | Demonstrate | Explain or verify |
|---|---|---|
| 0:00–0:40 | `BookForm` beside `Book`, GET handler and form template | Dedicated binding fields, `@Valid`, form/genres model attributes, `th:object`, `th:field` and escaped output. |
| 0:40–1:20 | Submit with title empty and the other required fields valid | The same page returns with a field message, red border and summary. Author, year and genre remain populated; no book is saved. |
| 1:20–1:55 | Submit year 1200 with no genre | Both relevant field errors and both summary entries appear. Point to `BindingResult` immediately after `BookForm` and to the refilled genre model. |
| 1:55–2:20 | Set author to `Bloch 2018` | The custom no-digits message appears; point to its annotation, validator and both message keys. Explain null separately from requiredness. |
| 2:20–3:00 | Open `?lang=ru`, then submit invalid input | Russian labels, genre names, summary and field messages appear. Explain session locale, interceptor and message interpolation. |
| 3:00–3:40 | Demonstrate ISBN `9780134685992`, then a valid ISBN or blank | The incorrect checksum has its own error. Explain length, prefix and checksum; optional blank passes. |
| 3:40–4:25 | Submit a fully valid form with a unique title, then F5 | POST redirects with 302, the GET renders 200 and shows the localized flash once. Refresh is another GET; the message disappears and the count stays unchanged. |
| 4:25–5:10 | Browser `/books/999` and HTTP client `/api/books/999` | Show actual 404 for both; HTML for the page and `application/problem+json` for the API. Explain disjoint advice scoping. |
| 5:10–5:50 | Briefly show error-message bundles, the ISBN rule and actual local Git state | Trace one message, explain handler ordering, and state what was actually committed or published. Finish with declarative-validation benefits. |

For a browser-only demonstration of server errors, the form uses `novalidate`. The year input uses text with numeric input mode so rejected text such as `abc` can remain visible. A failed form POST renders directly; only a successful POST redirects. The real server may serialize its redirect `Location` as an absolute URL and append a session suffix; `/books` remains the logical destination.

The existing `Book` record stores title, author and year. Genre, availability and ISBN demonstrate form binding and validation here and are not persisted by that record. Do not claim those fields are later retrieved from storage. The in-memory catalogue resets when the application restarts.
