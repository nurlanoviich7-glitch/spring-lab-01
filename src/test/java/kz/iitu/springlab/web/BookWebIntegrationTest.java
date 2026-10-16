package kz.iitu.springlab.web;

import kz.iitu.springlab.catalog.Book;
import kz.iitu.springlab.catalog.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookWebIntegrationTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private BookService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void mvcResolvesRealTemplateAndServesStaticStylesheetAndWelcomePage() throws Exception {
        mvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/list"))
                .andExpect(model().attribute("title", "Catalogue"))
                .andExpect(model().attributeExists("books", "owner"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Effective Java")))
                .andExpect(content().string(containsString("/css/app.css")));

        var filtered = mvc.perform(get("/books").param("author", "bLoCh"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("author", "bLoCh"))
                .andExpect(content().string(containsString("Effective Java")))
                .andExpect(content().string(not(containsString("Clean Code"))))
                .andReturn();
        @SuppressWarnings("unchecked")
        List<Book> books = (List<Book>) filtered.getModelAndView().getModel().get("books");
        assertThat(books).extracting(Book::author).containsExactly("Joshua Bloch");

        mvc.perform(get("/css/app.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"))
                .andExpect(content().string(containsString("body")));
        mvc.perform(get("/"))
                .andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("/books")));
    }

    @Test
    void postRedirectGetDisplaysFlashOnceAndRefreshDoesNotCreateAnotherRecord() throws Exception {
        int initialCount = service.findAll(null).size();
        var posted = mvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "Java Precisely").param("author", "Sestoft").param("year", "2016")
                        .param("genre", "PROGRAMMING"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/books"))
                .andExpect(flash().attribute("message", "Book \"Java Precisely\" has been added"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) posted.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/books").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("message", "Book \"Java Precisely\" has been added"))
                .andExpect(content().string(containsString("has been added")));
        mvc.perform(get("/books").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("message"))
                .andExpect(content().string(not(containsString("has been added"))));
        assertThat(service.findAll(null)).hasSize(initialCount + 1);
        assertThat(service.searchByTitle("Java Precisely")).hasSize(1);
    }

    @Test
    void actualApplicationRoutingProducesAllFiveAssignmentErrors() throws Exception {
        mvc.perform(get("/api/books/abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/nothing")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/books")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/books").contentType(MediaType.TEXT_PLAIN).content("A book"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get("/api/books").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
        mvc.perform(get("/api/books/search").param("title", "sPrInG"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Spring in Action"));
    }

    @Test
    void invalidFormDataRedisplaysErrorsWithoutCreatingABook() throws Exception {
        mvc.perform(post("/books").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "Title").param("author", "Author").param("year", "abc")
                        .param("genre", "PROGRAMMING"))
                .andExpect(status().isOk()).andExpect(view().name("books/form"))
                .andExpect(model().attributeHasFieldErrors("form", "year"));
        mvc.perform(post("/books").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", " ").param("author", "Author").param("year", "2024")
                        .param("genre", "PROGRAMMING"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "title"));
        mvc.perform(post("/books").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "Title").param("author", "Author").param("year", "10000")
                        .param("genre", "PROGRAMMING"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "year"));
        assertThat(service.findAll(null)).hasSize(3);
    }
}
