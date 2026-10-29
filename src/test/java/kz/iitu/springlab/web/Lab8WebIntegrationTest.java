package kz.iitu.springlab.web;
import com.jayway.jsonpath.JsonPath;
import kz.iitu.springlab.catalog.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @ActiveProfiles("test") @Transactional
class Lab8WebIntegrationTest {
    @Autowired WebApplicationContext context;@Autowired BookRepository books;
    MockMvc mvc;
    @BeforeEach void setup(){mvc=MockMvcBuilders.webAppContextSetup(context).build();}
    String body(String title){return "{\"title\":\""+title+"\",\"author\":\"Example Writer\",\"year\":2025,\"genre\":\"SCIENCE\"}";}
    @Test void collectionAlwaysReturnsADatabasePageWithDefaults()throws Exception{
        mvc.perform(get("/api/books")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(8))
            .andExpect(jsonPath("$.size").value(5)).andExpect(jsonPath("$.content.length()").value(5))
            .andExpect(jsonPath("$.content[0].title").value("API Design Notes"));
    }
    @Test void secondPageSortAndGenreFilterWork()throws Exception{
        mvc.perform(get("/api/books").param("page","1").param("size","3").param("sort","year,desc").param("genre","PROGRAMMING"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.number").value(1)).andExpect(jsonPath("$.totalElements").value(8))
            .andExpect(jsonPath("$.content[*].year",contains(2016,2016,2008)));
    }
    @Test void authorAndPublisherFiltersExecuteBeforePaging()throws Exception{
        mvc.perform(get("/api/books").param("author","bLoCh").param("size","2")).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.content.length()").value(2));
        mvc.perform(get("/api/books").param("publisher","mAnNiNg")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
    }
    @Test void invalidSortReturnsSafe400Problem()throws Exception{
        mvc.perform(get("/api/books").param("sort","nosuchfield")).andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("Unsupported sort property: nosuchfield")).andExpect(jsonPath("$.trace").doesNotExist());
    }
    @Test void negativeAndMalformedPagingAndUnknownGenreReturn400()throws Exception{
        for(String[] pair:new String[][]{{"page","-1"},{"page","abc"},{"size","0"},{"size","101"},{"genre","MISSING"}})
            mvc.perform(get("/api/books").param(pair[0],pair[1])).andExpect(status().isBadRequest());
    }
    @Test void creationReturnsLocationAndARealDatabaseRecord()throws Exception{
        MvcResult result=mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(body("Database creation")))
            .andExpect(status().isCreated()).andExpect(header().exists("Location")).andExpect(jsonPath("$.createdAt").isNotEmpty()).andReturn();
        long id=((Number)JsonPath.read(result.getResponse().getContentAsString(),"$.id")).longValue();
        assertThat(books.findById(id)).isPresent();mvc.perform(get(result.getResponse().getHeader("Location"))).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Database creation"));
    }
    @Test void duplicateTitleAndAuthorReturn409()throws Exception{
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title":" EFFECTIVE JAVA ","author":" joshua bloch ","year":2025,"genre":"PROGRAMMING"}
                """))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.conflictingTitle").exists());
    }
    @Test void replacementRetainsServerMetadata()throws Exception{
        Book book=books.findByTitleContainingIgnoreCase("Effective Java").getFirst();long id=book.id();String created=book.createdAt().toString();
        mvc.perform(put("/api/books/"+id).contentType(MediaType.APPLICATION_JSON).content(body("Revised API book")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.createdAt").value(created));
    }
    @Test void deletionReturns204AndRepeatedDeletionReturns404()throws Exception{
        long id=books.findByTitleContainingIgnoreCase("Effective Java").getFirst().id();
        mvc.perform(delete("/api/books/"+id)).andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(delete("/api/books/"+id)).andExpect(status().isNotFound());
    }
    @Test void beanValidationRejectsInvalidFieldsBeforeStorage()throws Exception{
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title":" ","author":"Writer 2","year":1200,"genre":null}
                """))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[*].field",hasItems("title","author","year","genre")));
        assertThat(books.count()).isEqualTo(8);
    }
    @Test void htmlCatalogueAndNewBookFormStillRender()throws Exception{
        mvc.perform(get("/books")).andExpect(status().isOk()).andExpect(content().string(containsString("Effective Java")));
        mvc.perform(get("/books/new")).andExpect(status().isOk()).andExpect(model().attributeExists("form","genres"));
    }
    @Test void formErrorsAndPostRedirectGetStillWork()throws Exception{
        mvc.perform(post("/books").contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("title","Form sample").param("author","Writer").param("year","1200").param("genre","PROGRAMMING"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","year"));
        mvc.perform(post("/books").contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("title","Form sample").param("author","Writer").param("year","2025").param("genre","PROGRAMMING"))
            .andExpect(status().isFound()).andExpect(redirectedUrl("/books"));assertThat(books.count()).isEqualTo(9);
    }
    @Test void sqlEvidenceEndpointsReturnMeasuredCounts()throws Exception{
        mvc.perform(get("/api/lab8/queries")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(6));
        mvc.perform(get("/api/lab8/n-plus-one")).andExpect(status().isOk()).andExpect(jsonPath("$.before.sqlCount").value(4)).andExpect(jsonPath("$.after.sqlCount").value(1));
        mvc.perform(get("/api/lab8/page-vs-slice")).andExpect(status().isOk()).andExpect(jsonPath("$.page.sqlCount").value(2)).andExpect(jsonPath("$.slice.sqlCount").value(1));
    }
    @Test void schemaAndPublisherVariantAreVisible()throws Exception{
        mvc.perform(get("/api/lab8/database")).andExpect(status().isOk()).andExpect(jsonPath("$.server.version",startsWith("PostgreSQL")))
            .andExpect(jsonPath("$.tables.length()").value(5)).andExpect(jsonPath("$.foreignKeys.length()").value(4));
        mvc.perform(get("/api/lab8/publishers").param("name","Manning")).andExpect(status().isOk()).andExpect(jsonPath("$.result.length()").value(3));
    }
}
