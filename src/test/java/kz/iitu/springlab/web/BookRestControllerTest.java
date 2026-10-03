package kz.iitu.springlab.web;

import kz.iitu.springlab.catalog.Book;
import kz.iitu.springlab.catalog.BookRepository;
import kz.iitu.springlab.catalog.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookRestControllerTest {
    private MockMvc mvc;
    private BookService service;

    @BeforeEach
    void setUp() {
        service = new BookService(new BookRepository());
        mvc = MockMvcBuilders.standaloneSetup(new BookRestController(service)).build();
    }

    @Test
    void listFiltersAuthorCaseInsensitivelyAndUsesDefaultLimitTen() throws Exception {
        for (int number = 0; number < 9; number++) {
            service.create(new Book(null, "Additional " + number, "Another author", 2024));
        }
        mvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].id").value(1));
        mvc.perform(get("/api/books").param("author", "bLoCh").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Effective Java"));
        mvc.perform(get("/api/books").param("limit", "0"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/books").param("author", "missing"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void findReturnsBookOr404() throws Exception {
        mvc.perform(get("/api/books/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Effective Java"));
        mvc.perform(get("/api/books/999"))
                .andExpect(status().isNotFound()).andExpect(content().string(""));
    }

    @Test
    void variantOneSearchHasLiteralRouteAndCaseInsensitiveTitleFragments() throws Exception {
        mvc.perform(get("/api/books/search").param("title", "jAvA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Effective Java"));
        mvc.perform(get("/api/books/search").param("title", "No such title"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/books/search"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void postReturnsCreatedAndResolvableLocationWithoutOverwritingTheBodyId() throws Exception {
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":1,"title":"Pro Spring 6","author":"Cosmina","year":2023}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/books/4"))
                .andExpect(jsonPath("$.id").value(4));
        mvc.perform(get("/api/books/4"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Pro Spring 6"));
        mvc.perform(get("/api/books/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Effective Java"));
    }

    @Test
    void putReplacesOnlyExistingRecordAndPathIdWins() throws Exception {
        String replacement = """
                {"id":777,"title":"Clean Code Revised","author":"Robert Martin","year":2025}
                """;
        mvc.perform(put("/api/books/2").contentType(MediaType.APPLICATION_JSON).content(replacement))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.year").value(2025));
        mvc.perform(get("/api/books/777")).andExpect(status().isNotFound());
        mvc.perform(put("/api/books/999").contentType(MediaType.APPLICATION_JSON).content(replacement))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/books")).andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void deleteReturnsNoContentThen404AndDeletedRecordCannotBeReplaced() throws Exception {
        mvc.perform(delete("/api/books/2"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(delete("/api/books/2")).andExpect(status().isNotFound());
        mvc.perform(put("/api/books/2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Replacement\",\"author\":\"Author\",\"year\":2025}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/books/2")).andExpect(status().isNotFound());
    }

    @Test
    void invalidNumbersAndMalformedOrInvalidJsonReturn400() throws Exception {
        mvc.perform(get("/api/books/abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/books").param("limit", "abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/books").param("limit", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Limit must not be negative"));
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"author\":\"Author\",\"year\":2025}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Title\",\"author\":\"Author\",\"year\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/books")).andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void methodAndMediaTypeMismatchesReturn405415And406() throws Exception {
        mvc.perform(delete("/api/books")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/books").contentType(MediaType.TEXT_PLAIN).content("A book"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get("/api/books").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }
}
