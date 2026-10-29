package kz.iitu.springlab.web;
import kz.iitu.springlab.catalog.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class Lab8PageController {
    private final JdbcTemplate jdbc;
    public Lab8PageController(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @GetMapping({"/lab8","/lab8/database"})
    public String overview(Model model){
        model.addAttribute("server",jdbc.queryForMap("select current_database() as database, current_user as username,version() as version"));
        model.addAttribute("books",jdbc.queryForList("""
            select b.id,b.title,b.publication_year,b.genre,b.archived,b.author_id,a.name as author,
                   b.publisher_id,p.name as publisher
            from book b join author a on a.id=b.author_id join publisher p on p.id=b.publisher_id order by b.id
            """));
        model.addAttribute("counts",jdbc.queryForMap("select (select count(*) from book) as books,(select count(*) from author) as authors,(select count(*) from tag) as tags,(select count(*) from publisher) as publishers"));
        return "lab8/overview";
    }
}
