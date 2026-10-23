package kz.iitu.springlab.web;

import kz.iitu.springlab.catalog.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.WebApplicationContext;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookFormWebIntegrationTest {
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private BookService service;
    @Autowired
    private MessageSource messages;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private MockHttpServletRequestBuilder validPost() {
        return post("/books").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("title", "Effective Java Companion").param("author", "Joshua Bloch")
                .param("year", "2018").param("genre", "PROGRAMMING")
                .param("available", "true").param("isbn", "9780134685991");
    }

    private BindingResult binding(MvcResult result) {
        return (BindingResult) result.getModelAndView().getModel()
                .get(BindingResult.MODEL_KEY_PREFIX + "form");
    }

    @Test
    void getCreatesTheMutableFormAndAllFiveGenres() throws Exception {
        var result = mvc.perform(get("/books/new"))
                .andExpect(status().isOk()).andExpect(view().name("books/form"))
                .andExpect(model().attributeExists("form", "genres", "owner"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("name=\"isbn\"")))
                .andReturn();
        assertThat(result.getModelAndView().getModel().get("form")).isInstanceOf(BookForm.class);
        assertThat((Genre[]) result.getModelAndView().getModel().get("genres"))
                .containsExactly(Genre.values());
    }

    @Test
    void rejectedFormKeepsUserInputAndRefillsGenresWithoutSaving() throws Exception {
        var result = mvc.perform(post("/books").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "").param("author", "Bloch 2018")
                        .param("year", "1200").param("genre", "")
                        .param("available", "true").param("isbn", "9780134685992"))
                .andExpect(status().isOk()).andExpect(view().name("books/form"))
                .andExpect(model().attributeHasFieldErrors("form", "title", "author", "year", "genre", "isbn"))
                .andExpect(content().string(containsString("Bloch 2018")))
                .andExpect(content().string(containsString("9780134685992")))
                .andReturn();
        BookForm form = (BookForm) result.getModelAndView().getModel().get("form");
        assertThat(form.getAuthor()).isEqualTo("Bloch 2018");
        assertThat(form.getYear()).isEqualTo(1200);
        assertThat(form.isAvailable()).isTrue();
        assertThat((Genre[]) result.getModelAndView().getModel().get("genres")).hasSize(5);
        assertThat(binding(result).getFieldError("isbn").getDefaultMessage())
                .isEqualTo(messages.getMessage("book.isbn.invalid", null, Locale.ENGLISH));
        assertThat(service.findAll(null)).hasSize(3);
    }

    @Test
    void typeConversionErrorsPreserveRejectedTextInBindingResult() throws Exception {
        var result = mvc.perform(post("/books").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "A retained title").param("author", "Author")
                        .param("year", "not-a-year").param("genre", "UNKNOWN"))
                .andExpect(status().isOk()).andExpect(view().name("books/form"))
                .andExpect(model().attributeHasFieldErrors("form", "year", "genre"))
                .andReturn();
        assertThat(binding(result).getFieldError("year").getRejectedValue()).isEqualTo("not-a-year");
        assertThat(binding(result).getFieldError("genre").getRejectedValue()).isEqualTo("UNKNOWN");
        assertThat(((BookForm) result.getModelAndView().getModel().get("form")).getTitle())
                .isEqualTo("A retained title");
        assertThat(service.findAll(null)).hasSize(3);
    }

    @Test
    void languageSwitchPersistsInSessionAndLocalizesConstraintMessages() throws Exception {
        Locale russian = Locale.forLanguageTag("ru");
        var selected = mvc.perform(get("/books/new").param("lang", "ru"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(messages.getMessage("book.new", null, russian))))
                .andReturn();
        MockHttpSession session = (MockHttpSession) selected.getRequest().getSession(false);
        assertThat(session).isNotNull();
        var rejectedRu = mvc.perform(post("/books").session(session)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "").param("author", "Author")
                        .param("year", "2018").param("genre", "PROGRAMMING"))
                .andExpect(status().isOk()).andReturn();
        String russianMessage = messages.getMessage("book.title.required", null, russian);
        assertThat(binding(rejectedRu).getFieldError("title").getDefaultMessage()).isEqualTo(russianMessage);
        assertThat(rejectedRu.getResponse().getContentAsString()).contains(russianMessage);

        mvc.perform(get("/books/new").session(session).param("lang", "en")).andExpect(status().isOk());
        var rejectedEn = mvc.perform(post("/books").session(session)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "").param("author", "Author")
                        .param("year", "2018").param("genre", "PROGRAMMING"))
                .andExpect(status().isOk()).andReturn();
        String englishMessage = messages.getMessage("book.title.required", null, Locale.ENGLISH);
        assertThat(englishMessage).isNotEqualTo(russianMessage);
        assertThat(binding(rejectedEn).getFieldError("title").getDefaultMessage()).isEqualTo(englishMessage);
    }

    @Test
    void validSubmissionIgnoresMassAssignedIdAndUsesLocalizedFlash() throws Exception {
        Locale russian = Locale.forLanguageTag("ru");
        String expected = messages.getMessage("book.added", new Object[]{"Effective Java Companion"}, russian);
        mvc.perform(validPost().param("id", "1").param("lang", "ru"))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/books"))
                .andExpect(flash().attribute("message", expected));
        assertThat(service.findAll(null)).hasSize(4);
        assertThat(service.requireById(1).title()).isEqualTo("Effective Java");
        assertThat(service.requireById(4).title()).isEqualTo("Effective Java Companion");
    }

    @Test
    void duplicateFormSubmissionRedisplaysLocalizedTitleErrorWithoutCreatingABook() throws Exception {
        for (String language : new String[]{"en", "ru"}) {
            Locale locale = Locale.forLanguageTag(language);
            String expected = messages.getMessage("book.duplicate", null, locale);
            var result = mvc.perform(post("/books")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("lang", language)
                            .param("title", "  EFFECTIVE JAVA  ")
                            .param("author", "  joshua bloch  ")
                            .param("year", "2018").param("genre", "PROGRAMMING"))
                    .andExpect(status().isOk()).andExpect(view().name("books/form"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(model().attributeHasFieldErrors("form", "title"))
                    .andExpect(model().attributeExists("genres"))
                    .andExpect(content().string(containsString(expected)))
                    .andReturn();
            assertThat(binding(result).getFieldError("title").getDefaultMessage()).isEqualTo(expected);
            assertThat(((BookForm) result.getModelAndView().getModel().get("form")).getTitle())
                    .isEqualTo("  EFFECTIVE JAVA  ");
            assertThat((Genre[]) result.getModelAndView().getModel().get("genres"))
                    .containsExactly(Genre.values());
            assertThat(service.findAll(null)).hasSize(3);
            assertThat(service.requireById(1).title()).isEqualTo("Effective Java");
        }
    }

    @Test
    void sameMissingBookBecomesHtmlForPagesAndProblemJsonForApi() throws Exception {
        mvc.perform(get("/books/1"))
                .andExpect(status().isOk()).andExpect(view().name("books/detail"))
                .andExpect(model().attributeExists("book"));
        mvc.perform(get("/books/999"))
                .andExpect(status().isNotFound()).andExpect(view().name("error/not-found"))
                .andExpect(model().attribute("bookId", 999L))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString(messages.getMessage("book.notFound.title", null, Locale.ENGLISH))));
        mvc.perform(get("/api/books/999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value(messages.getMessage("book.notFound.title", null, Locale.ENGLISH)))
                .andExpect(jsonPath("$.instance").value("/api/books/999"));
        mvc.perform(get("/api/books/999").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
        mvc.perform(get("/api/books/999").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotAcceptable());
    }
}
