# Laboratory Work 5 — English defence guide

Authors: Akhmet Didar and Magazhan Nurdaulet. Group: IS-2405.

Use this guide alongside the completed application and its recorded responses. The individual variant has not yet been confirmed. The statuses below describe the required contract; the report must show the statuses actually observed during verification.

## Answers to all 16 defence questions

1. **Trace the path of a request from the browser to your method and back.**

   The servlet container receives the request and passes it to `DispatcherServlet`. A `HandlerMapping` selects the handler. A `HandlerAdapter` resolves and converts arguments, then invokes the controller and its service. For the API, a message converter writes JSON; for the page, a resolved view renders HTML from the model. The response returns to the browser. [Spring MVC processing](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/sequence.html)

2. **What does HandlerMapping do and what does HandlerAdapter do?**

   `HandlerMapping` finds a handler using conditions such as the URL, HTTP method and media types. `HandlerAdapter` knows how to invoke that handler: for annotated controllers, it resolves method arguments, performs conversion and processes the return value. These responsibilities allow `DispatcherServlet` to support different handler types. [Spring MVC special bean types](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/special-bean-types.html)

3. **What exactly does @ResponseBody change? Where is it in your code?**

   It treats the return value as response content written by an `HttpMessageConverter`, rather than as a logical view name. `@RestController` includes `@Controller` and `@ResponseBody`, so it applies to `BookRestController`. `BookPageController` uses `@Controller`; its `"books/list"` return value selects a view. `ResponseEntity` also supplies a response body, status and headers. [@ResponseBody](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responsebody.html), [ResponseEntity](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html)

4. **Which part of the request does each parameter of your list method come from?**

   In the REST list method, `author` and `limit` come from query parameters through `@RequestParam`. For example, `?author=Bloch&limit=1` supplies both. A missing `author` is allowed; a missing or empty `limit` uses its default of 10. In the MVC list method, `author` is also a query parameter, while `Model` is supplied by Spring. [RequestParam API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/bind/annotation/RequestParam.html)

5. **What happens if limit is given a value that is not a number, and why is your method not called?**

   With `?limit=abc`, conversion to `int` fails while Spring resolves the method arguments. Spring returns **400 Bad Request** before entering the method. This is a binding failure after the mapping matched. An empty `?limit=` is a different case: `defaultValue="10"` supplies 10.

6. **Why does @RequestParam(required = false) behave differently from a @RequestParam without attributes?**

   A plain `@RequestParam` is required by default, so an absent parameter causes 400. `required=false` permits omission, which gives `null` for the optional `String author`. A `defaultValue` also makes the parameter optional and supplies a fallback. An optional primitive still needs an appropriate default or a nullable wrapper type. [RequestParam API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/bind/annotation/RequestParam.html)

7. **Why does creation return 201 and not 200? What is in the Location header?**

   **201 Created** specifically reports that the request created a resource. `ResponseEntity.created(...)` sets this status and the `Location` header. The header contains the new resource's URI, such as `/api/books/4`; the body contains the saved book with its generated identifier. The identifier should come from the saved result, not an assumed sequence value.

8. **Why does the second DELETE of the same identifier return 404?**

   The first request removes the existing book and returns **204 No Content**, with no response body. The second request finds that the book no longer exists, so the controller returns **404 Not Found**. DELETE remains idempotent because repeating it leaves the same final state, even though the statuses differ.

9. **What is the model in your page controller? Who fills it in and who reads it?**

   `Model` holds named attributes for rendering the page. `BookPageController` adds `books`, `author` and `title`. Spring supplies the model, and the Thymeleaf view reads these attributes through expressions such as `${books}`. Flash attributes from the preceding redirect can also be added automatically to this request's model.

10. **How does the name books/list become a file? Which component decides that?**

    A `ViewResolver` resolves the logical name. With the Thymeleaf integration and its default template location, the template resolver uses `classpath:/templates/` and the `.html` suffix, selecting `templates/books/list.html`. Thymeleaf processes the template with the model and writes the resulting HTML. The controller returns the logical name rather than reading the file itself. [View resolution contracts](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/special-bean-types.html), [Boot template engines](https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.template-engines)

11. **Why is /css/app.css served although no controller maps it?**

    Spring Boot configures Spring MVC's `ResourceHttpRequestHandler` for static resources. The file is at `src/main/resources/static/css/app.css`, so it is exposed as `/css/app.css`. Boot also discovers `static/index.html` as a welcome page for `/`, when no application route takes precedence. An annotated controller is unnecessary for these resources. [Boot static content and welcome pages](https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.static-content)

12. **How many requests does the browser make in Post/Redirect/Get, and what is the status of each?**

    The submission cycle has two application requests: `POST /books` creates the record and returns **302 Found** with a `Location` header pointing to the logical destination `/books`. The server may serialize this as an absolute URL and append a `;jsessionid=...` suffix. The browser follows with `GET /books`, which returns **200 OK** and the page. F5 then performs another GET, so it does not run the creation handler again. Stylesheet and other resource requests are additional requests, outside this two-step cycle.

13. **Where is the flash message stored between the two requests, and who removes it?**

    `addFlashAttribute` puts the message into the output `FlashMap`. The default `SessionFlashMapManager` stores it in the HTTP session. On the matching redirected GET, the manager retrieves it and removes the stored map; Spring exposes its attributes in the target controller's model. The first page displays it, and the next refresh has no flash message. The client must retain the session cookie. [Flash attributes](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/flash-attributes.html), [SessionFlashMapManager](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/support/SessionFlashMapManager.html)

