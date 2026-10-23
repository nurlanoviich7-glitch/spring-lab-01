package kz.iitu.springlab.web;

import jakarta.validation.Valid;
import kz.iitu.springlab.catalog.Book;
import kz.iitu.springlab.catalog.BookService;
import kz.iitu.springlab.catalog.DuplicateTitleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Controller
@RequestMapping("/books")
public class BookPageController {
    private final BookService service;
    private final String owner;
    private final MessageSource messages;

    public BookPageController(BookService service, @Value("${app.owner:}") String owner,
                              MessageSource messages) {
        this.service = service;
        this.owner = owner;
        this.messages = messages;
    }

    @InitBinder("form")
    public void formBinder(WebDataBinder binder) {
        binder.setAllowedFields("title", "author", "year", "genre", "available", "isbn");
    }

    @ModelAttribute("owner")
    public String owner() {
        return owner;
    }

    @GetMapping
    public String list(@RequestParam(name = "author", required = false) String author,
                       Model model, Locale locale) {
        model.addAttribute("books", service.findAll(author));
        model.addAttribute("author", author);
        model.addAttribute("title", messages.getMessage("catalogue.title", null, locale));
        return "books/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new BookForm());
        model.addAttribute("genres", Genre.values());
        return "books/form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable(name = "id") long id, Model model, Locale locale) {
        model.addAttribute("book", service.requireById(id));
        model.addAttribute("title", messages.getMessage("book.details", null, locale));
        return "books/detail";
    }

    @PostMapping(consumes = "application/x-www-form-urlencoded")
    public String create(@Valid @ModelAttribute("form") BookForm form,
                         BindingResult binding,
                         Model model, RedirectAttributes redirect, Locale locale) {
        if (binding.hasErrors()) {
            model.addAttribute("genres", Genre.values());
            return "books/form";
        }
        // Genre is now part of the Lab 7 domain. ISBN and availability remain
        // form validation examples from Lab 6.
        Book saved;
        try {
            saved = service.create(new Book(null, form.getTitle(), form.getAuthor(),
                    form.getYear(), form.getGenre(), null));
        } catch (DuplicateTitleException exception) {
            binding.rejectValue("title", "book.duplicate",
                    messages.getMessage("book.duplicate", null, locale));
            model.addAttribute("genres", Genre.values());
            return "books/form";
        }
        redirect.addFlashAttribute("message",
                messages.getMessage("book.added", new Object[]{saved.title()}, locale));
        return "redirect:/books";
    }
}
