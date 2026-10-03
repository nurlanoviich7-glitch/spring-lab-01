package kz.iitu.springlab.web;

import kz.iitu.springlab.catalog.Book;
import kz.iitu.springlab.catalog.BookService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/books")
public class BookPageController {
    private final BookService service;
    private final String owner;

    public BookPageController(BookService service, @Value("${app.owner:}") String owner) {
        this.service = service;
        this.owner = owner;
    }

    @GetMapping
    public String list(@RequestParam(name = "author", required = false) String author,
                       Model model) {
        model.addAttribute("books", service.findAll(author));
        model.addAttribute("author", author);
        model.addAttribute("title", "Catalogue");
        model.addAttribute("owner", owner);
        return "books/list";
    }

    @PostMapping(consumes = "application/x-www-form-urlencoded")
    public String create(@RequestParam(name = "title") String title,
                         @RequestParam(name = "author") String author,
                         @RequestParam(name = "year") int year,
                         RedirectAttributes redirect) {
        Book saved;
        try {
            saved = service.create(new Book(null, title, author, year));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
        redirect.addFlashAttribute("message", "Book \"" + saved.title() + "\" has been added");
        return "redirect:/books";
    }
}
