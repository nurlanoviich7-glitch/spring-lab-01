package kz.iitu.springlab.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest
class OpenApiIntegrationTest {
    @Autowired
    private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void generatedDocumentMatchesTheFiveCrudOperationsAndDtoValidation() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.paths['/api/books'].get").exists())
                .andExpect(jsonPath("$.paths['/api/books'].post").exists())
                .andExpect(jsonPath("$.paths['/api/books/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/books/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/books/{id}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/books'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/books/{id}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/books/{id}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.components.schemas.BookRequest.properties.title.maxLength").value(120))
                .andExpect(jsonPath("$.components.schemas.BookRequest.properties.genre").exists())
                .andExpect(jsonPath("$.components.schemas.BookRequest.properties.id").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.BookRequest.properties.createdAt").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.BookRequest.required", hasItems("title", "author", "genre")))
                .andExpect(jsonPath("$.components.schemas.BookResponse.properties.createdAt").exists())
                .andExpect(jsonPath("$.paths['/api/books'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/books'].get.parameters[*].name", hasItems("page", "size", "author")));
    }

    @Test
    void swaggerEntryPointWorksAndApiAdviceDoesNotConvertPageErrorsIntoJson() throws Exception {
        var swagger = mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertThat(swagger.getResponse().getHeader("Location")).contains("swagger-ui/index.html");
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Swagger UI")));
        mvc.perform(get("/books/999"))
                .andExpect(status().isNotFound()).andExpect(view().name("error/not-found"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        mvc.perform(get("/api/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.instance").value("/api/books/999"));
    }

    @Test
    void actualMvcPipelineUsesProblemDetailsForPreControllerFailuresAndCustomValidation() throws Exception {
        mvc.perform(delete("/api/books"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").value("/api/books"))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(jsonPath("$.trace").doesNotExist());
        mvc.perform(post("/api/books").contentType(MediaType.TEXT_PLAIN).content("A book"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.instance").value("/api/books"))
                .andExpect(jsonPath("$.trace").doesNotExist());
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("The request body must be valid JSON with the expected field types"))
                .andExpect(jsonPath("$.trace").doesNotExist());
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"author\":\"Author\",\"year\":2025,\"genre\":\"SCIENCE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("title")));
        mvc.perform(get("/api/books").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }
}