14. **For each row of the status-code table, which part of the mapping did not match?**

    Use the five-row table below. For **400**, the mapping itself matched, but converting `abc` to `long` failed. **404** means no requested endpoint or resource exists. **405** concerns the HTTP method. **415** concerns the request body's media type. **406** concerns the response media types acceptable to the client. Do not describe all five failures as URL mismatches.

15. **Why is 415 returned for Content-Type: text/plain and 406 for Accept: application/xml?**

    `Content-Type` describes the body sent to the server: a JSON-consuming book endpoint cannot read `text/plain`, so it returns **415**. `Accept` describes the response the client will accept: a JSON-producing endpoint cannot satisfy XML-only acceptance, so it returns **406**. Mapping `consumes` and `produces` conditions make these requirements explicit. [Mapping requests by media type](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html)

16. **What would change in your controllers if the template engine were replaced?**

    The REST controller would be unaffected. The page controller could keep the same mappings, model attribute names and logical view name if the replacement view setup preserves that contract. The dependency, view resolution configuration and template syntax would change; a new template naming convention could require changing the returned view name. [Controller return values](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/return-types.html)

## The five mandatory error responses

| Request | Required status | Precise explanation |
|---|---:|---|
| `GET /api/books/abc` | **400** | `/{id}` matches, but `abc` cannot be converted to the declared `long` argument. |
| `GET /api/nothing` | **404** | No such controller endpoint exists; the static-resource fallback also finds no resource. |
| `DELETE /api/books` | **405** | The collection URL exists, but the compulsory base controller has no collection DELETE method. |
| `POST /api/books`, `Content-Type: text/plain` | **415** | The request media type is unsupported for this JSON book body. |
| `GET /api/books`, `Accept: application/xml` | **406** | The endpoint produces JSON and cannot meet an XML-only acceptance condition. |

Spring's standard exception resolver defines these status translations. Capture the response status and relevant headers from the running application; error body formatting may vary with the application's error configuration. [DefaultHandlerExceptionResolver](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/support/DefaultHandlerExceptionResolver.html)

Variant 9 adds a collection DELETE operation and therefore changes the third row. If that variant is selected, explicitly explain the conflict and record the base-controller experiment separately. Adding a required `author` parameter to the new mapping does not automatically preserve 405: an absent required mapping parameter can produce 400.

## A 5–6 minute demonstration

Before the timer starts, start the application, open both controller files and the template, open the browser and HTTP client, and prepare requests in execution order. Use the application's actual port. Begin with freshly seeded in-memory data or adapt identifiers to the current data. Keep one browser session for the flash demonstration. Each student should be able to explain the full sequence.

| Time | Show | Say or verify |
|---|---|---|
| 0:00–0:35 | Both controllers and their constructor dependencies | Explain `@Controller`, `@RestController`, `@ResponseBody`, and the request flow through the front controller. |
| 0:35–1:10 | `GET /api/books`, then `?author=Bloch&limit=1` | Show JSON and the filter/limit. Point to the two `@RequestParam` arguments and their defaults. |
| 1:10–1:35 | An existing ID and `/api/books/999` | Show 200 and 404. Point to `ResponseEntity.ok` and `notFound`. |
| 1:35–2:10 | JSON POST, followed by GET of its `Location` | Show 201, the generated ID, the `Location` header and the saved body. |
| 2:10–2:45 | PUT of that ID, then DELETE of it twice | Show replacement with 200, then 204 and 404. Explain full replacement and the absence of a 204 body. |
| 2:45–3:25 | `/books`, filtered `/books?author=Bloch`, `/css/app.css`, `/` | Point to model attributes, the template loop, the applied CSS and the static welcome page. |
| 3:25–4:10 | Submit the creation form; inspect POST and redirect; press F5 | Show POST 302 followed by GET 200, the one-time flash message, no message after refresh, and an unchanged record count after refresh. |
| 4:10–4:55 | The five prepared error requests and their recorded table | Explain conversion, endpoint absence, method selection, input media type and output negotiation. |
| 4:55–5:35 | The confirmed individual endpoint and its code | Show its required successful result and its important edge case. State the actual variant number only after confirmation. |
| 5:35–5:50 | Conclusions and the actual local branch/repository evidence | Explain how the front controller centralises routing, conversion and response handling. Describe only Git actions actually completed. |

PRG prevents resubmission caused by refreshing the redirected page; it does not provide general duplicate-POST protection against double-clicks or independent repeated submissions. The repository is in memory, so restarting the application resets its contents.

## Points to keep accurate in the report

- Report all five REST operations, including PUT; the sample service interface and REST listing leave its implementation to the student.
- Use `limit=abc` for the nonnumeric experiment. Handle negative limits deliberately instead of letting `Stream.limit` produce a server error.
- Creation should allocate a new ID rather than allowing a supplied POST ID to overwrite an existing record. The PUT path ID should identify the replacement target, and a missing target must remain 404.
- The sample page needs a form or another explicit form-encoded submission method for demonstrating PRG. `@RequestParam` reads its form fields; `@RequestBody Book` is for the JSON API.
- The assignment uses Maven dependency syntax, but an existing Gradle project should retain its build system and suitable dependencies for its actual Spring Boot version.
- No variant number or GitHub publication is asserted by this guide. Record the confirmed variant and a real branch URL only when available.
