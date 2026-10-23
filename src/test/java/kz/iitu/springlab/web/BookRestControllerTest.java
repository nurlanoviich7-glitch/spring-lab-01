package kz.iitu.springlab.web;

import com.jayway.jsonpath.JsonPath;
import kz.iitu.springlab.catalog.Book;
import kz.iitu.springlab.catalog.BookRepository;
import kz.iitu.springlab.catalog.BookService;
import kz.iitu.springlab.web.dto.BookMapper;
import kz.iitu.springlab.web.dto.BookRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookRestControllerTest {
    private MockMvc mvc;
    private BookService service;

    @BeforeEach
    void setUp() {
        service = new BookService(new BookRepository());
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("book.notFound", Locale.ENGLISH, "Book {0} was not found");
        messages.addMessage("book.notFound.title", Locale.ENGLISH, "Book not found");
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new BookApiController(service, new BookMapper()))
                .setControllerAdvice(new ApiExceptionHandler(messages)).setValidator(validator).build();
    }

    private String request(String title, String author, int year) {
        return """
                {"title":"%s","author":"%s","year":%d,"genre":"PROGRAMMING"}
                """.formatted(title, author, year);
    }

    private MvcResult create(String body) throws Exception {
        return mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
    }

    private void assertProblem(MvcResult result, int expectedStatus) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
        assertThat(MediaType.parseMediaType(result.getResponse().getContentType())
                .isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).isTrue();
        String json = result.getResponse().getContentAsString();
        assertThat(JsonPath.<Integer>read(json, "$.status")).isEqualTo(expectedStatus);
        assertThat(JsonPath.<String>read(json, "$.title")).isNotBlank();
        assertThat(JsonPath.<String>read(json, "$.detail")).isNotBlank();
        assertThat(JsonPath.<String>read(json, "$.type")).isNotBlank();
        assertThat(JsonPath.<String>read(json, "$.instance")).startsWith("/api/books");
        assertThat(json).doesNotContain("\"trace\"", "java.lang.", "Exception", "kz.iitu.");
    }

    @Test
    void collectionAndItemExposeResponseDtosAndRetainAuthorFiltering() throws Exception {
        mvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].genre").value("PROGRAMMING"))
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty());
        mvc.perform(get("/api/books").param("author", "  bLoCh  "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Effective Java"));
        mvc.perform(get("/api/books").param("author", "missing"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/books/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Effective Java"));
        assertThat(Arrays.stream(BookRequest.class.getRecordComponents()).map(c -> c.getName()))
                .containsExactly("title", "author", "year", "genre");
    }

    @Test
    void creationReturns201LocationAndResourceThatCanBeRetrieved() throws Exception {
        MvcResult created = create(request("Practical Java", "Alice Writer", 2025));
        String location = created.getResponse().getHeader("Location");
        assertThat(location).isEqualTo("/api/books/4");
        String json = created.getResponse().getContentAsString();
        assertThat(JsonPath.<Integer>read(json, "$.id")).isEqualTo(4);
        assertThat(Instant.parse(JsonPath.read(json, "$.createdAt"))).isNotNull();
        mvc.perform(get(location)).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Practical Java"))
                .andExpect(jsonPath("$.genre").value("PROGRAMMING"));
    }

    @Test
    void clientsCannotSupplyTheIdentifierOrCreationTimestamp() throws Exception {
        String body = """
                {"id":1,"createdAt":"2000-01-01T00:00:00Z","title":"Server Metadata",
                 "author":"Alice Writer","year":2025,"genre":"SCIENCE"}
                """;
        MvcResult created = create(body);
        assertThat(created.getResponse().getHeader("Location")).isEqualTo("/api/books/4");
        String json = created.getResponse().getContentAsString();
        assertThat(JsonPath.<Integer>read(json, "$.id")).isEqualTo(4);
        assertThat(JsonPath.<String>read(json, "$.createdAt")).isNotEqualTo("2000-01-01T00:00:00Z");
        assertThat(service.requireById(1).title()).isEqualTo("Effective Java");
    }

    @Test
    void putReplacesEditableFieldsButPreservesServerMetadata() throws Exception {
        MvcResult before = mvc.perform(get("/api/books/2")).andReturn();
        String createdAt = JsonPath.read(before.getResponse().getContentAsString(), "$.createdAt");
        mvc.perform(put("/api/books/2").contentType(MediaType.APPLICATION_JSON)
                        .content(request("Clean Code Revised", "Robert Martin", 2025)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.createdAt").value(createdAt))
                .andExpect(jsonPath("$.year").value(2025));
        mvc.perform(put("/api/books/2").contentType(MediaType.APPLICATION_JSON)
                        .content(request("Clean Code Revised", "Robert Martin", 2025)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.createdAt").value(createdAt));
        assertThat(service.findAll(null)).hasSize(3);
        MvcResult missing = mvc.perform(put("/api/books/999").contentType(MediaType.APPLICATION_JSON)
                        .content(request("Missing Replacement", "Alice Writer", 2025)))
                .andExpect(status().isNotFound()).andReturn();
        assertProblem(missing, 404);
        assertThat(service.findById(999)).isEmpty();
    }

    @Test
    void deletionReturnsAnEmpty204ThenADeliberate404AndNeverResurrects() throws Exception {
        mvc.perform(delete("/api/books/2")).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        MvcResult repeated = mvc.perform(delete("/api/books/2"))
                .andExpect(status().isNotFound()).andReturn();
        assertProblem(repeated, 404);
        mvc.perform(get("/api/books/2")).andExpect(status().isNotFound());
        mvc.perform(put("/api/books/2").contentType(MediaType.APPLICATION_JSON)
                        .content(request("Replacement", "Alice Writer", 2025)))
                .andExpect(status().isNotFound());
        assertThat(service.findAll(null)).hasSize(2);
    }

    @Test
    void duplicateCreationAndConflictingUpdateReturn409WithoutChangingStoredData() throws Exception {
        MvcResult duplicate = mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .content(request("  EFFECTIVE JAVA  ", "  joshua bloch  ", 2025)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.title").value("Duplicate title"))
                .andExpect(jsonPath("$.conflictingTitle").exists()).andReturn();
        assertProblem(duplicate, 409);
        MvcResult conflict = mvc.perform(put("/api/books/2").contentType(MediaType.APPLICATION_JSON)
                        .content(request("Effective Java", "Joshua Bloch", 2024)))
                .andExpect(status().isConflict()).andReturn();
        assertProblem(conflict, 409);
        assertThat(service.requireById(2).title()).isEqualTo("Clean Code");
        assertThat(service.findAll(null)).hasSize(3);
        create(request("Effective Java", "Another Writer", 2024));
        assertThat(service.findAll(null)).hasSize(4);
    }

    @Test
    void beanValidationProvidesRejectedFieldNamesThroughTheBaseProblemPipeline() throws Exception {
        MvcResult rejected = mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"author\":\"Bloch 2018\",\"year\":1200,\"genre\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("title")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("author")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("year")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("genre"))).andReturn();
        assertProblem(rejected, 400);
        assertThat(service.findAll(null)).hasSize(3);
        mvc.perform(put("/api/books/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Incomplete\",\"author\":\"Author\",\"year\":2025}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("genre")));
    }

    @Test
    void titleLengthAndYearBoundariesAreValidatedBeforeStorage() throws Exception {
        create(request("T".repeat(120), "Alice Writer", 1450));
        create(request("Upper Year", "Alice Writer", 2100));
        for (String body : new String[]{request("T".repeat(121), "Alice Writer", 2025),
                request("Too Early", "Alice Writer", 1449), request("Too Late", "Alice Writer", 2101)}) {
            assertProblem(mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andExpect(status().isBadRequest()).andReturn(), 400);
        }
        assertThat(service.findAll(null)).hasSize(5);
    }

    @Test
    void malformedJsonUnknownGenreAndWrongTypesReturnSafe400ProblemDetails() throws Exception {
        for (String body : new String[]{"{", "{\"title\":\"Wrong Genre\",\"author\":\"Author\",\"year\":2025,\"genre\":\"UNKNOWN\"}",
                "{\"title\":\"Wrong Year\",\"author\":\"Author\",\"year\":\"abc\",\"genre\":\"SCIENCE\"}"}) {
            MvcResult invalid = mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andReturn();
            assertProblem(invalid, 400);
        }
        assertProblem(mvc.perform(get("/api/books/not-a-number"))
                .andExpect(status().isBadRequest()).andReturn(), 400);
        assertThat(service.findAll(null)).hasSize(3);
    }

    @Test
    void missingBookHasAllFiveProblemFieldsAnd404ContentType() throws Exception {
        MvcResult missing = mvc.perform(get("/api/books/999"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.instance").value("/api/books/999"))
                .andReturn();
        assertProblem(missing, 404);
    }

    @Test
    void explicitPagingReturnsAccurateMetadataAndStableNonoverlappingPages() throws Exception {
        mvc.perform(get("/api/books").param("page", "0").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1)).andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/books").param("page", "1").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(3)).andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void pagingDefaultsAndAuthorFilteringApplyBeforeCountsAndOffsets() throws Exception {
        service.create(new Book(null, "Another Bloch Book", "Joshua Bloch", 2025));
        mvc.perform(get("/api/books").param("size", "1").param("author", "bLoCh"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1));
        mvc.perform(get("/api/books").param("page", "1").param("size", "1").param("author", "bLoCh"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(4));
        mvc.perform(get("/api/books").param("page", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.content.length()").value(4));
    }

    @Test
    void outOfRangeOrEmptyFilteredPagesReturnEmptyContentWithCorrectTotals() throws Exception {
        mvc.perform(get("/api/books").param("page", "99").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(99)).andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/books").param("page", "2147483647").param("size", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/books").param("page", "0").param("author", "Nobody"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0)).andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void invalidPagingParametersReturn400ProblemDetailsRatherThan500() throws Exception {
        for (String page : new String[]{"-1", "abc", "2147483648"}) {
            assertProblem(mvc.perform(get("/api/books").param("page", page))
                    .andExpect(status().isBadRequest()).andReturn(), 400);
        }
        for (String size : new String[]{"0", "-1", "101", "abc"}) {
            assertProblem(mvc.perform(get("/api/books").param("size", size))
                    .andExpect(status().isBadRequest()).andReturn(), 400);
        }
        mvc.perform(get("/api/books").param("size", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void retainedSearchUsesResponseDtosAndLiteralSearchRoute() throws Exception {
        mvc.perform(get("/api/books/search").param("title", "jAvA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Effective Java"))
                .andExpect(jsonPath("$[0].genre").exists()).andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    void methodAndMediaTypeFailuresRetainDeliberateProtocolStatuses() throws Exception {
        // A standalone setup has no Boot fallback advice for pre-controller failures.
        // Their ProblemDetail bodies are verified with the real application below.
        mvc.perform(delete("/api/books")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/books").contentType(MediaType.TEXT_PLAIN).content("A book"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get("/api/books").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }
}
