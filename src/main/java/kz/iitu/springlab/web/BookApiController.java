package kz.iitu.springlab.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.iitu.springlab.catalog.Book;
import kz.iitu.springlab.catalog.BookService;
import kz.iitu.springlab.web.dto.BookMapper;
import kz.iitu.springlab.web.dto.BookPageResponse;
import kz.iitu.springlab.web.dto.BookRequest;
import kz.iitu.springlab.web.dto.BookResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Tag(name = "Books", description = "Book catalogue CRUD and optional collection paging")
@RestController
@RequestMapping(value = "/api/books", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookApiController {
    private final BookService service;
    private final BookMapper mapper;

    public BookApiController(BookService service, BookMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(summary = "List books or request a page",
            description = "Without page and size, returns an array. Supply either parameter for a page envelope. "
                    + "Pages start at zero; default page is 0, default size is 10, maximum size is 100. "
                    + "Author filtering happens before paging; results are ordered by identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Books or a page of books",
                    content = @Content(schema = @Schema(oneOf = {BookResponse[].class, BookPageResponse.class}))),
            @ApiResponse(responseCode = "400", description = "Invalid page or size",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public Object list(@RequestParam(name = "author", required = false) String author,
                       @Parameter(description = "Zero-based page number", example = "0")
                       @RequestParam(name = "page", required = false) Integer page,
                       @Parameter(description = "Books per page, between 1 and 100", example = "2")
                       @RequestParam(name = "size", required = false) Integer size) {
        List<Book> books = service.findAll(author);
        if (page == null && size == null) {
            return mapper.toResponseList(books);
        }
        int requestedPage = page == null ? 0 : page;
        int requestedSize = size == null ? 10 : size;
        if (requestedPage < 0 || requestedSize < 1 || requestedSize > 100) {
            throw new IllegalArgumentException("Page must be zero or greater; size must be between 1 and 100");
        }
        long total = books.size();
        long offset = (long) requestedPage * requestedSize;
        List<Book> selected = offset >= total ? List.of()
                : books.subList((int) offset, (int) Math.min(total, offset + requestedSize));
        return new BookPageResponse(mapper.toResponseList(selected), requestedPage, requestedSize,
                total, (total + requestedSize - 1) / requestedSize);
    }

    @Operation(summary = "Find a book by its identifier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The book was found"),
            @ApiResponse(responseCode = "404", description = "No book with this identifier",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public BookResponse find(@Parameter(description = "Identifier of the book", example = "42")
                             @PathVariable(name = "id") long id) {
        return mapper.toResponse(service.requireById(id));
    }

    @Operation(summary = "Search book titles", description = "Retains the earlier literal title search route.")
    @GetMapping("/search")
    public List<BookResponse> search(@RequestParam(name = "title", defaultValue = "") String title) {
        return mapper.toResponseList(service.searchByTitle(title));
    }

    @Operation(summary = "Create a book", description = "Identifiers and creation timestamps are assigned by the server.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created; Location identifies the saved book",
                    headers = @Header(name = "Location", description = "URI of the created book",
                            schema = @Schema(type = "string", example = "/api/books/4"))),
            @ApiResponse(responseCode = "400", description = "Malformed JSON or invalid fields",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The same title and author already exist",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request) {
        Book saved = service.create(mapper.toDomain(request));
        return ResponseEntity.created(URI.create("/api/books/" + saved.id()))
                .body(mapper.toResponse(saved));
    }

    @Operation(summary = "Replace the editable fields of a book",
            description = "All request fields are required. The stored identifier and creation timestamp are retained.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The existing book was updated"),
            @ApiResponse(responseCode = "400", description = "Malformed JSON or invalid fields",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "No book with this identifier",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The title and author conflict with another book",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BookResponse update(@PathVariable(name = "id") long id,
                               @Valid @RequestBody BookRequest request) {
        return mapper.toResponse(service.update(id, mapper.toDomain(request)));
    }

    @Operation(summary = "Delete an existing book",
            description = "Returns 204 for a deletion and 404 on a repeated deletion. Both leave the resource absent.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "No book with this identifier",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable(name = "id") long id) {
        service.deleteRequired(id);
        return ResponseEntity.noContent().build();
    }
}
