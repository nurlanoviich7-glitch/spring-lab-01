# Laboratory Work 7 English Defence Guide

Authors: Akhmet Didar and Magazhan Nurdaulet. Group: IS-2405. Variant 1: pagination.

## Questions and answers

1. **Why is the URI a noun? What would be wrong with `/api/getBooks`?**

   `/api/books` identifies the collection. GET already describes retrieval, so putting the verb into the URI repeats the operation and makes one resource look like several unrelated commands. Our collection and item URIs remain the same when the HTTP method changes.

2. **Which endpoints are safe and which are idempotent?**

   Our GET requests only read the catalogue, so they are safe and idempotent. PUT and DELETE change state but repeating them has the same intended final effect. POST creates another resource and is not generally idempotent. Logging does not make a read operation unsafe. [HTTP semantics](https://www.rfc-editor.org/rfc/rfc9110.html#section-9.2)

3. **Why does creation return 201 and what does Location provide?**

   201 communicates that a new resource was created. Our response also includes its representation. `Location` supplies the item's URI, so the client can retrieve it without guessing the identifier or URI pattern.

4. **Why does deletion return 204 rather than 200 with a body?**

   The deletion succeeded and there is no representation to return. The 204 response therefore has no body. A 200 response could be designed for a useful deletion result, but our contract deliberately uses 204.

5. **What happens on the second DELETE and why?**

   The first deletion returns 204; the second returns a 404 problem because the item is already absent. Both leave the resource absent. Idempotency describes the intended state effect, not a requirement that every repeated response have the same status. [Idempotency](https://www.rfc-editor.org/rfc/rfc9110.html#section-9.2.2)

6. **How would PUT and PATCH treat a field missing from the body?**

   Our PUT requires the complete editable representation. Missing title, author or genre fails validation, and a missing primitive year becomes an invalid zero. PATCH would require a separately designed partial-update contract in which absent fields remain unchanged. This variant does not implement PATCH.

7. **What is a DTO and what could go wrong if the entity were returned?**

   A data transfer object defines the API's boundary. Returning the domain type directly can expose internal fields, couple client JSON names to storage refactoring, and let persistence-specific relationships or types enter the contract. The risks depend on the domain; our immutable in-memory record has no lazy-loading relationships. The response DTO explicitly names the six published fields.

8. **Why are request and response DTOs separate types?**

   The client supplies title, author, year and genre. The server supplies identifier and creation time. Separate records make this difference visible in validation and OpenAPI schemas and prevent the request type from treating server metadata as editable fields.

9. **Where is the mapper and why is it not inside the controller?**

   `BookMapper` is a component in `kz.iitu.springlab.web.dto`. It converts a request to the domain record and domain records to response DTOs. The controller coordinates HTTP, the service handles catalogue behaviour, and the mapper owns the conversion in one reusable place.

10. **What status answers unparsable JSON and what answers valid JSON that breaks a rule?**

    Both return 400 in this API, but they take different paths. Invalid JSON cannot be read into the request DTO. Valid JSON can be bound successfully and then fail Bean Validation, for example an empty title. The latter response includes our field-error extension.

11. **When do you return 409 and why not 400?**

    A request may be structurally valid yet conflict with the current catalogue. Our example is a title-and-author pair that already exists. 409 distinguishes this state conflict from malformed or invalid input, allowing the client to choose another title or author.

12. **Which RFC defines problem details and what are the five standard members?**

    RFC 9457 describes the problem format: `type`, `title`, `status`, `detail` and `instance`. They identify the kind of problem, give its short summary and status, explain this occurrence, and identify the occurrence URI. Our API assigns explicit type URIs and its recorded errors include all five. More generally, the RFC permits an omitted type to mean `about:blank`; not every member is mandatory in every problem response. [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html#section-3.1)

13. **Which media type do errors use and who sets it?**

    Our API returns `application/problem+json`. Spring's problem response support and JSON message conversion select that representation. The REST advice supplies application problems; the separate framework-only fallback covers standard failures raised before a controller is selected. The HTTP status agrees with the `status` member.

14. **What does extending ResponseEntityExceptionHandler provide?**

    It supplies Spring MVC exception handling for standard failures such as unreadable request bodies and failed validation. We add the catalogue's 404 and 409 mappings. Our validation override calls the base implementation and enriches its problem with `errors`; the base class alone does not promise an `errors[].field` member. A separate framework-only subclass handles failures such as 405 or 415 when there is no selected REST controller. [Spring error responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)

15. **How is the advice scoped and what would happen to Lab 6 pages without scoping?**

    `@RestControllerAdvice(annotations = RestController.class)` limits the API business advice to REST controllers. `@Order(-1)` gives it priority over the framework-only fallback at order 0. The HTML page controller retains its missing-book handler and view. The fallback maps only standard MVC exceptions; it does not handle catalogue exceptions. An unscoped business advice could compete with page handling and return JSON where a user expects HTML.

16. **Why must stack traces not be returned?**

    They reveal implementation details and are not a stable client contract. A client needs an understandable status and explanation. Diagnostic traces belong in server logs; our responses avoid Java exception class names and stack fields.

17. **What OpenAPI information was generated without your documentation annotations?**

    Spring mappings provide paths and methods; Java types and record components provide parameter, request and response shapes; supported validation annotations supply constraints. Our operation summaries, explicit response descriptions, path-variable example and page/array alternatives add information that inference alone does not explain. [springdoc](https://springdoc.org/)

18. **Where does title's maxLength come from?**

    The request record has `@Size(max = 120)` on title. springdoc reflects this into `components.schemas.BookRequest.properties.title.maxLength`. Bean Validation enforces the actual rule; the OpenAPI property documents it.

19. **Where are the OpenAPI document and Swagger UI served?**

    In this local application, the document is `http://127.0.0.1:8080/v3/api-docs` and the entry page is `http://127.0.0.1:8080/swagger-ui.html`. The UI entry can redirect to its index page. The server must already be running.

20. **Why control Swagger UI on a public server and how would you close it?**

    Swagger UI exposes the API contract and can allow interactive requests with the visitor's permissions. It is not authentication. For a deployment that should hide documentation, use an environment profile with `springdoc.swagger-ui.enabled=false` and `springdoc.api-docs.enabled=false`, or protect those paths with the deployment's access controls. [springdoc endpoint configuration](https://springdoc.org/)

## Explain Variant 1

The original collection GET returns an array. Supplying `page` or `size` opts into a page envelope, so ordinary requests retain their original shape. The envelope contains `content`, `page`, `size`, `totalElements` and `totalPages`. Page numbering starts at zero. Missing values default to page 0 and size 10; sizes outside 1 through 100 are rejected. Filtering is applied before pagination, so the total describes the filtered collection. Identifier ordering makes consecutive pages deterministic for an unchanged catalogue. A valid page beyond the end is an empty result, not a missing resource. The offset calculation uses a long value to avoid integer overflow for large page numbers.

## Five minute demonstration

Prepare the application, Swagger UI and `src/test/http/books.http` before speaking. Use the known seed identifier 1 for a read and 999 for a missing item. Use a unique demonstration title for creation. Delete only the disposable book created during the demonstration.

| Time | Show | Explain |
|---|---|---|
| 0:00-0:45 | BookRequest, BookResponse and Book | Editable input versus published output; metadata belongs to the server. |
| 0:45-1:20 | Mapper and GET collection/item | Conversion is outside the controller; JSON has the response DTO fields. |
| 1:20-2:05 | POST valid JSON; follow Location | 201 identifies creation; GET proves the resource exists. |
| 2:05-2:50 | Empty title, then duplicate request | 400 comes through the base exception pipeline; 409 is a catalogue conflict. |
| 2:50-3:30 | Swagger GET unknown id | Executed 404, problem media type and the five standard problem members. |
| 3:30-4:05 | First and second DELETE | 204 with no body, then 404; explain repeated final state. |
| 4:05-4:45 | `?page=0&size=2`, then next page | Metadata and stable ordering; show an invalid size returning 400. |
| 4:45-5:30 | OpenAPI schema and HTML missing page | maxLength comes from validation; scoped advice keeps HTML and API responses distinct. |

## Ready to say during the defence

Good afternoon. We are Akhmet Didar and Magazhan Nurdaulet from IS-2405. Our seventh laboratory work develops the book catalogue into a REST API. The main goal is to give a client a clear contract: which address to call, what data to send, and how to understand the result. The application keeps the earlier HTML pages and adds an API that can be tested directly through Swagger UI or an HTTP client.

The API uses three different representations. BookRequest describes the fields a client may send: title, author, year and genre. Book is the internal catalogue record. BookResponse describes the published result, including the identifier and creation time supplied by the server. The mapper connects these representations. Keeping the conversion in one component makes the controller easier to read and makes changes to the contract easier to review.

There are five main operations. GET reads the collection or one item. POST creates a book, PUT replaces the editable fields of an existing book, and DELETE removes it. A successful read returns 200. A successful creation returns 201 and a Location header. We can follow that header to retrieve the exact resource we just created. Successful deletion returns 204 because there is no response representation to display.

Validation is part of the request contract. A title and author are required, the title has a maximum of 120 characters, the year must be between 1450 and 2100, and a genre must be selected. The author also uses the custom no-digits rule from the previous laboratory. The controller marks the request with @Valid. If a client sends an empty title, the response is 400 and the field-error extension identifies title.

Other failures have different meanings. A book that does not exist produces 404. A valid request with a title-and-author pair that already exists produces 409. These are expected client situations, so they should not become a server error. One API advice converts them into problem details. It extends Spring's standard exception handler, so malformed JSON and other MVC failures follow the same error format. The advice is scoped to REST controllers, leaving HTML error pages separate.

Each problem response provides a type, a title, a status, a detail and an instance. These members let a client identify the problem and understand this occurrence. The duplicate response uses a separate conflictingTitle extension instead of overwriting the standard title. Validation uses an errors array for rejected fields. We deliberately avoid returning stack traces or Java exception class names, because those are diagnostics rather than useful API data.

Our individual variant adds pagination. Without pagination parameters, the collection remains a JSON array. With page or size, the response contains the selected content and metadata. Page zero is the first page. Filtering happens before slicing, and the total count describes all matching books. A page beyond the end has an empty content array. Invalid sizes return 400. This demonstrates how the contract can support larger collections without requiring every client to use pagination immediately.

Finally, springdoc generates an OpenAPI document from the actual mappings, types and validation rules. Our annotations add summaries, response descriptions and an example identifier. Swagger UI makes that contract visible and lets us execute a request. During the demonstration, we show a real 404 response and a real 201 response with Location. The catalogue remains in memory and resets on restart. The result is a small API whose behaviour can be explained, reproduced and checked rather than assumed.
